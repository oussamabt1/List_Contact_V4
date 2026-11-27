package com.example.contacts.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.contacts.R
import com.example.contacts.data.Contact
import com.example.contacts.data.isValidPosition
import com.example.contacts.databinding.FragmentCarteContactBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

/** Affiche la position du contact (chargé par son _id) sur Google Maps. */
class CarteContactFragment : Fragment() {

    private var _binding: FragmentCarteContactBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ContactViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentCarteContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val id = requireArguments().getLong(ARG_ID)
        viewLifecycleOwner.lifecycleScope.launch {
            val contact = viewModel.getContact(id)
            when {
                contact == null -> showMessage(R.string.err_contact_introuvable)
                !isValidPosition(contact.latitude, contact.longitude) ->
                    showMessage(R.string.carte_pas_de_position)
                else -> showMap(contact)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.setTitle(R.string.titre_carte)
    }

    private fun showMessage(resId: Int) {
        binding.map.isVisible = false
        binding.tvMessage.setText(resId)
        binding.tvMessage.isVisible = true
    }

    private fun showMap(contact: Contact) {
        binding.tvMessage.isVisible = false
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
            ?: return showMessage(R.string.err_carte)
        mapFragment.getMapAsync { map ->
            val position = LatLng(contact.latitude!!, contact.longitude!!)
            map.addMarker(
                MarkerOptions().position(position).title("${contact.prenom} ${contact.nom}")
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 14f))
            map.uiSettings.isZoomControlsEnabled = true
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ID = "contact_id"
        fun newInstance(id: Long) = CarteContactFragment().apply {
            arguments = Bundle().apply { putLong(ARG_ID, id) }
        }
    }
}
