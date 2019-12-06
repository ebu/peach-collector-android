package ch.ebu.peachdemokotlin

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.SeekBar
import android.widget.Toast
import ch.ebu.peachcollector.*
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var runnable:Runnable
    private var handler: Handler = Handler()
    private var pause:Boolean = false

    private val component = EventContextComponent()
    private lateinit var eventContext: EventContext
    private lateinit var eventProps: EventProperties

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PeachCollector.isUnitTesting = true
        PeachCollector.shouldCollectAnonymousEvents = true
        PeachCollector.init(application)

        val publisher = Publisher("zzebu00000000017")
        PeachCollector.addPublisher(publisher, "DefaultPublisher")


        component.name = "MainPlayer"
        component.type = "media_player"
        component.version = "2.3.0"

        eventContext = EventContext.mediaContext("reco00", null, null, component)

        eventProps = EventProperties()
        eventProps.audioMode = Constant.Media.AudioMode.Normal
        eventProps.playbackPosition = 0


        recommendationBtn.setOnClickListener{
            Event.sendRecommendationHit("reco00", "media0", 1, null, null, null)
        }



        var url  = "https://lyssna-cdn.sr.se/Isidor/EREG/sr_varmland/2019/05/37_rekordmanga_kvinnor_antag_21a9b7f_a192.m4a"



        // Start the media player
        playBtn.setOnClickListener{
            if(pause){
                mediaPlayer.seekTo(mediaPlayer.currentPosition)
                mediaPlayer.start()
                pause = false
                Toast.makeText(this,"media playing",Toast.LENGTH_SHORT).show()

                eventProps.playbackPosition = mediaPlayer.currentSeconds
                Event.sendMediaPause("media00", eventProps, eventContext, null)
                eventProps.previousPlaybackPosition = eventProps.playbackPosition
            }else{
                mediaPlayer = MediaPlayer()
                mediaPlayer.setDataSource(url)
                mediaPlayer.prepare()
                mediaPlayer.start()

                Toast.makeText(this,"media playing",Toast.LENGTH_SHORT).show()

                eventProps.playbackPosition = mediaPlayer.currentSeconds
                Event.sendMediaPlay("media00", eventProps, eventContext, null)
                eventProps.previousPlaybackPosition = eventProps.playbackPosition

            }
            initializeSeekBar()
            playBtn.isEnabled = false
            pauseBtn.isEnabled = true
            stopBtn.isEnabled = true

            mediaPlayer.setOnCompletionListener {
                playBtn.isEnabled = true
                pauseBtn.isEnabled = false
                stopBtn.isEnabled = false
                Toast.makeText(this,"end",Toast.LENGTH_SHORT).show()
                eventProps.playbackPosition = mediaPlayer.currentSeconds
                Event.sendMediaEnd("media00", eventProps, eventContext, null)
                eventProps.previousPlaybackPosition = eventProps.playbackPosition
            }
        }
        // Pause the media player
        pauseBtn.setOnClickListener {
            if(mediaPlayer.isPlaying){
                mediaPlayer.pause()
                pause = true
                playBtn.isEnabled = true
                pauseBtn.isEnabled = false
                stopBtn.isEnabled = true
                Toast.makeText(this,"media pause",Toast.LENGTH_SHORT).show()

                eventProps.playbackPosition = mediaPlayer.currentSeconds
                Event.sendMediaPause("media00", eventProps, eventContext, null)
                eventProps.previousPlaybackPosition = eventProps.playbackPosition
            }
        }
        // Stop the media player
        stopBtn.setOnClickListener{
            if(mediaPlayer.isPlaying || pause.equals(true)){
                eventProps.playbackPosition = mediaPlayer.currentSeconds
                pause = false
                seek_bar.setProgress(0)
                mediaPlayer.stop()
                mediaPlayer.reset()
                mediaPlayer.release()
                handler.removeCallbacks(runnable)

                playBtn.isEnabled = true
                pauseBtn.isEnabled = false
                stopBtn.isEnabled = false
                tv_pass.text = ""
                tv_due.text = ""
                Toast.makeText(this,"media stop",Toast.LENGTH_SHORT).show()

                Event.sendMediaStop("media00", eventProps, eventContext, null)
                eventProps.previousPlaybackPosition = eventProps.playbackPosition
            }
        }
        // Seek bar change listener
        seek_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if (b) {
                    eventProps.previousPlaybackPosition = mediaPlayer.currentSeconds
                    mediaPlayer.seekTo(i * 1000)
                    eventProps.playbackPosition = mediaPlayer.currentSeconds
                    Event.sendMediaSeek("media00", eventProps, eventContext, null)
                    eventProps.previousPlaybackPosition = eventProps.playbackPosition
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

    }

    private fun initializeSeekBar() {
        seek_bar.max = mediaPlayer.seconds

        runnable = Runnable {
            seek_bar.progress = mediaPlayer.currentSeconds

            tv_pass.text = "${mediaPlayer.currentSeconds} sec"
            val diff = mediaPlayer.seconds - mediaPlayer.currentSeconds
            tv_due.text = "$diff sec"

            handler.postDelayed(runnable, 1000)
        }
        handler.postDelayed(runnable, 1000)
    }
}

val MediaPlayer.seconds:Int
    get() {
        return this.duration / 1000
    }
// Creating an extension property to get media player current position in seconds
val MediaPlayer.currentSeconds:Int
    get() {
        return this.currentPosition/1000
    }