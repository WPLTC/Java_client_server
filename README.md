# Application de Chat Client-Serveur Java avec Interface Graphique

## Présentation

Cette application est un système de chat client-serveur en Java, avec interface graphique (Swing), permettant à plusieurs utilisateurs de discuter en temps réel, en privé ou en groupe (conférence). Elle gère l'inscription, la connexion, la persistance des utilisateurs, les groupes/conférences, les messages privés, et l'historique des messages.

## Fonctionnalités principales

- **Inscription et connexion des utilisateurs** (avec persistance dans `users.txt` côté serveur)
- **Interface graphique moderne** (Swing)
- **Chat global** (tous les utilisateurs connectes)
- **Messages privés** (sélection d'un utilisateur, bouton "Message privé")
- **Groupes/Conférences** :
  - Création de groupes/conférences (le créateur devient moderateur)
  - Ajout/retrait de membres par le moderateur
  - Discussion de groupe (messages visibles par tous les membres)
  - Fenêtre dédiée pour chaque conférence
- **Affichage de la liste des utilisateurs connectes**
- **Persistance des utilisateurs** (connexion possible après redémarrage du serveur)
- **Stockage local des identifiants côté client** (`credentials.txt`)
- **Bouton "Envoyer" pour le chat**

## Installation

1. **Prérequis** : Java 8 ou supérieur
2. **Cloner ou télécharger le projet**
3. **Compiler tous les fichiers Java** :
   ```sh
   javac *.java
   ```

## Utilisation

> **Important :** Avant de lancer les commandes ci-dessous, placez-vous dans le dossier `socket_GUI` :
> ```sh
> cd socket_GUI
> ```

### 1. Lancer le serveur
Dans un terminal, exécute :
```sh
java EchoServerGUI
```

### 2. Lancer un ou plusieurs clients
Dans d'autres terminaux (ou sur d'autres machines), exécute :
```sh
java EchoClientGUI
```

### 3. Inscription et connexion
- À la première utilisation, choisis "Inscription", entre un nom d'utilisateur et un mot de passe.
- Les identifiants sont enregistrés côté serveur (`users.txt`) et côté client (`credentials.txt`).
- Pour te reconnecter, choisis ton nom dans la liste déroulante et clique sur "Connexion".

### 4. Chat et groupes
- Envoie des messages dans le chat global ou en privé.
- Crée un groupe/conférence, sélectionne les membres connectes, et discutez dans une fenêtre dédiée.
- Le moderateur peut ajouter/retirer des membres à tout moment.

## Commandes principales (pour les utilisateurs avancés)
- `/mp NomUtilisateur message` : envoyer un message privé
- `/group create NomGroupe` : créer un groupe/conférence
- `/group add NomGroupe NomUtilisateur` : ajouter un membre (moderateur uniquement)
- `/group remove NomGroupe NomUtilisateur` : retirer un membre (moderateur uniquement)
- `/group send NomGroupe message` : envoyer un message à un groupe
- `exit` : se déconnecter

## Fichiers importants
- `EchoServerGUI.java` : serveur graphique
- `EchoClientGUI.java` : client graphique
- `UserManager.java` : gestion et persistance des utilisateurs
- `users.txt` : base des utilisateurs côté serveur
- `credentials.txt` : identifiants enregistrés côté client

## Remarques
- Les groupes et messages ne sont pas persistés (perdus si le serveur redémarre)
- Les utilisateurs sont persistés côté serveur (`users.txt`)
- Le client peut gérer plusieurs comptes (menu déroulant à la connexion)

## Auteurs
- Projet réalisé par [Votre Nom] dans le cadre d'un TP Java Client-Serveur. 