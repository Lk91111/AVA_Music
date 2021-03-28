package com.lk.ava.utils

import androidx.appcompat.app.AppCompatActivity
import com.lk.ava.services.MusicService
import kotlinx.coroutines.*

@ExperimentalCoroutinesApi
abstract class MusicClientActivity: AppCompatActivity(), CoroutineScope, MusicService.MusicClient {

    override val coroutineContext = Dispatchers.Main + SupervisorJob()

    override fun onResume() {
        super.onResume()
        launch(Dispatchers.Default) {
            MusicService.registerClient(this@MusicClientActivity)
        }
    }

    override fun onPause() {
        super.onPause()
        launch(Dispatchers.Default) {
            MusicService.unregisterClient(this@MusicClientActivity)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancelChildren()
        MusicService.unregisterClient(this@MusicClientActivity)
    }
}