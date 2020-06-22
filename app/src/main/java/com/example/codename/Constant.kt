package com.example.codename

import android.media.SoundPool

const val dbCollection = "com.example.codename"

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
var isWaiter = false

var soundPool: SoundPool? = null

var soundIdCorrect = 0
var soundIdIncorrect = 0
var soundIdButtonClicked = 0
var soundIdWinner = 0
var soundIdLoser = 0
var soundIdShock = 0

enum class Turn{
    RED_TEAM_TURN,
    BLUE_TEAM_TURN,
}