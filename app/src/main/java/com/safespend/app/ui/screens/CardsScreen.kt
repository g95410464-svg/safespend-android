package com.safespend.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safespend.app.data.SavedCardEntity
import com.safespend.app.service.CardSecret
import com.safespend.app.service.SecureCardVault
import com.safespend.app.ui.theme.FinanceColors
import com.safespend.app.viewmodel.FinanceViewModel
import java.time.YearMonth
import javax.crypto.Cipher
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(vm: FinanceViewModel, onBack: () -> Unit) {
    val cards by vm.savedCards.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val vault = remember { SecureCardVault() }
    var showAdd by remember { mutableStateOf(false) }
    var revealedId by remember { mutableStateOf<Long?>(null) }
    var revealedSecret by remember { mutableStateOf<CardSecret?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<SavedCardEntity?>(null) }

    val window = (activity as? Activity)?.window
    DisposableEffect(window) {
        val hadSecureFlag = window?.attributes?.flags?.and(WindowManager.LayoutParams.FLAG_SECURE) != 0
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            revealedSecret = null
            if (!hadSecureFlag) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    LaunchedEffect(revealedSecret) {
        if (revealedSecret != null) {
            delay(30_000)
            revealedSecret = null
            revealedId = null
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mis tarjetas", fontWeight = FontWeight.Bold)
                        Text("Bóveda local cifrada", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineMedium) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Agregar tarjeta")
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F3F8)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Lock, null, tint = FinanceColors.Cobalt)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Solo referencia", fontWeight = FontWeight.SemiBold)
                        Text(
                            "No se conecta al banco ni permite pagos. Los datos se cifran en este teléfono y se muestran tras verificar tu biometría.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "El CVV queda guardado cifrado. No lo compartas ni lo muestres a otras personas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FinanceColors.Warning
                        )
                    }
                }
            }

            if (cards.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.CreditCard, null, tint = FinanceColors.Cobalt, modifier = Modifier.size(36.dp))
                        Text("Todavía no guardas tarjetas", fontWeight = FontWeight.Bold)
                        Text("Agrega una para consultar sus datos con huella o rostro.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { showAdd = true }) { Text("Agregar tarjeta") }
                    }
                }
            }

            cards.forEach { card ->
                SavedCardTile(
                    card = card,
                    revealed = revealedId == card.id,
                    secret = if (revealedId == card.id) revealedSecret else null,
                    onReveal = {
                        if (revealedId == card.id && revealedSecret != null) {
                            revealedId = null
                            revealedSecret = null
                        } else {
                            error = null
                            if (activity == null) {
                                error = "No se pudo abrir la verificación biométrica."
                            } else {
                                runCatching { vault.newDecryptCipher(card.iv) }
                                    .onSuccess { cipher -> authenticateCardCipher(
                                        activity, cipher, "Ver tarjeta",
                                        onSuccess = { authCipher ->
                                            runCatching { vault.decrypt(authCipher, card.encryptedDetails) }
                                                .onSuccess { revealedId = card.id; revealedSecret = it }
                                                .onFailure { error = biometricFailureMessage(it) }
                                        },
                                        onError = { error = it }
                                    ) }
                                    .onFailure { error = biometricFailureMessage(it) }
                            }
                        }
                    },
                    onDelete = { deleteTarget = card }
                )
            }

            error?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(it, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            Spacer(Modifier.height(84.dp))
        }
    }

    if (showAdd) AddCardDialog(
        onDismiss = { showAdd = false },
        onSave = { label, secret, brand ->
            error = null
            if (activity == null) {
                error = "No se pudo abrir la verificación biométrica."
            } else {
                runCatching { vault.newEncryptCipher() }
                    .onSuccess { cipher -> authenticateCardCipher(
                        activity, cipher, "Guardar tarjeta",
                        onSuccess = { authCipher ->
                            runCatching { vault.encrypt(authCipher, secret) }
                                .onSuccess { (encrypted, iv) ->
                                    vm.saveCard(
                                        SavedCardEntity(
                                            label = label.ifBlank { brand }, brand = brand,
                                            lastFour = secret.number.takeLast(4), iv = iv,
                                            encryptedDetails = encrypted
                                        )
                                    )
                                    showAdd = false
                                }
                                .onFailure { error = biometricFailureMessage(it) }
                        },
                        onError = { error = it }
                    ) }
                    .onFailure { error = biometricFailureMessage(it) }
            }
        }
    )

    deleteTarget?.let { card ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Eliminar tarjeta") },
            text = { Text("Se borrará la referencia cifrada de ${card.label} •••• ${card.lastFour}.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteCard(card); deleteTarget = null }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SavedCardTile(
    card: SavedCardEntity,
    revealed: Boolean,
    secret: CardSecret?,
    onReveal: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, FinanceColors.Outline)
    ) {
        Column(Modifier.fillMaxWidth().background(
            Brush.linearGradient(listOf(Color(0xFFF4F7FC), Color.White))
        ).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CreditCard, null, tint = FinanceColors.Cobalt)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(card.label, fontWeight = FontWeight.Bold)
                    Text(card.brand.uppercase(), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Eliminar tarjeta", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Text("••••  ••••  ••••  ${card.lastFour}", fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

            if (revealed && secret != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecretRow("Titular", secret.cardholder)
                    SecretRow("Número", formatCardNumber(secret.number))
                    SecretRow("Vence", "${secret.expiryMonth}/${secret.expiryYear}")
                    SecretRow("CVV", secret.cvv)
                }
            }

            TextButton(onClick = onReveal, modifier = Modifier.align(Alignment.End)) {
                Icon(if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                Spacer(Modifier.width(6.dp))
                Text(if (revealed) "Ocultar datos" else "Ver datos · biometría")
            }
        }
    }
}

@Composable
private fun SecretRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall)
        Text(value, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AddCardDialog(onDismiss: () -> Unit, onSave: (String, CardSecret, String) -> Unit) {
    var label by remember { mutableStateOf("") }
    var holder by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var month by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var showCvv by remember { mutableStateOf(false) }
    val cleanNumber = number.filter(Char::isDigit)
    val monthNumber = month.toIntOrNull()
    val yearNumber = year.toIntOrNull()
    val expiryValid = monthNumber != null && monthNumber in 1..12 && yearNumber != null &&
        yearNumber in YearMonth.now().year % 100..99 &&
        YearMonth.of(2000 + yearNumber, monthNumber) >= YearMonth.now()
    val valid = label.isNotBlank() && holder.isNotBlank() && cleanNumber.length in 13..19 &&
        passesLuhn(cleanNumber) && expiryValid && cvv.length in 3..4
    val brand = cardBrand(cleanNumber)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar tarjeta") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Los datos solo se guardan en este teléfono. No guardes tarjetas ajenas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(label, { label = it.take(32) }, label = { Text("Nombre para identificarla") }, singleLine = true)
                OutlinedTextField(holder, { holder = it.take(48) }, label = { Text("Nombre del titular") }, singleLine = true)
                OutlinedTextField(
                    number, { number = it.filter(Char::isDigit).take(19) }, label = { Text("Número de tarjeta") },
                    placeholder = { Text("•••• •••• •••• ••••") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(month, { month = it.filter(Char::isDigit).take(2) }, label = { Text("Mes") },
                        singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                    OutlinedTextField(year, { year = it.filter(Char::isDigit).take(2) }, label = { Text("Año (2 dígitos)") },
                        singleLine = true, modifier = Modifier.weight(1.3f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                }
                OutlinedTextField(
                    cvv, { cvv = it.filter(Char::isDigit).take(4) }, label = { Text("CVV") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (showCvv) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showCvv = !showCvv }) {
                            Icon(if (showCvv) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Mostrar CVV")
                        }
                    }
                )
                Text("Tipo detectado: $brand", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(label.trim(), CardSecret(holder.trim(), cleanNumber, month.padStart(2, '0'), year, cvv), brand)
            }) { Text("Cifrar y guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun authenticateCardCipher(
    activity: FragmentActivity,
    cipher: Cipher,
    title: String,
    onSuccess: (Cipher) -> Unit,
    onError: (String) -> Unit
) {
    val availability = BiometricManager.from(activity)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
        onError("Configura una huella o un rostro biométrico fuerte en los ajustes de Android.")
        return
    }
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                result.cryptoObject?.cipher?.let(onSuccess)
                    ?: onError("La verificación no pudo desbloquear la bóveda.")
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_CANCELED
                ) onError(errString.toString())
            }
        }
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle("SafeSpend no recibe ni comparte estos datos")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .setNegativeButtonText("Cancelar")
        .build()
    prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return current as? FragmentActivity
}

private fun biometricFailureMessage(error: Throwable): String = when {
    error is java.security.UnrecoverableKeyException || error.javaClass.simpleName == "KeyPermanentlyInvalidatedException" ->
        "Cambió la biometría del teléfono y Android bloqueó esta bóveda. Elimina y vuelve a agregar la tarjeta."
    else -> error.message ?: "No se pudo abrir la bóveda cifrada."
}

private fun cardBrand(number: String): String = when {
    number.startsWith("4") -> "Visa"
    number.startsWith("34") || number.startsWith("37") -> "American Express"
    number.startsWith("5") || (number.length >= 2 && number.take(2).toIntOrNull()?.let { it in 22..27 } == true) -> "Mastercard"
    number.startsWith("6") -> "Discover"
    else -> "Tarjeta"
}

private fun passesLuhn(number: String): Boolean {
    if (number.length !in 13..19) return false
    val sum = number.reversed().mapIndexed { index, char ->
        var digit = char.digitToInt()
        if (index % 2 == 1) {
            digit *= 2
            if (digit > 9) digit -= 9
        }
        digit
    }.sum()
    return sum % 10 == 0
}

private fun formatCardNumber(number: String): String = number.chunked(4).joinToString(" ")
