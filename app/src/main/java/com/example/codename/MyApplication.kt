package com.example.codename

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build

class MyApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        setSoundPool()
    }

    private fun setSoundPool() {

        soundPool = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            SoundPool.Builder().setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
                .setMaxStreams(1)
                .build()

        }else{
            SoundPool(1, AudioManager.STREAM_MUSIC, 0)
        }

        soundIdCorrect = soundPool!!.load(this, R.raw.correct2, 1)
        soundIdIncorrect = soundPool!!.load(this, R.raw.incorrect1, 1)
        soundIdButtonClicked = soundPool!!.load(this, R.raw.button03a, 1)
        soundIdWinner = soundPool!!.load(this, R.raw.cheer1, 1)
        soundIdLoser = soundPool!!.load(this, R.raw.stupid5, 1)
        soundIdShock = soundPool!!.load(this, R.raw.shock3, 1)

    }
}

