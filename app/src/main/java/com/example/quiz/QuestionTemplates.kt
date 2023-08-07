package com.example.quiz

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RadioButtonQuestionTemplate(
    question: String,
    options: List<String>,
    correctAnswers: List<Int>, // Updated to accept a list of Int
    onSubmission: (Boolean) -> Unit,
    selectedOption: MutableState<Int?> // Updated to Int?
) {
    Column {
        Text(text = question)
        options.forEachIndexed { index, option -> // Use forEachIndexed to get index as well
            Row(
                Modifier.selectable(
                    selected = (index == selectedOption.value),
                    onClick = { selectedOption.value = index }
                )
            ) {
                RadioButton(
                    selected = (index == selectedOption.value),
                    onClick = null
                )
                Text(text = option)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            // Check if the selected option index matches the correct answer
            onSubmission(selectedOption.value == correctAnswers.sorted().first())
        }) {
            Text("Submit")
        }
    }
}


@Composable
fun CheckboxQuestionTemplate(
    question: String,
    options: List<String>,
    correctAnswers: List<Int>, // Updated to accept a list of Int
    onSubmission: (Boolean) -> Unit,
    selectedOptions: MutableState<List<Int>>
) {
    Column {
        Text(text = question)
        options.forEachIndexed { index, option ->
            Row(
                Modifier.selectable(
                    selected = selectedOptions.value.contains(index),
                    onClick = {
                        if (selectedOptions.value.contains(index)) {
                            selectedOptions.value = selectedOptions.value - listOf(index)
                        } else {
                            selectedOptions.value = selectedOptions.value + listOf(index)
                        }
                    }
                )
            ) {
                Checkbox(
                    checked = selectedOptions.value.contains(index),
                    onCheckedChange = null
                )
                Text(text = option)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            // Check if the selected options match the correct answers
            onSubmission(selectedOptions.value.sorted() == correctAnswers.sorted())
        }) {
            Text("Submit")
        }
    }
}


