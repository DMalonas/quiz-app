package com.example.quiz

import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.quiz.network.RetrofitClient
import com.example.quiz.network.ScoreData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {

    // This should be set by each user
    private val userName = "YourNameHere"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val snackbarState = remember { mutableStateOf<SnackbarData?>(null) }
            val currentScreen: MutableState<Screen> = remember { mutableStateOf(Screen.Intro) }
            val currentRound = remember { mutableStateOf(1) }
            val score = remember { mutableStateOf(0) }
            val retrofit = Retrofit.Builder()
                .baseUrl("https://quizappcl-a7d35f534d01.herokuapp.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val api = retrofit.create(QuizApi::class.java)
            val questionsData = remember { mutableStateListOf<QuestionData>() }
            // Fetch the data asynchronously in the background
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    try {
                        val response = api.getQuestions()
                        withContext(Dispatchers.Main) {
                            response.forEach { questionResponse ->
                                val type =
                                    if (questionResponse.answer.size > 1) QuestionType.CHECKBOX else QuestionType.RADIO
                                val correctAnswers = questionResponse.answer.map { it.toString() }
                                questionsData.add(
                                    QuestionData(
                                        type,
                                        questionResponse.prompt,
                                        questionResponse.choices,
                                        correctAnswers
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // Handle the exception, e.g., show a Snackbar or a Toast
                        e.printStackTrace()
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) { // Add the Box here
                Navigation(
                    currentScreen = currentScreen,
                    questionsData = questionsData,
                    currentRound = currentRound,
                    score = score,
                    snackbarState = snackbarState
                ) // Use the Navigation composable here
                snackbarState.value?.let { snackbarData ->
                    // Snackbar display
                    val screenHeight = LocalConfiguration.current.screenHeightDp
                    val offsetValue = (screenHeight / 4).dp
                    Snackbar(
                        modifier = Modifier.align(Alignment.BottomCenter)  // Align to bottom center
                            .offset(y = -offsetValue) // Move upwards by the offset
                            .padding(horizontal = 32.dp),  // Add horizontal padding

                        action = {
                            TextButton(onClick = { snackbarState.value = null }) {
                                Text("DISMISS")
                            }
                        },
                        actionOnNewLine = false
                    ) {
                        Text(snackbarData.message)
                    }
                    // Set a delay to auto-dismiss the Snackbar after a certain duration
                    CoroutineScope(Dispatchers.Main).launch {
                        val delayDuration = snackbarData.duration.toMillis()
                        if (snackbarData.duration != SnackbarDuration.Indefinite) {
                            delay(delayDuration)
                            snackbarState.value = null
                        }
                    }
                }
            }
        }
    }

    private suspend fun submitScore(score: Int) {
        val scoreData = ScoreData(user = userName, score = score, date = "")
        try {
            val response = RetrofitClient.instance.submitScore(scoreData)
            // Handle successful submission
            println("Score submitted successfully: $response")
        } catch (e: Exception) {
            // Handle error
            e.printStackTrace()
            println("Error submitting score: ${e.message}")
        }
    }

    @Composable
    fun Navigation(
        currentScreen: MutableState<Screen>,
        questionsData: MutableList<QuestionData>,
        currentRound: MutableState<Int>,
        score: MutableState<Int>,
        snackbarState: MutableState<SnackbarData?>
    ) {
        when (val screen = currentScreen.value) {
            is Screen.Intro -> IntroPage(onStartQuiz = {
                score.value = 0 // Reset score for new round
                currentScreen.value = Screen.Quiz
            })
            is Screen.Quiz -> QuizScreen(
                questionsData = questionsData,
                currentRound = currentRound,
                score = score,
                snackbarState = snackbarState,
                onQuizFinished = {
                    // Submit the score when the quiz is finished
                    CoroutineScope(Dispatchers.IO).launch {
                        submitScore(score.value)
                    }
                    currentScreen.value = Screen.Score(currentRound.value, score.value)
                }
            )
            is Screen.Score -> ScoreScreen(
                round = screen.round,
                score = screen.score,
                onContinue = {
                    currentRound.value += 1 // Increment the round when continuing to the next round
                    currentScreen.value = Screen.Intro
                }
            )

            else -> {}
        }
    }

    @Composable
    fun QuizScreen(
        questionsData: MutableList<QuestionData>,
        currentRound: MutableState<Int>,
        score: MutableState<Int>,
        snackbarState: MutableState<SnackbarData?>,
        onQuizFinished: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEDEDED))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Quiz Round ${currentRound.value}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            DisplayQuestions(
                questions = questionsData,
                currentRound = currentRound,
                score = score,
                onQuizFinished = onQuizFinished,
                snackbarState = snackbarState
            )
        }
    }

    private @Composable
    fun RegistrationScreen(onRegistrationSuccess: () -> Unit) {
        TODO("Not yet implemented")
    }

    private @Composable
    fun LoginScreen(onLoginSuccess: () -> Unit) {
        TODO("Not yet implemented")
    }

    fun SnackbarDuration.toMillis(): Long {
        return when (this) {
            SnackbarDuration.Short -> 1500L
            SnackbarDuration.Long -> 2750L
            SnackbarDuration.Indefinite -> Long.MAX_VALUE // Represents an indefinitely long duration.
        }
    }

    @Composable
    fun IntroPage(onStartQuiz: () -> Unit) {
        val context = LocalContext.current

        val backgroundVideoView = remember { VideoView(context) }
        val buttonTextureVideoView = remember { VideoView(context) }

        // Setting up background video
        DisposableEffect(Unit) {
            backgroundVideoView.setVideoURI(Uri.parse("android.resource://${context.packageName}/${R.raw.intro_video}"))
            backgroundVideoView.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                mediaPlayer.start()
            }

            onDispose {
                backgroundVideoView.stopPlayback()
            }
        }

        // Setting up button texture video
        DisposableEffect(Unit) {
            buttonTextureVideoView.setVideoURI(Uri.parse("android.resource://${context.packageName}/${R.raw.patterns}"))
            buttonTextureVideoView.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                mediaPlayer.start()
            }

            onDispose {
                buttonTextureVideoView.stopPlayback()
            }
        }

        val buttonWidth = 200.dp
        val buttonHeight = 60.dp

        Box(modifier = Modifier.fillMaxSize()) {
            // Background VideoView
            AndroidView({ backgroundVideoView }, Modifier.fillMaxSize())

            // Centered Box for button video texture and button
            Box(
                modifier = Modifier.align(Alignment.Center)
            ) {
                // Button Texture VideoView
                AndroidView({ buttonTextureVideoView }, Modifier.size(buttonWidth, buttonHeight).align(Alignment.Center))

                // Actual Button with transparent background
                Button(
                    onClick = onStartQuiz,
                    modifier = Modifier.size(buttonWidth, buttonHeight).align(Alignment.Center).background(Color.Transparent),
                    colors = ButtonDefaults.buttonColors(Color.Transparent),
                    contentPadding = PaddingValues(0.dp) // This is optional, but can help in ensuring the button remains completely transparent
                ) {
                    Text("Start Quiz")
                }
            }
        }
    }

    @Composable
    fun DisplayQuestions(
        questions: List<QuestionData>,
        currentRound: MutableState<Int>,
        score: MutableState<Int>,
        onQuizFinished: () -> Unit,
        snackbarState: MutableState<SnackbarData?>  // Passed state to use in the function
    ) {
        val currentQuestionIndex = remember { mutableStateOf(0) }

        if (currentQuestionIndex.value < questions.size) {
            val questionData = questions[currentQuestionIndex.value]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = questionData.question,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                when (questionData.type) {
                    QuestionType.RADIO -> RadioButtonQuestionWrapper(
                        data = questionData,
                        onAnswered = { isCorrect ->
                            handleAnswer(isCorrect, score, currentQuestionIndex, snackbarState)
                        }
                    )
                    QuestionType.CHECKBOX -> CheckboxQuestionWrapper(
                        data = questionData,
                        onAnswered = { isCorrect ->
                            handleAnswer(isCorrect, score, currentQuestionIndex, snackbarState)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            // Round finished, reset questions and increase round number.
            onQuizFinished()
        }
    }

    fun handleAnswer(
        isCorrect: Boolean,
        score: MutableState<Int>,
        currentQuestionIndex: MutableState<Int>,
        snackbarState: MutableState<SnackbarData?>  // Passed state to use in the function
    ) {
        val feedbackMessage = if (isCorrect) {
            score.value += 1
            "Correct!"
        } else {
            "Wrong answer. Try again."
        }
        snackbarState.value = SnackbarData(message = feedbackMessage, SnackbarDuration.Short)

        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)  // 1 second delay
            currentQuestionIndex.value += 1
        }
    }

    @Composable
    fun RadioButtonQuestionWrapper(
        data: QuestionData,
        onAnswered: (Boolean) -> Unit
    ) {
        val question = data.question
        val options = data.choices
        val correctAnswersInt = data.correctAnswers.map { it.toInt() }

        // Use data.id as a unique key for the state
        val selectedOption = remember(data.choices + data.question) { mutableStateOf<Int?>(null) }

        RadioButtonQuestionTemplate(
            question = question,
            options = options,
            correctAnswers = correctAnswersInt,
            onSubmission = { isCorrect ->
                onAnswered(isCorrect)
            },
            selectedOption = selectedOption
        )
    }

    @Composable
    fun CheckboxQuestionWrapper(
        data: QuestionData,
        onAnswered: (Boolean) -> Unit
    ) {
        val question = data.question
        val options = data.choices
        val correctAnswersInt = data.correctAnswers.map { it.toInt() } // Convert to list of integers
        // Using data.question as a key for remember
        val selectedOptionsIndices = remember(data.choices + data.question) { mutableStateOf<List<Int>>(emptyList()) }
        CheckboxQuestionTemplate(
            question = question,
            options = options,
            correctAnswers = correctAnswersInt,
            onSubmission = { isCorrect ->
                onAnswered(isCorrect)
            },
            selectedOptions = selectedOptionsIndices
        )
    }

    @Composable
    fun ScoreScreen(round: Int, score: Int, onContinue: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEDEDED))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Round $round Score",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "You scored $score points!",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                Button(onClick = onContinue) {
                    Text("Continue")
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun RegistrationLoginUI(onLoginSuccessful: () -> Unit, onRegisterSuccessful: () -> Unit) {
        val username = remember { mutableStateOf("") }
        val password = remember { mutableStateOf("") }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Username Input
            TextField(
                value = username.value,
                onValueChange = { username.value = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            // Password Input
            TextField(
                value = password.value,
                onValueChange = { password.value = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                visualTransformation = PasswordVisualTransformation()
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Registration Button
                Button(onClick = {
                    // TODO: Handle registration logic
                    onRegisterSuccessful()
                }) {
                    Text("Register")
                }

                // Login Button
                Button(onClick = {
                    // TODO: Handle login logic
                    onLoginSuccessful()
                }) {
                    Text("Login")
                }
            }
        }
    }

    sealed class Screen {
        object RegistrationLogin : Screen()
        object Intro : Screen()
        object Quiz : Screen()
        data class Score(val round: Int, val score: Int) : Screen()
    }

    data class SnackbarData(
        val message: String,
        val duration: SnackbarDuration = SnackbarDuration.Short
    )

    data class QuestionData(
        val type: QuestionType,
        val question: String,
        val choices: List<String>,
        val correctAnswers: List<String>
    )

    enum class QuestionType {
        RADIO, CHECKBOX
    }



}
