package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GarageViewModel
import kotlinx.coroutines.delay
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R

@Composable
fun AuthFlowContainer(
    viewModel: GarageViewModel,
    onAuthSuccess: () -> Unit
) {
    val showSplash by viewModel.showSplash.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isRegistered by viewModel.isRegistered.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(showSplash) {
        if (showSplash) {
            delay(2500) // 2.5 seconds delay for premium feel
            viewModel.dismissSplash()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D0F16), // Dark premium canvas background
                            Color(0xFF171B26)
                        )
                    )
                )
        ) {


            AnimatedContent(
                targetState = when {
                    showSplash -> AuthState.SPLASH
                    !isRegistered -> AuthState.REGISTER
                    !isLoggedIn -> AuthState.LOGIN
                    else -> AuthState.AUTHENTICATED
                },
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                },
                label = "auth_state"
            ) { state ->
                when (state) {
                    AuthState.SPLASH -> {
                        SplashScreenLayout()
                    }
                    AuthState.REGISTER -> {
                        RegisterScreenLayout(
                            onRegister = { name, email, password, brand, model, odometer, plate, vehicleType ->
                                viewModel.registerUser(name, email, password, brand, model, odometer, plate, vehicleType)
                                Toast.makeText(context, "¡Usuario registrado correctamente!", Toast.LENGTH_SHORT).show()
                                onAuthSuccess()
                            },
                            onNavigateToLogin = {
                                // Forces register state to false to show login if they somehow claim they are registered
                                Toast.makeText(context, "Por favor, regístrate para iniciar.", Toast.LENGTH_SHORT).show()
                            },
                            onSkipOnboarding = {
                                viewModel.registerUser(
                                    name = "Usuario Beta",
                                    email = "beta@garagepulse.com",
                                    password = "1234",
                                    vehicleBrand = "Toyota",
                                    vehicleModel = "Hilux",
                                    initialOdometer = 15000.0,
                                    licensePlate = "PRUEBA"
                                )
                                Toast.makeText(context, "¡Modo de Prueba Activado! Bienvenido.", Toast.LENGTH_SHORT).show()
                                onAuthSuccess()
                            }
                        )
                    }
                    AuthState.LOGIN -> {
                        LoginScreenLayout(
                            viewModel = viewModel,
                            onLoginSuccess = {
                                Toast.makeText(context, "¡Sesión iniciada correctamente!", Toast.LENGTH_SHORT).show()
                                onAuthSuccess()
                            }
                        )
                    }
                    AuthState.AUTHENTICATED -> {
                        // Will instantly navigate out to the MainAppContainer
                        SideEffect {
                            onAuthSuccess()
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

enum class AuthState {
    SPLASH, REGISTER, LOGIN, AUTHENTICATED
}

@Composable
fun SplashScreenLayout() {
    var animatedScale by remember { mutableStateOf(0.8f) }
    val scale by animateFloatAsState(
        targetValue = animatedScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logo_scale"
    )

    LaunchedEffect(Unit) {
        animatedScale = 1.0f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon Frame without glow
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo_vector),
                contentDescription = "Logo de GaragePulse",
                modifier = Modifier.size(100.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "GaragePulse",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Mantenimiento Inteligente & Salud Predictiva",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(64.dp))

        CircularProgressIndicator(
            color = Color(0xFFE75C31),
            strokeWidth = 3.dp,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun RegisterScreenLayout(
    onRegister: (String, String, String, String, String, Double, String, String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onSkipOnboarding: () -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }

    // --- Step 2: Personal Profile States ---
    var name by remember { mutableStateOf("Carlos Rodríguez") }
    var email by remember { mutableStateOf("carlos.rod@gmail.com") }
    var password by remember { mutableStateOf("admin123") }
    var confirmPassword by remember { mutableStateOf("admin123") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // --- Step 3: Vehicle Specs States ---
    var vehicleType by remember { mutableStateOf("Car") } // "Car" or "Motorcycle"
    var vehicleBrand by remember { mutableStateOf("") }
    var vehicleModel by remember { mutableStateOf("") }
    var vehiclePlate by remember { mutableStateOf("G-PUL5E") }
    var initialOdometerText by remember { mutableStateOf("15000") }

    var selectedBrand by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("") }

    val carModels = remember {
        mapOf(
            "Toyota" to listOf("Hilux", "Corolla", "Yaris"),
            "Chery" to listOf("Arauca", "Tiggo 2", "Arauca 1.3"),
            "BMW" to listOf("Serie 3", "X5", "M3"),
            "Mercedes" to listOf("Clase C", "GLA 200", "A200"),
            "Porsche" to listOf("911 Carrera", "Macan S", "Cayenne"),
            "Renault" to listOf("Sandero", "Duster", "Kwid")
        )
    }

    val motoModels = remember {
        mapOf(
            "Vespa" to listOf("GTS 300", "Primavera 150", "Sprint 150"),
            "Yamaha" to listOf("R6", "MT-07", "Fazer 250"),
            "Honda" to listOf("CB500", "Africa Twin", "PCX 160"),
            "Suzuki" to listOf("V-Strom 650", "Gixxer 150", "GSX-R1000"),
            "Ducati" to listOf("Monster 821", "Panigale V4", "Multistrada")
        )
    }

    // --- Validations Errors States ---
    var nameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var confirmPasswordError by remember { mutableStateOf(false) }
    var brandError by remember { mutableStateOf(false) }
    var modelError by remember { mutableStateOf(false) }
    var plateError by remember { mutableStateOf(false) }
    var odometerError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Minimalist Page Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            (1..3).forEach { step ->
                val isSelected = currentStep == step
                val dotWidth by animateDpAsState(targetValue = if (isSelected) 24.dp else 8.dp, label = "dotWidth")
                val dotColor by animateColorAsState(targetValue = if (isSelected) Color(0xFFE75C31) else Color.White.copy(alpha = 0.3f), label = "dotColor")
                
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(dotWidth)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }

        // Animating transitions between steps
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width / 2 } + fadeIn(tween(300)))
                            .togetherWith(slideOutHorizontally { width -> -width / 2 } + fadeOut(tween(300)))
                    } else {
                        (slideInHorizontally { width -> -width / 2 } + fadeIn(tween(300)))
                            .togetherWith(slideOutHorizontally { width -> width / 2 } + fadeOut(tween(300)))
                    }
                },
                label = "step_transition"
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (step == 1) Arrangement.Center else Arrangement.Top
                ) {
                    when (step) {
                        1 -> {
                            // --- PASO 1: PRESENTACIÓN DE GARA_PULSE ---
                            Spacer(modifier = Modifier.height(16.dp))

                            // Icon Frame without glow
                            Box(
                                modifier = Modifier.size(110.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_app_logo_vector),
                                    contentDescription = "Logo de GaragePulse",
                                    modifier = Modifier.size(100.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            Text(
                                text = "GaragePulse",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Mantenimiento Inteligente & Salud Predictiva",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // Value propositions presentation cards
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BenefitRow(
                                    icon = Icons.Default.Analytics,
                                    title = "Cuidado Predictivo Avanzado",
                                    description = "Conoce de antemano el desgaste real de bujías, aceite, filtros y frenos según tu odómetro."
                                )
                                BenefitRow(
                                    icon = Icons.Default.Build,
                                    title = "Soporte Proactivo Integral",
                                    description = "Visualiza estatus rápidos de vida útil en colores intuitivos y recibe alertas inteligentes."
                                )
                                BenefitRow(
                                    icon = Icons.Default.Notifications,
                                    title = "Historial Unificado",
                                    description = "Registra y consulta cada servicio mecánico realizado para un control absoluto."
                                )
                            }

                            Spacer(modifier = Modifier.height(44.dp))

                            Button(
                                onClick = { currentStep = 2 },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("onboarding_start_button"),
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE75C31),
                                    contentColor = Color.White
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Crear Mi Cuenta",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        2 -> {
                            // --- PASO 2: DATOS PERSONALES DE USUARIO ---
                            Spacer(modifier = Modifier.height(16.dp))

                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = Color(0xFFE75C31),
                                modifier = Modifier.size(58.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Tus Credenciales",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Usa tus datos reales para resguardar de forma segura tu garaje.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.62f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            // Name input
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    nameError = false
                                },
                                label = { Text("Nombre Completo") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFE75C31)) },
                                isError = nameError,
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_name_input"),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedLabelColor = Color.White.copy(alpha = 0.5f),
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                            if (nameError) {
                                Text(
                                    text = "Por favor, ingresa tu nombre",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Email input
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    emailError = false
                                },
                                label = { Text("Correo Electrónico") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFFE75C31)) },
                                isError = emailError,
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_email_input"),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedLabelColor = Color.White.copy(alpha = 0.5f),
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                            )
                            if (emailError) {
                                Text(
                                    text = "Ingresa un correo electrónico válido",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Password input
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    passwordError = false
                                },
                                label = { Text("Contraseña") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFE75C31)) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (passwordVisible) "Ocultar Contraseña" else "Mostrar Contraseña",
                                            tint = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                isError = passwordError,
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_password_input"),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedLabelColor = Color.White.copy(alpha = 0.5f),
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
                            )
                            if (passwordError) {
                                Text(
                                    text = "La contraseña debe tener al menos 4 caracteres",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Confirm Password input
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    confirmPasswordError = false
                                },
                                label = { Text("Confirmar Contraseña") },
                                leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = Color(0xFFE75C31)) },
                                trailingIcon = {
                                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (confirmPasswordVisible) "Ocultar Contraseña" else "Mostrar Contraseña",
                                            tint = Color.White.copy(alpha = 0.5f)
                                        )
                                    }
                                },
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                isError = confirmPasswordError,
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_confirm_password_input"),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedLabelColor = Color.White.copy(alpha = 0.5f),
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { /* Next step flow */ })
                            )
                            if (confirmPasswordError) {
                                Text(
                                    text = "Las contraseñas no coinciden",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, top = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(36.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { currentStep = 1 },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .testTag("register_back_step_1"),
                                    shape = RoundedCornerShape(26.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.15f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(
                                        text = "Atrás",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                Button(
                                    onClick = {
                                        var hasError = false
                                        if (name.isBlank()) {
                                            nameError = true
                                            hasError = true
                                        }
                                        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                            emailError = true
                                            hasError = true
                                        }
                                        if (password.length < 4) {
                                            passwordError = true
                                            hasError = true
                                        }
                                        if (password != confirmPassword) {
                                            confirmPasswordError = true
                                            hasError = true
                                        }

                                        if (!hasError) {
                                            currentStep = 3
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(52.dp)
                                        .testTag("register_next_step_3"),
                                    shape = RoundedCornerShape(26.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE75C31),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Siguiente",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        3 -> {
                            // --- PASO 3: DETALLES DE VEHÍCULO INICIAL ---
                            Spacer(modifier = Modifier.height(16.dp))

                            Icon(
                                imageVector = Icons.Default.Garage,
                                contentDescription = null,
                                tint = Color(0xFFE75C31),
                                modifier = Modifier.size(58.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Mi Primer Vehículo",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Escribe los detalles para ajustar tu odómetro y calibrar los desgaste predictivos.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.62f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            Spacer(modifier = Modifier.height(28.dp))

                            // 1. SELECT VEHICLE TYPE
                            Text(
                                text = "Tipo de Vehículo",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth().align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(72.dp)
                                        .clickable {
                                            vehicleType = "Car"
                                            selectedBrand = ""
                                            vehicleBrand = ""
                                            selectedModel = ""
                                            vehicleModel = ""
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (vehicleType == "Car") Color(0xFFE75C31).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f)
                                    ),
                                    border = BorderStroke(
                                        width = 1.5.dp,
                                        color = if (vehicleType == "Car") Color(0xFFE75C31) else Color.White.copy(alpha = 0.08f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            tint = if (vehicleType == "Car") Color(0xFFE75C31) else Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Carro",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (vehicleType == "Car") Color(0xFFE75C31) else Color.White
                                        )
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(72.dp)
                                        .clickable {
                                            vehicleType = "Motorcycle"
                                            selectedBrand = ""
                                            vehicleBrand = ""
                                            selectedModel = ""
                                            vehicleModel = ""
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (vehicleType == "Motorcycle") Color(0xFFE75C31).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f)
                                    ),
                                    border = BorderStroke(
                                        width = 1.5.dp,
                                        color = if (vehicleType == "Motorcycle") Color(0xFFE75C31) else Color.White.copy(alpha = 0.08f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TwoWheeler,
                                            contentDescription = null,
                                            tint = if (vehicleType == "Motorcycle") Color(0xFFE75C31) else Color.White.copy(alpha = 0.6f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Moto",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (vehicleType == "Motorcycle") Color(0xFFE75C31) else Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // 2. SELECT BRAND
                            Text(
                                text = "Selecciona la Marca",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth().align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            val brands = if (vehicleType == "Car") {
                                listOf("Toyota", "Chery", "BMW", "Mercedes", "Porsche", "Renault", "Otro")
                            } else {
                                listOf("Vespa", "Yamaha", "Honda", "Suzuki", "Ducati", "Otro")
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                brands.forEach { brand ->
                                    Card(
                                        modifier = Modifier
                                            .width(96.dp)
                                            .clickable {
                                                selectedBrand = brand
                                                if (brand != "Otro") {
                                                    vehicleBrand = brand
                                                    brandError = false
                                                } else {
                                                    vehicleBrand = ""
                                                }
                                                selectedModel = ""
                                                vehicleModel = ""
                                            },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedBrand == brand) Color(0xFFE75C31).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f)
                                        ),
                                        border = BorderStroke(
                                            width = 1.5.dp,
                                            color = if (selectedBrand == brand) Color(0xFFE75C31) else Color.White.copy(alpha = 0.08f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            BrandLogo(brandName = brand, modifier = Modifier.size(48.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = brand,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (selectedBrand == brand) Color(0xFFE75C31) else Color.White.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }

                            if (selectedBrand == "Otro") {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = vehicleBrand,
                                    onValueChange = {
                                        vehicleBrand = it
                                        brandError = false
                                    },
                                    label = { Text("Escribe la Marca") },
                                    leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFFE75C31)) },
                                    isError = brandError,
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("register_vehicle_brand"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White.copy(alpha = 0.15f),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                        focusedLabelColor = Color.White.copy(alpha = 0.5f),
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                )
                            }
                            if (brandError) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Por favor, selecciona o ingresa la marca de tu vehículo",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // 3. SELECT MODEL
                            if (selectedBrand.isNotEmpty()) {
                                Text(
                                    text = "Selecciona el Modelo",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.fillMaxWidth().align(Alignment.Start)
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                val models = if (selectedBrand == "Otro") {
                                    listOf("Otro")
                                } else {
                                    (if (vehicleType == "Car") carModels[selectedBrand] else motoModels[selectedBrand])?.plus("Otro") ?: listOf("Otro")
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    models.forEach { model ->
                                        val modelImage = when {
                                            selectedBrand == "Toyota" && model == "Hilux" -> com.example.R.drawable.img_toyota_hilux_2_8_1781704989578
                                            selectedBrand == "Vespa" && model == "GTS 300" -> com.example.R.drawable.img_vespa_gts_300_1781704971852
                                            selectedBrand == "Chery" && model == "Arauca" -> "https://lh3.googleusercontent.com/aida-public/AB6AXuDqhohr5P2dCxFvCtHhd8LekMaLiFz-QggHJfhsrrcVqfFKuZuDDlSbDBlaT3H1rSTzcsCZw--vWptlNLmvJi6IVrQgEsk1tb6vB8aXeCphAqFozzE6J5S3Ez7B-PBMalJpYSdrPKfUdQX8-rmvmtsu9C1yAo3ZvoSH_EcLZBQYUp8_IH0qADqtNCdOUyIeXA9XwU-t5M_YrkMKrGbmae3u7hdQBSjhbvDWI0bgcvy8ZlXWGyM7pkkB4dZ1W5Y2MDYKh58xeg6N_fE"
                                            vehicleType == "Motorcycle" -> com.example.R.drawable.img_default_moto_1781706682868
                                            else -> com.example.R.drawable.img_default_car_1781706668505
                                        }

                                        Card(
                                            modifier = Modifier
                                                .width(136.dp)
                                                .clickable {
                                                    selectedModel = model
                                                    if (model != "Otro") {
                                                        vehicleModel = model
                                                        modelError = false
                                                    } else {
                                                        vehicleModel = ""
                                                    }
                                                },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (selectedModel == model) Color(0xFFE75C31).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f)
                                            ),
                                            border = BorderStroke(
                                                width = 1.5.dp,
                                                color = if (selectedModel == model) Color(0xFFE75C31) else Color.White.copy(alpha = 0.08f)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (model == "Otro") {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(116.dp, 75.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(Color.White.copy(alpha = 0.05f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = null,
                                                            tint = Color.White.copy(alpha = 0.5f),
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(116.dp, 75.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(Color.White.copy(alpha = 0.03f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        SubcomposeAsyncImage(
                                                            model = modelImage,
                                                            contentDescription = model,
                                                            contentScale = ContentScale.Fit,
                                                            modifier = Modifier.fillMaxSize()
                                                        ) {
                                                            val state = painter.state
                                                            if (state is AsyncImagePainter.State.Loading) {
                                                                CircularProgressIndicator(
                                                                    color = Color(0xFFE75C31),
                                                                    strokeWidth = 2.dp,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            } else if (state is AsyncImagePainter.State.Error) {
                                                                Icon(
                                                                    imageVector = if (vehicleType == "Motorcycle") Icons.Default.TwoWheeler else Icons.Default.DirectionsCar,
                                                                    contentDescription = null,
                                                                    tint = Color.White.copy(alpha = 0.3f),
                                                                    modifier = Modifier.size(28.dp)
                                                                )
                                                            } else {
                                                                SubcomposeAsyncImageContent()
                                                            }
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = model,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = if (selectedModel == model) Color(0xFFE75C31) else Color.White,
                                                    maxLines = 1,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (selectedModel == "Otro") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = vehicleModel,
                                        onValueChange = {
                                            vehicleModel = it
                                            modelError = false
                                        },
                                        label = { Text("Escribe el Modelo") },
                                        leadingIcon = { Icon(Icons.Default.Garage, contentDescription = null, tint = Color(0xFFE75C31)) },
                                        isError = modelError,
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("register_vehicle_model"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color.White.copy(alpha = 0.15f),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                            focusedLabelColor = Color.White.copy(alpha = 0.5f),
                                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                                        ),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                    )
                                }
                                if (modelError) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Por favor, selecciona o ingresa el modelo de tu vehículo",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // 4. LICENSE PLATE
                            Text(
                                text = "Placa o Patente",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth().align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = vehiclePlate,
                                onValueChange = {
                                    vehiclePlate = it
                                    plateError = false
                                },
                                label = { Text("Placa del Vehículo (ej: ABC-123)") },
                                leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = Color(0xFFE75C31)) },
                                isError = plateError,
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_vehicle_plate"),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedLabelColor = Color.White.copy(alpha = 0.5f),
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                            if (plateError) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ingresa la placa o patente de tu vehículo",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // 5. INITIAL ODOMETER
                            Text(
                                text = "Kilometraje / Odómetro Inicial",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth().align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = initialOdometerText,
                                onValueChange = {
                                    initialOdometerText = it
                                    odometerError = false
                                },
                                label = { Text("Kilometraje Inicial (km)") },
                                leadingIcon = { Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFFE75C31)) },
                                isError = odometerError,
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_initial_odometer"),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedLabelColor = Color.White.copy(alpha = 0.5f),
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { /* Done! */ })
                            )
                            if (odometerError) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ingresa un kilometraje inicial válido",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(36.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { currentStep = 2 },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .testTag("register_back_step_2"),
                                    shape = RoundedCornerShape(26.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White.copy(alpha = 0.15f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(
                                        text = "Atrás",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                Button(
                                    onClick = {
                                        var hasError = false
                                        if (vehicleBrand.isBlank()) {
                                            brandError = true
                                            hasError = true
                                        }
                                        if (vehicleModel.isBlank()) {
                                            modelError = true
                                            hasError = true
                                        }
                                        if (vehiclePlate.isBlank()) {
                                            plateError = true
                                            hasError = true
                                        }
                                        val odoDouble = initialOdometerText.toDoubleOrNull()
                                        if (odoDouble == null || odoDouble < 0.0) {
                                            odometerError = true
                                            hasError = true
                                        }

                                        if (!hasError && odoDouble != null) {
                                            onRegister(
                                                name.trim(),
                                                email.trim(),
                                                password,
                                                vehicleBrand.trim(),
                                                vehicleModel.trim(),
                                                odoDouble,
                                                vehiclePlate.trim(), vehicleType
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .height(52.dp)
                                        .testTag("register_submit_button"),
                                    shape = RoundedCornerShape(26.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE75C31),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Finalizar",
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            TextButton(
                                onClick = { onSkipOnboarding() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_bypass_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ExitToApp,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Salir de Prueba",
                                        color = Color.White.copy(alpha = 0.5f),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BenefitRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE75C31).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFE75C31),
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.55f),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun LoginScreenLayout(
    viewModel: GarageViewModel,
    onLoginSuccess: () -> Unit
) {
    val registeredEmail = viewModel.getRegisteredEmail().ifBlank { "prueba@garagepulse.com" }
    val registeredPassword = viewModel.getRegisteredPassword().ifBlank { "1234" }
    var email by remember { mutableStateOf(registeredEmail) }
    var password by remember { mutableStateOf(registeredPassword) }
    var passwordVisible by remember { mutableStateOf(false) }
    var loginFailed by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // App visual icon/branding
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFFE75C31).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFFE75C31),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Bienvenido de nuevo",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Inicia sesión con tus credenciales registradas o utiliza la cuenta de prueba autocompletada por defecto.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (loginFailed) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Correo o contraseña incorrectos. Por favor, verifica.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Email input
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                loginFailed = false
            },
            label = { Text("Correo Electrónico") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFFE75C31)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_email_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.15f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedLabelColor = Color.White.copy(alpha = 0.5f),
                unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password input
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                loginFailed = false
            },
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFE75C31)) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Ocultar Contraseña" else "Mostrar Contraseña",
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_password_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.15f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                focusedLabelColor = Color.White.copy(alpha = 0.5f),
                unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { /* execute login logic */ })
        )

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    val success = viewModel.loginUser(email.trim(), password)
                    if (success) {
                        onLoginSuccess()
                    } else {
                        loginFailed = true
                    }
                } else {
                    loginFailed = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("login_submit_button"),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE75C31),
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Iniciar Sesión",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun BrandLogo(brandName: String, modifier: Modifier = Modifier.size(52.dp)) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when (brandName) {
            "Mercedes-Benz", "Mercedes" -> {
                Canvas(modifier = Modifier.size(28.dp)) {
                    val radius = size.minDimension / 2
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                    drawCircle(color = Color.Black, radius = radius, style = Stroke(width = 1.6f.dp.toPx()))
                    
                    val angle1 = -Math.PI / 2
                    val angle2 = angle1 + 2 * Math.PI / 3
                    val angle3 = angle2 + 2 * Math.PI / 3
                    
                    drawLine(
                        color = Color.Black,
                        start = center,
                        end = androidx.compose.ui.geometry.Offset(
                            (center.x + radius * Math.cos(angle1)).toFloat(),
                            (center.y + radius * Math.sin(angle1)).toFloat()
                        ),
                        strokeWidth = 1.6f.dp.toPx()
                    )
                    drawLine(
                        color = Color.Black,
                        start = center,
                        end = androidx.compose.ui.geometry.Offset(
                            (center.x + radius * Math.cos(angle2)).toFloat(),
                            (center.y + radius * Math.sin(angle2)).toFloat()
                        ),
                        strokeWidth = 1.6f.dp.toPx()
                    )
                    drawLine(
                        color = Color.Black,
                        start = center,
                        end = androidx.compose.ui.geometry.Offset(
                            (center.x + radius * Math.cos(angle3)).toFloat(),
                            (center.y + radius * Math.sin(angle3)).toFloat()
                        ),
                        strokeWidth = 1.6f.dp.toPx()
                    )
                }
            }
            "BMW" -> {
                Canvas(modifier = Modifier.size(28.dp)) {
                    val radius = size.minDimension / 2
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                    drawCircle(color = Color.Black, radius = radius)
                    drawCircle(color = Color.White, radius = radius * 0.8f)
                    
                    drawArc(
                        color = Color(0xFF1C69D8),
                        startAngle = 180f,
                        sweepAngle = 90f,
                        useCenter = true,
                        size = androidx.compose.ui.geometry.Size(radius * 1.6f, radius * 1.6f),
                        topLeft = androidx.compose.ui.geometry.Offset(center.x - radius * 0.8f, center.y - radius * 0.8f)
                    )
                    drawArc(
                        color = Color(0xFF1C69D8),
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = true,
                        size = androidx.compose.ui.geometry.Size(radius * 1.6f, radius * 1.6f),
                        topLeft = androidx.compose.ui.geometry.Offset(center.x - radius * 0.8f, center.y - radius * 0.8f)
                    )
                    
                    drawCircle(color = Color.Black, radius = radius * 0.8f, style = Stroke(width = 0.8f.dp.toPx()))
                }
            }
            "Toyota" -> {
                Canvas(modifier = Modifier.size(28.dp)) {
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                    drawOval(
                        color = Color(0xFFD32F2F),
                        style = Stroke(width = 2.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.9f, size.height * 0.62f),
                        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.05f, size.height * 0.19f)
                    )
                    drawOval(
                        color = Color(0xFFD32F2F),
                        style = Stroke(width = 1.5f.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.65f, size.height * 0.38f),
                        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.175f, size.height * 0.31f)
                    )
                    drawOval(
                        color = Color(0xFFD32F2F),
                        style = Stroke(width = 1.5f.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.32f, size.height * 0.58f),
                        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.34f, size.height * 0.21f)
                    )
                }
            }
            "Porsche" -> {
                Canvas(modifier = Modifier.size(28.dp)) {
                    val path = Path().apply {
                        moveTo(size.width * 0.5f, size.height * 0.05f)
                        lineTo(size.width * 0.85f, size.height * 0.15f)
                        lineTo(size.width * 0.85f, size.height * 0.55f)
                        quadraticTo(size.width * 0.85f, size.height * 0.85f, size.width * 0.5f, size.height * 0.95f)
                        quadraticTo(size.width * 0.15f, size.height * 0.85f, size.width * 0.15f, size.height * 0.55f)
                        lineTo(size.width * 0.15f, size.height * 0.15f)
                        close()
                    }
                    drawPath(path = path, color = Color(0xFFD4AF37))
                    drawPath(path = path, color = Color.Black, style = Stroke(width = 0.8f.dp.toPx()))
                    
                    drawOval(
                        color = Color.Black,
                        size = androidx.compose.ui.geometry.Size(size.width * 0.22f, size.height * 0.32f),
                        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.39f, size.height * 0.34f)
                    )
                }
            }
            "Renault" -> {
                Canvas(modifier = Modifier.size(28.dp)) {
                    val path = Path().apply {
                        moveTo(size.width * 0.5f, size.height * 0.1f)
                        lineTo(size.width * 0.85f, size.height * 0.5f)
                        lineTo(size.width * 0.5f, size.height * 0.9f)
                        lineTo(size.width * 0.15f, size.height * 0.5f)
                        close()
                    }
                    drawPath(path = path, color = Color(0xFFFFD54F))
                    drawPath(path = path, color = Color.Black, style = Stroke(width = 1.5f.dp.toPx()))
                }
            }
            "Vespa" -> {
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("V", style = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF007A33)))
                }
            }
            "Honda" -> {
                Canvas(modifier = Modifier.size(28.dp)) {
                    val radius = size.minDimension / 2
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                    drawCircle(color = Color(0xFFD32F2F), radius = radius)
                    drawCircle(color = Color.White, radius = radius * 0.85f)
                    drawLine(
                        color = Color(0xFFD32F2F),
                        start = androidx.compose.ui.geometry.Offset(size.width * 0.32f, size.height * 0.28f),
                        end = androidx.compose.ui.geometry.Offset(size.width * 0.32f, size.height * 0.72f),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFFD32F2F),
                        start = androidx.compose.ui.geometry.Offset(size.width * 0.68f, size.height * 0.28f),
                        end = androidx.compose.ui.geometry.Offset(size.width * 0.68f, size.height * 0.72f),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawLine(
                        color = Color(0xFFD32F2F),
                        start = androidx.compose.ui.geometry.Offset(size.width * 0.32f, size.height * 0.5f),
                        end = androidx.compose.ui.geometry.Offset(size.width * 0.68f, size.height * 0.5f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            "Yamaha" -> {
                Canvas(modifier = Modifier.size(28.dp)) {
                    val radius = size.minDimension / 2
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                    drawCircle(color = Color(0xFF1976D2), radius = radius, style = Stroke(width = 1.5f.dp.toPx()))
                    val angle1 = -Math.PI / 2
                    val angle2 = angle1 + 2 * Math.PI / 3
                    val angle3 = angle2 + 2 * Math.PI / 3
                    listOf(angle1, angle2, angle3).forEach { angle ->
                        drawLine(
                            color = Color(0xFF1976D2),
                            start = center,
                            end = androidx.compose.ui.geometry.Offset(
                                (center.x + radius * 0.8 * Math.cos(angle)).toFloat(),
                                (center.y + radius * 0.8 * Math.sin(angle)).toFloat()
                            ),
                            strokeWidth = 1.5f.dp.toPx()
                        )
                    }
                }
            }
            "Suzuki" -> {
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("S", style = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Black, fontSize = 21.sp, color = Color(0xFFD32F2F)))
                }
            }
            "Ducati" -> {
                Canvas(modifier = Modifier.size(28.dp)) {
                    val path = Path().apply {
                        moveTo(size.width * 0.5f, size.height * 0.1f)
                        lineTo(size.width * 0.85f, size.height * 0.22f)
                        lineTo(size.width * 0.8f, size.height * 0.65f)
                        quadraticTo(size.width * 0.5f, size.height * 0.93f, size.width * 0.5f, size.height * 0.93f)
                        quadraticTo(size.width * 0.2f, size.height * 0.65f, size.width * 0.2f, size.height * 0.65f)
                        lineTo(size.width * 0.15f, size.height * 0.22f)
                        close()
                    }
                    drawPath(path = path, color = Color(0xFFD32F2F))
                    drawPath(path = path, color = Color.White, style = Stroke(width = 1.5f.dp.toPx()))
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFE75C31), Color(0xFFFF8E53))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (brandName.isNotEmpty()) brandName.take(1).uppercase() else "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
