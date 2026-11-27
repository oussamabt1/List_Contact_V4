package com.example.contacts.data

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.preference.PreferenceManager

/** Valeurs déjà converties, prêtes à être appliquées aux vues. */
data class DisplayPrefs(
    val tailleSp: Float,
    @ColorInt val couleur: Int,
    val appelActif: Boolean
)

/** Point d'accès unique aux SharedPreferences de l'application. */
class PreferencesManager(context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    fun read(): DisplayPrefs {
        val taille = prefs.getString(KEY_TAILLE, DEFAULT_TAILLE)?.toFloatOrNull() ?: DEFAULT_TAILLE.toFloat()
        val couleur = try {
            Color.parseColor(prefs.getString(KEY_COULEUR, DEFAULT_COULEUR) ?: DEFAULT_COULEUR)
        } catch (e: IllegalArgumentException) {
            Color.BLACK
        }
        return DisplayPrefs(taille, couleur, prefs.getBoolean(KEY_APPEL, false))
    }

    companion object {
        const val KEY_TAILLE = "taille_police"
        const val KEY_COULEUR = "couleur_preferee"
        const val KEY_APPEL = "appel_depuis_liste"
        private const val DEFAULT_TAILLE = "16"
        private const val DEFAULT_COULEUR = "#000000"
    }
}
