# Publication sur Google Play · version 1.0.0

Dossier complet pour publier **Survivor: End of the World** (`com.aymen.survivor`) sur Google Play : textes, images,
politique de confidentialité, réponses aux déclarations de la Play Console et marche à suivre. Même modèle que
Hello English! ; le fichier pour Google Play (`.aab`) est construit sur le VPS avec `scripts/build-vps.sh`.

| Dossier / fichier | Contenu |
|---|---|
| `textes/fr.md`, `textes/en.md` | Nom, description courte, description complète, notes de version (limites vérifiées par `scripts/check_texts.py`) |
| `images/icon-512.png` | Icône de la fiche (512 × 512) |
| `images/feature-graphic-fr.png` (`-en`) | Bannière de la fiche (1024 × 500) |
| `images/screenshots-fr/` (`-en`) | 8 captures de téléphone légendées (1080 × 1920) par langue |
| `../docs/confidentialite.html` | Politique de confidentialité en ligne (français et anglais), même texte que dans l'application |
| `../docs/index.html` | Petite page de présentation (champ « Site web », facultatif) |
| `scripts/` | Captures, composition des images, politique de confidentialité, contrôle des textes |

Ordre des captures : accueil, carte hors ligne avec position GPS (Tunis), fiche « massage cardiaque », métronome de
massage cardiaque, Morse, « Mes cartes », kit d'urgence à cocher, mode nuit rouge.

## Récapitulatif (à copier-coller)

| Élément | Valeur |
|---|---|
| Nom du paquet | `com.aymen.survivor` (définitif après le premier envoi) |
| Version / code de version | `1.0.0` / `6` (`app/build.gradle.kts` › `versionName`, `versionCode`) |
| Android ciblé | Android 16 (API 36), exigé pour les nouvelles applications depuis le 31 août 2026 ; minimum Android 7.0 |
| Nom sur Google Play | `Survivor: End of the World` (fr et en) |
| Application ou jeu | **Application** |
| Catégorie | **Livres et références** |
| E-mail de contact | `aymenkssi@gmail.com` (affiché publiquement sur la fiche) |
| Site web | adresse de `docs/index.html` (facultatif, voir étape 1) |
| Politique de confidentialité | adresse de `docs/confidentialite.html` (voir étape 1) |
| Autorisations | Internet (cartes), localisation (facultative, premier plan uniquement), micro (facultatif) |
| Annonces | Non |
| Achats dans l'application | Non (lien de don PayPal ouvert dans le navigateur) |
| Public cible | **13 ans et plus** |
| Prix | Gratuit |

## Check-list avant l'envoi

- [ ] La politique de confidentialité s'ouvre en ligne (étape 1).
- [ ] L'APK de release a été essayé sur un vrai téléphone : fiches, lampe, Morse (y compris **Écouter**), boussole,
      métronome, **téléchargement d'une carte, puis ouverture en mode avion avec la position GPS**.
- [ ] Le fichier `.aab` est construit sur le VPS (étape 2) et la clé `~/.survive/` est **sauvegardée**.
- [ ] Les 14 fiches de premiers secours (français et anglais) ont été relues par un secouriste ou un soignant.
- [ ] Compte développeur Google Play actif (frais d'inscription uniques de 25 $, vérification d'identité faite).
- [ ] Le nom de la fiche a été vérifié (voir « Points d'attention » en bas de page).

---

## 1. Mettre en ligne la politique de confidentialité

Google l'exige ici, car l'application demande la localisation et le micro. Le texte est déjà dans l'application
(**Réglages › Politique de confidentialité**, lisible hors ligne) et dans `docs/confidentialite.html`.

Le dépôt `survive` est **privé** : GitHub Pages gratuit n'est pas possible tel quel. Deux solutions :

**A. Sur le VPS (recommandé)** : publier le dossier `docs/` sur un sous-domaine, par exemple
`survivor.creationapp.academy` :

```bash
sudo mkdir -p /var/www/survivor && sudo cp ~/survive/docs/*.html /var/www/survivor/
```

puis un bloc serveur nginx avec `root /var/www/survivor;` et un certificat HTTPS (`sudo certbot --nginx -d
survivor.creationapp.academy`). Adresse à donner à Google : `https://survivor.creationapp.academy/confidentialite.html`.
À refaire (`cp`) après chaque modification de la politique.

**B. Avec GitHub Pages** : rendre le dépôt public, puis **Settings › Pages › Deploy from a branch**, dossier
**`/docs`**. Adresse : `https://aymenkssi.github.io/survive/confidentialite.html`.

Pour modifier la politique : changer `app/src/main/assets/privacy/privacy.json`, puis
`python3 play-store/scripts/build_privacy.py` (un test vérifie que la page en ligne et celle de l'application ont le
même texte, et que chaque autorisation y est expliquée).

## 2. Construire le fichier pour Google Play (.aab) sur le VPS

```bash
cd survive
git pull
./scripts/build-vps.sh setup   # une seule fois, ou à refaire après le passage à Android 16 (API 36)
./scripts/build-vps.sh aab
```

- Les fichiers arrivent dans `~/builds/survive/` : `survivor-end-of-the-world-1.0.0-6.aab` et
  `survivor-end-of-the-world-1.0.0-6-mapping.txt`. Pour les récupérer :
  `scp 'utilisateur@vps:builds/survive/survivor-end-of-the-world-1.0.0-6*' .`
- Le code est réduit par **R8**. Le fichier `-mapping.txt` est le **fichier de désobscurcissement** : après l'import
  du `.aab`, **Explorateur d'app bundles** › la version › onglet **Téléchargements** › **Fichier de désobscurcissement**
  › **Importer**. Sans lui, les rapports de plantage sont illisibles.
- La clé créée au premier build (`~/.survive/`) devient la **clé d'importation** : au premier envoi, laisser Google
  Play gérer la clé de signature finale (**Signature d'application Play**, proposé par défaut). Sauvegarder
  `~/.survive/` ailleurs que sur le VPS.
- Google Play refuse un code de version déjà envoyé (même en test interne) : **avant chaque nouveau build, augmenter
  `versionCode` de 1** dans `app/build.gradle.kts`, et `versionName` (1.0.1, 1.1.0…) pour chaque version publiée.

## 3. Créer l'application dans la Play Console

https://play.google.com/console › **Créer une application** :

| Champ | Valeur |
|---|---|
| Nom de l'application | `Survivor: End of the World` |
| Langue par défaut | Français – fr-FR |
| Application ou jeu | Application |
| Sans frais ou payante | Sans frais (définitif : une application gratuite ne peut pas devenir payante) |
| Déclarations | Cocher les règles du programme pour les développeurs et les lois d'exportation des États-Unis |

## 4. Fiche du Store

**Développer la présence sur le Store** › **Fiche principale du Store**.

1. Pour `fr-FR`, puis `en-US` (**Gérer les traductions** › **Ajouter vos propres traductions**) : **Nom**,
   **description courte**, **description complète** depuis `textes/<langue>.md` ; **bannière**
   `images/feature-graphic-<langue>.png` ; **captures de téléphone** : les 8 fichiers de `images/screenshots-<langue>/`,
   dans l'ordre.
2. **Icône** : `images/icon-512.png`.
3. **Paramètres du Store** : catégorie **Livres et références** ; tags proposés : « Référence », « Sécurité »,
   « Outils », « Plein air », « Navigation » (choisir parmi ceux proposés par la Console).
4. **Coordonnées** : e-mail `aymenkssi@gmail.com` ; site web : l'adresse de `docs/index.html` (facultatif).

Les captures pour tablettes sont facultatives.

## 5. Contenu de l'application (déclarations)

**Règles et programmes** › **Contenu de l'application** :

| Rubrique | Réponse |
|---|---|
| Règles de confidentialité | L'adresse de l'étape 1 |
| Accès à l'application | **Toutes les fonctionnalités sont disponibles sans restriction** (pas de compte) |
| Annonces | **Non, mon application ne contient pas d'annonces** |
| Classification du contenu | Questionnaire › catégorie **Référence, actualités ou éducation** ; **Non** à toutes les questions (violence, sang, peur, sexualité, langage, substances, jeux d'argent, interactions entre utilisateurs, partage de la position **avec d'autres utilisateurs**, achats numériques, navigateur web). Les fiches parlent de blessures, mais en texte, sans image : ce n'est pas de la violence au sens du questionnaire. Résultat attendu : tous publics (PEGI 3) |
| Public cible et contenu | **13-15 ans, 16-17 ans, 18 ans et plus**. « L'application pourrait-elle attirer involontairement les enfants ? » : **Non** |
| Applications d'actualités | Non |
| Applications de santé | Déclarer les fonctionnalités de santé : cocher la catégorie d'**informations médicales de référence / éducation** (premiers secours) ; aucune autre (pas de suivi de santé, pas de Health Connect, pas de dispositif médical) |
| Fonctionnalités financières | **Mon application ne propose aucune fonctionnalité financière** |
| Applications gouvernementales | Non |
| Sécurité des données | Voir ci-dessous |

### Sécurité des données

Google appelle « collecte » le fait d'envoyer des données **hors de l'appareil**. Or ici :

- la position et le son du micro sont traités **uniquement sur le téléphone**, jamais enregistrés ni envoyés ;
- favoris, réglages, cases cochées et cartes restent sur le téléphone ;
- l'application ne contient ni statistiques, ni publicité, ni outil de rapport de plantage ;
- Internet sert seulement à télécharger, à la demande, des fichiers de cartes publics (aucune donnée de l'utilisateur
  n'est envoyée ; le serveur voit l'adresse IP, comme pour tout téléchargement, ce que la politique précise).

Réponses : **Votre application collecte-t-elle ou partage-t-elle des données utilisateur ?** **Non.** La Console
affiche alors « Aucune donnée collectée » sur la fiche.

### Autorisations

- **Localisation** : premier plan uniquement (pas de `ACCESS_BACKGROUND_LOCATION`) : aucun formulaire spécial.
- **Micro** : aucun formulaire spécial ; demandé seulement dans Morse › Écouter, avec une explication.
- Pas de service de premier plan, pas d'alarme exacte, pas d'accès à tous les fichiers (l'import d'une carte passe par
  le sélecteur de fichiers d'Android) : rien d'autre à déclarer.

## 6. Tester avant la production

1. **Tester et publier** › **Test interne** › **Créer une version** : importer le `.aab`, nom `1.0.0 (6)`, puis son
   fichier de désobscurcissement (étape 2), notes de version (bloc ci-dessous), ajouter votre Gmail dans les testeurs et
   installer l'application depuis le lien de test. Essayer chaque outil et **une carte en mode avion**.
2. **Comptes personnels récents** : l'accès à la production demande d'abord un **test fermé** avec **au moins
   12 testeurs** inscrits pendant **14 jours d'affilée**, puis **Demander l'accès à la production** (questionnaire sur
   le test). Vérifier le nombre demandé dans la Console : Google le modifie parfois.

```
<fr-FR>
Première version : 73 fiches de survie en français et en anglais, cartes hors ligne avec position GPS, lampe et SOS, Morse (émettre, écouter, traduire), boussole et métronome de massage cardiaque.
</fr-FR>
<en-US>
First release: 73 survival guides in English and French, offline maps with GPS position, flashlight and SOS, Morse (send, listen, translate), compass and CPR metronome.
</en-US>
```

## 7. Publier en production

**Production** › **Créer une version** (ou **Promouvoir la version** depuis le test fermé) › **Pays et régions** :
tous les pays (l'application est en français et en anglais, les numéros d'urgence 112 / 911 sont indiqués) ›
**Envoyer pour examen**. Premier examen : de quelques jours à une ou deux semaines.

## 8. Après la publication

- Répondre aux avis depuis la Play Console (**Notes et avis**).
- Pour chaque mise à jour : augmenter `versionCode`, `./gradlew test`, `./scripts/build-vps.sh aab`, nouvelle version
  en test interne puis en production, nouvelles notes de version, nouveau fichier de désobscurcissement.

## Refaire les images

Après un changement visible de l'application, depuis la racine du dépôt (JDK 17+, SDK Android, Python 3 avec Pillow,
police DejaVu : `sudo apt-get install -y python3-pil fonts-dejavu-core`) :

```bash
curl -o /tmp/africa__tunisia.map https://download.mapsforge.org/maps/v5/africa/tunisia.map   # carte de la capture n° 2
STORE_RAW_DIR=$PWD/play-store/build/raw STORE_MAP=/tmp/africa__tunisia.map \
  ./gradlew recordRoborazziDebug --tests '*StoreImagesTest*'      # captures brutes, en français et en anglais
python3 play-store/scripts/compose_images.py                     # icône, bannières et captures légendées
python3 play-store/scripts/check_texts.py                        # après un changement des textes
```

Les captures sont faites par l'application elle-même (Robolectric), avec une position GPS simulée au centre de Tunis.
La carte est dessinée par le vrai moteur Mapsforge à partir du fichier OpenStreetMap.

## Points d'attention

- **Erreur « Your APK or Android App Bundle needs to have the package name com.aymen.survivor » ou « content provider
  authorities are in use by other developers: com.survive.app.androidx-startup »** : le fichier envoyé a été construit
  avec l'**ancien code** (nom de paquet `com.survive.app`, déjà pris par un autre développeur). Sur le VPS :
  `git pull`, `./scripts/build-vps.sh setup` (SDK Android 16), puis `./scripts/build-vps.sh aab`, et envoyer le fichier
  `survivor-end-of-the-world-1.0.0-6.aab` (le nom contient la version : un fichier `survive-…` ou `…-0.x…` est ancien).
  Le script refuse désormais de produire un fichier dont le nom de paquet n'est pas `com.aymen.survivor`, et s'arrête
  si le VPS n'a pas la dernière version du code.

- **Nom** : « Survivor » est aussi le nom d'une émission de télévision connue (marque déposée). Le nom complet
  « Survivor: End of the World » et le contenu (guide de survie, sans rapport avec l'émission) limitent le risque de
  confusion, mais Google peut refuser un nom jugé trop proche d'une marque. En cas de refus : changer le nom de la fiche
  et `app_name` dans `app/src/main/res/values*/strings.xml` (le nom du paquet, lui, ne change pas), par exemple
  « End of the World: Survival Guide ».
- **Contenu médical** : la fiche et l'application précisent que l'application ne remplace pas une formation aux
  premiers secours ni les secours. Faire relire les fiches de premiers secours avant la publication.
- **Lien de don** : Google interdit de vendre des contenus numériques hors de Google Play Billing ; un don volontaire,
  sans contrepartie dans l'application, ouvert dans le navigateur, est la même pratique que dans Hello English! et Umrah.
  Ne jamais débloquer de contenu en échange d'un don.
- **Cartes** : données OpenStreetMap (licence ODbL) : la mention « © OpenStreetMap contributors » doit rester visible
  sur la carte (c'est le cas). Les fichiers viennent du serveur du projet Mapsforge ; s'il change d'adresse, modifier
  `MapCatalog.ROOT`.
- **Nom du paquet** : `com.aymen.survivor` est définitif après le premier envoi. Les APK de test installés avant
  (`com.survive.app`) apparaissent comme une autre application : les désinstaller.
