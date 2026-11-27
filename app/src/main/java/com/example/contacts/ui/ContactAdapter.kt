package com.example.contacts.ui

import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.contacts.R
import com.example.contacts.data.Contact
import com.example.contacts.data.DisplayPrefs
import com.example.contacts.databinding.ItemContactBinding
import java.io.File

/**
 * Reçoit des valeurs déjà converties ([DisplayPrefs]) : l'adapter ne lit jamais les préférences.
 * L'identifiant de ligne est l'_id de la base, jamais la position.
 */
class ContactAdapter(
    private val onClick: (Contact) -> Unit,
    private val onCall: (Contact) -> Unit
) : ListAdapter<Contact, ContactAdapter.VH>(Diff) {

    private var prefs = DisplayPrefs(16f, Color.BLACK, false)

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id

    /** Réapplique les préférences à toutes les lignes visibles (sans redémarrage). */
    fun updatePrefs(newPrefs: DisplayPrefs) {
        if (newPrefs == prefs) return
        prefs = newPrefs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemContactBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(c: Contact) {
            b.tvNom.text = c.nom
            b.tvPrenom.text = c.prenom
            b.tvTelephone.text = c.telephone

            listOf(b.tvNom, b.tvPrenom, b.tvTelephone).forEach {
                it.setTextSize(TypedValue.COMPLEX_UNIT_SP, prefs.tailleSp)
            }
            b.tvNom.setTextColor(prefs.couleur)
            b.tvPrenom.setTextColor(prefs.couleur)

            val model: Any = c.photoUri?.let { File(it) } ?: R.drawable.ic_person
            Glide.with(b.imgPhoto).load(model).circleCrop()
                .placeholder(R.drawable.ic_person).error(R.drawable.ic_person).into(b.imgPhoto)

            // Bouton Appeler : GONE si la préférence est désactivée.
            b.btnAppeler.isVisible = prefs.appelActif
            // Le bouton est cliquable : son clic ne se propage pas à la ligne (pas d'ouverture du détail).
            b.btnAppeler.setOnClickListener { onCall(c) }
            b.root.setOnClickListener { onClick(c) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(a: Contact, b: Contact) = a.id == b.id
        override fun areContentsTheSame(a: Contact, b: Contact) = a == b
    }
}
