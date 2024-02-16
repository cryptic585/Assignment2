


package com.example.assignment2

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.health.connect.ReadRecordsRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log




import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.platform.client.proto.DataProto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity(), ActivityResultRegistryOwner {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

                val PERMISSIONS =
                    setOf(
                        HealthPermission.getReadPermission(HeartRateRecord::class),
                        HealthPermission.getWritePermission(HeartRateRecord::class),
                        HealthPermission.getReadPermission(StepsRecord::class),
                        HealthPermission.getWritePermission(StepsRecord::class)
                    )

                val healthConnectClient = HealthConnectClient.getOrCreate(this)
                val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()
                val requestPermissions = registerForActivityResult(requestPermissionActivityContract) { granted ->
                    if (granted.containsAll(PERMISSIONS)) {
                        // Permissions successfully granted
                    } else {
                        // Lack of required permissions
                        runBlocking {
                            launch(Dispatchers.IO) {
                                val granted = healthConnectClient.permissionController.getGrantedPermissions()
                                if (granted.containsAll(PERMISSIONS)) {
                                    // Permissions already granted; proceed with inserting or reading data
                                } else {

                                }
                            }
                            }
                    }
                }
        requestPermissions.launch(PERMISSIONS)


        setContent {
            //MyScreen(context = applicationContext)
            MyScreen(activity = this)
        }
        runBlocking {
            launch(Dispatchers.IO) {
                checkHealthConnectStatus(this@MainActivity)
            }}





    }


}

@Composable
//fun MyScreen(context: Context) {
fun MyScreen(activity: Activity) {
    var hrCountText by remember { mutableStateOf(TextFieldValue()) }
    var timeText by remember { mutableStateOf(TextFieldValue()) }
    var names by remember { mutableStateOf(listOf<String>()) }


    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = hrCountText,
            onValueChange = { hrCountText = it },
            label = { Text("Heart Rate Count") },
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TextField(
            value = timeText,
            onValueChange = { timeText = it },
            label = { Text("Time Stamp") },
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(onClick = {
                Log.e("varun galat hai", "varun galat hai")

                val hrCount = hrCountText.text.toLongOrNull()
                val timeMillis = timeText.text.toLongOrNull()
                Log.e("varun galat hai", "$hrCount")
                Log.e("varun galat hai", "$timeMillis")
                val healthConnectClient = HealthConnectClient.getOrCreate(activity)
                runBlocking {
                    launch(Dispatchers.IO) {


                        if (hrCount != null) {
                            if (timeMillis != null) {
                                insertSteps(healthConnectClient, hrCount, timeMillis)
//                                if (hasWriteStepsPermission(activity)) {
//                                    Log.e("varun galat hai", "varun galat hai")
//                                    insertSteps(healthConnectClient, hrCount, timeMillis)
//                                } else {
//                                    // Request WRITE_STEPS permission from the user
//                                    requestWriteStepsPermission(activity)
//                                }
                                // insertSteps(healthConnectClient, hrCount, timeMillis)
                            }
                        }

                    }
                }


            }) {
                Text("Save")
            }
            Button(onClick = {
                val healthConnectClient = HealthConnectClient.getOrCreate(activity)
                runBlocking {
                    launch(Dispatchers.IO) {
//                        if (hasReadStepsPermission(activity)) {
                            readHeartRate(healthConnectClient)
//                        } else {
//                            requestReadStepsPermission(activity)
//                        }
                        //readHeartRate(healthConnectClient)
                    }
                }

            }) {
                Text("Load")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .border(width = 1.dp, color = Color.Black)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(names) { name ->
                    Text(text = name, modifier = Modifier.padding(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display name and student ID with border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = Color.Black)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Name: Varun Bhatt")
                Text(text = "Student ID: 301364446")
            }
        }
    }
    LaunchedEffect(key1 = Unit) {
        //checkHealthConnectStatus(activity)
    }
}



private suspend fun checkHealthConnectStatus(activity: Activity) {
    val providerPackageName = "com.google.android.apps.healthdata" // Replace with actual provider package name

    val availabilityStatus = HealthConnectClient.getSdkStatus(activity, providerPackageName)
    if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE) {
        return // early return as there is no viable integration
    }
    if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
        // Optionally redirect to package installer to find a provider, for example:
        val uriString =
            "market://details?id=$providerPackageName&url=healthconnect%3A%2F%2Fonboarding"
        activity.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setPackage("com.android.vending")
                data = Uri.parse(uriString)
                putExtra("overlay", true)
                putExtra("callerId", activity.packageName)
            }
        )
        return
    }



}

private fun hasWriteStepsPermission(context: Context): Boolean {
    // Check if WRITE_STEPS permission is granted
    val permission = "android.permission.health.WRITE_STEPS"
    val permissionStatus = context.checkSelfPermission(permission)
    return permissionStatus == PackageManager.PERMISSION_GRANTED
}

private fun requestWriteStepsPermission(context: Context) {
    // Request WRITE_STEPS permission from the user
    val REQUEST_WRITE_STEPS_PERMISSION = 124
    val permission = "android.permission.health.WRITE_STEPS"
    ActivityCompat.requestPermissions(
        context as Activity,
        arrayOf(permission),
        REQUEST_WRITE_STEPS_PERMISSION
    )
}



private fun hasReadStepsPermission(context: Context): Boolean {
    val permission = "android.permission.health.READ_STEPS"
    val permissionStatus = context.checkSelfPermission(permission)
    return permissionStatus == PackageManager.PERMISSION_GRANTED
}

private fun requestReadStepsPermission(activity: Activity) {
    val REQUEST_READ_STEPS_PERMISSION = 125
    val permission = "android.permission.health.READ_STEPS"
    ActivityCompat.requestPermissions(
        activity,
        arrayOf(permission),
        REQUEST_READ_STEPS_PERMISSION
    )
}

@SuppressLint("SuspiciousIndentation")
suspend fun insertSteps(healthConnectClient: HealthConnectClient, count: Long, time: Long) {

    Log.e("cccccccccccccc","$count")
    val startTimeMillis: Long = time //12445
    val startTimeInstant: Instant = Instant.ofEpochMilli(startTimeMillis)
    val endTimeMillis: Long = startTimeMillis + (60 * 60 * 1000)
    val endTimeInstant: Instant = Instant.ofEpochMilli(endTimeMillis)
    try {
        val stepsRecord = StepsRecord(
            count = count,
            startTime = startTimeInstant,
            endTime = endTimeInstant,
            startZoneOffset = ZoneOffset.UTC,
            endZoneOffset = ZoneOffset.UTC
        )
        //coroutineScope {
        //  launch {
        healthConnectClient.insertRecords(listOf(stepsRecord))
        Log.d("Data Inserted", "Inserted")
        //}
        //}

    } catch (e: Exception) {
        // Run error handling here
        Log.e("InsertSteps", "Error inserting steps record", e)

    }
}


private suspend fun readHeartRate(healthConnectClient: HealthConnectClient) {
    Log.e("lllllllllll", "lll")
    try {
        val today = ZonedDateTime.now()
        val startOfDay = today.truncatedTo(ChronoUnit.DAYS)
        val response = healthConnectClient.readRecords(
            androidx.health.connect.client.request.ReadRecordsRequest(
                StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(
                    startOfDay.toLocalDateTime(),
                    today.toLocalDateTime()
                )
            )
        )
        for (heartRateRecord in response.records) {
            // Print each heart rate record
            Log.d("HeartRate", "Heart Rate: ${heartRateRecord.count}")
            Log.d("HeartRate", "Time: ${heartRateRecord.startTime}")
            // You can print other properties of the heart rate record as well
        }
    } catch (e: Exception) {
        // Handle exceptions
        Log.e("ReadHeartRate", "Error reading heart rate data", e)
    }
}










/*@Preview
@Composable
fun PreviewMyScreen() {
    val fakeContext = LocalContext.current
    MyScreen(context = fakeContext)
}*/
@Preview
@Composable
fun PreviewMyScreen() {
    val activity = LocalContext.current as Activity
    MyScreen(activity = activity)
}


