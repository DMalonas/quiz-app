package com.example.quiz.network



import com.example.quiz.Question
import com.example.quiz.QuestionData
import retrofit2.http.GET
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


interface QuizApi {
    @GET("/api/questions")
    suspend fun getQuestions(): List<Question>
}


object RetrofitClient {
    private const val BASE_URL = "https://your-heroku-app.herokuapp.com"

    val instance: QuizApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(QuizApi::class.java)
    }

    interface QuizApi {
        @GET("/api/questions")
        suspend fun getQuestions(): List<Question>
    }
}