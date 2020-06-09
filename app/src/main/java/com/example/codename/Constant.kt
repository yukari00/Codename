package com.example.codename

import android.media.SoundPool

const val dbCollection = "COLLECTION"

enum class Status{
    CREATE_ROOM,
    JOIN_ROOM
}

enum class Team {
    RED,
    BLUE
}

var isDataFinished = false

var status: Status = Status.JOIN_ROOM

var isHost = false
var isMyTeam = Team.RED

var soundPool: SoundPool? = null

var soundIdCorrect = 0
var soundIdIncorrect = 0
var soundIdButtonClicked = 0