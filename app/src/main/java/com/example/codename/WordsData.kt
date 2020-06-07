package com.example.codename

data class WordsData(val word: String?, val color: String?= "RED")

data class Member(
    val name: String,
    var isHost: Boolean = false,
    var team: Team = Team.RED,
    var isPrepared: Boolean = false
)