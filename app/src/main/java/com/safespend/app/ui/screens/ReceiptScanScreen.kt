package com.safespend.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safespend.app.data.AccountType
import com.safespend.app.domain.ParsedReceipt
import com.safespend.app.service.ReceiptScanner
import com.safespend.app.util.toCents
import com.safespend.app.util.toInputString
import com.safespend.app.viewmodel.FinanceViewModel
import com.safespend.app.viewmodel.ScanUiState
import kotlinx.coroutines.launch

@Composable
fun ReceiptScanScreen(vm: FinanceViewModel, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val scan by vm.scan.collectAsStateWithLifecycle()

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.CAMERA) }

    val scanner = remember { ReceiptScanner(ctx) }
    DisposableEffect(Unit) { onDispose { scanner.close(); vm.resetScan() } }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { c -> PreviewView(c).also { scanner.bind(lifecycleOwner, it) } }
            )
            ScanOverlay()
            Column(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Encuadra el ticket o el comprobante del cajero", color = Color.White)
                (scan as? ScanUiState.Error)?.let { Text(it.message, color = MaterialTheme.colorScheme.error) }
                Button(
                    enabled = scan !is ScanUiState.Processing,
                    onClick = {
                        vm.onScanStarted()
                        scope.launch {
                            try { vm.onTextRecognized(scanner.captureAndRecognize()) }
                            catch (e: Exception) { vm.onScanFailed(e.message ?: "No se pudo leer el ticket") }
                        }
                    }
                ) { Text("📸  Capturar") }
            }
        } else {
            Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Necesito permiso de cámara para escanear tickets.", color = Color.White)
                TextButton(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Dar permiso") }
            }
        }

        TextButton(onClick = onClose, modifier = Modifier.statusBarsPadding().align(Alignment.TopStart)) {
            Text("✕ Cerrar", color = Color.White)
        }
        if (scan is ScanUiState.Processing) CircularProgressIndicator(Modifier.align(Alignment.Center))
    }

    (scan as? ScanUiState.Review)?.let { review ->
        ReviewSheet(
            parsed = review.parsed,
            onDismiss = { vm.resetScan() },
            onConfirm = { cents, withdrawal, account, category, note ->
                vm.confirmScan(cents, withdrawal, account, category, note)
                onClose()
            }
        )
    }
}

@Composable
private fun ScanOverlay() {
    Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        drawRect(Color.Black.copy(alpha = 0.55f))
        val w = size.width * 0.85f
        val h = size.height * 0.62f
        val topLeft = Offset((size.width - w) / 2f, (size.height - h) / 2f - 40.dp.toPx())
        val corner = CornerRadius(24.dp.toPx())
        drawRoundRect(Color.Transparent, topLeft, Size(w, h), corner, blendMode = BlendMode.Clear)
        drawRoundRect(Color(0xFF4ADE80), topLeft, Size(w, h), corner, style = Stroke(3.dp.toPx()))
    }
}

/** One-tap confirmation sheet, pre-filled with what OCR + parser found. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewSheet(
    parsed: ParsedReceipt,
    onDismiss: () -> Unit,
    onConfirm: (Long, Boolean, AccountType, String, String) -> Unit
) {
    var amount by remember(parsed) { mutableStateOf(parsed.amountCents?.toInputString() ?: "") }
    var withdrawal by remember(parsed) { mutableStateOf(parsed.isWithdrawal) }
    var account by remember(parsed) { mutableStateOf(AccountType.BANK) }
    var category by remember(parsed) { mutableStateOf(parsed.category) }
    var note by remember(parsed) { mutableStateOf(parsed.merchant) }
    val cents = amount.toCents()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Revisa y confirma", style = MaterialTheme.typography.titleLarge)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !withdrawal, onClick = { withdrawal = false }, label = { Text("💸 Gasto") })
                FilterChip(selected = withdrawal, onClick = { withdrawal = true }, label = { Text("🏧 Retiro (Banco → Efectivo)") })
            }
            if (withdrawal) Text(
                "Un retiro mueve dinero de tu banco a tu efectivo. No cuenta como gasto.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = amount, onValueChange = { amount = it }, label = { Text("Monto") },
                prefix = { Text("$") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                isError = cents == null || cents <= 0,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            if (!withdrawal) {
                AccountChips(account) { account = it }
                OutlinedTextField(category, { category = it }, label = { Text("Categoría") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(note, { note = it }, label = { Text("Comercio / nota") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = { cents?.let { onConfirm(it, withdrawal, account, category, note) } },
                enabled = cents != null && cents > 0,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Confirmar") }
        }
    }
}
