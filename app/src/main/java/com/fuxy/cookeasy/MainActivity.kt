package com.fuxy.cookeasy

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import com.fuxy.cookeasy.fragment.AddRecipeFragment
import com.fuxy.cookeasy.fragment.RecipesFragment
import com.fuxy.cookeasy.fragment.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private var bottomNavigationView: BottomNavigationView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bnv_navigation)
        bottomNavigationView?.setOnNavigationItemSelectedListener(this)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fl_fragment_container, RecipesFragment())
            .commit()
    }

    private fun loadFragment(fragment: Fragment?, name: String?): Boolean {
        if (fragment != null) {
            val transaction = supportFragmentManager
                .beginTransaction()
                .replace(R.id.fl_fragment_container, fragment)

            val count = supportFragmentManager.backStackEntryCount

            if (name != RecipesFragment.FRAGMENT_NAME) {
                transaction.addToBackStack(name)
            }

            transaction.commit()

            supportFragmentManager.addOnBackStackChangedListener(object : FragmentManager.OnBackStackChangedListener {
                override fun onBackStackChanged() {
                    if (supportFragmentManager.backStackEntryCount <= count) {
                        supportFragmentManager.popBackStack(name, POP_BACK_STACK_INCLUSIVE)
                        supportFragmentManager.removeOnBackStackChangedListener(this)
                        bottomNavigationView?.menu?.getItem(0)?.isChecked = true
                    }
                }
            })

            return true
        }
        return false
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        var fragment: Fragment? = null
        var name: String? = null

        when(item.itemId) {
            R.id.navigation_recipes -> {
                fragment = RecipesFragment()
                name = RecipesFragment.FRAGMENT_NAME
            }
            R.id.navigation_add_recipe -> {
                fragment = AddRecipeFragment()
                name = AddRecipeFragment.FRAGMENT_NAME
            }
            R.id.navigation_settings -> {
                fragment = SettingsFragment()
                name = SettingsFragment.FRAGMENT_NAME

            }
        }

        return loadFragment(fragment, name)
    }
}
