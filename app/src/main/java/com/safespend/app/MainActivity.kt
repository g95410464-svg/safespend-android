package com.safespend.app

import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.fragment.app.FragmentActivity
import com.safespend.app.ui.screens.AddTransactionScreen
import com.safespend.app.ui.screens.CardsScreen
import com.safespend.app.ui.screens.DashboardScreen
import com.safespend.app.ui.screens.ReceiptScanScreen
import com.safespend.app.ui.screens.SubscriptionsScreen
import com.safespend.app.ui.theme.SafeSpendTheme
import com.safespend.app.viewmodel.FinanceViewModel
import com.safespend.app.viewmodel.FinanceViewModelFactory

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FinanceApp).container
        setContent {
            SafeSpendTheme {
                val vm: FinanceViewModel = viewModel(factory = FinanceViewModelFactory(container))
                BiometricEntryGate { AppNav(vm) }
            }
        }
    }
}

@Composable
private fun BiometricEntryGate(content: @Composable () -> Unit) {
    val activity = androidx.compose.ui.platform.LocalContext.current as? FragmentActivity
    var unlocked by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var hasStartedOnce by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> if (unlocked) {
                    unlocked = false
                    message = null
                }
                Lifecycle.Event.ON_START -> {
                    if (hasStartedOnce && !unlocked) attempt++
                    hasStartedOnce = true
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(activity, attempt) {
        if (unlocked) return@LaunchedEffect
        if (activity == null) {
            message = "No se pudo iniciar la verificación de seguridad."
            return@LaunchedEffect
        }
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
        val availability = BiometricManager.from(activity).canAuthenticate(authenticators)
        if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
            message = when (availability) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Registra una huella o un rostro fuerte en los ajustes de Android."
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Este teléfono no tiene biometría compatible."
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "La biometría no está disponible ahora. Inténtalo de nuevo."
                else -> "Configura una huella o un rostro fuerte para entrar a SafeSpend."
            }
            return@LaunchedEffect
        }

        message = null
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlocked = true
                    message = null
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    message = if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_CANCELED
                    ) "Verifica tu identidad para abrir SafeSpend." else errString.toString()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear SafeSpend")
            .setSubtitle("Usa tu huella o rostro para acceder a tus finanzas")
            .setAllowedAuthenticators(authenticators)
            .setNegativeButtonText("Cancelar")
            .build()
        prompt.authenticate(info)
    }

    if (unlocked) {
        content()
    } else {
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("SS", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium)
                    Text("SafeSpend", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                    Text("Tu información financiera está protegida.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium)
                    message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Spacer(Modifier.height(2.dp))
                    Button(onClick = { message = null; attempt++ }, shape = RoundedCornerShape(14.dp)) {
                        Text("Verificar con biometría")
                    }
                    TextButton(onClick = { activity?.finish() }) {
                        Text("Salir")
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNav(vm: FinanceViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBottomBar = destination?.route != "scan"

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val items = listOf(
                        Triple("dashboard", "⌂", "Inicio"),
                        Triple("cards", "▣", "Tarjetas"),
                        Triple("scan", "▤", "Escanear"),
                        Triple("subs", "◈", "Bóveda"),
                        Triple("add", "+", "Agregar")
                    )
                    items.forEach { (route, symbol, label) ->
                        val selected = destination?.hierarchy?.any { it.route == route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(route) {
                                    popUpTo("dashboard") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(symbol) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    ) { contentPadding ->
        NavHost(
            navController = nav,
            startDestination = "dashboard",
            modifier = Modifier.padding(contentPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    vm,
                    onScan = { nav.navigate("scan") },
                    onAdd = { nav.navigate("add") },
                    onSubs = { nav.navigate("subs") },
                    onCards = { nav.navigate("cards") }
                )
            }
            composable("cards") { CardsScreen(vm, onBack = { nav.popBackStack() }) }
            composable("scan") { ReceiptScanScreen(vm, onClose = { nav.popBackStack() }) }
            composable("add") { AddTransactionScreen(vm, onBack = { nav.popBackStack() }) }
            composable("subs") { SubscriptionsScreen(vm, onBack = { nav.popBackStack() }) }
        }
    }
}
