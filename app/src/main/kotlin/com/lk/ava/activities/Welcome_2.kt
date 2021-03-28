package com.lk.ava.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.github.dreierf.materialintroscreen.MaterialIntroActivity
import io.github.dreierf.materialintroscreen.SlideFragment
import io.github.dreierf.materialintroscreen.SlideFragmentBuilder
import com.lk.ava.R
import kotlinx.android.synthetic.main.player320.*
import kotlinx.android.synthetic.main.welcome_2.*
import kotlinx.android.synthetic.main.welcome_2.start
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * The welcome screen that shows up when the required permissions haven't been granted.
 */
@ExperimentalCoroutinesApi
class Welcome_2: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("working","Welcome screen")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.welcome_2)
        supportActionBar?.hide()
        window?.statusBarColor= ContextCompat.getColor(this, R.color.dark_bg)
        val handler = Handler()
        handler.postDelayed({
            startActivity(Intent(this@Welcome_2, Splash::class.java))
            finish()
        },10)

        start.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                when(p1?.action){
                    MotionEvent.ACTION_DOWN ->
                        startActivity(Intent(this@Welcome_2, MainActivity::class.java))

                }
                return p0?.onTouchEvent(p1)?:true
            }
        })
    }
}

