package com.safespend.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safespend.app.data.AccountType
import com.safespend.app.data.TransactionEntity
import com.safespend.app.data.TxKind
import com.safespend.app.ui.theme.FinanceColors
import com.safespend.app.util.toCents
import com.safespend.app.util.toMoney
import com.safespend.app.viewmodel.FinanceViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun DashboardScreen(
    vm: FinanceViewModel,
    onScan: () -> Unit,
    onAdd: () -> Unit,
    onSubs: () -> Unit,
    onCards: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val cards by vm.savedCards.collectAsStateWithLifecycle()
    var showThreshold by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(FinanceColors.Canvas),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 22.dp, top = 24.dp, end = 22.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SAFESPEND", style = MaterialTheme.typography.labelMedium, color = FinanceColors.Cobalt,
                        fontWeight = FontWeight.Bold)
                    Text("Tu dinero, bajo control", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.size(42.dp).background(Color(0xFF192235), CircleShape), contentAlignment = Alignment.Center) {
                    Text("SS", color = FinanceColors.Cobalt, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (state.belowThreshold) item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showThreshold = true },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3F0)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.WarningAmber, null, tint = FinanceColors.Warning)
                    Column(Modifier.weight(1f)) {
                        Text("Saldo bancario bajo", fontWeight = FontWeight.SemiBold)
                        Text("${state.bankCents.toMoney()} · Límite ${state.thresholdCents.toMoney()}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Editar", color = FinanceColors.Cobalt, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, FinanceColors.Outline)
            ) {
                Column(Modifier.background(Brush.linearGradient(listOf(Color.White, Color(0xFFEEF3FF))))
                    .padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("DISPONIBLE SEGURO", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Text("LOCAL · MANUAL", style = MaterialTheme.typography.labelSmall, color = FinanceColors.Positive)
                    }
                    Text(state.safeToSpendCents.toMoney(), style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Después de reservar tus suscripciones", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = onAdd) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(5.dp)); Text("Movimiento")
                        }
                        OutlinedButton(onClick = onCards) {
                            Icon(Icons.Default.CreditCard, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.size(5.dp));
                            Text("Tarjetas ${cards.size}")
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BalanceTile("Banco", state.bankCents.toMoney(), "Saldo registrado", FinanceColors.Cobalt, Modifier.weight(1f))
                BalanceTile("Efectivo", state.cashCents.toMoney(), "En tu cartera", FinanceColors.Positive, Modifier.weight(1f))
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onSubs),
                colors = CardDefaults.cardColors(containerColor = FinanceColors.Panel), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Lock, null, tint = FinanceColors.Warning)
                    Column(Modifier.weight(1f)) {
                        Text("Bóveda de suscripciones", fontWeight = FontWeight.SemiBold)
                        Text("${state.vaultCents.toMoney()} reservados · Patrimonio ${state.netWorthCents.toMoney()}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = FinanceColors.Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Savings, null, tint = FinanceColors.Positive)
                        Spacer(Modifier.size(9.dp))
                        Text("Ritmo de gasto", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(state.runwayDays?.let { "$it días" } ?: "—", color = FinanceColors.Positive, fontWeight = FontWeight.Bold)
                    }
                    Text("Promedio diario ${state.dailyBurnCents.toMoney()} · ${state.burnRatePct.toInt()}% del disponible",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(
                        progress = { (state.burnRatePct / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(7.dp), color = FinanceColors.Positive,
                        trackColor = Color(0xFF29303A)
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onScan),
                shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, FinanceColors.Outline)) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.ReceiptLong, null, tint = FinanceColors.Cobalt)
                    Column(Modifier.weight(1f)) {
                        Text("Escanea un recibo", fontWeight = FontWeight.SemiBold)
                        Text("Registra gastos sin escribirlos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Actividad reciente", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onAdd) { Text("Agregar") }
            }
        }
        if (state.recent.isEmpty()) item {
            Text("Tus movimientos aparecerán aquí.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp))
        }
        items(state.recent, key = { it.id }) { tx -> TransactionRow(tx) }
    }

    if (showThreshold) ThresholdDialog(
        currentCents = state.thresholdCents,
        onDismiss = { showThreshold = false },
        onSave = { cents -> vm.setThreshold(cents); showThreshold = false }
    )
}

@Composable
private fun BalanceTile(title: String, amount: String, detail: String, tint: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = FinanceColors.Panel),
        shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = tint)
            Text(amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TransactionRow(tx: TransactionEntity) {
    val color = when (tx.kind) {
        TxKind.INCOME -> FinanceColors.Positive
        TxKind.EXPENSE -> MaterialTheme.colorScheme.onSurface
        TxKind.TRANSFER -> FinanceColors.Cobalt
    }
    val sign = when (tx.kind) { TxKind.INCOME -> "+"; TxKind.EXPENSE -> "−"; TxKind.TRANSFER -> "↔" }
    val title = tx.note.ifBlank { tx.category }
    Card(colors = CardDefaults.cardColors(containerColor = FinanceColors.Panel), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(38.dp).background(Color(0xFF202632), CircleShape), contentAlignment = Alignment.Center) {
                Text(if (tx.kind == TxKind.TRANSFER) "↔" else tx.category.take(1).uppercase(), color = FinanceColors.Cobalt,
                    fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${tx.category} · ${DateFormat.getDateInstance(DateFormat.SHORT).format(Date(tx.timestamp))}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$sign${tx.amountCents.toMoney()}", fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

@Composable
private fun ThresholdDialog(currentCents: Long, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var amount by remember(currentCents) { mutableStateOf("${currentCents / 100}.${(currentCents % 100).toString().padStart(2, '0')}") }
    val cents = amount.toCents()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Límite de saldo bajo") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = amount, onValueChange = { amount = it }, label = { Text("Avisarme por debajo de") },
                prefix = { Text("$") }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
            )
        },
        confirmButton = { TextButton(enabled = cents != null && cents >= 0, onClick = { cents?.let(onSave) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
