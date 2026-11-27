package com.example.contacts.data

import android.content.ContentValues
import android.database.Cursor
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getStringOrNull
import com.example.contacts.data.ContactContract.COL_EMAIL
import com.example.contacts.data.ContactContract.COL_ID
import com.example.contacts.data.ContactContract.COL_LATITUDE
import com.example.contacts.data.ContactContract.COL_LONGITUDE
import com.example.contacts.data.ContactContract.COL_NOM
import com.example.contacts.data.ContactContract.COL_PHOTO_URI
import com.example.contacts.data.ContactContract.COL_PRENOM
import com.example.contacts.data.ContactContract.COL_TELEPHONE
import com.example.contacts.data.ContactContract.ORDER_BY_NAME
import com.example.contacts.data.ContactContract.TABLE

/**
 * Seule classe qui manipule du SQL. Toutes les valeurs variables passent par des "?"
 * (selectionArgs / ContentValues). À appeler HORS du thread principal.
 */
class ContactDao(private val helper: ContactDbHelper) {

    /** @return l'_id généré, ou -1 si l'insertion a échoué. */
    fun insert(contact: Contact): Long =
        helper.writableDatabase.insert(TABLE, null, contact.toContentValues())

    /** @return le nombre de lignes modifiées (0 = contact introuvable). */
    fun update(contact: Contact): Int =
        helper.writableDatabase.update(
            TABLE, contact.toContentValues(), "$COL_ID = ?", arrayOf(contact.id.toString())
        )

    /** @return le nombre de lignes supprimées (0 = contact introuvable). */
    fun delete(id: Long): Int =
        helper.writableDatabase.delete(TABLE, "$COL_ID = ?", arrayOf(id.toString()))

    fun getAll(): List<Contact> = query(null, null)

    fun getById(id: Long): Contact? =
        helper.readableDatabase.query(
            TABLE, null, "$COL_ID = ?", arrayOf(id.toString()), null, null, null, "1"
        ).use { c -> if (c.moveToFirst()) c.toContact() else null }

    /** Recherche insensible à la casse (ASCII) sur nom, prénom et téléphone. */
    fun search(texte: String): List<Contact> {
        val q = texte.trim()
        if (q.isEmpty()) return getAll()
        val pattern = "%" + q.escapeLike() + "%"
        val selection = "$COL_NOM LIKE ? ESCAPE '\\' OR " +
            "$COL_PRENOM LIKE ? ESCAPE '\\' OR " +
            "$COL_TELEPHONE LIKE ? ESCAPE '\\'"
        return query(selection, arrayOf(pattern, pattern, pattern))
    }

    private fun query(selection: String?, args: Array<String>?): List<Contact> =
        helper.readableDatabase.query(
            TABLE, null, selection, args, null, null, ORDER_BY_NAME
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(c.toContact())
            }
        }

    /** % et _ saisis par l'utilisateur sont traités comme des caractères ordinaires. */
    private fun String.escapeLike(): String =
        replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private fun Cursor.toContact() = Contact(
        id = getLong(getColumnIndexOrThrow(COL_ID)),
        nom = getString(getColumnIndexOrThrow(COL_NOM)),
        prenom = getString(getColumnIndexOrThrow(COL_PRENOM)),
        telephone = getString(getColumnIndexOrThrow(COL_TELEPHONE)),
        email = getStringOrNull(getColumnIndexOrThrow(COL_EMAIL)),
        photoUri = getStringOrNull(getColumnIndexOrThrow(COL_PHOTO_URI)),
        latitude = getDoubleOrNull(getColumnIndexOrThrow(COL_LATITUDE)),
        longitude = getDoubleOrNull(getColumnIndexOrThrow(COL_LONGITUDE))
    )
}

/** Conversion modèle -> ContentValues (null => colonne NULL). L'_id n'est jamais fourni. */
internal fun Contact.toContentValues(): ContentValues = ContentValues().apply {
    put(COL_NOM, nom)
    put(COL_PRENOM, prenom)
    put(COL_TELEPHONE, telephone)
    put(COL_EMAIL, email)
    put(COL_PHOTO_URI, photoUri)
    put(COL_LATITUDE, latitude)
    put(COL_LONGITUDE, longitude)
}
