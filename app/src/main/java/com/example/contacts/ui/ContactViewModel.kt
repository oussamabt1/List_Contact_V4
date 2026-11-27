package com.example.contacts.ui

import android.app.Application
import android.database.SQLException
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.contacts.R
import com.example.contacts.data.Contact
import com.example.contacts.data.ContactDao
import com.example.contacts.data.ContactDbHelper
import com.example.contacts.util.PhotoStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Événements ponctuels (consommés une seule fois) envoyés aux fragments. */
sealed interface UiEvent {
    data class Message(@StringRes val resId: Int) : UiEvent
    data object Saved : UiEvent
    data object Deleted : UiEvent
}

private enum class Outcome { OK, NOT_FOUND, INSERT_FAILED, CONSTRAINT }

/**
 * ViewModel partagé (portée Activity) : survit aux rotations. Tous les accès SQLite
 * s'exécutent sur Dispatchers.IO ; le thread principal ne fait que publier les résultats.
 */
class ContactViewModel(app: Application) : AndroidViewModel(app) {

    private val appContext = app.applicationContext
    private val dao = ContactDao(ContactDbHelper.getInstance(app))

    private val _contacts = MutableLiveData<List<Contact>>(emptyList())
    val contacts: LiveData<List<Contact>> = _contacts

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    var query: String = ""
        private set
    private var loadJob: Job? = null

    /** Filtrage au fil de la saisie, avec un léger anti-rebond. */
    fun setQuery(text: String) {
        if (text == query) return
        query = text
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            delay(150)
            fetch()
        }
    }

    /** Recharge la liste depuis la base (appelé à chaque affichage de la liste). */
    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { fetch() }
    }

    private suspend fun fetch() {
        try {
            _contacts.value = withContext(Dispatchers.IO) { dao.search(query) }
        } catch (e: SQLException) {
            _events.send(UiEvent.Message(R.string.err_chargement))
        }
    }

    suspend fun getContact(id: Long): Contact? = withContext(Dispatchers.IO) {
        try {
            dao.getById(id)
        } catch (e: SQLException) {
            null
        }
    }

    /** Création (id == 0) ou mise à jour. La liste est rechargée avant l'événement Saved. */
    fun save(contact: Contact) {
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                try {
                    if (contact.id == 0L) {
                        if (dao.insert(contact) == -1L) Outcome.INSERT_FAILED else Outcome.OK
                    } else {
                        val previous = dao.getById(contact.id)
                        if (dao.update(contact) == 0) {
                            Outcome.NOT_FOUND
                        } else {
                            if (previous?.photoUri != contact.photoUri) {
                                PhotoStorage.deleteIfOwned(appContext, previous?.photoUri)
                            }
                            Outcome.OK
                        }
                    }
                } catch (e: SQLException) {
                    Outcome.CONSTRAINT
                }
            }
            when (outcome) {
                Outcome.OK -> {
                    fetch()
                    _events.send(UiEvent.Saved)
                }
                Outcome.NOT_FOUND -> _events.send(UiEvent.Message(R.string.err_contact_introuvable))
                Outcome.INSERT_FAILED -> _events.send(UiEvent.Message(R.string.err_insertion))
                Outcome.CONSTRAINT -> _events.send(UiEvent.Message(R.string.err_contrainte))
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                try {
                    val previous = dao.getById(id)
                    if (dao.delete(id) == 0) {
                        Outcome.NOT_FOUND
                    } else {
                        PhotoStorage.deleteIfOwned(appContext, previous?.photoUri)
                        Outcome.OK
                    }
                } catch (e: SQLException) {
                    Outcome.CONSTRAINT
                }
            }
            if (outcome == Outcome.OK) {
                fetch()
                _events.send(UiEvent.Deleted)
            } else {
                _events.send(UiEvent.Message(R.string.err_suppression))
            }
        }
    }
}
