package com.example.bucketlist

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bucketlist.models.Destination
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private lateinit var db: FirebaseFirestore
    private var destinations by mutableStateOf<List<Destination>>(emptyList())
    private var name by mutableStateOf("")
    private var location by mutableStateOf("")
    private var description by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            db = FirebaseFirestore.getInstance()

            fetchDestinations()

            BucketListApp(
                destinations = destinations,
                name = name,
                location = location,
                description = description,
                onNameChange = { name = it },
                onLocationChange = { location = it },
                onDescriptionChange = { description = it },
                onAddDestination = { addDestination(name, location, description) }
            )
        }
    }

    private fun fetchDestinations() {
        db.collection("destinations")
            .get()
            .addOnSuccessListener { result ->
                val destinationList = mutableListOf<Destination>()
                for (document in result) {
                    val destination = document.toObject(Destination::class.java)
                    destinationList.add(destination)
                }
                destinations = destinationList // Update the list state
            }
            .addOnFailureListener { e ->
                Toast.makeText(applicationContext, "Failed to load destinations: $e", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addDestination(name: String, location: String, description: String) {
        if (name.isNotEmpty() && location.isNotEmpty() && description.isNotEmpty()) {
            val newDestination = Destination(name = name, location = location, description = description)

            // Add the new destination to Firestore
            db.collection("destinations")
                .add(newDestination)
                .addOnSuccessListener {
                    Toast.makeText(applicationContext, "Destination added!", Toast.LENGTH_SHORT).show()
                    fetchDestinations() // Refresh the list after adding a destination
                }
                .addOnFailureListener {
                    Toast.makeText(applicationContext, "Failed to add destination", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(applicationContext, "Please fill in all fields", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BucketListApp(
    destinations: List<Destination>,
    name: String,
    location: String,
    description: String,
    onNameChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAddDestination: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bucket List") }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Destination Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = location,
                    onValueChange = onLocationChange,
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onAddDestination,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Destination")
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(destinations) { destination ->
                        DestinationItem(destination)
                    }
                }
            }
        }
    )
}

@Composable
fun DestinationItem(destination: Destination) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(text = "Destination: ${destination.name}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Location: ${destination.location}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Description: ${destination.description}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BucketListApp(
        destinations = listOf(
            Destination("Paris", "France", "Eiffel Tower"),
            Destination("New York", "USA", "Statue of Liberty")
        ),
        name = "",
        location = "",
        description = "",
        onNameChange = {},
        onLocationChange = {},
        onDescriptionChange = {},
        onAddDestination = {}
    )
}
