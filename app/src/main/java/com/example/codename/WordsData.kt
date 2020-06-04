package com.example.codename

data class WordsData(
    val word1: String,
    val word2: String,
    val word3: String,
    val word4: String,
    val word5: String,
    val word6: String,
    val word7: String,
    val word8: String,
    val word9: String,
    val word10: String,
    val word11: String,
    val word12: String,
    val word13: String,
    val word14: String,
    val word15: String,
    val word16: String,
    val word17: String,
    val word18: String,
    val word19: String,
    val word20: String,
    val word21: String,
    val word22: String,
    val word23: String,
    val word24: String,
    val word25: String
)

data class Member(
    val name: String,
    var isHost: Boolean = false,
    var team: Team = Team.RED
)