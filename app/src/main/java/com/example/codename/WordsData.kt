package com.example.codename

data class Member(
    val name: String,
    var isHost: Boolean = false,
    var team: Team = Team.RED,
    var isPrepared: Boolean = false
)