package com.lk.ava.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import com.lk.ava.R
import com.lk.ava.adapters.PlaybumAdapter
import com.lk.ava.models.Song
import com.lk.ava.services.MusicService
import com.lk.ava.utils.Shared
import kotlinx.android.synthetic.main.albumplaylist.*
import kotlinx.android.synthetic.main.search.loading_view
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import org.schabi.newpipe.extractor.ServiceList.YouTube
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.lang.ref.WeakReference

/**
 * The activity that shows up when a user taps on an album or playlist
 * from the search results.
 */
@ExperimentalCoroutinesApi
class AlbumPlaylist : AppCompatActivity(), CoroutineScope {
    private lateinit var serviceConn: ServiceConnection
    private val resultArray = ArrayList<Song>()
    var mService: MutableStateFlow<MusicService?> = MutableStateFlow(null)
    var isBound = false

    override val coroutineContext = Dispatchers.Main + SupervisorJob()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ViewPump.init(
            ViewPump.builder()
                .addInterceptor(
                    CalligraphyInterceptor(
                        CalligraphyConfig.Builder()
                            .setDefaultFontPath("fonts/inter.otf")
                            .setFontAttrId(R.attr.fontPath)
                            .build()
                    )
                )
                .build()
        )
        setContentView(R.layout.albumplaylist)
        loading_view.progress = 0.3080229f
        loading_view.playAnimation()
        val name = intent.getStringExtra("name") ?: ""
        val artist = intent.getStringExtra("artist") ?: ""
        val art = intent.getStringExtra("art") ?: ""
        val link = intent.getStringExtra("link") ?: ""

        serviceConn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                mService.value = (service as MusicService.MusicBinder).getService()
                isBound = true
            }

            override fun onServiceDisconnected(name: ComponentName) {
                isBound = false
            }
        }

        playbum_name.text = name
        playbum_artist.text = artist
        Glide
            .with(this@AlbumPlaylist)
            .load(art)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(playbum_art)

        playbum_play.setOnClickListener {
            var freshStart = false
            if (!Shared.serviceRunning(MusicService::class.java, this)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(Intent(this, MusicService::class.java))
                } else {
                    startService(Intent(this, MusicService::class.java))
                }

                bindEvent()
                freshStart = true
            }

            launch(Dispatchers.Default) {
                val playSong = fun(){
                    val mService = mService.value!!
                    mService.setQueue(resultArray)
                    mService.setIndex(0)
                    if(freshStart)
                        MusicService.registeredClients.forEach(MusicService.MusicClient::serviceStarted)
                }

                if(mService.value != null) playSong()
                else {
                    mService.collect {
                        if(it != null) {
                            playSong()
                        }
                    }
                }
            }
        }

        launch(Dispatchers.IO) {
            val plExtractor = YouTube.getPlaylistExtractor(link)
            plExtractor.fetchPage()
            for (song in plExtractor.initialPage.items) {
                val ex = song as StreamInfoItem
                if (song.thumbnailUrl.contains("ytimg")) {
                    val songId = Shared.getIdFromLink(ex.url)
                    song.thumbnailUrl = "https://i.ytimg.com/vi/$songId/maxresdefault.jpg"
                }
                resultArray.add(
                    Song(
                        name = ex.name,
                        artist = ex.uploaderName,
                        youtubeLink = ex.url,
                        ytmThumbnail = song.thumbnailUrl
                    )
                )
            }

            launch(Dispatchers.Main) {
                ap_rv.adapter =
                    PlaybumAdapter(resultArray, WeakReference(this@AlbumPlaylist), "Song")
                ap_rv.layoutManager = LinearLayoutManager(this@AlbumPlaylist)
                loading_view.visibility = View.GONE
                loading_view.pauseAnimation()
                ap_rv.alpha = 0f
                ap_rv.visibility = View.VISIBLE
                ap_rv.animate().alpha(1f).duration = 200
                sr_pr.alpha = 0f
                sr_pr.visibility = View.VISIBLE
                sr_pr.animate().alpha(1f).duration = 200
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase!!))
    }

    private fun bindEvent() {
        if (Shared.serviceRunning(MusicService::class.java, this)) {
            try {
                this.also {
                    it.bindService(Intent(it, MusicService::class.java), serviceConn, 0)
                }
            } catch (e: Exception) {
                Log.e("ERR>", e.toString())
            }
        }
    }

    /**
     * invoked when an item is pressed in the recyclerview.
     */
    fun itemPressed(array: ArrayList<Song>, index: Int) {
        var freshStart = false
        if (!Shared.serviceRunning(MusicService::class.java, this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(Intent(this, MusicService::class.java))
            } else {
                startService(Intent(this, MusicService::class.java))
            }

            bindEvent()
            freshStart = true
        }

        launch(Dispatchers.Default) {
            val playSong = fun() {
                val mService = mService.value!!
                mService.setQueue(array)
                mService.setIndex(index)
                if(freshStart)
                    MusicService.registeredClients.forEach(MusicService.MusicClient::serviceStarted)
            }

            if(mService.value != null) playSong()
            else {
                mService.collect {
                    if(it != null){
                        playSong()
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Handler(Looper.getMainLooper()).postDelayed({
            if (!this.isDestroyed)
                Glide.with(this).clear(playbum_art)
        }, 300)
        Glide.with(this).clear(playbum_art)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!this.isDestroyed)
            Glide.with(this).clear(playbum_art)
    }

    override fun onResume() {
        super.onResume()
        if(mService.value == null)
            bindEvent()
    }
}