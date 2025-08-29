package com.example.helpy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helpy.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isLoading by authViewModel.isLoading.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    // Navigate to home if already logged in
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background decorative elements
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.1f)
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size((100 + index * 50).dp)
                        .offset(
                            x = (50 + index * 100).dp,
                            y = (100 + index * 150).dp
                        )
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape((20 + index * 10).dp)
                        )
                        .alpha(0.3f - index * 0.1f)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // App Logo Section
                Card(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "H",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Welcome Text Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Selamat Datang!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Masuk untuk menggunakan Helpy",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Feature highlights
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FeatureItem(
                        icon = Icons.Default.PlayArrow,
                        text = "Cepat",
                        color = MaterialTheme.colorScheme.primary
                    )
                    FeatureItem(
                        icon = Icons.Default.Lock,
                        text = "Aman",
                        color = MaterialTheme.colorScheme.primary
                    )
                    FeatureItem(
                        icon = Icons.Default.Favorite,
                        text = "Terpercaya",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                // Google Sign-In Button
                ElevatedButton(
                    onClick = {
                        scope.launch {
                            authViewModel.signInWithGoogle(context)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 8.dp
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_dialog_info),
                                contentDescription = "Google",
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Text(
                                text = "Masuk dengan Google",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Additional info section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🔒 Login aman dengan Google",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Terms and Privacy
                Text(
                    text = "Dengan masuk, Anda menyetujui syarat dan ketentuan",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Card(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.1f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    modifier = Modifier.size(20.dp),
                    tint = color
                )
            }
        }
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}