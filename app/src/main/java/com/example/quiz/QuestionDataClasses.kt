package com.example.quiz

import androidx.compose.material3.SnackbarDuration
import retrofit2.http.GET

interface QuizApi {
    @GET("api/questions")
    suspend fun getQuestions(): List<QuestionResponse>
}

data class QuestionResponse(
    val id: Int,
    val prompt: String,
    val choices: List<String>,
    val answer: List<Int>
)

enum class QuestionType {
    RADIO, CHECKBOX
}
data class QuestionData(
    val type: QuestionType,
    val question: String,  // Changed from Int to String
    val choices: List<String>,  // Changed from Int to List<String>
    val correctAnswers: List<String>
)


data class Question(
    val prompt: String,
    val choices: List<String>,
    val answer: List<Int>
)

data class RoundScore(val correct: Int, val total: Int)
data class SnackbarData(val message: String, val duration: SnackbarDuration = SnackbarDuration.Short)
