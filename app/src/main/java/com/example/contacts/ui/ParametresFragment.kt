package com.example.contacts.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.contacts.R

/** Écran des 3 préférences (stockées dans les SharedPreferences par défaut). */
class ParametresFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        activity?.setTitle(R.string.titre_parametres)
    }
}
