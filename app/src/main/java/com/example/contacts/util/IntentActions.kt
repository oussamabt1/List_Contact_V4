package com.example.contacts.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.contacts.R

/** Intents implicites partagés par la liste et le détail. Aucune permission requise. */
object IntentActions {

    /** ACTION_DIAL ouvre le composeur sans lancer l'appel : pas de permission CALL_PHONE. */
    fun dial(context: Context, phone: String?) {
        val number = phone.orEmpty().filter { it.isDigit() || it == '+' }
        if (number.isEmpty()) {
            toast(context, R.string.err_numero_vide)
            return
        }
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)))
        } catch (e: ActivityNotFoundException) {
            toast(context, R.string.err_pas_app_tel)
        }
    }

    fun email(context: Context, address: String?) {
        val to = address.orEmpty().trim()
        if (to.isEmpty()) {
            toast(context, R.string.err_email_vide)
            return
        }
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast(context, R.string.err_pas_app_mail)
        }
    }

    fun toast(context: Context, resId: Int) =
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
}
