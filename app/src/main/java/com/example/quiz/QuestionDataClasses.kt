package com.example.quiz

enum class QuestionType {
    RADIO, CHECKBOX
}

data class QuestionData(
    val type: QuestionType,
    val questionResId: Int,
    val optionsResId: Int,
    val correctAnswers: List<String>
)

data class RoundScore(val correct: Int, val total: Int)
