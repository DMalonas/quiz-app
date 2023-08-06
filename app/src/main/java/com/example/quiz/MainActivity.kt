package com.example.quiz

import android.content.res.Resources
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
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

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                DisplayQuestions(questions = questionsData, currentRound = currentRound, score = score)
            }
        }
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


    @Composable
    fun DisplayQuestions(questions: List<QuestionData>, currentRound: MutableState<Int>, score: MutableState<Int>) {
        val context = LocalContext.current

        val currentQuestionIndex = remember { mutableStateOf(0) }

        if (currentQuestionIndex.value < questions.size) {
            val questionData = questions[currentQuestionIndex.value]
            when (questionData.type) {
                QuestionType.RADIO -> RadioButtonQuestionWrapper(
                    data = questionData,
                    onAnsweredCorrectly = {
                        score.value += 1
                        currentQuestionIndex.value += 1
                    },
                    onAnsweredIncorrectly = {
                        currentQuestionIndex.value += 1
                    }
                )
                QuestionType.CHECKBOX -> CheckboxQuestionWrapper(
                    data = questionData,
                    onAnsweredCorrectly = {
                        score.value += 1
                        currentQuestionIndex.value += 1
                    },
                    onAnsweredIncorrectly = {
                        currentQuestionIndex.value += 1
                    }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        } else {
            // Round finished, reset questions and increase round number.
            currentRound.value += 1
            currentQuestionIndex.value = 0
            Toast.makeText(context, "Round finished!", Toast.LENGTH_SHORT).show()
        }
    }


    @Composable
    fun RadioButtonQuestionWrapper(
        data: QuestionData,
        onAnsweredCorrectly: () -> Unit,
        onAnsweredIncorrectly: () -> Unit
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
                if (isCorrect) {
                    Toast.makeText(context, "Correct!", Toast.LENGTH_SHORT).show()
                    onAnsweredCorrectly()
                } else {
                    Toast.makeText(context, "Wrong answer. Try again.", Toast.LENGTH_SHORT).show()
                    onAnsweredIncorrectly()
                }
            },
            selectedOption = selectedOption
        )
    }

    @Composable
    fun CheckboxQuestionWrapper(
        data: QuestionData,
        onAnsweredCorrectly: () -> Unit,
        onAnsweredIncorrectly: () -> Unit
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
                if (isCorrect) {
                    Toast.makeText(context, "Correct!", Toast.LENGTH_SHORT).show()
                    onAnsweredCorrectly()
                } else {
                    Toast.makeText(context, "Wrong answers. Try again.", Toast.LENGTH_SHORT).show()
                    onAnsweredIncorrectly()
                }
            },
            selectedOptions = selectedOptions
        )
    }
}
