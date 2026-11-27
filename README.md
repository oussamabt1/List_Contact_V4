# List_Contacts_V4 — TP Android : Gestion des contacts (V4)

Persistance **SQLite native** (SQLiteOpenHelper, sans Room), **préférences utilisateur**
(AndroidX Preference), **fragments** et **Google Maps**. Kotlin + vues XML (ViewBinding).

## Lancer le projet
1. Android Studio (Koala ou plus récent, JDK 17) → *Open* → dossier du projet. Laisser Gradle se synchroniser.
2. Créer une clé « Maps SDK for Android » dans la console Google Cloud.
3. Copier `local.properties.example` en `local.properties` et renseigner `MAPS_API_KEY=...`
   (le fichier est dans `.gitignore` : **la clé n'est jamais publiée**). Sans clé, tout fonctionne sauf l'affichage de la carte.
4. Exécuter sur un émulateur/appareil (API 26+).

## Structure
```
data/   Contact, ContactContract, ContactDbHelper (singleton), ContactDao (seul SQL), PreferencesManager
ui/     ListeContactsFragment, DetailContactFragment, CarteContactFragment, ParametresFragment,
        ContactAdapter, ContactViewModel (partagé), Navigation
util/   PhotoStorage (copie en filesDir + FileProvider), IntentActions (DIAL / e-mail)
```
Tests : `./gradlew connectedAndroidTest` (DAO, apostrophes, injection SQL, tri, GPS NULL).
Inspection de la base : *App Inspection → Database Inspector → contacts.db*.

## Remarque
Thème clair uniquement : les couleurs de texte proposées sont choisies pour être lisibles sur fond clair.
