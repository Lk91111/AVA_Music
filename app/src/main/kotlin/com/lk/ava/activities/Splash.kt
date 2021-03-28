package com.lk.ava.activities

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lk.ava.R
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * The splash screen.
 */
@ExperimentalCoroutinesApi
class Splash: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.statusBarColor= ContextCompat.getColor(this, R.color.dark_bg)
        startActivity(Intent(this@Splash, MainActivity::class.java))
        finish()
    }
}