package com.lk.ava.activities

import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.media.AudioManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import androidx.preference.PreferenceManager
import co.revely.gradient.RevelyGradient
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.getInputLayout
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.customListAdapter
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import com.lk.ava.R
import com.lk.ava.adapters.SongAdapter
import com.lk.ava.fragments.Home
import com.lk.ava.models.Song
import com.lk.ava.models.SongState
import com.lk.ava.services.MusicService
import com.lk.ava.utils.Constants
import com.lk.ava.utils.MusicClientActivity
import com.lk.ava.utils.Shared
import kotlinx.android.synthetic.main.player.artist_name
import kotlinx.android.synthetic.main.player.complete_position
import kotlinx.android.synthetic.main.player.next_song
import kotlinx.android.synthetic.main.player.player_bg
import kotlinx.android.synthetic.main.player.player_center_icon
import kotlinx.android.synthetic.main.player.player_current_position
import kotlinx.android.synthetic.main.player.player_seekbar
import kotlinx.android.synthetic.main.player.previous_song
import kotlinx.android.synthetic.main.player.repeat_button
import kotlinx.android.synthetic.main.player.shuffle_button
import kotlinx.android.synthetic.main.player.song_name
import kotlinx.android.synthetic.main.player320.*
import kotlinx.android.synthetic.main.player410.*
import kotlinx.android.synthetic.main.player410.album_art
import kotlinx.android.synthetic.main.player410.img_albart
import kotlinx.android.synthetic.main.player410.note_ph
import kotlinx.android.synthetic.main.player410.player_down_arrow
import kotlinx.android.synthetic.main.player410.player_queue
import kotlinx.android.synthetic.main.player410.top_controls
import kotlinx.android.synthetic.main.player410.youtubeProgressbar
import kotlinx.coroutines.*
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * The Player UI activity.
 */

@ExperimentalCoroutinesApi
class Player : MusicClientActivity() {
    private lateinit var serviceConn: ServiceConnection
    private var mService: MusicService? = null
    private lateinit var timer: Timer
    private var playing = SongState.paused
    private var scheduled = false
    private var onShuffle = false
    private var onRepeat = false
    private var volume_mute=false
    private var gone=false
    private lateinit var audioManager: AudioManager
    private var lastSelectedColor = 0x00fbfbfb

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


        val ydpi = DisplayMetrics().run {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(this)
            this.ydpi
        }

        when (PreferenceManager.getDefaultSharedPreferences(this@Player)
            .getString("player_layout_key", "Default")) {
            "Tiny" -> setContentView(R.layout.player220)

            "Small" -> setContentView(R.layout.player320)

            "Normal" -> setContentView(R.layout.player400)

            "Large" -> setContentView(R.layout.player410)

            "Massive" -> setContentView(R.layout.playermassive)

            else -> {
                if (ydpi > 400) setContentView(R.layout.player410)
                else if (ydpi >= 395) setContentView(R.layout.player400)
                else if (ydpi < 395 && ydpi > 230) setContentView(R.layout.player320)
                else setContentView(R.layout.player220)
            }
        }

        /**
         * Since we don't use fitSystemWindows, we need to manually
         * apply window insets as margin.
         */
        if (bottom_cast != null) {
            bottom_cast.setOnApplyWindowInsetsListener { _, insets ->
                val kek = bottom_cast.layoutParams as ViewGroup.MarginLayoutParams
                @Suppress("DEPRECATION")
                kek.setMargins(0, 0, 0, insets.systemWindowInsetBottom)
                insets
            }
        }

        if (top_controls != null) {
            top_controls.setOnApplyWindowInsetsListener { _, insets ->
                val kek = top_controls.layoutParams as ViewGroup.MarginLayoutParams
                @Suppress("DEPRECATION")
                kek.setMargins(0, insets.systemWindowInsetTop, 0, 0)
                insets
            }
        }

        player_down_arrow?.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_out_bottom_fast,R.anim.slide_in_bottom_fast)
        }

        shuffle_button.setOnClickListener {
            if (onShuffle) {
                mService?.setShuffleRepeat(shuffle = false, repeat = onRepeat)
            } else {
                mService?.setShuffleRepeat(shuffle = true, repeat = onRepeat)
            }
        }

        player_center_icon.setOnClickListener {
            launch(Dispatchers.Default) {
                if (playing == SongState.playing) mService?.setPlayPause(SongState.paused)
                else mService?.setPlayPause(SongState.playing)
            }
        }

        repeat_button.setOnClickListener {
            if (onRepeat) {
                mService?.setShuffleRepeat(shuffle = onShuffle, repeat = false)
                DrawableCompat.setTint(repeat_button.drawable, Color.parseColor("#80fbfbfb"))
            } else {
                mService?.setShuffleRepeat(shuffle = onShuffle, repeat = true)
                DrawableCompat.setTint(repeat_button.drawable, Color.parseColor("#805e92f3"))
            }
        }

        next_song.setOnClickListener {
            mService?.setNextPrevious(next = true)
        }

        previous_song.setOnClickListener {
            mService?.setNextPrevious(next = false)
        }

        setBgColor(0x002171)

        player_seekbar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if(mService != null) {
                    val ms = mService as MusicService
                    ms.seekTo(seekBar.progress)
                    player_seekbar.progress = ms.getMediaPlayer().currentPosition
                    player_current_position.text = getDurationFromMs(player_seekbar.progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
        })

        AudioManager.STREAM_MUSIC
        audioManager= getSystemService(Context.AUDIO_SERVICE) as AudioManager
        volume_seekbar.max=audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        volume_seekbar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))

        volume_seekbar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,progress,0)
            }
        })

        volume_layout1.visibility=View.INVISIBLE
        volume_layout.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                when(p1?.action){
                    MotionEvent.ACTION_DOWN ->if (!gone){
                        volume_layout1.visibility=View.INVISIBLE
                        gone=true
                    }
                    else{
                        volume_layout1.visibility=View.VISIBLE
                        gone=false
                    }

                }
                return p0?.onTouchEvent(p1)?:true
            }
        })

        volume_off.setOnClickListener {

                volume_off.setImageDrawable(volume_off.drawable.run { this.setTint(Color.GRAY); this })
                volume_on.setImageDrawable(volume_on.drawable.run { this.setTint(Color.WHITE); this })
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,0,0)

        }

        volume_on.setOnClickListener {
                volume_off.setImageDrawable(volume_off.drawable.run { this.setTint(Color.WHITE); this })
                volume_on.setImageDrawable(volume_on.drawable.run { this.setTint(Color.GRAY); this })
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,volume_seekbar.progress,0)
        }


        album_art.setOnClickListener {
            if(mService != null) {
                val ms = mService as MusicService
                if (ms.getPlayQueue()[ms.getCurrentIndex()].filePath.contains("emulated/0/"))
                    MaterialDialog(this@Player).show {
                        cornerRadius(20f)
                        title(text = this@Player.getString(R.string.enter_song))
                        input(this@Player.getString(R.string.song_ex)) { _, charSequence ->
                            updateAlbumArt(charSequence.toString(), true)
                        }
                        getInputLayout().boxBackgroundColor = Color.parseColor("#000000")
                    }
            }
        }


        album_art.setOnLongClickListener {
            if(mService != null) {
                val ms = mService as MusicService
                if(ms.getPlayQueue()[ms.getCurrentIndex()].filePath.contains("emulated/0/"))
                MaterialDialog(this@Player).show {
                    cornerRadius(20f)
                    title(text = this@Player.getString(R.string.enter_song))
                    input(prefill = ms.getPlayQueue()[ms.getCurrentIndex()].name) { _, charSequence ->
                        updateAlbumArt(charSequence.toString(), true)
                    }
                    getInputLayout().boxBackgroundColor = Color.parseColor("#000000")
                }
            }
            true
        }

        song_name.setOnClickListener {
            if(mService != null) {
                val mService = this.mService as MusicService
                val current = mService.getPlayQueue()[mService.getCurrentIndex()]
                if (current.filePath.contains("emulated/0/")) {
                    MaterialDialog(this@Player).show {
                        title(text = this@Player.getString(R.string.enter_new_song))
                        input(this@Player.getString(R.string.song_ex2)) { _, charSequence ->
                            val ext = current.filePath.run {
                                this.substring(this.lastIndexOf(".") + 1)
                            }
                            when (val rc = FFmpeg.execute(
                                "-i " +
                                        "\"${current.filePath}\" -y -c copy " +
                                        "-metadata title=\"$charSequence\" " +
                                        "-metadata artist=\"${current.artist}\"" +
                                        " \"${current.filePath}.new.$ext\""
                            )) {
                                Config.RETURN_CODE_SUCCESS -> {
                                    File(current.filePath).delete()
                                    File(current.filePath + ".new.$ext").renameTo(File(current.filePath))
                                    if (current.isLocal) {
                                        /* update media store for the song in question */
                                        MediaScannerConnection.scanFile(this@Player,
                                            arrayOf(current.filePath), null,
                                            object :
                                                MediaScannerConnection.MediaScannerConnectionClient {
                                                override fun onMediaScannerConnected() {}

                                                override fun onScanCompleted(
                                                    path: String?,
                                                    uri: Uri?
                                                ) {
                                                    changeMetadata(
                                                        name = charSequence.toString(),
                                                        artist = current.artist
                                                    )
                                                }
                                            })
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
                        }
                        getInputField().setText(current.name)
                        getInputLayout().boxBackgroundColor = Color.parseColor("#000000")
                    }
                }
            }
        }

        artist_name.setOnClickListener {
            if(mService != null) {
                val mService = this.mService as MusicService
                val current = mService.getPlayQueue()[mService.getCurrentIndex()]
                if (current.filePath.contains("emulated/0/")) {
                    MaterialDialog(this@Player).show {
                        title(text = this@Player.getString(R.string.enter_new_art))
                        input(this@Player.getString(R.string.art_ex)) { _, charSequence ->
                            val ext = current.filePath.run {
                                this.substring(this.lastIndexOf(".") + 1)
                            }
                            when (val rc = FFmpeg.execute(
                                "-i " +
                                        "\"${current.filePath}\" -c copy " +
                                        "-metadata title=\"${current.name}\" " +
                                        "-metadata artist=\"$charSequence\"" +
                                        " \"${current.filePath}.new.$ext\""
                            )) {
                                Config.RETURN_CODE_SUCCESS -> {
                                    File(current.filePath).delete()
                                    File(current.filePath + ".new.$ext").renameTo(File(current.filePath))
                                    if (current.isLocal) {
                                        /* update media store for the song in question */
                                        MediaScannerConnection.scanFile(this@Player,
                                            arrayOf(current.filePath), null,
                                            object :
                                                MediaScannerConnection.MediaScannerConnectionClient {
                                                override fun onMediaScannerConnected() {}

                                                override fun onScanCompleted(
                                                    path: String?,
                                                    uri: Uri?
                                                ) {
                                                    changeMetadata(
                                                        name = current.name,
                                                        artist = charSequence.toString()
                                                    )
                                                }
                                            })
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
                        }
                        getInputField().setText(current.artist)
                        getInputLayout().boxBackgroundColor = Color.parseColor("#000000")
                    }
                }
            }
        }

        (shuffle_button.parent as View).post {
            val rect = Rect().also {
                shuffle_button.getHitRect(it)
                it.top -= 200
                it.left -= 200
                it.bottom += 200
                it.right += 100
            }

            (shuffle_button.parent as View).touchDelegate = TouchDelegate(rect, shuffle_button)
        }

        (repeat_button.parent as View).post {
            val rect = Rect().also {
                repeat_button.getHitRect(it)
                it.top -= 200
                it.left -= 100
                it.bottom += 200
                it.right += 200
            }

            (repeat_button.parent as View).touchDelegate = TouchDelegate(rect, repeat_button)
        }

        (previous_song.parent as View).post {
            val rect = Rect().also {
                previous_song.getHitRect(it)
                it.top -= 200
                it.left -= 100
                it.bottom += 200
                it.right += 100
            }

            (previous_song.parent as View).touchDelegate = TouchDelegate(rect, previous_song)
        }

        (next_song.parent as View).post {
            val rect = Rect().also {
                next_song.getHitRect(it)
                it.top -= 200
                it.left -= 100
                it.bottom += 200
                it.right += 100
            }

            (next_song.parent as View).touchDelegate = TouchDelegate(rect, next_song)
        }

        (player_center_icon.parent as View).post {
            val rect = Rect().also {
                player_center_icon.getHitRect(it)
                it.top -= 200
                it.left -= 50
                it.bottom += 200
                it.right += 50
            }

            (player_center_icon.parent as View).touchDelegate =
                TouchDelegate(rect, player_center_icon)
        }

        player_queue?.setOnClickListener {
            if(mService != null) {
                val mService = this.mService as MusicService
                MaterialDialog(this@Player, BottomSheet()).show {
                    customListAdapter(
                        SongAdapter(
                            ArrayList(
                                mService.getPlayQueue().subList(
                                    mService.getCurrentIndex(),
                                    mService.getPlayQueue().size
                                )
                            ),
                            mServiceFromPlayer = mService
                        )
                    )
                }
            }
        }


        serviceConn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                mService = (service as MusicService.MusicBinder).getService()
                onBindDone()
            }

            override fun onServiceDisconnected(name: ComponentName) {}
        }
        youtubeProgressbar?.visibility = View.GONE
    }

    private fun onBindDone() {
        if (mService!!.getMediaPlayer().isPlaying) player_center_icon.setImageDrawable(
            ContextCompat.getDrawable(
                this@Player,
                R.drawable.pause
            )
        )
        else player_center_icon.setImageDrawable(
            ContextCompat.getDrawable(
                this@Player,
                R.drawable.play
            )
        )

        playing = if(mService!!.getMediaPlayer().isPlaying) SongState.playing else SongState.paused
        playPauseEvent(playing)

        songChangeEvent()
    }

    private fun bindEvent() {
        if (Shared.serviceRunning(MusicService::class.java, this@Player))
            bindService(
                Intent(this@Player, MusicService::class.java),
                serviceConn,
                0
            )
    }

    override fun onPause() {
        super.onPause()
        if (scheduled) {
            scheduled = false
            timer.cancel()
            timer.purge()
        }
    }

    private fun startSeekbarUpdates() {
        if (!scheduled) {
            scheduled = true
            timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    if(mService != null) {
                        val mService = mService as MusicService
                        launch(Dispatchers.Main) {
                            val songPosition = mService.getMediaPlayer().currentPosition
                            player_seekbar.progress = songPosition
                            player_current_position.text = getDurationFromMs(songPosition)
                        }
                    }
                }
            }, 0, 1000)
        }
    }

    /**
     * @param rawColor a color in Integer form (hex).
     * Tints the control buttons to rawColor.
     */
    private fun tintControls(rawColor: Int) {
        val color = if (Shared.isColorDark(rawColor))
            ColorUtils.blendARGB(rawColor, Color.WHITE, 0.9F)
        else
            ColorUtils.blendARGB(rawColor, Color.WHITE, 0.3F)

        lastSelectedColor = color

        previous_song.run {
            this.setImageDrawable(this.drawable.run { this.setTint(Color.WHITE); this })
        }


        next_song.run {
            this.setImageDrawable(this.drawable.run { this.setTint(Color.WHITE); this })
        }
    }

    /**
     * @param color the color to set on the background as a gradient.
     * @param lightVibrantColor the color to set on the seekbar, usually
     * derived from the album art.
     */
    private fun setBgColor(
        color: Int,
        lightVibrantColor: Int? = null,
        palette: Palette? = null
    ) {
        RevelyGradient
            .linear()
            .colors(
                intArrayOf(
                    color,
                    Color.parseColor("#212121")
                )
            )
            .angle(90f)
            .alpha(0.76f)
            .onBackgroundOf(player_bg)

        if (Shared.isColorDark(color)) {
            player_down_arrow.setImageDrawable(
                ContextCompat.getDrawable(
                    this@Player,
                    R.drawable.down_arrow
                )
            )
            player_queue.setImageDrawable(
                ContextCompat.getDrawable(
                    this@Player,
                    R.drawable.pl_playlist
                )
            )
            if (lightVibrantColor != null) {
                if ((lightVibrantColor and 0xff000000.toInt()) shr 24 == 0) {
                    val newTitleColor = palette?.darkVibrantSwatch?.titleTextColor
                        ?: palette?.dominantSwatch?.titleTextColor
                    player_seekbar.progressDrawable.setTint(newTitleColor!!)
                    player_seekbar.thumb.setTint(newTitleColor)
                    tintControls(0x002171)
                } else {
                    player_seekbar.progressDrawable.setTint(lightVibrantColor)
                    player_seekbar.thumb.setTint(lightVibrantColor)
                    tintControls(lightVibrantColor)
                }
            }
        } else {
            player_down_arrow.setImageDrawable(
                ContextCompat.getDrawable(
                    this@Player,
                    R.drawable.down_arrow
                )
            )
            player_queue.setImageDrawable(
                ContextCompat.getDrawable(
                    this@Player,
                    R.drawable.pl_playlist
                )
            )
            player_seekbar.progressDrawable.setTint(color)
            player_seekbar.thumb.setTint(color)
            tintControls(color)
        }
    }

    private fun playPauseEvent(ss: SongState) {
        playing = ss
        launch(Dispatchers.Main) {
            if (playing == SongState.playing) player_center_icon.setImageDrawable(
                ContextCompat.getDrawable(
                    this@Player,
                    R.drawable.pause
                )
            )
            else player_center_icon.setImageDrawable(
                ContextCompat.getDrawable(
                    this@Player,
                    R.drawable.play
                )
            )

            tintControls(lastSelectedColor)
        }

        if (playing == SongState.playing) {
            startSeekbarUpdates()
        } else {
            if (scheduled) {
                scheduled = false
                timer.cancel()
                timer.purge()
            }
        }
    }

    private fun updateAlbumArt(customSongName: String? = null, forceDeezer: Boolean = false) {
        if(mService != null) {
            val mService = mService as MusicService
            /* set helper variables */
            img_albart.visibility = View.GONE
            note_ph.visibility = View.VISIBLE
            var didGetArt = false
            val current = mService.getPlayQueue()[mService.getCurrentIndex()]
            val img = File(
                Constants.ableSongDir.absolutePath + "/album_art",
                File(current.filePath).nameWithoutExtension
            )
            val cacheImg = File(
                Constants.ableSongDir.absolutePath + "/cache",
                "sCache" + Shared.getIdFromLink(MusicService.playQueue[MusicService.currentIndex].youtubeLink)
            )

            launch(Dispatchers.IO) {
                /*
                Check priority:
                1) Album art from metadata (if the song is a local song)
                2) Album art from disk (if the song is not a local song)
                3) Album art from Deezer (regardless of song being local or not)

                in case (3), if the song is local, the album art should be added
                to the song metadata, and if not, it should be stored in the Able
                album art folder.
                */


                /* (1) Check albumart in song metadata (if the song is a local song) */
                if (current.isLocal && !forceDeezer) {
                    Log.i("INFO>", "Fetching from metadata")
                    try {
                        note_ph.visibility = View.GONE
                        val sArtworkUri =
                            Uri.parse("content://media/external/audio/albumart")

                        Shared.bmp = Glide
                            .with(this@Player)
                            .load(ContentUris.withAppendedId(sArtworkUri, current.albumId))
                            .signature(ObjectKey("player"))
                            .submit()
                            .get().toBitmap()

                        launch(Dispatchers.Main) {
                            img_albart.setImageBitmap(Shared.bmp)
                            img_albart.visibility = View.VISIBLE
                            note_ph.visibility = View.GONE
                            Palette.from(Shared.getSharedBitmap()).generate {
                                setBgColor(
                                    it?.getDominantColor(0x002171) ?: 0x002171,
                                    it?.getLightMutedColor(0x002171) ?: 0x002171,
                                    it
                                )
                            }
                        }
                        didGetArt = true
                    } catch (e: java.lang.Exception) {
                        didGetArt = false
                    }
                }

                /* (2) Album art from disk (if the song is not a local song) */
                if (!didGetArt && !forceDeezer) {
                    if (!current.isLocal) {
                        Log.i("INFO>", "Fetching from AVA folder")
                        val imgToLoad = if (img.exists()) img else cacheImg
                        if (imgToLoad.exists()) {
                            launch(Dispatchers.Main) {
                                img_albart.visibility = View.VISIBLE
                                note_ph.visibility = View.GONE
                                Glide.with(this@Player)
                                    .load(imgToLoad)
                                    .centerCrop()
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .skipMemoryCache(true)
                                    .into(img_albart)
                                try {
                                    Shared.bmp?.recycle()
                                    Shared.bmp = BitmapFactory.decodeFile(imgToLoad.absolutePath)
                                    Palette.from(Shared.getSharedBitmap()).generate {
                                        setBgColor(
                                            it?.getDominantColor(0x002171) ?: 0x002171,
                                            it?.getLightMutedColor(0x002171) ?: 0x002171,
                                            it // causes transparent bar
                                        )

                                        Shared.clearBitmap()
                                    }
                                } catch (e: java.lang.Exception) {
                                    e.printStackTrace()
                                }
                            }
                            didGetArt = true
                        }
                    }
                }

                /* (3) Album art from Deezer (regardless of song being local or not) */
                if (!didGetArt && Shared.isInternetConnected(this@Player)) {
                    Log.i("INFO>", "Fetching from Deezer")
                    val albumArtRequest = if (customSongName == null) {
                        Request.Builder()
                            .url(Constants.DEEZER_API + current.name)
                            .get()
                            .addHeader("x-rapidapi-host", "deezerdevs-deezer.p.rapidapi.com")
                            .addHeader("x-rapidapi-key", Constants.RAPID_API_KEY)
                            .cacheControl(CacheControl.FORCE_NETWORK)
                            .build()
                    } else {
                        Request.Builder()
                            .url(Constants.DEEZER_API + customSongName)
                            .get()
                            .addHeader("x-rapidapi-host", "deezerdevs-deezer.p.rapidapi.com")
                            .addHeader("x-rapidapi-key", Constants.RAPID_API_KEY)
                            .build()
                    }

                    val response = OkHttpClient().newCall(albumArtRequest).execute().body

                    try {
                        if (response != null) {
                            val json = JSONObject(response.string()).getJSONArray("data")
                                .getJSONObject(0).getJSONObject("album")
                            val imgLink = json.getString("cover_big")
                            val albumName = json.getString("title")

                            try {
                                Shared.bmp = Glide
                                    .with(this@Player)
                                    .load(imgLink)
                                    .centerCrop()
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .skipMemoryCache(true)
                                    .submit()
                                    .get().toBitmap()

                                Palette.from(Shared.getSharedBitmap()).generate {
                                    setBgColor(
                                        it?.getDominantColor(0x002171) ?: 0x002171,
                                        it?.getLightVibrantColor(0x002171) ?: 0x002171,
                                        it
                                    )
                                }

                                if (img.exists()) img.delete()
                                Shared.saveAlbumArtToDisk(Shared.getSharedBitmap(), img)

                                launch(Dispatchers.Main) {
                                    img_albart.setImageBitmap(Shared.getSharedBitmap())
                                    img_albart.visibility = View.VISIBLE
                                    note_ph.visibility = View.GONE
                                    if (mService.getMediaPlayer().isPlaying) {
                                        mService.showNotification(
                                            mService.generateAction(
                                                R.drawable.notif_pause,
                                                "Pause",
                                                "ACTION_PAUSE"
                                            ), Shared.getSharedBitmap()
                                        )
                                    } else {
                                        mService.showNotification(
                                            mService.generateAction(
                                                R.drawable.notif_play,
                                                "Play",
                                                "ACTION_PLAY"
                                            ), Shared.getSharedBitmap()
                                        )
                                    }
                                }
                                Shared.addThumbnails(
                                    current.filePath,
                                    albumName,
                                    this@Player
                                )
                                didGetArt = true
                                launch(Dispatchers.Main) {
                                    Home.songAdapter?.notifyItemChanged(mService.getCurrentIndex())
                                }
                            } catch (e: Exception) {
                                didGetArt = false
                                Log.e("ERR>", e.toString())
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ERR>", e.toString())
                    }
                }

                if (!didGetArt) {
                    launch(Dispatchers.Main) {
                        img_albart.visibility = View.GONE
                        note_ph.visibility = View.VISIBLE
                        setBgColor(0x002171)
                        player_seekbar.progressDrawable.setTint(
                            ContextCompat.getColor(
                                this@Player,
                                R.color.thatAccent
                            )
                        )
                        player_seekbar.thumb.setTint(
                            ContextCompat.getColor(
                                this@Player,
                                R.color.colorPrimary
                            )
                        )
                        tintControls(0x002171)
                    }
                }
            }
        }
    }

    private fun changeMetadata(name: String, artist: String) {
        if(mService != null) {
            val mService = mService as MusicService
            launch(Dispatchers.Main) {
                song_name.text = name
                artist_name.text = artist
            }

            if (mService.getMediaPlayer().isPlaying) {
                mService.showNotification(
                    mService.generateAction(
                        R.drawable.notif_pause,
                        getString(R.string.pause),
                        "ACTION_PAUSE"
                    ), nameOverride = name, artistOverride = artist
                )
            } else {
                mService.showNotification(
                    mService.generateAction(
                        R.drawable.notif_play,
                        getString(R.string.play),
                        "ACTION_PLAY"
                    ), nameOverride = name, artistOverride = artist
                )
            }

            // TODO MusicService.registeredClients.forEach { it.queueChanged() }
        }
    }

    private fun songChangeEvent() {
        if(mService != null) {
            val mService = mService as MusicService
            updateAlbumArt()

            val duration = mService.getMediaPlayer().duration
            player_seekbar.max = duration
            complete_position.text = getDurationFromMs(duration)

            val song = mService.getPlayQueue()[mService.getCurrentIndex()]
            song_name.text = song.name
            artist_name.text = song.artist
            player_seekbar.progress = mService.getMediaPlayer().currentPosition
        }
    }

    private fun getDurationFromMs(durtn: Int): String {
        val duration = durtn.toLong()
        val seconds = (TimeUnit.MILLISECONDS.toSeconds(duration) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)))

        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)

        var ret = "${minutes}:"
        if (seconds < 10) ret += "0"
        return ret + seconds
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase!!))
    }

    override fun onResume() {
        super.onResume()
        if(mService == null)
            bindEvent()
        else
            playPauseEvent(if((mService as MusicService)
                    .getMediaPlayer().isPlaying) SongState.playing else SongState.paused)
    }

    override fun onBackPressed() {

        super.onBackPressed()
        Handler(Looper.getMainLooper()).postDelayed({
            if (!this.isDestroyed) Glide.with(this@Player).clear(img_albart)
        }, 300)
        finish()
        overridePendingTransition(R.anim.slide_out_bottom_fast,R.anim.slide_in_bottom_fast)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!this.isDestroyed)
            Glide.with(this@Player).clear(img_albart)
    }

    override fun playStateChanged(state: SongState) {
        playPauseEvent(state)
    }

    override fun songChanged() = runOnUiThread {
        songChangeEvent()
    }

    override fun durationChanged(duration: Int) {
        launch(Dispatchers.Main) {
            player_seekbar.max = duration
            complete_position.text = getDurationFromMs(duration)
        }
    }

    override fun isExiting() {
        finish()
    }

    override fun queueChanged(arrayList: ArrayList<Song>) {}

    override fun shuffleRepeatChanged(onShuffle: Boolean, onRepeat: Boolean) {
        launch(Dispatchers.Main) {
            if (onShuffle)
                DrawableCompat.setTint(shuffle_button.drawable, Color.parseColor("#805e92f3"))
            else
                DrawableCompat.setTint(shuffle_button.drawable, Color.parseColor("#fbfbfb"))

            if (onRepeat)
                DrawableCompat.setTint(repeat_button.drawable, Color.parseColor("#805e92f3"))
            else
                DrawableCompat.setTint(repeat_button.drawable, Color.parseColor("#fbfbfb"))

            (this@Player).onShuffle = onShuffle
            (this@Player).onRepeat = onRepeat
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode==KeyEvent.KEYCODE_VOLUME_UP){
            if ((volume_seekbar.progress+1)>(volume_seekbar.max)){
                volume_seekbar.setProgress(volume_seekbar.max)
            }
            else{
                volume_seekbar.setProgress(volume_seekbar.progress+1)
            }

        }
        else if (keyCode==KeyEvent.KEYCODE_VOLUME_DOWN){
            if ((volume_seekbar.progress-1)<0){
                volume_seekbar.setProgress(0)
            }
            else{
                volume_seekbar.setProgress(volume_seekbar.progress-1)
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    override fun indexChanged(index: Int) {}

    override fun isLoading(doLoad: Boolean) {}

    override fun spotifyImportChange(starting: Boolean) {}

    override fun serviceStarted() {
        bindEvent()
    }
}