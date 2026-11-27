package com.example.contacts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.contacts.data.Contact
import com.example.contacts.data.ContactDao
import com.example.contacts.data.ContactDbHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Tests instrumentés du DAO (à lancer sur émulateur/appareil : ./gradlew connectedAndroidTest). */
@RunWith(AndroidJUnit4::class)
class ContactDaoTest {

    private lateinit var dao: ContactDao
    private val created = mutableListOf<Long>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dao = ContactDao(ContactDbHelper.getInstance(context))
    }

    @After
    fun tearDown() {
        created.forEach { dao.delete(it) }
    }

    private fun insert(c: Contact): Long = dao.insert(c).also { assertTrue(it > 0); created += it }

    @Test
    fun demoContactsArePresentAndSortedIgnoringCase() {
        val all = dao.getAll()
        assertTrue(all.size >= 5)
        val names = all.map { it.nom.lowercase() }
        assertEquals(names.sorted(), names)
    }

    @Test
    fun apostropheInNameAndInFilterDoesNotBreakSql() {
        val id = insert(Contact(nom = "L'Ariana", prenom = "Test", telephone = "+216 11 111 111"))
        assertTrue(dao.search("L'A").any { it.id == id })
        assertTrue(dao.search("'; DROP TABLE contacts; --").isEmpty())
        assertNotNull(dao.getById(id))              // la table existe toujours
    }

    @Test
    fun percentAndUnderscoreAreLiteralInSearch() {
        val id = insert(Contact(nom = "Zz100%", prenom = "Pct", telephone = "0000"))
        assertTrue(dao.search("100%").any { it.id == id })
        assertTrue(dao.search("%").all { "%" in it.nom + it.prenom + it.telephone })
    }

    @Test
    fun updateReturnsRowCountAndPersistsNullGps() {
        val id = insert(Contact(nom = "Upd", prenom = "A", telephone = "1", latitude = 36.8, longitude = 10.1))
        val rows = dao.update(Contact(id = id, nom = "Upd2", prenom = "A", telephone = "1"))
        assertEquals(1, rows)
        val c = dao.getById(id)!!
        assertEquals("Upd2", c.nom)
        assertNull(c.latitude)
        assertNull(c.longitude)
        assertEquals(0, dao.update(Contact(id = -42, nom = "X", prenom = "Y", telephone = "1")))
    }

    @Test
    fun deleteReturnsRowCount() {
        val id = insert(Contact(nom = "Del", prenom = "B", telephone = "2"))
        assertEquals(1, dao.delete(id))
        assertNull(dao.getById(id))
        assertEquals(0, dao.delete(id))
    }
}
