package com.example.contacts.ui

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.contacts.R
import com.example.contacts.data.Contact
import com.example.contacts.data.PreferencesManager
import com.example.contacts.databinding.FragmentDetailContactBinding
import com.example.contacts.util.IntentActions
import com.example.contacts.util.PhotoStorage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Création (id = 0), consultation, modification et suppression d'un contact. */
class DetailContactFragment : Fragment() {

    private var _binding: FragmentDetailContactBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ContactViewModel by activityViewModels()

    private var contactId = 0L

    /** Photo actuellement affichée (chemin interne) et photo présente en base. */
    private var photoPath: String? = null
    private var originalPhoto: String? = null

    /** Évite d'écraser une saisie en cours (rotation, retour de la carte) par les données de la base. */
    private var dataLoaded = false
    private var pendingCameraPath: String? = null
    private var confirmDialog: AlertDialog? = null

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) onGalleryPicked(uri)          // uri == null : sélection annulée
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            onCameraResult(success)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactId = requireArguments().getLong(ARG_ID)
        savedInstanceState?.let {
            photoPath = it.getString(KEY_PHOTO)
            originalPhoto = it.getString(KEY_ORIGINAL_PHOTO)
            dataLoaded = it.getBoolean(KEY_LOADED)
            pendingCameraPath = it.getString(KEY_PENDING_CAMERA)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentDetailContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val creation = contactId == 0L
        applyPrefs()
        binding.btnSupprimer.isVisible = !creation

        binding.btnGalerie.setOnClickListener { pickImage.launch("image/*") }
        binding.btnCamera.setOnClickListener { launchCamera() }
        binding.btnAppeler.setOnClickListener {
            IntentActions.dial(requireContext(), binding.editTelephone.text?.toString())
        }
        binding.btnEmail.setOnClickListener {
            IntentActions.email(requireContext(), binding.editEmail.text?.toString())
        }
        binding.btnCarte.setOnClickListener {
            if (creation) {
                IntentActions.toast(requireContext(), R.string.carte_enregistrer_d_abord)
            } else {
                navigateTo(CarteContactFragment.newInstance(contactId))
            }
        }
        binding.btnEnregistrer.setOnClickListener { onSave() }
        binding.btnAnnuler.setOnClickListener { onCancel() }
        binding.btnSupprimer.setOnClickListener { confirmDelete() }

        when {
            dataLoaded -> showPhoto()
            creation -> { dataLoaded = true; showPhoto() }
            else -> loadContact()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is UiEvent.Saved -> {
                            IntentActions.toast(requireContext(), R.string.contact_enregistre)
                            parentFragmentManager.popBackStack()
                        }
                        is UiEvent.Deleted -> {
                            IntentActions.toast(requireContext(), R.string.contact_supprime)
                            parentFragmentManager.popBackStack()
                        }
                        is UiEvent.Message -> {
                            binding.btnEnregistrer.isEnabled = true
                            IntentActions.toast(requireContext(), event.resId)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.setTitle(if (contactId == 0L) R.string.titre_creation else R.string.titre_detail)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PHOTO, photoPath)
        outState.putString(KEY_ORIGINAL_PHOTO, originalPhoto)
        outState.putBoolean(KEY_LOADED, dataLoaded)
        outState.putString(KEY_PENDING_CAMERA, pendingCameraPath)
    }

    override fun onDestroyView() {
        confirmDialog?.dismiss()
        confirmDialog = null
        _binding = null
        super.onDestroyView()
    }

    // ---------------------------------------------------------------- Préférences

    private fun applyPrefs() {
        val p = PreferencesManager(requireContext()).read()
        listOf(binding.editNom, binding.editPrenom, binding.editTelephone).forEach {
            it.setTextSize(TypedValue.COMPLEX_UNIT_SP, p.tailleSp)
        }
        binding.editNom.setTextColor(p.couleur)
        binding.editPrenom.setTextColor(p.couleur)
    }

    // ---------------------------------------------------------------- Lecture

    private fun loadContact() {
        viewLifecycleOwner.lifecycleScope.launch {
            val c = viewModel.getContact(contactId)
            if (c == null) {
                IntentActions.toast(requireContext(), R.string.err_contact_introuvable)
                parentFragmentManager.popBackStack()
                return@launch
            }
            binding.editNom.setText(c.nom)
            binding.editPrenom.setText(c.prenom)
            binding.editTelephone.setText(c.telephone)
            binding.editEmail.setText(c.email.orEmpty())
            binding.editLatitude.setText(c.latitude?.toString().orEmpty())
            binding.editLongitude.setText(c.longitude?.toString().orEmpty())
            originalPhoto = c.photoUri
            photoPath = c.photoUri
            dataLoaded = true
            showPhoto()
        }
    }

    private fun showPhoto() {
        val model: Any = photoPath?.let { File(it) } ?: R.drawable.ic_person
        Glide.with(binding.imgPhoto).load(model).circleCrop()
            .placeholder(R.drawable.ic_person).error(R.drawable.ic_person).into(binding.imgPhoto)
    }

    // ---------------------------------------------------------------- Enregistrement

    private fun onSave() {
        val contact = buildAndValidate() ?: return
        binding.btnEnregistrer.isEnabled = false      // évite le double clic pendant l'écriture
        viewModel.save(contact)
    }

    /** Valide tous les champs ; renvoie null (et affiche les erreurs) si rien ne doit être écrit en base. */
    private fun buildAndValidate(): Contact? {
        val b = binding
        listOf(b.tilNom, b.tilPrenom, b.tilTelephone, b.tilEmail, b.tilLatitude, b.tilLongitude)
            .forEach { it.error = null }

        val nom = b.editNom.text?.toString().orEmpty().trim()
        val prenom = b.editPrenom.text?.toString().orEmpty().trim()
        val tel = b.editTelephone.text?.toString().orEmpty().trim()
        val email = b.editEmail.text?.toString().orEmpty().trim()
        var ok = true

        if (nom.isEmpty()) { b.tilNom.error = getString(R.string.err_nom_requis); ok = false }
        if (prenom.isEmpty()) { b.tilPrenom.error = getString(R.string.err_prenom_requis); ok = false }
        if (tel.isEmpty() || tel.none { it.isDigit() }) {
            b.tilTelephone.error = getString(R.string.err_tel_requis); ok = false
        }
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            b.tilEmail.error = getString(R.string.err_email_invalide); ok = false
        }

        // GPS : les deux champs vides = pas de position (NULL, NULL) ; sinon les deux doivent être valides.
        val latTxt = b.editLatitude.text?.toString().orEmpty().trim()
        val lonTxt = b.editLongitude.text?.toString().orEmpty().trim()
        var lat: Double? = null
        var lon: Double? = null
        if (latTxt.isNotEmpty() || lonTxt.isNotEmpty()) {
            lat = parseCoordinate(latTxt, b.tilLatitude, -90.0, 90.0, R.string.err_lat_limites)
            lon = parseCoordinate(lonTxt, b.tilLongitude, -180.0, 180.0, R.string.err_lon_limites)
            if (lat == null || lon == null) ok = false
        }
        if (!ok) return null

        return Contact(
            id = contactId,
            nom = nom,
            prenom = prenom,
            telephone = tel,
            email = email.ifEmpty { null },
            photoUri = photoPath,
            latitude = lat,
            longitude = lon
        )
    }

    private fun parseCoordinate(
        text: String, layout: TextInputLayout, min: Double, max: Double, limitsError: Int
    ): Double? {
        if (text.isEmpty()) {
            layout.error = getString(R.string.err_gps_incomplet); return null
        }
        val value = text.replace(',', '.').toDoubleOrNull()
        if (value == null || !value.isFinite()) {       // "abc", "NaN", "Infinity"
            layout.error = getString(R.string.err_gps_nombre); return null
        }
        if (value < min || value > max) {
            layout.error = getString(limitsError); return null
        }
        return value
    }

    private fun onCancel() {
        // Une photo choisie mais non enregistrée est supprimée du stockage interne.
        if (photoPath != originalPhoto) PhotoStorage.deleteIfOwned(requireContext(), photoPath)
        parentFragmentManager.popBackStack()
    }

    // ---------------------------------------------------------------- Suppression

    private fun confirmDelete() {
        val name = "${binding.editPrenom.text} ${binding.editNom.text}".trim()
        confirmDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.supprimer_titre)
            .setMessage(getString(R.string.supprimer_message, name))
            .setPositiveButton(R.string.supprimer) { _, _ -> viewModel.delete(contactId) }
            .setNegativeButton(R.string.annuler, null)
            .show()
    }

    // ---------------------------------------------------------------- Photo

    private fun onGalleryPicked(uri: Uri) {
        val appContext = requireContext().applicationContext
        lifecycleScope.launch {
            val path = withContext(Dispatchers.IO) { PhotoStorage.copyFromUri(appContext, uri) }
            if (path == null) {
                IntentActions.toast(appContext, R.string.err_photo_import)
            } else {
                setNewPhoto(path)
            }
        }
    }

    private fun launchCamera() {
        val file = PhotoStorage.newFile(requireContext())
        pendingCameraPath = file.absolutePath
        try {
            takePicture.launch(PhotoStorage.uriFor(requireContext(), file))
        } catch (e: ActivityNotFoundException) {
            file.delete()
            pendingCameraPath = null
            IntentActions.toast(requireContext(), R.string.err_pas_camera)
        }
    }

    private fun onCameraResult(success: Boolean) {
        val path = pendingCameraPath ?: return
        pendingCameraPath = null
        val file = File(path)
        if (success && file.length() > 0) setNewPhoto(path) else file.delete()   // annulé : aucun plantage
    }

    private fun setNewPhoto(path: String) {
        // Remplace une éventuelle photo temporaire non enregistrée.
        if (photoPath != originalPhoto) context?.let { PhotoStorage.deleteIfOwned(it, photoPath) }
        photoPath = path
        if (_binding != null) showPhoto()
    }

    companion object {
        private const val ARG_ID = "contact_id"
        private const val KEY_PHOTO = "photo_path"
        private const val KEY_ORIGINAL_PHOTO = "original_photo"
        private const val KEY_LOADED = "data_loaded"
        private const val KEY_PENDING_CAMERA = "pending_camera"

        /** @param id _id du contact, ou 0 pour créer un nouveau contact. */
        fun newInstance(id: Long) = DetailContactFragment().apply {
            arguments = Bundle().apply { putLong(ARG_ID, id) }
        }
    }
}
