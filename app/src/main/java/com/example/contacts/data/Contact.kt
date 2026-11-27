package com.example.contacts.data

/**
 * Modèle d'un contact. [id] vaut 0 tant que le contact n'est pas enregistré,
 * puis prend la valeur de la colonne _id attribuée par SQLite.
 */
data class Contact(
    val id: Long = 0,
    val nom: String,
    val prenom: String,
    val telephone: String,
    val email: String? = null,
    val photoUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/** Vrai si les deux coordonnées existent et sont des nombres dans les limites GPS. */
fun isValidPosition(latitude: Double?, longitude: Double?): Boolean =
    latitude != null && longitude != null &&
        latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0
