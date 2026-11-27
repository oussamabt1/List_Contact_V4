package com.example.contacts.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.contacts.R
import com.example.contacts.data.PreferencesManager
import com.example.contacts.databinding.FragmentListeContactsBinding
import com.example.contacts.util.IntentActions
import kotlinx.coroutines.launch

class ListeContactsFragment : Fragment() {

    private var _binding: FragmentListeContactsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactViewModel by activityViewModels()
    private lateinit var prefsManager: PreferencesManager
    private lateinit var adapter: ContactAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentListeContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        prefsManager = PreferencesManager(requireContext())

        adapter = ContactAdapter(
            onClick = { navigateTo(DetailContactFragment.newInstance(it.id)) },
            onCall = { IntentActions.dial(requireContext(), it.telephone) }
        )
        adapter.updatePrefs(prefsManager.read())
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        binding.fabAjouter.setOnClickListener { navigateTo(DetailContactFragment.newInstance(0L)) }

        // Le texte du filtre est restauré automatiquement par la vue (rotation) ;
        // le ViewModel conserve la requête courante.
        binding.editFiltre.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }

        viewModel.contacts.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvVide.isVisible = list.isEmpty()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    if (event is UiEvent.Message) IntentActions.toast(requireContext(), event.resId)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.setTitle(R.string.titre_liste)
        // Préférences modifiées dans Paramètres : appliquées immédiatement au retour.
        adapter.updatePrefs(prefsManager.read())
        // Liste rechargée depuis la base à chaque affichage.
        viewModel.refresh()
    }

    override fun onDestroyView() {
        binding.recycler.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
