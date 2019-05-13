package com.fuxy.cookeasy.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import androidx.preference.PreferenceManager
import com.fuxy.cookeasy.R
import com.fuxy.cookeasy.fragment.RecipesFragment
import com.fuxy.cookeasy.fragment.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    companion object {
        @JvmField
        val ADD_RECIPE_REQUEST = 301
    }

    private var bottomNavigationView: BottomNavigationView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

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
        var result = false
        when(item.itemId) {
            R.id.navigation_recipes -> {
                result = loadFragment(RecipesFragment(), RecipesFragment.FRAGMENT_NAME)
            }
            R.id.navigation_add_recipe -> {
                val intent = Intent(this, EditRecipeActivity::class.java)
                intent.putExtra(EditRecipeActivity.EXTRA_MODE, EditRecipeActivity.Mode.ADDING.name)
                startActivityForResult(intent, ADD_RECIPE_REQUEST)
                result = false
            }
            R.id.navigation_settings -> {
                result = loadFragment(SettingsFragment(), SettingsFragment.FRAGMENT_NAME)
            }
        }

        return result
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_RECIPE_REQUEST && resultCode == Activity.RESULT_OK) {
            loadFragment(RecipesFragment(), RecipesFragment.FRAGMENT_NAME)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            if (currentFocus is EditText?) {
                val outRect = Rect()
                currentFocus?.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    currentFocus?.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}
