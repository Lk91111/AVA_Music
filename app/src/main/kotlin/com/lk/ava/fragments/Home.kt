package com.lk.ava.fragments

import android.app.Activity
import android.content.*
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.revely.gradient.RevelyGradient
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.list.customListAdapter
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import com.koonat.easyfont.TextView
import com.makeramen.roundedimageview.RoundedImageView
import com.lk.ava.R
import com.lk.ava.activities.MainActivity
import com.lk.ava.activities.Player
import com.lk.ava.activities.Settings
import com.lk.ava.activities.Welcome
import com.lk.ava.adapters.SongAdapter
import com.lk.ava.models.CacheStatus
import com.lk.ava.models.Format
import com.lk.ava.models.Song
import com.lk.ava.models.SongState
import com.lk.ava.services.MusicService
import com.lk.ava.services.MusicService.Companion.currentIndex
import com.lk.ava.utils.Constants
import com.lk.ava.utils.MusicClientActivity
import com.lk.ava.utils.Shared
import com.lk.ava.utils.SwipeController
import kotlinx.android.synthetic.main.home.*
import kotlinx.android.synthetic.main.player410.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList

/**
 * The first fragment. Shows a list of songs present on the user's device.
 */
@ExperimentalCoroutinesApi
class Home : Fragment(), CoroutineScope, MusicService.MusicClient {
    private lateinit var okClient: OkHttpClient
    private lateinit var serviceConn: ServiceConnection

    private var songList = ArrayList<Song>()
    private var songId = "temp"
    var currentIndex = -1
    var isBound = false
    var onShuffle = false
    var mService: MutableStateFlow<MusicService?> = MutableStateFlow(null)

    override val coroutineContext = Dispatchers.Main + SupervisorJob()

    companion object {
        var songAdapter: SongAdapter? = null
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancelChildren()
        MusicService.unregisterClient(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(
            R.layout.home,
            container, false
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val songs = view.findViewById<RecyclerView>(R.id.songs)
        var random_song_img: RoundedImageView? = view.findViewById(R.id.random_song_img)
        var random_song_name= view.findViewById<TextView>(R.id.random_song_name)
        var player_queue=view.findViewById<TextView>(R.id.player_queue)
        var card_random=view.findViewById<CardView>(R.id.card_random)
        okClient = OkHttpClient()

        random_song_name.typeface =
            Typeface.createFromAsset(random_song_name.context.assets, "fonts/cursive_2.ttf")
        RevelyGradient
            .linear()
            .colors(
                intArrayOf(
                    Color.parseColor("#7F7FD5"),
                    Color.parseColor("#86A8E7"),
                    Color.parseColor("#91EAE4")
                )
            )
            .on(view.findViewById<TextView>(R.id.able_header))

        view.findViewById<TextView>(R.id.able_header).typeface =
            Typeface.createFromAsset(view.findViewById<TextView>(R.id.able_header).context.assets, "fonts/title.ttf")

        settings.setOnClickListener {
            startActivity(Intent(requireContext(), Settings::class.java))

            (requireContext() as Activity).overridePendingTransition(R.anim.right_to_left_50,R.anim.static_layout)
        }

        serviceConn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                mService.value = (service as MusicService.MusicBinder).getService()
                isBound = true
            }

            override fun onServiceDisconnected(name: ComponentName) {
                isBound = false
            }
        }

        bindEvent()

        if (songList.isEmpty()) {
            songList = Shared.getSongList(Constants.ableSongDir)
            songList.addAll(Shared.getLocalSongs(requireContext()))
            if (songList.isNotEmpty()) songList = ArrayList(songList.sortedBy {
                it.name.toUpperCase(
                    Locale.getDefault()
                )
            })

        }



// random start
        if (songList.size!=0){
            val lenght = songList.size
            val randomm=(0..lenght).random()
            val current= songList[randomm]
            val wr=WeakReference(this@Home)
            random_song_name.text=current.name

            random_song_img.run {
                if (this != null) {
                    File(
                        Constants.ableSongDir.absolutePath + "/album_art",
                        File(current.filePath).nameWithoutExtension
                    ).also {
                        when {
                            it.exists() -> Glide.with(getContext())
                                .load(it)
                                .dontAnimate()
                                .signature(ObjectKey("home"))
                                .into(this)

                            else -> {
                                try {
                                    val sArtworkUri =
                                        Uri.parse("content://media/external/audio/albumart")
                                    val albumArtUri =
                                        ContentUris.withAppendedId(sArtworkUri, current.albumId)
                                    Glide
                                        .with(getContext())
                                        .load(albumArtUri)
                                        .dontAnimate()
                                        .signature(ObjectKey("home"))
                                        .into(this)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
            }


            if (currentIndex >= 0 && currentIndex < songList.size && songList.size != 0) {
                if (current.placeholder) random_song_name.setTextColor(Color.parseColor("#66bb6a"))
                else {
                    val service = wr?.get()?.mService?.value
                    if(service != null) {
                        if (current.filePath == service.getPlayQueue()[service.getCurrentIndex()].filePath) {
                            random_song_name.setTextColor(Color.parseColor("#5e92f3"))
                        } else random_song_name.setTextColor(Color.parseColor("#fbfbfb"))
                    }
                }
            }

            //on click random

            val mServiceFromPlayer :MusicService? = null

            card_random.setOnClickListener {
                var freshStart = false
                if (!current.placeholder) {
                    if (!Shared.serviceRunning(MusicService::class.java, card_random.context)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            card_random.context.startForegroundService(
                                Intent(
                                    card_random.context,
                                    MusicService::class.java
                                )
                            )
                        } else {
                            card_random.context.startService(
                                Intent(
                                    card_random.context,
                                    MusicService::class.java
                                )
                            )
                        }
                        wr?.get()!!.bindEvent()
                        freshStart = true
                    }

                    launch(Dispatchers.Default) {
                        var mService: MutableStateFlow<MusicService?> = MutableStateFlow(mServiceFromPlayer)
                        if(wr?.get()!=null)
                            mService = wr.get()!!.mService

                        val playSong = fun(){
                            if (currentIndex != randomm) {
                                currentIndex = randomm
                                launch(Dispatchers.Default) {
                                    if (onShuffle) {
                                        mService.value?.addToQueue(current)
                                        mService.value?.setNextPrevious(next = true)
                                    } else {
                                        currentIndex = randomm
                                        mService.value?.setQueue(songList)
                                        mService.value?.setIndex(randomm)
                                    }

                                    if(freshStart)
                                        MusicService.registeredClients.forEach(MusicService.MusicClient::serviceStarted)
                                }
                            }
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
            }
        }

   //random end

        //see all
        player_queue?.setOnClickListener {
                context?.let { it1 ->
                    MaterialDialog(it1, BottomSheet()).show {
                        customListAdapter(
                            SongAdapter(
                                songList, WeakReference(this@Home), false
                                )
                            )

                    }
                }

        }

        songAdapter = SongAdapter(songList, WeakReference(this@Home), true)
        val lam = LinearLayoutManager(requireContext(),LinearLayoutManager.HORIZONTAL,false)
        lam.initialPrefetchItemCount = 12
        lam.isItemPrefetchEnabled = true
        val itemTouchHelper = ItemTouchHelper(SwipeController(context, "Home", mService))
        songs.adapter = songAdapter
        songs.layoutManager = lam
        itemTouchHelper.attachToRecyclerView(songs)
    }

    override fun onResume() {
        super.onResume()
        MusicService.registerClient(this)
    }

    override fun onPause() {
        super.onPause()
        MusicService.unregisterClient(this)
    }

    fun bindEvent() {
        if (Shared.serviceRunning(MusicService::class.java, requireContext())) {
            try {
                requireContext().bindService(
                    Intent(
                        requireContext(),
                        MusicService::class.java
                    ), serviceConn, 0
                )
            } catch (e: Exception) {
                Log.e("ERR>", e.toString())
            }
        }
    }

    /**
     * A helper function that streams a song
     * This is different from the implementation
     * in MusicService, since this separates
     * the caching proxy implementation
     * from the overly simple method used
     * in the service. Could be unified in
     * the future.
     *
     * @param song the song to stream.
     * @param toCache whether we want to save this to the disk.
     */
    fun streamAudio(song: Song, toCache: Boolean) {
        var freshStart = false
        if (isAdded) {
            if (!Shared.serviceRunning(MusicService::class.java, requireContext())) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireActivity().startForegroundService(
                        Intent(
                            requireContext(),
                            MusicService::class.java
                        )
                    )
                } else {
                    requireActivity().startService(
                        Intent(
                            requireContext(),
                            MusicService::class.java
                        )
                    )
                }

                bindEvent()
                freshStart = true
            }
        } else
            Log.e("ERR> ", "Context Lost")

        launch(Dispatchers.IO) {
            val playSong = fun(){
                mService.value?.setQueue(
                    arrayListOf(
                        Song(
                            name = getString(R.string.loading),
                            artist = ""
                        )
                    )
                )
                mService.value?.setCurrentIndex(0)
                mService.value?.showNotif()

                val tmp: StreamInfo?

                try {
                    tmp = StreamInfo.getInfo(song.youtubeLink)
                } catch (e: Exception) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Something went wrong!", Toast.LENGTH_SHORT)
                            .show()
                        MusicService.registeredClients.forEach { it.isLoading(false) }
                    }
                    Log.e("ERR>", e.toString())
                    return
                }

                val streamInfo = tmp ?: StreamInfo.getInfo(song.youtubeLink)
                val stream = streamInfo.audioStreams.run { this[size - 1] }

                val url = stream.url
                val bitrate = stream.averageBitrate
                val ext = stream.getFormat().suffix
                songId = Shared.getIdFromLink(song.youtubeLink)

                File(Constants.ableSongDir, "$songId.tmp.webm").run {
                    if (exists()) delete()
                }

                if (song.ytmThumbnail.isNotBlank()) {
                    Glide.with(requireContext())
                        .asBitmap()
                        .load(song.ytmThumbnail)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .signature(ObjectKey("save"))
                        .skipMemoryCache(true)
                        .listener(object : RequestListener<Bitmap> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Bitmap>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                return false
                            }

                            override fun onResourceReady(
                                resource: Bitmap?,
                                model: Any?,
                                target: Target<Bitmap>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                if (resource != null) {
                                    if (toCache) {
                                        if (cacheMusic(song, url, ext, bitrate))
                                            Shared.saveAlbumArtToDisk(
                                                resource,
                                                File(Constants.albumArtDir, songId)
                                            )
                                    } else {
                                        song.filePath = url

                                        Shared.saveStreamingAlbumArt(
                                            resource,
                                            Shared.getIdFromLink(song.youtubeLink)
                                        )
                                    }

                                    mService.value?.setQueue(arrayListOf(song))
                                    mService.value?.setIndex(0)
                                    MusicService.registeredClients.forEach { it.isLoading(false) }
                                    if(freshStart)
                                        MusicService.registeredClients.forEach(MusicService.MusicClient::serviceStarted)
                                }
                                return false
                            }
                        }).submit()
                }
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

    fun cacheMusic(song: Song, songUrl: String, ext: String, bitrate: Int): Boolean {
        if (song.filePath.endsWith(ext)) // edge case when the song is already saved
            return false

        song.cacheStatus = CacheStatus.STARTED

        song.filePath = "caching"
        song.streamMutexes = arrayOf(Mutex(), Mutex())

        GlobalScope.launch(Dispatchers.IO) {
            val req = Request.Builder().url(songUrl).build()
            val resp = okClient.newCall(req).execute()
            val body = resp.body!!
            val iStream = BufferedInputStream(body.byteStream())

            song.internalStream = ByteArray(body.contentLength().toInt())
            song.streams = arrayOf(ByteArray(body.contentLength().toInt()), ByteArray(body.contentLength().toInt()))

            var read: Int
            val data = ByteArray(1024)

            var off = 0
            while (iStream.read(data).let { read = it; read != -1 }) {
                for (i in 0.until(read)) {
                    song.internalStream[i+off] = data[i]
                }
                off += read

                while (song.streamMutexes[0].isLocked and song.streamMutexes[1].isLocked) {
                    delay(50)
                }

                val streamNum = if (song.streamMutexes[0].isLocked) 1 else 0
                song.streamMutexes[streamNum].withLock {
                    song.streams[streamNum] = song.internalStream
                }

                song.streamProg = (off*100) / body.contentLength().toInt()
            }

            iStream.close()


            val tempFile = File(
                Constants.ableSongDir.absolutePath
                        + "/" + songId + ".tmp.$ext"
            )
            tempFile.createNewFile()
            FileOutputStream(tempFile).write(song.internalStream)

            val format =
                if (PreferenceManager.getDefaultSharedPreferences(
                        context
                    )
                        .getString(
                            "format_key",
                            "webm"
                        ) == "mp3"
                ) Format.MODE_MP3
                else Format.MODE_WEBM

            var command = "-i " +
                    "\"${tempFile.absolutePath}\" -c copy " +
                    "-metadata title=\"${song.name}\" " +
                    "-metadata artist=\"${song.artist}\" -y "

            if (format == Format.MODE_MP3 || Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
                command += "-vn -ab ${bitrate}k -c:a mp3 -ar 44100 "

            command += "\"${
                tempFile.absolutePath.replace(
                    "tmp.$ext",
                    ""
                )
            }"

            command += if (format == Format.MODE_MP3) "mp3\"" else "$ext\""

            when (val rc = FFmpeg.execute(command)) {
                Config.RETURN_CODE_SUCCESS -> {
                    tempFile.delete()
                    launch(Dispatchers.Main) {
                        songList =
                            Shared.getSongList(Constants.ableSongDir)
                        songList.addAll(
                            Shared.getLocalSongs(
                                requireContext()
                            )
                        )
                        songList =
                            ArrayList(songList.sortedBy {
                                it.name.toUpperCase(
                                    Locale.getDefault()
                                )
                            })
                        launch(Dispatchers.Main) {
                            songAdapter?.update(songList)
                        }
                        songAdapter?.update(songList)
                        songAdapter?.notifyDataSetChanged()
                    }
                }
                Config.RETURN_CODE_CANCEL -> {
                    Log.e(
                        "ERR>",
                        "Command execution cancelled by user."
                    )
                }
                else -> {
                    Log.e(
                        "ERR>",
                        String.format(
                            "Command execution failed with rc=%d and the output below.",
                            rc
                        )
                    )
                }
            }

            song.filePath = tempFile.absolutePath.replace(
                "tmp.$ext",
                ext
            )
            song.cacheStatus = CacheStatus.NULL

            // Destroy mutexes and stream buffers (I don't trust the GC here for some reason)
            song.streamMutexes = arrayOf()
            song.internalStream = ByteArray(0)
            song.streams = arrayOf()
        }

         return true
    }

    fun updateSongList() {
        songList = Shared.getSongList(Constants.ableSongDir)
        if (context != null) songList.addAll(Shared.getLocalSongs(context as Context))
        songList = ArrayList(songList.sortedBy {
            it.name.toUpperCase(
                Locale.getDefault()
            )
        })
        launch(Dispatchers.Main) {
            songAdapter?.update(songList)
        }
    }

    override fun playStateChanged(state: SongState) {}

    override fun songChanged() {}

    override fun durationChanged(duration: Int) {}

    override fun isExiting() {}

    override fun queueChanged(arrayList: ArrayList<Song>) {}

    override fun shuffleRepeatChanged(onShuffle: Boolean, onRepeat: Boolean) {
        this.onShuffle = onShuffle
    }


    override fun indexChanged(index: Int) {}

    override fun isLoading(doLoad: Boolean) {}

    override fun spotifyImportChange(starting: Boolean) {}

    override fun serviceStarted() {
        bindEvent()
    }
}