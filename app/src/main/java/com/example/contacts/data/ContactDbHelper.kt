package com.example.contacts.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.contacts.data.ContactContract.DATABASE_NAME
import com.example.contacts.data.ContactContract.DATABASE_VERSION
import com.example.contacts.data.ContactContract.SQL_CREATE_TABLE
import com.example.contacts.data.ContactContract.TABLE

/**
 * Ouvre/crée contacts.db. Singleton construit avec l'applicationContext
 * (évite de retenir une Activity et d'ouvrir plusieurs connexions concurrentes).
 */
class ContactDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    /** Appelée UNE SEULE FOIS, quand le fichier contacts.db n'existe pas encore. */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_TABLE)
        insertDemoContacts(db)
    }

    /**
     * Appelée quand DATABASE_VERSION augmente. Migrations incrémentales : on n'efface
     * jamais les données de l'utilisateur. Exemple (bonus, version 2) :
     *
     *   if (oldVersion < 2) db.execSQL("ALTER TABLE $TABLE ADD COLUMN favori INTEGER NOT NULL DEFAULT 0")
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 : rien à migrer pour l'instant.
    }

    /** Insertion avec la base reçue en paramètre (getWritableDatabase ne doit pas être rappelé ici). */
    private fun insertDemoContacts(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            DEMO_CONTACTS.forEach { db.insertOrThrow(TABLE, null, it.toContentValues()) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        @Volatile
        private var instance: ContactDbHelper? = null

        fun getInstance(context: Context): ContactDbHelper =
            instance ?: synchronized(this) {
                instance ?: ContactDbHelper(context.applicationContext).also { instance = it }
            }

        // 6 contacts de démonstration, dont 2 sans coordonnées GPS.
        private val DEMO_CONTACTS = listOf(
            Contact(nom = "Ben Salah", prenom = "Amine", telephone = "+216 20 123 456",
                email = "amine.bensalah@example.com", latitude = 36.8065, longitude = 10.1815),
            Contact(nom = "Trabelsi", prenom = "Leïla", telephone = "+216 22 987 654",
                email = "leila.trabelsi@example.com", latitude = 35.8256, longitude = 10.6084),
            Contact(nom = "Gharbi", prenom = "Youssef", telephone = "+216 98 555 010",
                email = "y.gharbi@example.com", latitude = 34.7406, longitude = 10.7603),
            Contact(nom = "Mansour", prenom = "Sarra", telephone = "+216 55 321 000",
                email = null, latitude = 36.8665, longitude = 10.1647),
            Contact(nom = "Haddad", prenom = "Karim", telephone = "+216 71 000 111",
                email = "karim.haddad@example.com"),            // sans GPS
            Contact(nom = "O'Neil", prenom = "Sean", telephone = "0612345678",
                email = "sean.oneil@example.com")                // sans GPS, apostrophe
        )
    }
}
