# Architecture

## Principe directeur : 100 % hors ligne

- **Aucune permission `INTERNET`** dans le manifeste : l'application ne peut pas dépendre du réseau, même par erreur.
  Le lien de don s'ouvre dans le navigateur du téléphone (intent `ACTION_VIEW`), pas dans l'application.
- Seule permission demandée : `RECORD_AUDIO`, au moment d'utiliser « Morse → Écouter ». L'audio est analysé
  trame par trame en mémoire et jamais enregistré.
- Tout le contenu est livré **dans l'APK** (`app/src/main/assets/content/`), en JSON.
- Chargement en mémoire au premier accès (quelques centaines de Ko), puis tout est instantané.
- `minSdk = 24` (Android 7.0) pour fonctionner sur un vieux téléphone de secours.

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
├── assets/content/
│   ├── categories.json              # liste des thématiques
│   └── guides/<categoryId>.json     # fiches d'une thématique
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

## Outils hors ligne

| Outil | Matériel utilisé | Repli si absent |
|---|---|---|
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
4. **v0.4** : illustrations vectorielles (nœuds, PLS, massage, signaux sol-air), relecture médicale des fiches.
5. **v0.5** : traduction (arabe, anglais) via des fichiers `content-<langue>/` ; widget d'accueil « SOS ».
6. **v0.6** : publication (signature release, F-Droid / Play Store).
