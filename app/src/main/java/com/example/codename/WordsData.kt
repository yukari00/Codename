package com.example.codename

data class WordsData(val word: String?, val color: String?= "NOTHING")

data class Member(
    val name: String,
    var team: Team,
    var isHost: Boolean = false,
    var isPrepared: Boolean = false
)