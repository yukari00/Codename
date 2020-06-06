package com.example.codename

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
