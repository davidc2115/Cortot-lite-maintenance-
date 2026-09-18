# Cortot Élite — Android

Application métier pour **Cortot Élite** : fiches clients photovoltaïques, devis, factures HT/TTC (TVA 0 / 10 / 20 %), déplacements.

Compte GitHub cible : `davidc2115`.  
Si le dépôt `cortot-elite-android` n’existe pas encore, crée-le vide sur GitHub puis pousse ce dossier.

## Fonctions

- Fiche client **particulier** ou **professionnel** (société / enseigne)
- Adresse de facturation + **adresse chantier**
- Téléphones fixe et portable
- Installation : puissance kWc, onduleur (marque / modèle / nombre), panneaux, toiture, **échelle**, accès
- Devis et factures numérotés (`D-2026-0001`, `F-2026-0001`)
- Lignes en **HT** ou saisie **TTC** (conversion automatique)
- Totaux HT / TVA / TTC
- Transformation devis → facture
- Déplacements : **au km**, **forfait**, **à l’heure** (tarifs société), option d’ajout sur le document
- Paramètres société : SIRET, n° TVA, taux, barème déplacement

## Compiler l’APK

1. Ouvre le dossier dans **Android Studio** (Ladybug / Koala ou plus récent).
2. Laisse Gradle générer le wrapper si besoin.
3. `Build > Build Bundle(s) / APK(s) > Build APK(s)`.
4. L’APK debug est dans `app/build/outputs/apk/debug/`.

En ligne de commande, une fois le wrapper présent :

```bash
./gradlew assembleDebug
```

## TVA

- Les montants sont **stockés en HT**.
- Affichage TTC calculé : `TTC = HT × (1 + taux/100)`.
- Particulier : l’écran oriente vers le TTC.
- Professionnel : l’écran oriente vers le HT.
- Taux proposés : 0 %, 10 %, 20 % (modifiables dans Société).

Ce n’est pas un logiciel de comptabilité certifié. Vérifie les mentions légales de tes factures (SIRET, n° TVA, pénalités de retard, etc.) avant usage réel.
