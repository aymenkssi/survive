# Architecture

## Principe directeur : 100 % hors ligne

- Fiches et outils n'utilisent jamais le réseau. La permission `INTERNET` ne sert qu'aux **cartes hors ligne**
  (parcourir le catalogue, télécharger un fichier), à la demande de l'utilisateur.
  Le lien de don s'ouvre dans le navigateur du téléphone (intent `ACTION_VIEW`).
- Permissions facultatives, demandées au moment de l'usage : `RECORD_AUDIO` (« Morse → Écouter », audio analysé
  en mémoire et jamais enregistré) et la localisation (position GPS sur les cartes, coordonnées).
- Tout le contenu est livré **dans l'APK** (`app/src/main/assets/content/`), en JSON.
- Chargement en mémoire au premier accès (quelques centaines de Ko), puis tout est instantané.
- `minSdk = 24` (Android 7.0) pour fonctionner sur un vieux téléphone de secours ; `targetSdk = 36` (Android 16),
  exigé par Google Play pour les nouvelles applications depuis le 31 août 2026. Nom du paquet : `com.aymen.survivor`.
- Politique de confidentialité : `assets/privacy/privacy.json`, affichée hors ligne (Réglages) et publiée en HTML
  (`docs/confidentialite.html`, générée par `play-store/scripts/build_privacy.py`, vérifiée par `PrivacyPolicyTest`).

## Langues

- Interface : ressources Android (`values/` = anglais par défaut, `values-fr/` = français).
- Contenu : `assets/content/fr/` et `assets/content/en/`, mêmes identifiants et même structure
  (vérifié par `ContentRepositoryTest.languagesHaveTheSameStructure`) : favoris et cases cochées
  restent valables quand on change de langue.
- Choix dans Réglages : Automatique (français si le téléphone est en français, sinon anglais), Français, English.
  `ui/Localization.kt` applique la langue choisie sans redémarrer l'application.
- Android 13+ : la langue est aussi réglable dans les paramètres du téléphone (`res/xml/locales_config.xml`).
- Ajouter une langue : un dossier `values-xx/`, un dossier `content/xx/`, et `xx` dans `SUPPORTED_LANGUAGES`,
  `resourceConfigurations` et `locales_config.xml`.

## Pile technique

| Couche | Choix | Pourquoi |
|---|---|---|
| Langage | Kotlin | Standard Android |
| UI | Jetpack Compose + Material 3 | Moins de code, thème clair/sombre automatique |
| Navigation | Navigation Compose | 4 écrans simples |
| Données | JSON + kotlinx.serialization | Contenu éditable sans toucher au code, versionnable dans Git |
| Recherche | Index en mémoire, insensible aux accents | Suffisant pour quelques centaines de fiches ; passer à Room + FTS si le contenu dépasse ~2 000 fiches |

## Arborescence

```
app/src/main/
├── AndroidManifest.xml              # sans permission INTERNET
├── assets/content/<lang>/           # fr et en, mêmes identifiants
│   ├── categories.json              # liste des thématiques
│   └── guides/<categoryId>.json     # fiches d'une thématique
├── res/values/strings.xml           # textes de l'interface en anglais (langue par défaut)
├── res/values-fr/strings.xml        # textes de l'interface en français
└── java/com/survive/app/
    ├── SurviveApp.kt                # Application : crée le ContentRepository
    ├── MainActivity.kt
    ├── data/
    │   ├── Models.kt                # Category, Guide, Priority, Tools
    │   ├── ContentRepository.kt     # chargement, filtres, recherche
    │   ├── AssetContentSource.kt    # lecture des assets Android
    │   └── UserPrefs.kt             # favoris, thème, grand texte, cases cochées
    ├── Links.kt                     # lien de don PayPal (ouvert dans le navigateur)
    ├── tools/                       # Morse + MorseListener (pur Kotlin, testés sur JVM), Torch, Beeper
    └── ui/
        ├── Navigation.kt            # routes : home, search, category/{id}, guide/{id}
        ├── theme/Theme.kt
        ├── components/              # tuiles, badges, barre de retour
        ├── home/                    # accueil : bandeau 112, accès urgence, grille des thèmes
        ├── category/                # liste des fiches d'un thème
        ├── guide/                   # détail : favori, outil lié, alertes, étapes ou checklist
        ├── search/                  # recherche plein texte
        ├── settings/                # thème (clair, sombre, nuit rouge), grand texte
        ├── support/                 # page « Soutenir l'application » (don PayPal)
        └── tools/                   # Lampe & SOS, Morse, Boussole, Métronome massage cardiaque
app/src/test/                        # tests JVM qui valident le contenu
```

## Cartes hors ligne

- **Source** : fichiers vectoriels Mapsforge (`https://download.mapsforge.org/maps/v5/`), données OpenStreetMap
  (© contributeurs OpenStreetMap, ODbL). Un fichier par pays, les grands pays aussi par région. Le téléchargement
  de fichiers entiers est l'usage prévu par ce serveur (contrairement au téléchargement massif de tuiles OSM, interdit).
- **Catalogue** : lu en direct sur le serveur (`MapCatalog.parseIndex`, page d'index), donc toujours complet et à jour.
- **Téléchargement** : `DownloadManager` d'Android (continue application fermée, reprend après coupure, notification),
  vers le dossier privé `Android/data/<app>/files/maps/` (aucune permission de stockage).
  Nom local : chemin du catalogue avec `__` (`europe__france__alsace.map`).
- **Import** : n'importe quel fichier `.map` Mapsforge (vérification de la signature `mapsforge binary OSM`).
- **Affichage** : bibliothèque Mapsforge 0.25 (`MapView` dans Compose via `AndroidView`), rendu local des tuiles,
  thème `MapsforgeThemes.DEFAULT`, point de position GPS (`LocationManager`, fonctionne sans réseau).
- **Tests** : `MapCatalogTest` (catalogue, tailles, coordonnées) ; `MapsScreenshotTest` rend une vraie carte si
  `SURVIVOR_TEST_MAP` pointe vers un petit fichier `.map` (ex. `europe/monaco.map`, 350 Ko).

## Outils hors ligne

| Outil | Matériel utilisé | Repli si absent |
|---|---|---|
| Cartes hors ligne | stockage, GPS | coordonnées non affichées sans GPS |
| Lampe & SOS | flash (`CameraManager.setTorchMode`, sans permission caméra) | écran plein blanc (ou rouge en mode nuit), luminosité max |
| Morse – émettre | flash + bip (`ToneGenerator`, flux alarme) + voyant écran | voyant écran |
| Morse – écouter | micro (`AudioRecord` 16 kHz) → `ToneDetector` (filtres de Goertzel 300–3000 Hz) → `SignalTracker` (seuil adaptatif au bruit) → `MorseDecoder` (vitesse estimée automatiquement) | onglet « Traduire » (saisie du code à la main) |
| Boussole | capteur de rotation, sinon accéléromètre + magnétomètre | message renvoyant à la méthode du bâton |
| Massage cardiaque | bip 110/min + pulsation visuelle, mode 30:2, rappel de relais toutes les 2 min | — |

Les écrans d'outils gardent l'écran allumé tant qu'ils sont affichés.

## Feuille de route

1. ~~v0.1 : structure, thématiques, fiches, recherche, accès rapide urgence.~~
2. ~~v0.2 : favoris, checklists, mode grand texte, mode nuit rouge, outils hors ligne, 12 thématiques / 73 fiches.~~
3. ~~v0.3 : Morse à l'écoute (micro) et traduction, page de dons.~~
4. **À venir** : illustrations vectorielles (nœuds, PLS, massage, signaux sol-air), relecture médicale des fiches.
5. ~~v0.4 : nom « Survivor: End of the World », application bilingue français / anglais.~~
6. ~~v0.5 : cartes hors ligne (catalogue mondial, import .map, position GPS).~~
7. ~~v1.0 : dossier Google Play, politique de confidentialité, Android 16.~~
8. **Ensuite** : traduction arabe ; widget d'accueil « SOS ».
