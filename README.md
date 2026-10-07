# Cortot Élite — Android

Application métier pour **Cortot Élite** : maintenance et entretien électrique de centrales photovoltaïques (pas d'installation).

## Modules

- **Clients** : particuliers / professionnels, adresse chantier, installation (kWc, onduleur, panneaux)
- **Devis & Factures** : numérotation, TVA 0/10/20 %, remises, PDF, envoi e-mail / appel
- **Déplacements** : km / forfait / heure, calcul auto société → chantier
- **Employés** : fiches (rôle, salaire base, commission %, dividendes %)
- **Fiches de paie** : calcul auto selon factures **PAYÉ / EN_ATTENTE / RETARD** du mois
- **Dividendes** : calcul pour dirigeants (% × CA payé annuel)
- **IA Gemini** : multi-clés + rotation automatique (descriptions devis, e-mails, analyse retards)

## Compiler l'APK

1. Ouvre le dossier dans **Android Studio**.
2. `Build > Build APK(s)` ou `./gradlew assembleDebug`
3. APK dans `app/build/outputs/apk/debug/`

## Configuration Gemini

Dans **Société** → renseigne une ou plusieurs clés API Gemini séparées par `|`  
Exemple : `AIzaSy...abc|AIzaSy...xyz`  
En cas de quota (429), la clé suivante est utilisée automatiquement.

## Paie

1. Crée des employés (onglet Employés)
2. Renseigne salaire de base + % commission (ou % société)
3. Sur une fiche employé → **Générer** pour un mois
4. La commission est calculée uniquement sur les factures au statut **PAYE** du mois
5. Les montants EN_ATTENTE et RETARD sont affichés pour information

## Statuts facture

`BROUILLON` → `ENVOYE` → `EN_ATTENTE` → `RETARD` (si délai dépassé) → `PAYE`

## Notes légales

Ce n'est **pas** un logiciel de paie certifié ni un outil de comptabilité.  
Vérifie les mentions légales, URSSAF et obligations fiscales avant usage réel.
