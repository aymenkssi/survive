# Survive

Application Android de survie **entièrement hors ligne** : toutes les astuces et les gestes utiles
quand Internet, le réseau mobile ou l'électricité ne fonctionnent plus.

- 11 thématiques : urgences, premiers secours, eau, feu, abri, nourriture, orientation,
  communication sans réseau, énergie, hygiène, préparation.
- 41 fiches pratiques sourcées (OMS, ERC, Croix-Rouge, gouvernement, manuel FM 21-76…).
- Recherche insensible aux accents, accès rapide aux gestes vitaux.
- Aucune permission Internet : rien ne dépend du réseau.

> ⚠️ Ce contenu ne remplace pas une formation aux premiers secours (PSC1) ni l'avis des secours.
> En cas d'urgence vitale : **112**.

## Compiler

Prérequis : JDK 17+ et le SDK Android (API 35). Ouvrir le projet dans Android Studio, ou :

```bash
./gradlew assembleDebug   # APK : app/build/outputs/apk/debug/app-debug.apk
./gradlew test            # valide le contenu JSON et la recherche
```

## Documentation

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) – structure du code et feuille de route
- [docs/CONTENU.md](docs/CONTENU.md) – format des fiches, règles de rédaction, état de la collecte
- [docs/SOURCES.md](docs/SOURCES.md) – sources de référence et licences
