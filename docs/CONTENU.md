# Collecte et rédaction du contenu

## Format d'une fiche

Fichier `app/src/main/assets/content/guides/<categoryId>.json` (tableau de fiches) :

```json
{
  "id": "ebullition",                 // unique, minuscules, tirets
  "categoryId": "eau",                // doit exister dans categories.json
  "title": "Rendre l'eau potable par ébullition",
  "summary": "Une phrase : quand et pourquoi.",
  "priority": "critique",             // critique | important | utile
  "materials": ["Casserole"],         // optionnel
  "steps": ["Étape 1", "Étape 2"],     // obligatoire, dans l'ordre
  "warnings": ["Danger à éviter"],    // optionnel, affiché en rouge
  "tips": ["Astuce"],                 // optionnel
  "tags": ["bouillir", "potable"],    // synonymes pour la recherche
  "sources": ["OMS", "CDC"],          // clés de docs/SOURCES.md, au moins une
  "checklist": false,                 // optionnel : étapes en cases à cocher mémorisées
  "tool": "cpr"                       // optionnel : torch | morse | compass | cpr
}
```

`priority: "critique"` = la fiche apparaît dans l'accès rapide « Urgence » de l'accueil. À réserver aux gestes vitaux.

## Règles de rédaction

1. **Une action par étape**, verbe à l'impératif ou à l'infinitif, phrases courtes : on lit sous stress.
2. **Chiffres précis** (durées, doses, distances) issus d'une source citée.
3. **Les dangers en `warnings`**, pas noyés dans les étapes.
4. **Reformuler** les sources, ne jamais copier un texte protégé.
5. **Premiers secours** : se conformer aux recommandations ERC / PSC1 en vigueur ; toute fiche médicale doit être relue par une personne formée (secouriste, soignant) avant publication.
6. Ajouter des **tags** avec les mots du langage courant (« s'étrangler », « gastro »…).

## État de la collecte

| Thématique | Fiches | Prochaines fiches à rédiger |
|---|---|---|
| Urgences & sécurité | 8 | Coupure de gaz, avalanche, feu de forêt, évacuation |
| Premiers secours | 14 | Réanimation de l'enfant et du nourrisson, convulsions, malaise diabétique, plaie à la tête, noyade, hypothermie de l'enfant |
| Eau | 7 | Distillation solaire, eau de piscine (usage non alimentaire) |
| Feu & chaleur | 5 | Poêle de fortune sécurisé, fabriquer des allume-feux |
| Abri | 4 | Abri de neige, isoler une fenêtre cassée |
| Nourriture | 4 | Plantes comestibles courantes (avec illustrations), pêche simple, conserves maison |
| Orientation | 5 | Lire une carte topographique (courbes de niveau), estimer une distance |
| Communiquer sans réseau | 7 | Code des signaux corporels pour hélicoptère, utilisation d'un scanner radio |
| Énergie & électricité | 5 | Groupe électrogène : branchement en sécurité |
| Hygiène & santé | 5 | Médicaments essentiels, soins dentaires, hygiène féminine |
| Préparation & kit | 5 | Kit de travail / bureau, kit bébé, kit animal |
| Nœuds & cordes | 4 | Nœud de huit, demi-clés, brêlage (assembler des perches) |
| **Thématiques possibles** | – | Météo sans prévisions, outils & réparations, personnes vulnérables |

Toutes les fiches de premiers secours (thème `secours`) doivent être **relues par un secouriste ou un soignant** avant publication.

## Ajouter une fiche

1. Ajouter l'objet JSON dans le fichier de la thématique.
2. Si la source est nouvelle, l'ajouter dans `docs/SOURCES.md`.
3. Lancer `./gradlew test` : le test vérifie identifiants, catégories, étapes et sources.
