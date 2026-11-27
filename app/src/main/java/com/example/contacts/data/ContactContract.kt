package com.example.contacts.data

/** Noms de la base, de la table et des colonnes : aucune chaîne en dur ailleurs. */
object ContactContract {
    const val DATABASE_NAME = "contacts.db"
    const val DATABASE_VERSION = 1

    const val TABLE = "contacts"
    const val COL_ID = "_id"
    const val COL_NOM = "nom"
    const val COL_PRENOM = "prenom"
    const val COL_TELEPHONE = "telephone"
    const val COL_EMAIL = "email"
    const val COL_PHOTO_URI = "photo_uri"
    const val COL_LATITUDE = "latitude"
    const val COL_LONGITUDE = "longitude"

    const val SQL_CREATE_TABLE = """
        CREATE TABLE $TABLE (
            $COL_ID        INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_NOM       TEXT NOT NULL,
            $COL_PRENOM    TEXT NOT NULL,
            $COL_TELEPHONE TEXT NOT NULL,
            $COL_EMAIL     TEXT,
            $COL_PHOTO_URI TEXT,
            $COL_LATITUDE  REAL,
            $COL_LONGITUDE REAL
        )
    """

    /** Tri par nom puis prénom, sans tenir compte de la casse (ASCII). */
    const val ORDER_BY_NAME = "$COL_NOM COLLATE NOCASE ASC, $COL_PRENOM COLLATE NOCASE ASC"
}
