# Tunexa — Cahier des fonctionnalités

> Marketplace e-commerce multi-vendeurs pour la Tunisie, inspirée d'Amazon.
> Version 1.0 — document de référence produit (fonctionnel + technique).

---

## Sommaire

1. [Vision et périmètre](#1-vision-et-périmètre)
2. [Acteurs et rôles](#2-acteurs-et-rôles)
3. [Spécificités tunisiennes](#3-spécificités-tunisiennes)
4. [Espace client (acheteur)](#4-espace-client-acheteur)
5. [Catalogue et recherche](#5-catalogue-et-recherche)
6. [Fiche produit](#6-fiche-produit)
7. [Panier et checkout](#7-panier-et-checkout)
8. [Paiement](#8-paiement)
9. [Livraison et logistique](#9-livraison-et-logistique)
10. [Commandes, retours et remboursements](#10-commandes-retours-et-remboursements)
11. [Espace vendeur (Seller Center)](#11-espace-vendeur-seller-center)
12. [Tunexa Fulfillment (équivalent FBA)](#12-tunexa-fulfillment-équivalent-fba)
13. [Avis, notes et Q&R](#13-avis-notes-et-qr)
14. [Marketing et fidélisation](#14-marketing-et-fidélisation)
15. [Abonnement Tunexa Plus (équivalent Prime)](#15-abonnement-tunexa-plus-équivalent-prime)
16. [Notifications et communication](#16-notifications-et-communication)
17. [Service client](#17-service-client)
18. [Back-office administrateur](#18-back-office-administrateur)
19. [Facturation, comptabilité et fiscalité](#19-facturation-comptabilité-et-fiscalité)
20. [Sécurité, anti-fraude et conformité](#20-sécurité-anti-fraude-et-conformité)
21. [Applications mobiles](#21-applications-mobiles)
22. [Exigences non fonctionnelles](#22-exigences-non-fonctionnelles)
23. [Architecture technique proposée](#23-architecture-technique-proposée)
24. [Modèle de données (entités principales)](#24-modèle-de-données-entités-principales)
25. [Indicateurs clés (KPI)](#25-indicateurs-clés-kpi)
26. [Feuille de route (phases)](#26-feuille-de-route-phases)

Légende des priorités : **P0** = indispensable au lancement (MVP), **P1** = peu après le lancement, **P2** = évolution future.

---

## 1. Vision et périmètre

**Objectif :** devenir la référence de l'achat en ligne en Tunisie en offrant :

- un très large choix de produits (vendeurs locaux + Tunexa en direct) ;
- des prix transparents en dinars tunisiens (TND) ;
- une livraison rapide dans les **24 gouvernorats** ;
- le **paiement à la livraison** et les moyens de paiement locaux ;
- une confiance forte : avis vérifiés, garantie de remboursement, service client réactif.

**Modèles économiques :**

| Source de revenus | Description |
|---|---|
| Commission vendeur | % par catégorie (ex. 5 % à 15 %) sur chaque vente |
| Abonnement vendeur | Formule mensuelle (Basique / Pro) |
| Fulfillment | Frais de stockage, préparation et expédition |
| Publicité | Produits sponsorisés, bannières |
| Tunexa Plus | Abonnement client (livraison gratuite, avantages) |
| Vente directe (1P) | Tunexa achète et revend en propre |

---

## 2. Acteurs et rôles

| Rôle | Description |
|---|---|
| Visiteur | Navigue, recherche, consulte sans compte |
| Client | Achète, suit ses commandes, laisse des avis |
| Client professionnel (B2B) | Achats en volume, facture avec matricule fiscal |
| Vendeur | Gère son catalogue, ses stocks, ses commandes |
| Livreur / transporteur | Récupère et livre les colis (app livreur) |
| Agent entrepôt | Réception, stockage, préparation (Fulfillment) |
| Agent service client | Traite tickets, litiges, retours |
| Modérateur | Valide produits, avis, vendeurs |
| Administrateur | Configuration globale, finances, droits |
| Comptable / Finance | Paiements vendeurs, rapprochements, fiscalité |

Gestion des droits par **RBAC** (rôles + permissions fines) côté back-office.

---

## 3. Spécificités tunisiennes

- **Langues :** arabe (RTL), français, anglais ; bascule à tout moment ; contenu produit multilingue. Support du *derja* dans la recherche (translittération : « tilifoun » → téléphone).
- **Devise :** TND avec 3 décimales (millimes) — ex. `1 249,900 DT`.
- **Adresses :** Gouvernorat → Délégation → Localité/Cité → Code postal (référentiel officiel préchargé), champ « point de repère » (très utilisé en Tunisie), géolocalisation optionnelle.
- **Téléphone :** format `+216 XX XXX XXX`, vérification par SMS OTP (Ooredoo, Orange, Tunisie Télécom).
- **Paiement à la livraison (COD)** : moyen principal au lancement.
- **Paiements locaux :** cartes bancaires tunisiennes (via passerelle locale), e-Dinar / D17 (La Poste), portefeuilles mobiles, virement.
- **Fiscalité :** TVA (taux 19 %, 13 %, 7 %, exonéré selon produit), **droit de timbre** sur facture, matricule fiscal vendeur / client B2B.
- **Cadre légal :** loi n° 2000-83 relative au commerce électronique, loi organique n° 2004-63 sur la protection des données personnelles (INPDP), droit de la consommation (droit de rétractation).
- **Calendrier commercial :** soldes d'été et d'hiver, Ramadan, Aïd el-Fitr, Aïd el-Adha, rentrée scolaire, Black Friday → campagnes dédiées.
- **Horaires Ramadan** pour livraisons et service client.

---

## 4. Espace client (acheteur)

### 4.1 Inscription et connexion — P0
- Inscription par **téléphone + OTP SMS** (prioritaire) ou e-mail + mot de passe.
- Connexion sociale : Google, Facebook, Apple (P1).
- Mot de passe oublié (SMS ou e-mail).
- Authentification à deux facteurs optionnelle (P1).
- Mode invité au checkout (commande sans compte, avec téléphone vérifié).

### 4.2 Profil — P0
- Nom, prénom, téléphone(s), e-mail, date de naissance (offres anniversaire), genre (optionnel).
- **Carnet d'adresses** multiples (domicile, travail…), adresse par défaut.
- Langue et préférences de notifications.
- Profil B2B : raison sociale, matricule fiscal, adresse de facturation.

### 4.3 Mon compte — P0
- Historique des commandes et statut en temps réel.
- Suivi de colis avec carte (P1).
- Factures téléchargeables (PDF).
- Demandes de retour / remboursement.
- Portefeuille **Tunexa Wallet** (avoirs, remboursements, cashback) — P1.
- Moyens de paiement enregistrés (tokenisés) — P1.
- Mes avis, mes questions.
- Suppression du compte et export des données personnelles (conformité INPDP).

### 4.4 Listes — P1
- **Liste d'envies** (wishlist), listes multiples, partageables (mariage, naissance, Aïd).
- « Acheter à nouveau ».
- Produits récemment consultés.
- Alerte prix / retour en stock.

---

## 5. Catalogue et recherche

### 5.1 Arborescence — P0
- Catégories multi-niveaux (ex. Électronique → Téléphones → Smartphones).
- Départements : High-tech, Électroménager, Mode, Beauté, Maison, Bébé, Sport, Auto-moto, Épicerie, Jouets, Livres & fournitures scolaires, Artisanat tunisien, etc.
- Attributs spécifiques par catégorie (taille, couleur, capacité, puissance…).

### 5.2 Moteur de recherche — P0
- Recherche plein texte tolérante aux fautes et multilingue (ar/fr/en + translittération derja).
- **Autocomplétion** et suggestions (produits, catégories, marques).
- Synonymes gérables par l'admin.
- Recherche vocale (P2) et par image (P2).

### 5.3 Filtres et tri — P0
- Filtres : catégorie, prix (min/max), marque, note, vendeur, disponibilité, livraison rapide, Tunexa Plus, état (neuf/reconditionné), attributs dynamiques, gouvernorat d'expédition.
- Tri : pertinence, prix croissant/décroissant, nouveautés, meilleures ventes, mieux notés.
- Pagination ou défilement infini.

### 5.4 Découverte — P1
- Page d'accueil personnalisée.
- « Les clients ayant acheté ceci ont aussi acheté ».
- « Fréquemment achetés ensemble ».
- Meilleures ventes, nouveautés, ventes flash, « Made in Tunisia ».
- Recommandations par IA basées sur l'historique (P2).

---

## 6. Fiche produit — P0

- Titre, marque, galerie d'images (zoom, 360° en P2), vidéo.
- Prix TND, prix barré, % de réduction, prix unitaire (au kg/litre).
- **Variantes** (couleur, taille…) avec stock et prix propres.
- Disponibilité et **date de livraison estimée selon le gouvernorat** du client.
- Vendeur affiché, note du vendeur, lien vers sa boutique.
- **Buy Box** : si plusieurs vendeurs proposent le même produit, sélection automatique de l'offre principale (prix, délai, performance vendeur) + lien « Autres vendeurs ».
- Description riche, caractéristiques techniques, contenu du coffret.
- Garantie (durée, type : constructeur/vendeur).
- Paiement en plusieurs fois (si disponible) — P2.
- Avis et notes, questions/réponses.
- Boutons : Ajouter au panier, Acheter maintenant, Ajouter à la liste d'envies, Partager (WhatsApp, Facebook, copie du lien).
- Signaler un produit (contrefaçon, contenu inapproprié).

---

## 7. Panier et checkout

### 7.1 Panier — P0
- Ajout/suppression, modification de quantité, limite par client.
- Panier persistant (compte) et fusion panier invité → compte.
- Regroupement par vendeur / par expédition.
- « Enregistrer pour plus tard ».
- Calcul en temps réel : sous-total, frais de livraison, réductions, TVA incluse, timbre fiscal, total.
- Vérification du stock et du prix au moment du checkout.

### 7.2 Checkout — P0
1. Adresse de livraison (sélection ou ajout, validation gouvernorat/délégation).
2. Mode de livraison (standard, express, retrait en point relais — P1).
3. Moyen de paiement.
4. Code promo / carte cadeau / solde wallet.
5. Récapitulatif et confirmation.
- **Confirmation de commande COD** par SMS ou appel automatique (réduit les refus de colis).
- Option facture B2B (matricule fiscal).
- Achat en 1 clic (P2).

---

## 8. Paiement

| Moyen | Priorité | Notes |
|---|---|---|
| Paiement à la livraison (espèces) | P0 | Plafond configurable ; frais COD optionnels |
| Carte bancaire tunisienne (CIB/Visa/Mastercard) | P0 | Via passerelle agréée locale (ex. ClicToPay, Konnect, Flouci…), 3-D Secure |
| e-Dinar / D17 (La Poste Tunisienne) | P1 | |
| Portefeuilles mobiles | P1 | |
| Virement bancaire (B2B) | P1 | Validation manuelle par la finance |
| Tunexa Wallet / cartes cadeaux | P1 | |
| Paiement en plusieurs fois | P2 | Partenariat bancaire/organisme de crédit |
| Cartes internationales (diaspora) | P2 | Commande depuis l'étranger, livraison en Tunisie |

Fonctions associées :
- Abstraction **PaymentProvider** pour brancher plusieurs passerelles.
- Webhooks de confirmation, idempotence, gestion des paiements en échec / expirés.
- Remboursements totaux et partiels (vers la carte ou le wallet).
- Rapprochement automatique des encaissements COD remis par les transporteurs.

---

## 9. Livraison et logistique

### 9.1 Modes de livraison — P0
- **Standard** (2–5 jours ouvrés selon zone).
- **Express** (J+1 Grand Tunis, Sfax, Sousse…) — P1.
- **Même jour** dans certaines villes — P2.
- **Retrait en point relais / lockers** — P1.
- Livraison des produits volumineux (électroménager) avec créneau et installation optionnelle — P1.

### 9.2 Tarification — P0
- Grille par zone (gouvernorat), poids/volume et mode.
- Livraison gratuite au-delà d'un seuil (configurable) ou pour Tunexa Plus.
- Expédition par le vendeur ou par Tunexa.

### 9.3 Intégration transporteurs — P0
- Connecteurs API avec les sociétés de livraison locales et la Poste (Rapid-Poste).
- Génération d'étiquettes et numéros de suivi.
- Synchronisation des statuts : préparé → remis au transporteur → en transit → en cours de livraison → livré / échec / retourné.
- Gestion des tentatives de livraison et des refus (COD).
- Rapprochement des fonds COD collectés.

### 9.4 Application livreur (flotte propre) — P2
- Tournées optimisées, scan des colis, preuve de livraison (photo, signature, OTP), encaissement COD, navigation GPS.

---

## 10. Commandes, retours et remboursements

### 10.1 Cycle de vie de la commande — P0
```
En attente de paiement → Confirmée → En préparation → Expédiée
  → En cours de livraison → Livrée
  ↘ Annulée   ↘ Échec de livraison → Retournée à l'expéditeur
```
- Une commande peut être **éclatée en plusieurs colis** (plusieurs vendeurs/entrepôts).
- Annulation par le client avant expédition.
- Historique horodaté de chaque changement d'état.

### 10.2 Retours — P0
- Demande de retour depuis « Mes commandes » (motif, photos).
- Délai de rétractation configurable (ex. 7 à 14 jours) ; produits non retournables (hygiène, alimentaire…).
- Enlèvement à domicile ou dépôt en point relais.
- Inspection à réception → acceptation / refus.
- Remboursement : moyen de paiement d'origine, wallet, ou échange.

### 10.3 Garantie A-à-Z Tunexa — P1
- Protection de l'acheteur si produit non reçu ou non conforme ; arbitrage par Tunexa entre client et vendeur.

---

## 11. Espace vendeur (Seller Center)

### 11.1 Inscription et KYC — P0
- Formulaire : raison sociale / personne physique, **matricule fiscal**, extrait RNE, CIN du gérant, RIB, adresse d'enlèvement.
- Vérification documentaire et validation par l'équipe Tunexa.
- Contrat et acceptation des CGV vendeur.
- Choix de l'abonnement.

### 11.2 Gestion du catalogue — P0
- Création de produit (formulaire guidé par catégorie) ou rattachement à une fiche existante (catalogue partagé, identifiant EAN/GTIN).
- Variantes, images (contrôle qualité : fond blanc, résolution), descriptions multilingues.
- **Import/export en masse** (Excel/CSV) — P0.
- Workflow de validation par la modération.
- Synchronisation API pour les gros vendeurs / ERP — P1.

### 11.3 Prix et stock — P0
- Prix, prix promotionnel avec dates, quantité par entrepôt.
- Alertes de stock bas.
- Règles de prix automatiques (s'aligner sur la Buy Box) — P2.

### 11.4 Commandes — P0
- Liste des commandes à traiter, acceptation, impression du bon de livraison et de l'étiquette.
- Délais de préparation (SLA) et compte à rebours.
- Gestion des retours et litiges.

### 11.5 Finances — P0
- Solde, ventes, commissions, frais, remboursements.
- **Virements périodiques** (ex. hebdomadaires) sur RIB après délai de sécurité.
- Relevés et factures de commission téléchargeables.

### 11.6 Performance et analytics — P1
- Taux d'annulation, retard d'expédition, taux de retour, note vendeur.
- Tableau de bord : CA, commandes, conversion, produits les plus vus.
- Sanctions automatiques sous un seuil (avertissement, suspension).

### 11.7 Publicité vendeur — P1
- Produits sponsorisés (coût par clic), budget quotidien, mots-clés, rapports.

### 11.8 Boutique vendeur — P1
- Page boutique personnalisée (logo, bannière, présentation), abonnés.

### 11.9 Multi-utilisateurs — P1
- Sous-comptes avec permissions (catalogue, commandes, finances).

---

## 12. Tunexa Fulfillment (équivalent FBA) — P1

- Le vendeur envoie son stock dans l'entrepôt Tunexa.
- **WMS** : réception, contrôle, étiquetage (code-barres), emplacement, inventaire, préparation (picking), emballage, expédition.
- Badge « Expédié par Tunexa » → éligible livraison rapide et Tunexa Plus.
- Facturation des frais de stockage (au m³/mois) et de traitement par unité.
- Gestion des retours et du stock invendable.
- Multi-entrepôts (Tunis, Sfax, Sousse) — P2.

---

## 13. Avis, notes et Q&R

- Notes de 1 à 5 étoiles + commentaire + photos/vidéos — P0.
- **Badge « Achat vérifié »** ; seuls les acheteurs peuvent noter — P0.
- Note du vendeur séparée de celle du produit — P0.
- Votes « utile », tri des avis, filtrage par note — P1.
- Modération (automatique + manuelle), détection des faux avis — P1.
- Réponse publique du vendeur — P1.
- Questions/réponses produit (réponses vendeur et communauté) — P1.

---

## 14. Marketing et fidélisation

- **Codes promo** : % ou montant fixe, minimum d'achat, par catégorie/vendeur/produit, usage unique ou multiple, date d'expiration — P0.
- **Ventes flash** avec compte à rebours et stock limité — P1.
- Offres groupées (bundles), « 2 achetés = 1 offert » — P1.
- **Programme de fidélité** : points par achat, convertibles en réductions — P2.
- **Parrainage** : crédit pour le parrain et le filleul — P1.
- Cartes cadeaux numériques — P1.
- Programme d'affiliation (influenceurs, sites partenaires) avec liens trackés — P2.
- Newsletters et campagnes e-mail/SMS segmentées — P1.
- Relance de panier abandonné (e-mail, SMS, push, WhatsApp) — P1.
- Pages événementielles (Ramadan, Aïd, rentrée, Black Friday) — P1.
- SEO : URLs propres, métadonnées, sitemap, données structurées (schema.org Product), hreflang ar/fr/en — P0.
- Pixels et analytics (Google Analytics, Meta) avec consentement — P0.

---

## 15. Abonnement Tunexa Plus (équivalent Prime) — P2

- Abonnement mensuel ou annuel en TND.
- Livraison gratuite et prioritaire illimitée sur produits éligibles.
- Accès anticipé aux ventes flash, offres exclusives.
- Essai gratuit, renouvellement automatique, résiliation simple.
- Avantages partenaires (streaming, restauration…) en option.

---

## 16. Notifications et communication — P0

| Événement | Canaux |
|---|---|
| Inscription / OTP | SMS |
| Commande confirmée | E-mail, SMS, push |
| Commande expédiée / en livraison | SMS, push, WhatsApp (P1) |
| Livrée | Push, e-mail + demande d'avis |
| Retour / remboursement | E-mail, push |
| Baisse de prix / retour en stock | Push, e-mail |
| Vendeur : nouvelle commande | E-mail, push, tableau de bord |

- Centre de notifications dans le compte.
- Modèles multilingues éditables par l'admin.
- Préférences d'opt-in/opt-out par canal.

---

## 17. Service client

- Centre d'aide / FAQ multilingue avec recherche — P0.
- Formulaire de contact et **tickets** (catégorie, commande liée, pièces jointes) — P0.
- Chat en direct + chatbot (suivi de commande, retours) — P1.
- Support WhatsApp Business — P1.
- Messagerie client ↔ vendeur sécurisée (sans échange de coordonnées personnelles) — P1.
- Centre d'appels : fiche client unifiée (commandes, tickets, historique) — P1.
- SLA de réponse et escalade.

---

## 18. Back-office administrateur

### 18.1 Tableau de bord — P0
- CA, commandes du jour, panier moyen, nouveaux clients, taux de conversion, commandes en retard, tickets ouverts.

### 18.2 Gestion — P0
- **Utilisateurs** : clients, vendeurs, staff ; blocage, réinitialisation.
- **Vendeurs** : validation KYC, commissions par catégorie, suspensions.
- **Catalogue** : catégories, attributs, marques, modération des fiches, fusion des doublons.
- **Commandes** : recherche, modification, annulation, remboursement manuel.
- **Contenu** : pages CMS, bannières, carrousels de la page d'accueil, FAQ.
- **Marketing** : promotions, codes, ventes flash, campagnes.
- **Logistique** : zones, tarifs, transporteurs, points relais.
- **Paiements** : passerelles, rapprochements, virements vendeurs.
- **Paramètres** : taxes, devise, langues, seuils, modèles de notifications.

### 18.3 Rapports — P1
- Ventes par catégorie, vendeur, gouvernorat, période ; export Excel/CSV.
- Rapports financiers et fiscaux.

### 18.4 Audit — P0
- Journal de toutes les actions sensibles (qui, quoi, quand, avant/après).

---

## 19. Facturation, comptabilité et fiscalité

- Génération automatique de factures conformes (mentions légales, matricule fiscal, TVA par taux, timbre fiscal) — P0.
- Factures émises au nom du vendeur (3P) ou de Tunexa (1P) — P0.
- Avoirs en cas de remboursement — P0.
- Factures de commission Tunexa → vendeur — P0.
- Préparation à la facturation électronique (format et signature électronique selon exigences TTN / administration fiscale) — P1.
- Export vers le logiciel comptable — P1.
- Gestion des retenues à la source si applicables — P1.

---

## 20. Sécurité, anti-fraude et conformité

### 20.1 Sécurité — P0
- HTTPS partout, HSTS, en-têtes de sécurité.
- Mots de passe hachés (Argon2/bcrypt), limitation des tentatives, captcha.
- Aucune donnée de carte stockée (tokenisation par la passerelle, conformité PCI-DSS).
- Protection OWASP Top 10 (injection, XSS, CSRF…).
- Sauvegardes chiffrées quotidiennes, plan de reprise.
- Tests d'intrusion avant lancement.

### 20.2 Anti-fraude — P1
- Score de risque des commandes COD (historique de refus, numéro, adresse).
- Liste noire téléphones/adresses, plafonds par client.
- Détection de comptes multiples, abus de codes promo, faux avis.
- Lutte contre la contrefaçon : signalement par les marques, programme « marque déposée ».

### 20.3 Conformité — P0
- Déclaration du traitement de données auprès de l'**INPDP**.
- Politique de confidentialité, CGU, CGV client et vendeur, mentions légales.
- Bannière de consentement cookies.
- Droit d'accès, de rectification et d'opposition.

---

## 21. Applications mobiles

- Applications **Android** (prioritaire, majoritaire en Tunisie) et **iOS** — P1 (PWA dès le MVP).
- Toutes les fonctions client : recherche, panier, paiement, suivi, retours, avis.
- Notifications push, liens profonds (deep links).
- Scanner de code-barres pour rechercher un produit — P2.
- Mode léger / faible consommation de données.
- App vendeur (gestion des commandes en mobilité) — P2.

---

## 22. Exigences non fonctionnelles

| Exigence | Cible |
|---|---|
| Temps de chargement (mobile 4G) | < 2,5 s (LCP) |
| Disponibilité | ≥ 99,9 % |
| Montée en charge | Pics x10 (Black Friday, Ramadan) |
| Accessibilité | WCAG 2.1 AA |
| Responsive | Mobile-first, RTL natif pour l'arabe |
| Hébergement | Données personnelles hébergées conformément à la réglementation tunisienne |
| Observabilité | Logs centralisés, métriques, alertes, traçage |
| Qualité | Tests unitaires, d'intégration, E2E ; CI/CD |

---

## 23. Architecture technique proposée

```
            ┌──────────────┐  ┌─────────────┐  ┌──────────────┐
            │ Web (Next.js)│  │ Apps mobiles│  │ Seller Center│
            └──────┬───────┘  └──────┬──────┘  └──────┬───────┘
                   └────────────┬────┴─────────────────┘
                          API Gateway / BFF
   ┌──────────┬──────────┬──────┴─────┬───────────┬────────────┬──────────┐
   │ Comptes  │ Catalogue│  Commandes │ Paiements │ Logistique │ Vendeurs │
   │ & Auth   │ & Search │  & Panier  │           │  & WMS     │ & Finance│
   └──────────┴──────────┴────────────┴───────────┴────────────┴──────────┘
        PostgreSQL · Redis (cache/sessions) · OpenSearch/Meilisearch (recherche)
        File de messages (RabbitMQ/Kafka) · Stockage objets (images) + CDN
```

**Stack suggérée :**
- **Frontend web :** Next.js (React, TypeScript), Tailwind CSS, i18n avec RTL.
- **Mobile :** React Native ou Flutter.
- **Backend :** Node.js (NestJS) ou Java/Kotlin (Spring Boot) — démarrer en **monolithe modulaire**, découper en microservices plus tard.
- **Base de données :** PostgreSQL ; Redis ; moteur de recherche OpenSearch ou Meilisearch.
- **Infra :** Docker, Kubernetes (ou PaaS au départ), CDN pour les images, CI/CD GitHub Actions.
- **Services externes :** passerelle(s) de paiement locales, fournisseur SMS, e-mail transactionnel, transporteurs, Google Maps / OpenStreetMap.

---

## 24. Modèle de données (entités principales)

| Entité | Champs clés |
|---|---|
| User | id, téléphone, e-mail, nom, langue, rôle, statut |
| Address | user_id, gouvernorat, délégation, localité, code_postal, rue, repère, lat/lng |
| Seller | id, raison_sociale, matricule_fiscal, rib, statut_kyc, note, abonnement |
| Category | id, parent_id, nom (ar/fr/en), attributs, taux_commission |
| Product | id, category_id, marque, titre/description (multilingue), ean, statut |
| ProductVariant | product_id, sku, attributs (couleur, taille…), poids, dimensions |
| Offer | variant_id, seller_id, prix, prix_promo, stock, condition, mode_expédition |
| Cart / CartItem | user_id, offer_id, quantité |
| Order | id, user_id, adresse, total, tva, timbre, statut, moyen_paiement |
| SubOrder / Shipment | order_id, seller_id, transporteur, n°_suivi, statut |
| OrderItem | sub_order_id, offer_id, quantité, prix_unitaire, taux_tva |
| Payment | order_id, fournisseur, montant, statut, référence |
| Refund / Return | order_item_id, motif, statut, montant |
| Review | product_id, user_id, note, texte, achat_vérifié |
| Coupon | code, type, valeur, conditions, validité |
| Payout | seller_id, période, montant, statut |
| Invoice | order_id / seller_id, numéro, pdf, type (facture/avoir) |
| Ticket | user_id, order_id, catégorie, statut, messages |
| AuditLog | acteur, action, entité, avant, après, date |

---

## 25. Indicateurs clés (KPI)

- **Business :** GMV, CA net (commissions + frais), panier moyen, nombre de commandes, part 1P/3P.
- **Conversion :** taux de conversion, abandon de panier, taux de rebond.
- **Clients :** nouveaux clients, taux de réachat, CLV, NPS.
- **Logistique :** délai moyen de livraison, taux de livraison réussie, **taux de refus COD**, taux de retour.
- **Vendeurs :** vendeurs actifs, taux d'annulation, retard d'expédition, note moyenne.
- **Service client :** temps de première réponse, temps de résolution, satisfaction.

---

## 26. Feuille de route (phases)

### Phase 1 — MVP (≈ 4–6 mois)
- Comptes clients (OTP SMS), catalogue, recherche + filtres, fiche produit.
- Panier, checkout, **paiement à la livraison** + carte bancaire locale.
- Seller Center de base (KYC, catalogue, import Excel, commandes, finances).
- Intégration 1–2 transporteurs, suivi de commande.
- Retours et remboursements, avis vérifiés.
- Back-office admin, factures conformes, notifications e-mail/SMS.
- Site responsive + PWA, trilingue avec RTL, SEO.

### Phase 2 — Croissance (+3–6 mois)
- Apps Android/iOS, wishlist, alertes prix, recommandations.
- Ventes flash, parrainage, cartes cadeaux, relance de panier.
- Tunexa Wallet, e-Dinar/D17, points relais, livraison express.
- Publicité vendeur, analytics vendeur, chat et WhatsApp.
- Tunexa Fulfillment (premier entrepôt), anti-fraude COD.

### Phase 3 — Expansion
- Tunexa Plus, fidélité, paiement en plusieurs fois.
- Multi-entrepôts, livraison le jour même, flotte propre + app livreur.
- Recherche vocale / par image, IA de recommandation et de tarification.
- Diaspora (paiement international), B2B avancé, affiliation.
- Ouverture régionale (Maghreb) — devises et fiscalités multiples.
