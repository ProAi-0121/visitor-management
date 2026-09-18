// Offline Mode (Auto Sync) , Multiple Photo Support , Auto-fill for Frequent Visitors,

package com.example.amrapaligm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import androidx.compose.runtime.rememberCoroutineScope
import com.example.amrapaligm.api.RetrofitClient
import com.example.amrapaligm.api.VisitorRequest
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import kotlinx.coroutines.delay

data class VisitorDetails(
    var name: String = "",
    var flatNumbers: TextFieldValue = TextFieldValue(""),
    var phoneNumber: String = ""
)
class MainActivity : ComponentActivity() {
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: Executor
    private var isLoading by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize preferences manager
        PreferencesManager.init(this)
        
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF6200EE),
                    secondary = Color(0xFF03DAC5),
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E),
                    onPrimary = Color.White,
                    onSecondary = Color.Black,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                var showCamera by remember { mutableStateOf(false) }
                var visitorDetails by remember { mutableStateOf(VisitorDetails()) }
                var showDetailsDialog by remember { mutableStateOf(false) }
                var flatNumberError by remember { mutableStateOf<String?>(null) }
                var phoneNumberError by remember { mutableStateOf<String?>(null) }
                var showSettingsDialog by remember { mutableStateOf(false) }
                
                val context = LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        showCamera = true
                    } else {
                        Toast.makeText(context, "Camera permission is required", Toast.LENGTH_LONG).show()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E1E1E),
                                    Color(0xFF121212)
                                )
                            )
                        )
                ) {
                    if (showCamera) {
                        CameraView(
                            onImageCaptured = { 
                                showCamera = false
                                showDetailsDialog = true
                            },
                            onError = { 
                                Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
                            },
                            onBack = {
                                showCamera = false
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(32.dp)
                        ) {
                            // Settings button at top right
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                IconButton(
                                    onClick = { showSettingsDialog = true },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color(0xFF6200EE).copy(alpha = 0.3f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = Color(0xFF6200EE),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Logo with card background
                            Card(
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(RoundedCornerShape(20.dp)),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2A2A2A)
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 8.dp
                                )
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.logo),
                                    contentDescription = "Logo",
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Title
                            Text(
                                text = "Amrapali Visitor Management",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            // Add Visitor Button
                            Button(
                                onClick = {
                                    when (PackageManager.PERMISSION_GRANTED) {
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.CAMERA
                                        ) -> showCamera = true
                                        else -> permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6200EE)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 8.dp,
                                    pressedElevation = 12.dp
                                )
                            ) {
                                Text(
                                    "Add Visitor",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }

                    if (showDetailsDialog) {
                        VisitorDetailsDialog(
                            visitorDetails = visitorDetails,
                            onVisitorDetailsChange = { visitorDetails = it },
                            flatNumberError = flatNumberError,
                            onFlatNumberErrorChange = { flatNumberError = it },
                            phoneNumberError = phoneNumberError,
                            onPhoneNumberErrorChange = { phoneNumberError = it },
                            onDismiss = {
                                showDetailsDialog = false
                                visitorDetails = VisitorDetails()
                            },
                            onSave = {
                                val error = validateFlatNumbers(visitorDetails.flatNumbers.text)
                                if (error == null) {
                                    showDetailsDialog = false
                                    Toast.makeText(
                                        context,
                                        "Visitor ${visitorDetails.name} added for flats: ${visitorDetails.flatNumbers.text}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    visitorDetails = VisitorDetails()
                                } else {
                                    flatNumberError = error
                                }
                            }
                        )
                    }

                    if (showSettingsDialog) {
                        SettingsDialog(
                            onDismiss = { showSettingsDialog = false },
                            context = context
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun VisitorDetailsDialog(
        visitorDetails: VisitorDetails,
        onVisitorDetailsChange: (VisitorDetails) -> Unit,
        flatNumberError: String?,
        onFlatNumberErrorChange: (String?) -> Unit,
        phoneNumberError: String?,
        onPhoneNumberErrorChange: (String?) -> Unit,
        onDismiss: () -> Unit,
        onSave: () -> Unit
    ) {
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current
        var showSuccessDialog by remember { mutableStateOf(false) }
        var showErrorDialog by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var uploadProgress by remember { mutableStateOf(0f) }
        var currentStatus by remember { mutableStateOf("") }

        // Success Dialog
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showSuccessDialog = false
                    onDismiss()
                },
                containerColor = Color(0xFF2A2A2A),
                icon = {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_dialog_info),
                        contentDescription = "Success",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = {
                    Text(
                        "Success!",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        "Visitor data saved successfully",
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            showSuccessDialog = false
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(
                            "OK",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }

        // Loading Dialog with Progress
        if (isLoading) {
            AlertDialog(
                onDismissRequest = { /* Prevent dismiss while loading */ },
                containerColor = Color(0xFF2A2A2A),
                icon = {
                    CircularProgressIndicator(
                        progress = uploadProgress,
                        modifier = Modifier.size(48.dp),
                        color = Color(0xFF6200EE)
                    )
                },
                title = {
                    Text(
                        "Processing...",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            currentStatus,
                            color = Color.White,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        LinearProgressIndicator(
                            progress = uploadProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF6200EE),
                            trackColor = Color(0xFF4A4A4A)
                        )
                        Text(
                            "${(uploadProgress * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                },
                confirmButton = {}
            )
        }

        // Error Dialog with Retry
        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                containerColor = Color(0xFF2A2A2A),
                icon = {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_dialog_alert),
                        contentDescription = "Error",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = {
                    Text(
                        "Error",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        errorMessage,
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showErrorDialog = false
                                coroutineScope.launch {
                                    handleVisitorSubmission(
                                        visitorDetails,
                                        context,
                                        onSuccess = { showSuccessDialog = true },
                                        onError = { error ->
                                            errorMessage = error
                                            showErrorDialog = true
                                        },
                                        onProgress = { progress, status ->
                                            uploadProgress = progress
                                            currentStatus = status
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6200EE)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Text(
                                "Try Again",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        OutlinedButton(
                            onClick = { 
                                showErrorDialog = false
                                onDismiss()
                            },
                            border = BorderStroke(1.dp, Color.Gray),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Text(
                                "Cancel",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }

        // Main Dialog
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color(0xFF2A2A2A),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { 
                Text(
                    "Visitor Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = visitorDetails.name,
                            onValueChange = { onVisitorDetailsChange(visitorDetails.copy(name = it)) },
                            label = { Text("Visitor Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                disabledTextColor = Color.Gray,
                                focusedBorderColor = Color(0xFF6200EE),
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = Color(0xFF6200EE),
                                unfocusedLabelColor = Color.Gray,
                                cursorColor = Color(0xFF6200EE)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                        )

                        Column {
                            OutlinedTextField(
                                value = visitorDetails.phoneNumber,
                                onValueChange = { input ->
                                    // Only allow digits and limit to 10 characters
                                    val filtered = input.filter { it.isDigit() }.take(10)
                                    onVisitorDetailsChange(visitorDetails.copy(phoneNumber = filtered))
                                    onPhoneNumberErrorChange(null)
                                },
                                label = { Text("Phone Number") },
                                placeholder = { Text("10 digit mobile number", color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    disabledTextColor = Color.Gray,
                                    focusedBorderColor = Color(0xFF6200EE),
                                    unfocusedBorderColor = Color.Gray,
                                    focusedLabelColor = Color(0xFF6200EE),
                                    unfocusedLabelColor = Color.Gray,
                                    cursorColor = Color(0xFF6200EE)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                isError = phoneNumberError != null
                            )
                            if (phoneNumberError != null) {
                                Text(
                                    text = phoneNumberError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                )
                            }
                        }

                        Column {
                            OutlinedTextField(
                                value = visitorDetails.flatNumbers,
                                onValueChange = { input ->
                                    val formattedInput = formatFlatNumbers(input)
                                    onVisitorDetailsChange(visitorDetails.copy(flatNumbers = formattedInput))
                                    onFlatNumberErrorChange(null)
                                },
                                label = { Text("Flat Numbers (comma-separated)") },
                                placeholder = { Text("Example: A-101, B-203", color = Color.Gray) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    disabledTextColor = Color.Gray,
                                    focusedBorderColor = Color(0xFF6200EE),
                                    unfocusedBorderColor = Color.Gray,
                                    focusedLabelColor = Color(0xFF6200EE),
                                    unfocusedLabelColor = Color.Gray,
                                    cursorColor = Color(0xFF6200EE)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                isError = flatNumberError != null
                            )
                            if (flatNumberError != null) {
                                Text(
                                    text = flatNumberError,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                )
                            }
                        }
                    }
                    
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(64.dp),
                                    color = Color(0xFF6200EE),
                                    strokeWidth = 6.dp
                                )
                                Text(
                                    "Saving visitor data...",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val flatError = validateFlatNumbers(visitorDetails.flatNumbers.text)
                        val phoneError = validatePhoneNumber(visitorDetails.phoneNumber)
                        
                        when {
                            flatError != null -> onFlatNumberErrorChange(flatError)
                            phoneError != null -> onPhoneNumberErrorChange(phoneError)
                            else -> {
                                isLoading = true
                                coroutineScope.launch {
                                    handleVisitorSubmission(
                                        visitorDetails,
                                        context,
                                        onSuccess = {
                                            isLoading = false
                                            showSuccessDialog = true
                                        },
                                        onError = { error ->
                                            isLoading = false
                                            errorMessage = error
                                            showErrorDialog = true
                                        },
                                        onProgress = { progress, status ->
                                            uploadProgress = progress
                                            currentStatus = status
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6200EE)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(
                        "Save",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, Color.Gray),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(
                        "Cancel",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    private fun validatePhoneNumber(phoneNumber: String): String? {
        return when {
            phoneNumber.isBlank() -> "Phone number is required"
            phoneNumber.length != 10 -> "Phone number must be 10 digits"
            !phoneNumber.all { it.isDigit() } -> "Phone number must contain only digits"
            !phoneNumber.startsWith("6") && 
            !phoneNumber.startsWith("7") && 
            !phoneNumber.startsWith("8") && 
            !phoneNumber.startsWith("9") -> "Invalid phone number format"
            else -> null
        }
    }

    private suspend fun handleVisitorSubmission(
        visitorDetails: VisitorDetails,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onProgress: (Float, String) -> Unit
    ) {
        try {
            // Check backend connectivity
            onProgress(0.1f, "Checking connection...")
            try {
                val response = RetrofitClient.visitorApi.checkConnection()
                if (!response.isSuccessful) {
                    throw Exception("Cannot connect to server")
                }
            } catch (e: Exception) {
                onProgress(0f, "")
                onError("Cannot connect to server. Please check your internet connection.")
                return
            }

            onProgress(0.2f, "Processing image...")
            
            // Get the last captured image
            val outputDirectory = context.cacheDir
            val photoFile = File(outputDirectory, "visitor_image.jpg")
            
            if (!photoFile.exists()) {
                onProgress(0f, "")
                onError("No image captured")
                return
            }

            // Load and optimize bitmap
            onProgress(0.4f, "Optimizing image...")
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(photoFile.path, options)

            options.apply {
                inJustDecodeBounds = false
                inSampleSize = calculateInSampleSize(options, 2048, 2048)
            }

            val bitmap = BitmapFactory.decodeFile(photoFile.path, options)
            
            onProgress(0.6f, "Preparing data...")
            val base64Image = convertBitmapToBase64(bitmap)
            bitmap.recycle()

            // Create and send request
            onProgress(0.8f, "Sending data...")
            val request = VisitorRequest(
                name = visitorDetails.name,
                flatNumbers = visitorDetails.flatNumbers.text,
                phoneNumber = visitorDetails.phoneNumber,
                image = base64Image
            )

            val response = RetrofitClient.visitorApi.addVisitor(request)
            
            if (response.isSuccessful) {
                onProgress(1f, "Complete!")
                delay(500) // Short delay to show completion
                onSuccess()
            } else {
                throw Exception(response.errorBody()?.string() ?: "Unknown error occurred")
            }
        } catch (e: Exception) {
            onProgress(0f, "")
            onError(e.message ?: "Failed to process visitor data")
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun convertBitmapToBase64(originalBitmap: Bitmap): String {
        try {
            // Calculate new dimensions while maintaining aspect ratio
            val maxDimension = 1024 // Max width or height
            val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
            val newWidth: Int
            val newHeight: Int
            
            if (ratio > 1) { // Width is greater than height
                newWidth = maxDimension
                newHeight = (maxDimension / ratio).toInt()
            } else {
                newHeight = maxDimension
                newWidth = (maxDimension * ratio).toInt()
            }

            // Create the final bitmap with all transformations
            val matrix = Matrix().apply {
                // First scale
                val scaleX = newWidth.toFloat() / originalBitmap.width
                val scaleY = newHeight.toFloat() / originalBitmap.height
                postScale(scaleX, scaleY)
                // Then rotate
                postRotate(90f)
            }

            // Apply all transformations at once
            val transformedBitmap = Bitmap.createBitmap(
                originalBitmap,
                0,
                0,
                originalBitmap.width,
                originalBitmap.height,
                matrix,
                true
            )
            
            val outputStream = ByteArrayOutputStream()
            // Compress with 80% quality - good balance between quality and size
            transformedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            
            // Recycle the transformed bitmap if it's different from original
            if (transformedBitmap != originalBitmap) {
                transformedBitmap.recycle()
            }
            
            val imageBytes = outputStream.toByteArray()
            return "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun formatFlatNumbers(input: TextFieldValue): TextFieldValue {
        val text = input.text
        val selection = input.selection
        
        // If the input is empty, return as is
        if (text.isEmpty()) {
            return input
        }

        val parts = text.split(",").map { it.trim() }
        
        val formattedParts = parts.mapIndexed { index, part ->
            when {
                part.isEmpty() -> ""
                // Don't format if it's just a wing letter or wing letter with hyphen
                part.matches(Regex("^[A-Ca-c](-)?$")) -> {
                    if (part.length == 1) {
                        part.uppercase()
                    } else {
                        "${part[0].uppercase()}-"
                    }
                }
                // Auto-format complete flat numbers
                part.startsWith("a", ignoreCase = true) && !part.contains("-") ->
                    "A-${part.substring(1)}"
                part.startsWith("b", ignoreCase = true) && !part.contains("-") ->
                    "B-${part.substring(1)}"
                part.startsWith("c", ignoreCase = true) && !part.contains("-") ->
                    "C-${part.substring(1)}"
                else -> {
                    // Get the first character and format it if it's a wing letter
                    val firstCharUpper = part.firstOrNull()?.uppercase() ?: ""
                    when {
                        firstCharUpper in listOf("A", "B", "C") -> "$firstCharUpper${part.substring(1)}"
                        else -> part
                    }
                }
            }
        }

        val formattedText = formattedParts.joinToString(",")
        
        // If no changes were made to the text, return original input to preserve cursor
        if (formattedText == text) {
            return input
        }

        // Calculate new cursor position
        val newCursorPos = when {
            selection.start == text.length -> formattedText.length
            else -> selection.start
        }

        return TextFieldValue(
            text = formattedText,
            selection = TextRange(newCursorPos)
        )
    }

    private fun validateFlatNumbers(flatNumbers: String): String? {
        if (flatNumbers.isBlank()) {
            return "Flat numbers cannot be empty"
        }

        val flats = flatNumbers.split(",").map { it.trim() }
        for (flat in flats) {
            // Check basic format: wing-floor0flat
            val pattern = "^[A-Ca-c]-[1-9]0[1-4]$".toRegex()
            if (!pattern.matches(flat)) {
                return "Invalid flat format. Use format: A-101, B-201, etc. (middle number must be 0)"
            }

            val wing = flat[0].uppercaseChar()
            val floorNumber = flat.substring(2, 3).toInt()
            val middleNumber = flat[3]
            val flatNumber = flat[4].toString().toInt()

            // Validate middle number is always 0
            if (middleNumber != '0') {
                return "Middle number must be 0"
            }

            // Validate floor number based on wing
            when (wing) {
                'A', 'B' -> if (floorNumber > 7) {
                    return "Wing $wing has maximum 7 floors"
                }
                'C' -> if (floorNumber > 8) {
                    return "Wing C has maximum 8 floors"
                }
            }

            // Validate flat number (1-4)
            if (flatNumber !in 1..4) {
                return "Flat number must be between 1 and 4"
            }
        }
        return null
    }
}

@Composable
fun CameraView(
    onImageCaptured: () -> Unit,
    onError: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx -> 
                PreviewView(ctx).also { previewView ->
                    val preview = Preview.Builder().build()
                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    try {
                        val cameraProvider = ProcessCameraProvider.getInstance(ctx).get()
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        preview.setSurfaceProvider(previewView.surfaceProvider)
                    } catch (e: Exception) {
                        onError(e.message ?: "Failed to start camera")
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Back button with improved visibility
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(48.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.7f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Camera capture button
        Button(
            onClick = {
                takePhoto(
                    imageCapture = imageCapture,
                    context = context,
                    onImageCaptured = onImageCaptured,
                    onError = onError
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .size(72.dp),
            shape = RoundedCornerShape(36.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6200EE)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                painter = painterResource(android.R.drawable.ic_menu_camera),
                contentDescription = "Take Photo",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

private fun takePhoto(
    imageCapture: ImageCapture?,
    context: Context,
    onImageCaptured: () -> Unit,
    onError: (String) -> Unit
) {
    imageCapture?.let { capture ->
        // Create a file to save the image
        val photoFile = File(context.cacheDir, "visitor_image.jpg")
        
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    try {
                        onImageCaptured()
                    } catch (e: Exception) {
                        onError(e.message ?: "Failed to save image")
                    }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    onError(exception.message ?: "Failed to capture image")
                }
            }
        )
    } ?: onError("Failed to initialize camera")
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    context: Context
) {
    var baseUrl by remember { mutableStateOf(PreferencesManager.getBaseUrl()) }
    var urlError by remember { mutableStateOf<String?>(null) }
    var showSaveSuccess by remember { mutableStateOf(false) }

    if (showSaveSuccess) {
        AlertDialog(
            onDismissRequest = {
                showSaveSuccess = false
                onDismiss()
            },
            containerColor = Color(0xFF2A2A2A),
            icon = {
                Icon(
                    painter = painterResource(android.R.drawable.ic_dialog_info),
                    contentDescription = "Success",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Success!",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    "URL updated successfully",
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveSuccess = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(
                        "OK",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2A2A2A),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Text(
                "Server Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Server URL:",
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = {
                        baseUrl = it
                        urlError = null
                    },
                    label = { Text("Base URL") },
                    placeholder = { Text("http://example.com:8080/") },
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6200EE),
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = Color(0xFF6200EE),
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = Color(0xFF6200EE)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp
                    )
                )

                if (urlError != null) {
                    Text(
                        urlError!!,
                        color = Color(0xFFFF5252),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Current URL: ${PreferencesManager.getBaseUrl()}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val error = validateUrl(baseUrl)
                    if (error != null) {
                        urlError = error
                    } else {
                        PreferencesManager.setBaseUrl(baseUrl)
                        RetrofitClient.resetRetrofit()
                        showSaveSuccess = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6200EE)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text(
                    "Save",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, Color.Gray),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text(
                    "Cancel",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

private fun validateUrl(url: String): String? {
    return when {
        url.isBlank() -> "URL cannot be empty"
        !url.startsWith("http://") && !url.startsWith("https://") -> 
            "URL must start with http:// or https://"
        !url.endsWith("/") -> "URL must end with /"
        url.length < 15 -> "URL seems too short"
        else -> null
    }
}