package com.example.helpy.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.helpy.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onUpdateProfile: (String, String, String, Uri?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Ambil user dari Firebase Auth
    val currentUser = authViewModel.currentUser.collectAsState().value

    // Local state
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // Image picker launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) photoUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 20.dp,end = 20.dp, top = 50.dp, bottom = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Judul
        Text(
            text = "Profile Details",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "View and edit your profile",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Foto profil
        Image(
            painter = rememberAsyncImagePainter(
                model = photoUri ?: currentUser?.photoUrl
            ),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .clickable { launcher.launch("image/*") },
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Nama
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Email (readonly)
        OutlinedTextField(
            value = currentUser?.email ?: "",
            onValueChange = {},
            label = { Text("Email Address") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Phone
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Address
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
            leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    onUpdateProfile(name, phone, address, photoUri)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp)
        ) {
            Text("Update Profile")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                authViewModel.signOut()
                onLogout()
            },
            modifier = Modifier
                .align(Alignment.End)
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red,
                contentColor = Color.White
            )
        ) {
            Text("Log Out")
        }


    }
}
