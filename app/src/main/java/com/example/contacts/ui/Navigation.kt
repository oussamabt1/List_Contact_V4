package com.example.contacts.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.example.contacts.R

/** replace() + addToBackStack() dans le FragmentContainerView de MainActivity. */
fun FragmentManager.navigateTo(fragment: Fragment) {
    commit {
        setReorderingAllowed(true)
        replace(R.id.fragment_container, fragment)
        addToBackStack(null)
    }
}

fun Fragment.navigateTo(fragment: Fragment) = parentFragmentManager.navigateTo(fragment)
