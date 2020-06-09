package com.example.codename

import android.media.SoundPool

fun SoundPool.play2 (soundId: Int){
    this.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f)
}