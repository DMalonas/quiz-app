package com.example.quiz

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.res.Resources
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val snackbarState = remember { mutableStateOf<SnackbarData?>(null) }
            val showIntroPage = remember { mutableStateOf(true) }
            val currentRound = remember { mutableStateOf(1) }
            val score = remember { mutableStateOf(0) }
            val resources = LocalContext.current.resources
            // Fetch the list of questions
            val questionIds = resources.obtainTypedArray(R.array.questions_list)
            val questionsData = mutableListOf<QuestionData>()
            // Dynamically create QuestionData for each question
            for (i in 0 until questionIds.length()) {
                val questionResId = questionIds.getResourceId(i, 0)
                val optionsResId = resources.getIdentifier("question_${i+1}_options", "array", packageName)
                val answersResId = resources.getIdentifier("question_${i+1}_answers", "array", packageName)
                val type = if (resources.getStringArray(answersResId).size > 1) QuestionType.CHECKBOX else QuestionType.RADIO
                questionsData.add(getQuestionDataFromResources(type, questionResId, optionsResId, answersResId, resources))
            }

            questionIds.recycle() // Remember to recycle the typed array
            Box(modifier = Modifier.fillMaxSize()) { // Add the Box here

                if (showIntroPage.value) {
                    IntroPage(onStartQuiz = {
                        showIntroPage.value = false
                    })
                } else {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        DisplayQuestions(
                            questions = questionsData,
                            currentRound = currentRound,
                            score = score,
                            onQuizFinished = {
                                showIntroPage.value = true
                            },
                            snackbarState = snackbarState
                        )
                    }
                }


                snackbarState.value?.let { snackbarData ->
                    // Snackbar display
                    val screenHeight = LocalConfiguration.current.screenHeightDp
                    val offsetValue = (screenHeight / 4).dp
                    Snackbar(
                        modifier = Modifier.align(Alignment.BottomCenter)  // Align to bottom center
                            .offset(y = -offsetValue), // Move upwards by the offset
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

    fun SnackbarDuration.toMillis(): Long {
        return when (this) {
            SnackbarDuration.Short -> 1500L
            SnackbarDuration.Long -> 2750L
            SnackbarDuration.Indefinite -> Long.MAX_VALUE // Represents an indefinitely long duration.
        }
    }



    @Composable
    fun IntroPage(onStartQuiz: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize(), // makes sure the column takes up all available space
            verticalArrangement = Arrangement.Center, // centers the items vertically
            horizontalAlignment = Alignment.CenterHorizontally // centers the items horizontally
        ) {
            Text(text = "QUIZ", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { onStartQuiz() }) {
                Text("Start Quiz")
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
        val context = LocalContext.current
        val currentQuestionIndex = remember { mutableStateOf(0) }

        if (currentQuestionIndex.value < questions.size) {
            val questionData = questions[currentQuestionIndex.value]
            when (questionData.type) {
                QuestionType.RADIO -> RadioButtonQuestionWrapper(
                    data = questionData,
                    onAnswered = { isCorrect ->
                        handleAnswer(isCorrect, score, currentQuestionIndex, context, snackbarState)
                    }
                )
                QuestionType.CHECKBOX -> CheckboxQuestionWrapper(
                    data = questionData,
                    onAnswered = { isCorrect ->
                        handleAnswer(isCorrect, score, currentQuestionIndex, context, snackbarState)
                    }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        } else {
            // Round finished, reset questions and increase round number.
            currentRound.value += 1
            currentQuestionIndex.value = 0
            snackbarState.value = SnackbarData(message = "Round finished!")
            onQuizFinished()
        }
    }

    fun handleAnswer(
        isCorrect: Boolean,
        score: MutableState<Int>,
        currentQuestionIndex: MutableState<Int>,
        context: Context,
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
        val question = stringResource(id = data.questionResId)
        val options = stringArrayResource(id = data.optionsResId).toList()
        val correctAnswers = data.correctAnswers
        val selectedOption = remember { mutableStateOf<String?>(null) }
        val context = LocalContext.current

        RadioButtonQuestionTemplate(
            question = question,
            options = options,
            correctAnswers = correctAnswers,
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
        val question = stringResource(id = data.questionResId)
        val options = stringArrayResource(id = data.optionsResId).toList()
        val correctAnswers = data.correctAnswers

        val selectedOptions = remember { mutableStateOf<List<String>>(emptyList()) }
        val context = LocalContext.current

        CheckboxQuestionTemplate(
            question = question,
            options = options,
            correctAnswers = correctAnswers,
            onSubmission = { isCorrect ->
                onAnswered(isCorrect)
            },
            selectedOptions = selectedOptions
        )
    }

    fun getQuestionDataFromResources(
        type: QuestionType,
        questionResId: Int,
        optionsResId: Int,
        correctAnswersResId: Int,
        resources: Resources
    ): QuestionData {
        val correctAnswers = resources.getStringArray(correctAnswersResId).toList()
        return QuestionData(type, questionResId, optionsResId, correctAnswers)
    }
}
