package com.example.bucketlist

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.bucketlist.models.Destination
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var destinations by mutableStateOf<List<Destination>>(emptyList())
    private var name by mutableStateOf("")
    private var location by mutableStateOf("")
    private var description by mutableStateOf("")
    private var qrCodeBitmap by mutableStateOf<Bitmap?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        db = FirebaseFirestore.getInstance()

        setContent {
            fetchDestinations()
            BucketListApp(
                destinations = destinations,
                name = name,
                location = location,
                description = description,
                onNameChange = { name = it },
                onLocationChange = { location = it },
                onDescriptionChange = { description = it },
                onAddDestination = { addDestination(name, location, description) },
                onFetchLocation = { fetchLocation() },
                onGenerateQRCode = { generateQRCode() },
                qrCodeBitmap = qrCodeBitmap
            )
        }

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun fetchDestinations() {
        db.collection("destinations")
            .orderBy("dateCreated", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val destinationList = mutableListOf<Destination>()
                for (document in result) {
                    val destination = document.toObject(Destination::class.java)
                    destinationList.add(destination)
                }
                destinations = destinationList
            }
            .addOnFailureListener { e ->
                Toast.makeText(applicationContext, "Failed to load destinations: $e", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addDestination(name: String, location: String, description: String) {
        if (name.isNotEmpty() && location.isNotEmpty() && description.isNotEmpty()) {
            val newDestination = Destination(
                name = name,
                location = location,
                description = description,
                dateCreated = Date()
            )

            db.collection("destinations")
                .add(newDestination)
                .addOnSuccessListener {
                    Toast.makeText(applicationContext, "Destination added!", Toast.LENGTH_SHORT).show()
                    fetchDestinations()
                }
                .addOnFailureListener {
                    Toast.makeText(applicationContext, "Failed to add destination", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(applicationContext, "Please fill in all fields", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fetchLocation() {
        withContext(Dispatchers.IO) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { locationResult ->
                    if (locationResult != null) {
                        val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(
                            locationResult.latitude,
                            locationResult.longitude,
                            1
                        )
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val city = address.locality ?: ""
                            val country = address.countryName ?: ""
                            val place = address.featureName ?: ""

                            // Update name and location logic
                            name = if (place.isNotEmpty() && place.all { it.isDigit() }) city else place
                            location = country // Only the country will be shown in location

                            runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Fetched Location: $name, $location",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "No address found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Failed to fetch location", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to fetch location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun generateQRCode() {
        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                try {
                    val jsonData = destinations.joinToString(separator = "\n") {
                        "${it.name}, ${it.location}, ${it.description}"
                    }
                    val qrCodeWriter = QRCodeWriter()
                    val bitMatrix = qrCodeWriter.encode(jsonData, BarcodeFormat.QR_CODE, 512, 512)
                    val width = bitMatrix.width
                    val height = bitMatrix.height
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                    for (x in 0 until width) {
                        for (y in 0 until height) {
                            bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                        }
                    }
                    qrCodeBitmap = bitmap
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
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
    onAddDestination: () -> Unit,
    onFetchLocation: suspend () -> Unit,
    onGenerateQRCode: () -> Unit,
    qrCodeBitmap: Bitmap?
) {
    var showForm by remember { mutableStateOf(true) }
    var showList by remember { mutableStateOf(true) }
    var showQRCode by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = { showForm = !showForm }) {
                        Text(if (showForm) "Hide Form" else "Show Form")
                    }
                    Button(onClick = { showList = !showList }) {
                        Text(if (showList) "Hide List" else "Show List")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (showForm) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = name,
                                onValueChange = onNameChange,
                                label = { Text("Destination Name") },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                coroutineScope.launch { onFetchLocation() }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = "Fetch Location"
                                )
                            }
                        }
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
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (showList) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, Color.Black),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(destinations) { destination ->
                            DestinationItem(destination)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showQRCode = !showQRCode; if (showQRCode) onGenerateQRCode() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showQRCode) "Hide QR Code" else "Share your list")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (showQRCode) {
                    qrCodeBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Generated QR Code",
                            modifier = Modifier.size(200.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun DestinationItem(destination: Destination) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Gray)
            .padding(8.dp)
    ) {
        Text(
            text = destination.name,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = destination.location,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = destination.description,
            modifier = Modifier.weight(2f)
        )
    }
}