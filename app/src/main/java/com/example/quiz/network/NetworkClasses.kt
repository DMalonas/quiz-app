package com.example.quiz.network

import com.example.quiz.Question
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface QuizApi {
    @GET("/api/questions")
    suspend fun getQuestions(): List<Question>

    @POST("/api/submit-score")
    suspend fun submitScore(@Body score: ScoreData): String

    @GET("/api/scores")
    suspend fun getScores(): List<ScoreData>
}

object RetrofitClient {
    private const val BASE_URL = "https://quizappcl-a7d35f534d01.herokuapp.com/"

    val instance: QuizApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(QuizApi::class.java)
    }
}

data class ScoreData(
    val user: String,
    val score: Int,
    val date: String
)
