package com.lk.ava.activities

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import io.github.dreierf.materialintroscreen.MaterialIntroActivity
import io.github.dreierf.materialintroscreen.SlideFragment
import io.github.dreierf.materialintroscreen.SlideFragmentBuilder
import com.lk.ava.R
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * The welcome screen that shows up when the required permissions haven't been granted.
 */
@ExperimentalCoroutinesApi
class Welcome: MaterialIntroActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("working","Welcome screen")
        super.onCreate(savedInstanceState)
        addSlide(
            SlideFragmentBuilder()
                .backgroundColor(R.color.dark_bg)
                .buttonsColor(R.color.colorAccent)
                .neededPermissions(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
                .image(R.drawable.track)
                .title(getString(R.string.welcome).format("AVAMusic"))
                .description(getString(R.string.storage_perm))
                .build()
        )

        addSlide(
            SlideFragmentBuilder()
                .backgroundColor(R.color.dark_bg)
                .buttonsColor(R.color.colorAccent)
                .image(R.drawable.welcome_icon  )
                .title("Thanks for installing!")
                .description("AVA is free and open source.")
                .build()
        )

    }
}

