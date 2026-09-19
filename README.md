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

## Déplacements automatiques (km)

1. Renseigne **Adresse** de la société (onglet Société) + tarif **€ HT / km**.
2. Sur chaque client : **Adresse chantier** (ou facturation).
3. Bouton **Auto** sur la fiche client → calcule et mémorise la distance aller (géocodage Android).
4. Sur un devis / facture : **Dépl. A/R auto** ou **Aller simple** → ajoute une ligne détaillée :
   - `Déplacement A/R — XX km × tarif € HT/km`
   - Trajet société → chantier
5. Onglet Déplacements : en choisissant un client, les km se préremplissent.

Estimation = distance à vol d'oiseau × **1,35** (facteur route). Affinable à la main sur la fiche client.

## Devis / factures détaillés

- Affichage **document complet** à l’ouverture (bouton Éditer pour les lignes)
- **TVA auto** : particulier → taux réduit (réglages), pro → taux normal
- **Ligne auto kWc** : puissance installation × prix HT/kWc (Société)
- **Remises** : commerciale % sur HT, et/ou % sur la TVA
- **PDF** : partager / enregistrer / envoyer
- **E-mail** et **Appeler** le client depuis la fiche document

## Mises à jour APK (sans désinstaller)

Les builds CI sont signés avec un **keystore stable** (`app/cortot-debug.keystore`).
Tu peux installer chaque nouvelle APK **par-dessus** l’ancienne sans perdre les données.

- `versionCode` doit augmenter à chaque release (actuel : **2** / `1.1.0`)
- **Une seule fois** : si l’APK déjà installée venait d’un ancien build (autre signature), désinstalle-la une dernière fois, puis réinstalle. Ensuite plus besoin.

## Installation / mises à jour

L’APK est signé avec un **keystore fixe** (`app/cortot-debug.keystore`).  
Le `versionCode` augmente à **chaque build** GitHub Actions.

- À partir de cette version : tu peux **installer par-dessus** sans désinstaller (données conservées).
- **Une seule fois** : si une ancienne APK était signée autrement (Android Studio debug local), Android demandera de désinstaller — c’est normal, une seule fois.
