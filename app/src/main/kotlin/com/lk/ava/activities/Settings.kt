package com.lk.ava.activities

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.takisoft.preferencex.PreferenceFragmentCompat
import com.lk.ava.R
import com.lk.ava.utils.Shared
import kotlinx.android.synthetic.main.home.*
import kotlinx.android.synthetic.main.player410.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * The settings page.
 */
@ExperimentalCoroutinesApi
class Settings: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        setContentView(R.layout.settings_view)
        title = getString(R.string.settings)
        supportActionBar?.hide()
        window?.statusBarColor=ContextCompat.getColor(this, R.color.dark_bg)
        if (Shared.isFirstOpen) Shared.isFirstOpen = false
        supportFragmentManager.beginTransaction()
            .replace(
                R.id.content,
                SettingsFragment()
            )
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return true
    }

}

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, null)
    }

}