# Corsica Soundscape — application Android

Application Android de carte sonore collaborative de la Corse. Elle permet d'explorer, d'enregistrer et de partager des ambiances géolocalisées. Elle accompagne la [PWA Corsica Soundscape](https://pwacorsica-soundscape.ounesshimi.workers.dev/).

## Fonctionnalités

- **Carte** : exploration avec OpenStreetMap, déplacement, zoom, regroupement des marqueurs proches et filtres par catégorie.
- **Sons** : fiches avec description, auteur, date, lecture audio, likes, nombre d'écoutes et commentaires.
- **Enregistrement** : capture au micro, aperçu du niveau sonore, choix d'une catégorie et position GPS ou saisie manuelle des coordonnées.
- **Populaires et parcours** : classement par likes ou écoutes ; création et consultation de balades composées de plusieurs sons.
- **Profil** : création d'un pseudo au premier lancement, conservé localement et synchronisé avec le serveur.
- **Hors ligne** : les sons et les enregistrements sont conservés dans Room ; WorkManager envoie les nouveaux sons et reprend les suppressions en attente au retour du réseau. Une carte marquée pour suppression reste visible avec un statut dédié jusqu'à sa synchronisation.
- **Interface** : thème clair/sombre mémorisé, navigation à quatre onglets, cartes adaptées au téléphone et notifications prises en charge via Firebase Cloud Messaging.

## Choix techniques

L'interface est développée en **Kotlin, Jetpack Compose et Material 3**, avec **Navigation 3**. La carte utilise **osmdroid** et la lecture audio **Media3**. **Room** est la source de vérité locale ; une outbox et **WorkManager** assurent la synchronisation. **Retrofit/OkHttp** communiquent avec l'API du Worker Cloudflare. Les préférences sont stockées dans **DataStore**. Les dépendances de l'application sont assemblées par `SoundscapeContainer` ; le projet n'utilise pas Hilt.

## Comment fonctionne la synchronisation ?

Il y a deux « workers » distincts : **SyncWorker**, une tâche Android exécutée par WorkManager, et le **Worker Cloudflare**, l'API distante qui enregistre les données dans Supabase.

1. **Ajout d'un son** : l'enregistrement et ses informations sont d'abord sauvegardés localement dans Room. Une entrée dans l'**outbox** indique qu'il faut les envoyer. Le son reste donc disponible dans l'application sans réseau.
2. **Retour du réseau** : WorkManager lance `SyncWorker` dès qu'une connexion est disponible. Il traite l'outbox, envoie le fichier et les informations à l'API, puis marque le son `SYNCED` et retire l'entrée de l'outbox. L'identifiant d'envoi évite de créer deux sons si une tentative est répétée.
3. **Suppression d'un son** : la carte passe d'abord à l'état `DELETING` dans Room et affiche « Suppression en attente ». Après les envois, `SyncWorker` appelle `DELETE /api/sounds/:id`. En cas de succès ou si le son est déjà absent du serveur (404), il supprime la ligne locale et le fichier audio local.
4. **Échec temporaire** : si le réseau ou le serveur échoue, les données en attente restent dans Room. WorkManager réessaie plus tard avec un délai croissant ; le son à supprimer garde son statut visible jusqu'à la réussite.

Le Worker Cloudflare reçoit les requêtes HTTP de l'application et utilise Supabase pour les sons et les fichiers audio. L'app Android ne stocke pas de clé Supabase.

## Lancer le projet

1. Ouvrir ce dossier dans Android Studio et laisser Gradle synchroniser le projet.
2. Installer le SDK Android demandé par le projet (`compileSdk 37`) et lancer un émulateur ou brancher un appareil Android **7.0 ou plus** (`minSdk 24`).
3. Exécuter la configuration **app**. Autoriser le microphone pour enregistrer, la position pour se géolocaliser et les notifications si souhaité. Une position peut aussi être saisie manuellement.

Sous Windows, pour générer l'APK de développement :

```powershell
.\gradlew.bat assembleDebug
```

L'APK généré se trouve dans `app/build/outputs/apk/debug/app-debug.apk`. Les fonctions réseau nécessitent une connexion et l'API configurée dans `ApiClient.kt`. La carte utilise les tuiles OpenStreetMap, qui ne sont pas garanties hors ligne.

**Télécharger l'APK Android :** [Corsica Soundscape v1.0 (APK de démonstration)](https://github.com/youneshimi/AndroidCorsica-Soundscape/releases/download/v1.0-android-demo/Corsica-Soundscape-v1.0-debug.apk). C'est un APK debug installable sur Android 7.0 ou plus, signé avec une clé de développement ; il n'est pas destiné au Play Store. La vidéo de démonstration sera ajoutée ici lorsqu'elle sera publiée.
