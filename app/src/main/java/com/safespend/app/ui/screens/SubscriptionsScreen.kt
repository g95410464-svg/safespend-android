package com.safespend.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safespend.app.ui.theme.FinanceColors
import com.safespend.app.util.toCents
import com.safespend.app.util.toMoney
import com.safespend.app.viewmodel.FinanceViewModel
import java.time.YearMonth

@Composable
fun SubscriptionsScreen(vm: FinanceViewModel, onBack: () -> Unit) {
    val subs by vm.subscriptions.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    val month = remember { YearMonth.now().toString() }

    ScreenScaffold(
        title = "Vault de suscripciones",
        onBack = onBack,
        fab = { FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "Agregar suscripción") } }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 22.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🔒 Bloqueado en el banco", style = MaterialTheme.typography.labelLarge)
                        Text(state.vaultCents.toMoney(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Banco libre: ${state.safeBankCents.toMoney()}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (subs.isEmpty()) item {
                Text("Agrega tus pagos fijos (ej. Google AI Pro · \$19.99) para reservarlos.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(subs, key = { it.id }) { sub ->
                val paid = sub.lastPaidMonth == month
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(sub.name, fontWeight = FontWeight.SemiBold)
                            Text("${sub.amountCents.toMoney()} · día ${sub.dayOfMonth}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (paid) "✅ Pagada este mes" else "🔒 Reservada en el Vault",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (paid) FinanceColors.Positive else FinanceColors.Warning
                            )
                        }
                        TextButton(onClick = { vm.paySubscription(sub) }, enabled = !paid) { Text("Pagar") }
                        IconButton(onClick = { vm.deleteSubscription(sub) }) { Icon(Icons.Default.Delete, "Eliminar") }
                    }
                }
            }
        }
    }

    if (showAdd) AddSubscriptionDialog(
        onDismiss = { showAdd = false },
        onSave = { name, cents, day -> vm.saveSubscription(name, cents, day); showAdd = false }
    )
}

@Composable
private fun AddSubscriptionDialog(onDismiss: () -> Unit, onSave: (String, Long, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("1") }
    val cents = amount.toCents()
    val dayInt = day.toIntOrNull()
    val valid = name.isNotBlank() && cents != null && cents > 0 && dayInt != null && dayInt in 1..31

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva suscripción") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true)
                OutlinedTextField(
                    amount, { amount = it }, label = { Text("Monto mensual") }, prefix = { Text("$") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    day, { day = it }, label = { Text("Día de cobro (1–31)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSave(name, cents!!, dayInt!!) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
