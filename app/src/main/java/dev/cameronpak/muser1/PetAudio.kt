package dev.cameronpak.muser1

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper

/** Local audio only; speech/recording and loss of window/audio focus always take priority. */
internal class PetAudio(private val context:Context) {
    private val prefs=context.getSharedPreferences("pet_audio",Context.MODE_PRIVATE)
    var mode:String
        get()=prefs.getString("mode","auto").orEmpty().takeIf { it in PetSoundtrack.modes } ?: "auto"
        set(value) { prefs.edit().putString("mode",value).apply();interrupted=false }
    var effects:Boolean
        get()=prefs.getBoolean("effects",true)
        set(value) { prefs.edit().putBoolean("effects",value).apply();if(!value)pool.autoPause() }
    var characterVoice:Boolean
        get()=prefs.getBoolean("character_voice",true)
        set(value){prefs.edit().putBoolean("character_voice",value).apply();if(!value)stopVoice()}
    private var voicePlayer:MediaPlayer?=null
    private var voiceAt=-10_000L
    private var effectsUntil=0L
    fun stopCharacterVoice()=stopVoice()
    private fun stopVoice(){voicePlayer?.release();voicePlayer=null}
    private val attributes=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
    private val manager=context.getSystemService(AudioManager::class.java)
    private var player:MediaPlayer?=null
    private var playingTrack:String?=null
    private var blocked=true
    private var granted=false
    private var interrupted=false
    private var closed=false
    private val focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(attributes).setOnAudioFocusChangeListener({ change ->
            if(!closed && change<0) { interrupted=true;granted=false;player?.pause();stopVoice();pool.autoPause() }
            else if(!closed && change==AudioManager.AUDIOFOCUS_GAIN) { interrupted=false;granted=true }
        },Handler(Looper.getMainLooper())).build()
    private val pool=SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attributes).build()
    private val sounds=mapOf("feed" to R.raw.pet_feed,"wash" to R.raw.pet_wash,"win" to R.raw.pet_win,"sleep" to R.raw.pet_sleep,"level" to R.raw.pet_level,"chirp" to R.raw.pet_chirp,"hatch" to R.raw.pet_hatch,"plant" to R.raw.pet_plant,"water" to R.raw.pet_water,"bloom" to R.raw.pet_bloom,"learn" to R.raw.pet_learn,"catch" to R.raw.pet_catch)
        .mapValues { pool.load(context,it.value,1) }
    fun update(level:Int,sleeping:Boolean,quiet:Boolean) {
        if(closed)return
        if(quiet) { mute();return }
        blocked=false
        val track=PetSoundtrack.track(mode,level,sleeping)
        if(track==null) { player?.release();player=null;playingTrack=null;if(voicePlayer==null && android.os.SystemClock.elapsedRealtime()>=effectsUntil)abandon();return }
        if(interrupted)return
        if(!granted)granted=manager.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if(!granted)return
        if(playingTrack!=track) {
            player?.release()
            val resource=when(track) { "forest"->R.raw.pet_forest;"musicbox"->R.raw.pet_musicbox;"lullaby"->R.raw.pet_lullaby;"sprout"->R.raw.pet_sprout;"sunny"->R.raw.pet_sunny;"meadow"->R.raw.pet_meadow;"starlight"->R.raw.pet_starlight;else->R.raw.pet_garden }
            player=MediaPlayer.create(context,resource,attributes,0)?.apply { isLooping=true;setVolume(.20f,.20f) }
            playingTrack=track
        }
        player?.let { if(!it.isPlaying)it.start() }
    }
    private fun requestFocus():Boolean {
        if(interrupted)return false
        if(!granted)granted=manager.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return granted
    }
    fun effect(name:String) { if(!closed && effects && !blocked && requestFocus())sounds[name]?.let {
        effectsUntil=android.os.SystemClock.elapsedRealtime()+2500;pool.play(it,.35f,.35f,1,0,1f)
    } }
    fun voice(name:String,language:DisplayLanguage) {
        val now=android.os.SystemClock.elapsedRealtime()
        if(closed || !characterVoice || blocked || now-voiceAt<6000 || !requestFocus())return
        val resource=when(language.name.lowercase()+"_"+name) {
            "ko_hello"->R.raw.pet_voice_ko_hello
            "ko_thanks"->R.raw.pet_voice_ko_thanks
            "ko_sleep"->R.raw.pet_voice_ko_sleep
            "ko_praise"->R.raw.pet_voice_ko_praise
            "ja_hello"->R.raw.pet_voice_ja_hello
            "ja_thanks"->R.raw.pet_voice_ja_thanks
            "ja_sleep"->R.raw.pet_voice_ja_sleep
            "ja_praise"->R.raw.pet_voice_ja_praise
            "en_hello"->R.raw.pet_voice_en_hello
            "en_thanks"->R.raw.pet_voice_en_thanks
            "en_sleep"->R.raw.pet_voice_en_sleep
            "en_praise"->R.raw.pet_voice_en_praise
            else->return
        }
        voiceAt=now;stopVoice()
        voicePlayer=MediaPlayer.create(context,resource,AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),0)?.apply {
            setVolume(.65f,.65f)
            setOnCompletionListener {finished->if(voicePlayer===finished){voicePlayer=null;finished.release()}}
            start()
        }
    }
    private fun abandon() { manager.abandonAudioFocusRequest(focus);granted=false }
    fun mute() { if(closed)return;val release=!blocked || granted;blocked=true;player?.pause();stopVoice();pool.autoPause();if(release)abandon();interrupted=false }
    fun close() { if(closed)return;mute();player?.release();player=null;pool.release();closed=true }
}
