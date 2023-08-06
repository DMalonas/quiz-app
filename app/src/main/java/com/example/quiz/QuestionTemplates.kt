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
    correctAnswers: List<String>, // Updated to accept a list
    onSubmission: (Boolean) -> Unit,
    selectedOption: MutableState<String?>
) {
    Column {
        Text(text = question)
        options.forEach { option ->
            Row(
                Modifier.selectable(
                    selected = (option == selectedOption.value),
                    onClick = { selectedOption.value = option }
                )
            ) {
                RadioButton(
                    selected = (option == selectedOption.value),
                    onClick = null
                )
                Text(text = option)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            // Check if the selected option matches the correct answer
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
    correctAnswers: List<String>, // New parameter for correct answers
    onSubmission: (Boolean) -> Unit, // New parameter to handle submission result
    selectedOptions: MutableState<List<String>>
) {
    Column {
        Text(text = question)
        options.forEach { option ->
            Row(
                Modifier.selectable(
                    selected = selectedOptions.value.contains(option),
                    onClick = {
                        if (selectedOptions.value.contains(option)) {
                            selectedOptions.value = selectedOptions.value - option
                        } else {
                            selectedOptions.value = selectedOptions.value + option
                        }
                    }
                )
            ) {
                Checkbox(
                    checked = selectedOptions.value.contains(option),
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


