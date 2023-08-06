package com.example.quiz

import androidx.compose.material3.SnackbarDuration

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




data class SnackbarData(val message: String, val duration: SnackbarDuration = SnackbarDuration.Short)
