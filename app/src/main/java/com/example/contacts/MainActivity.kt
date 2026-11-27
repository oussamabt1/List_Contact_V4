package com.example.contacts

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.fragment.app.commit
import com.example.contacts.databinding.ActivityMainBinding
import com.example.contacts.ui.ListeContactsFragment
import com.example.contacts.ui.ParametresFragment
import com.example.contacts.ui.navigateTo

/** Activité unique : barre d'application + FragmentContainerView. */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Uniquement au premier lancement : après une rotation, le FragmentManager
        // restaure lui-même les fragments (pas de doublon).
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.fragment_container, ListeContactsFragment())
            }
        }

        supportFragmentManager.addOnBackStackChangedListener { updateUpButton() }
        updateUpButton()

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId != R.id.action_parametres) return false
                val current = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (current !is ParametresFragment) supportFragmentManager.navigateTo(ParametresFragment())
                return true
            }
        })
    }

    private fun updateUpButton() {
        supportActionBar?.setDisplayHomeAsUpEnabled(supportFragmentManager.backStackEntryCount > 0)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
