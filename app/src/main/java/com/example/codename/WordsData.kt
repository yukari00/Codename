package com.example.codename

data class WordsData(val word: String?, val color: String?= "NOTHING")

data class Member(
    val name: String,
    var team: Team,
    var isHost: Boolean = false,
    val vote: MutableList<String>? = null
)

data class SelectedCardsInfo(val clickedWords: List<WordsData>, val turnCount: Int)