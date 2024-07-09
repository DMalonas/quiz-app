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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
    private val userName = "DMalonas"

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

            Box(modifier = Modifier.fillMaxSize()) {
                Navigation(
                    currentScreen = currentScreen,
                    questionsData = questionsData,
                    currentRound = currentRound,
                    score = score,
                    snackbarState = snackbarState
                )
                snackbarState.value?.let { snackbarData ->
                    val screenHeight = LocalConfiguration.current.screenHeightDp
                    val offsetValue = (screenHeight / 4).dp
                    Snackbar(
                        modifier = Modifier.align(Alignment.BottomCenter)
                            .offset(y = -offsetValue)
                            .padding(horizontal = 32.dp),
                        action = {
                            TextButton(onClick = { snackbarState.value = null }) {
                                Text("DISMISS")
                            }
                        },
                        actionOnNewLine = false
                    ) {
                        Text(snackbarData.message)
                    }
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
            println("Score submitted successfully: $response")
        } catch (e: Exception) {
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
            is Screen.Intro -> IntroPage(
                onStartQuiz = {
                    score.value = 0
                    currentScreen.value = Screen.Quiz
                },
                onViewScores = {
                    currentScreen.value = Screen.Scores
                }
            )
            is Screen.Quiz -> QuizScreen(
                questionsData = questionsData,
                currentRound = currentRound,
                score = score,
                snackbarState = snackbarState,
                onQuizFinished = {
                    currentScreen.value = Screen.Score(currentRound.value, score.value)
                }
            )
            is Screen.Score -> ScoreScreen(
                round = screen.round,
                score = screen.score,
                onContinue = {
                    CoroutineScope(Dispatchers.IO).launch {
                        submitScore(screen.score)
                    }
                    currentRound.value += 1
                    currentScreen.value = Screen.Intro
                }
            )
            is Screen.Scores -> ScoresScreen(
                onBack = {
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
            SnackbarDuration.Indefinite -> Long.MAX_VALUE
        }
    }

    @Composable
    fun IntroPage(onStartQuiz: () -> Unit, onViewScores: () -> Unit) {
        val context = LocalContext.current

        val backgroundVideoView = remember { VideoView(context) }
        val buttonTextureVideoView = remember { VideoView(context) }

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
            AndroidView({ backgroundVideoView }, Modifier.fillMaxSize())

            Box(modifier = Modifier.align(Alignment.Center)) {
                AndroidView({ buttonTextureVideoView }, Modifier.size(buttonWidth, buttonHeight).align(Alignment.Center))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Button(
                        onClick = onStartQuiz,
                        modifier = Modifier.size(buttonWidth, buttonHeight).background(Color.Transparent),
                        colors = ButtonDefaults.buttonColors(Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Start Quiz")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onViewScores,
                        modifier = Modifier.size(buttonWidth, buttonHeight).background(Color.Transparent),
                        colors = ButtonDefaults.buttonColors(Color.Transparent),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("View Scores")
                    }
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
        snackbarState: MutableState<SnackbarData?>
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
            onQuizFinished()
        }
    }

    fun handleAnswer(
        isCorrect: Boolean,
        score: MutableState<Int>,
        currentQuestionIndex: MutableState<Int>,
        snackbarState: MutableState<SnackbarData?>
    ) {
        val feedbackMessage = if (isCorrect) {
            score.value += 1
            "Correct!"
        } else {
            "Wrong answer. Try again."
        }
        snackbarState.value = SnackbarData(message = feedbackMessage, SnackbarDuration.Short)

        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
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
        val correctAnswersInt = data.correctAnswers.map { it.toInt() }
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
            TextField(
                value = username.value,
                onValueChange = { username.value = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

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
                Button(onClick = {
                    onRegisterSuccessful()
                }) {
                    Text("Register")
                }

                Button(onClick = {
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
        object Scores : Screen()
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

    @Composable
    fun ScoresScreen(onBack: () -> Unit) {
        val scores = remember { mutableStateOf<List<ScoreData>>(emptyList()) }
        val aggregateScores = remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
        val totalRounds = remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

        LaunchedEffect(Unit) {
            fetchScores { fetchedScores ->
                scores.value = fetchedScores
                aggregateScores.value = fetchedScores.groupBy { it.user }
                    .mapValues { entry -> entry.value.sumOf { it.score } }
                totalRounds.value = fetchedScores.groupBy { it.user }
                    .mapValues { entry -> entry.value.size }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(text = "Scoreboard", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(aggregateScores.value.toList()) { (user, totalScore) ->
                    val rounds = totalRounds.value[user] ?: 0
                    ScoreCard(user = user, rounds = rounds, score = totalScore)
                }
            }

            Button(
                onClick = onBack,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Back")
            }
        }    }

    @Composable
    fun ScoreCard(user: String, rounds: Int, score: Int) {
        Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(text = user, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Total Rounds: $rounds", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Total Score: $score", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    private fun fetchScores(onResult: (List<ScoreData>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scores = RetrofitClient.instance.getScores()
                withContext(Dispatchers.Main) {
                    onResult(scores)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
