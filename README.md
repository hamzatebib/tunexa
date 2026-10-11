# Tunexa

Marketplace e-commerce multi-vendeurs pour la Tunisie : paiement à la livraison, livraison dans les 24 gouvernorats, interface en français, arabe et anglais.

| Dossier | Contenu |
|---|---|
| `backend/` | API REST — Java 21, Spring Boot 3.5, PostgreSQL, Flyway, JWT |
| `frontend/` | Site web — React 19, TypeScript, Vite, Tailwind CSS, TanStack Query, i18next |
| `docs/` | Cahier des fonctionnalités, backlog des tickets (Excel), scénarios d'utilisation (Word) |

## Démarrer en local

Prérequis : Java 21, Maven, Node.js 22 et PostgreSQL 16 (ou Docker).

```bash
# 1. Base de données
docker compose up -d

# 2. Backend (http://localhost:8080) avec données de démonstration
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Frontend (http://localhost:5173), dans un autre terminal
cd frontend
npm install
npm run dev
```

Le profil `dev` crée des comptes de démonstration et affiche les codes SMS dans l'interface (aucun SMS réel n'est envoyé) :

| Rôle | Téléphone | Mot de passe |
|---|---|---|
| Client | 55 555 555 | Client12345 |
| Vendeur (TechZone Tunis) | 22 222 222 | Vendeur12345 |
| Administrateur | 20 000 000 | Admin12345 |

## Tests

```bash
cd backend && mvn test          # tests d'intégration de l'API (base H2 en mémoire)
cd frontend && npm run lint && npm run build
```

## Ce que couvre cette première version (MVP)

- **Clients** : inscription par téléphone + code SMS, connexion, mot de passe oublié, profil, carnet d'adresses (gouvernorat, délégation, point de repère).
- **Catalogue** : catégories, recherche (texte, catégorie, marque, prix, stock), tri, fiche produit avec date de livraison estimée selon le gouvernorat, plusieurs vendeurs par produit et choix automatique de la meilleure offre (Buy Box).
- **Achat** : panier, récapitulatif (livraison par zone, gratuite dès 300 DT, droit de timbre, TVA incluse), commande avec **paiement à la livraison** confirmée par code SMS, annulation automatique après 24 h sans confirmation.
- **Commandes** : un colis par vendeur, suivi et historique, annulation avant expédition avec remise en stock.
- **Vendeurs** : candidature (matricule fiscal, RNE, RIB), création de produits soumis à modération, prix et stocks, traitement des colis (accepter, expédier avec n° de suivi, annuler), commission et montant à encaisser.
- **Back-office** : indicateurs, validation des vendeurs, modération des produits, mise à jour des livraisons.

Les montants sont stockés en **millimes** (1 DT = 1000) pour éviter les erreurs d'arrondi.

## Configuration (variables d'environnement)

| Variable | Rôle | Défaut |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Connexion PostgreSQL | `jdbc:postgresql://localhost:5432/tunexa`, `tunexa`, `tunexa` |
| `JWT_SECRET` | Clé de signature des jetons (32 caractères minimum) | valeur de développement : **à changer en production** |
| `CORS_ORIGINS` | Origines autorisées pour le frontend | `http://localhost:5173` |
| `OTP_EXPOSE_CODE` | Renvoie les codes SMS dans l'API (développement uniquement) | `false` |
| `DEMO_DATA` | Crée les données de démonstration | `false` |

Les autres règles (frais de livraison par zone, seuil de gratuité, droit de timbre, plafond du paiement à la livraison, commission) sont dans `backend/src/main/resources/application.yml`.

## Prochaines étapes

Intégrations à brancher : fournisseur SMS réel, transporteurs (API de suivi), passerelle de paiement par carte et e-Dinar, factures PDF, puis retours, avis clients et application mobile (voir `docs/`).
