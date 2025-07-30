package com.example.quiz

import androidx.compose.material3.SnackbarDuration
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface QuizApi {
    @GET("api/questions")
    suspend fun getQuestions(): List<QuestionResponse>

    @POST("api/update-questions")
    suspend fun updateQuestions(
        @Body newQuestions: List<Question>,
        @Query("secret") secret: String
    ): String

    @GET("api/users")
    suspend fun getUsers(): List<User>

    @POST("api/users")
    suspend fun addUser(@Body newUser: User): User

    @GET("api/users/{userId}")
    suspend fun getUser(@Path("userId") userId: String): User?

    @POST("api/users/{userId}/rounds")
    suspend fun addRound(
        @Path("userId") userId: String,
        @Body newRound: Round
    ): Round?
}

sealed class Screen {
    object Intro : Screen()
    object Login : Screen()
    object Registration : Screen()
    object Quiz : Screen()
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

data class User(
    val id: String, // Could use UUID for auto-generation
    val username: String,
    val totalRoundsCompleted: Int,
    val rounds: List<Round>
)

data class Round(
    val topic: String,
    val correctAnswers: Int,
    val date: String  // You might want to use Date type, but for simplicity we'll use String for now
)

data class RoundScore(val correct: Int, val total: Int)
data class SnackbarData(val message: String, val duration: SnackbarDuration = SnackbarDuration.Short)
