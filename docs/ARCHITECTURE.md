# Architecture

## Principe directeur : 100 % hors ligne

- **Aucune permission `INTERNET`** dans le manifeste : l'application ne peut pas dépendre du réseau, même par erreur.
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
    │   ├── Models.kt                # Category, Guide, Priority
    │   ├── ContentRepository.kt     # chargement, filtres, recherche
    │   └── AssetContentSource.kt    # lecture des assets Android
    └── ui/
        ├── Navigation.kt            # routes : home, search, category/{id}, guide/{id}
        ├── theme/Theme.kt
        ├── components/              # tuiles, badges, barre de retour
        ├── home/                    # accueil : bandeau 112, accès urgence, grille des thèmes
        ├── category/                # liste des fiches d'un thème
        ├── guide/                   # détail : alertes, matériel, étapes, astuces, sources
        └── search/                  # recherche plein texte
app/src/test/                        # tests JVM qui valident le contenu
```

## Feuille de route

1. **v0.1 (actuelle)** : structure, 11 thématiques, ~40 fiches, recherche, accès rapide urgence.
2. **v0.2** : favoris et « fiches lues » (DataStore), mode lecture grand texte, mode nuit rouge (préserve la vision nocturne).
3. **v0.3** : outils hors ligne – lampe torche/stroboscope SOS, signal morse sonore et lumineux, boussole (capteur magnétique), minuteur de massage cardiaque (métronome 110/min).
4. **v0.4** : illustrations (vectoriels dans l'APK), checklists interactives du kit d'urgence.
5. **v0.5** : traduction (arabe, anglais) via des fichiers `content-<langue>/`.
