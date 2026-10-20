package com.safespend.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.safespend.app.data.AccountType
import com.safespend.app.util.toCents
import com.safespend.app.viewmodel.FinanceViewModel

private enum class Mode(val label: String) {
    EXPENSE("💸 Gasto"), INCOME("💰 Ingreso"), BANK_TO_CASH("🏧 Banco → Efectivo"), CASH_TO_BANK("🏦 Efectivo → Banco")
}

@Composable
fun AddTransactionScreen(vm: FinanceViewModel, onBack: () -> Unit) {
    var mode by remember { mutableStateOf(Mode.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var account by remember { mutableStateOf(AccountType.BANK) }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val cents = amount.toCents()
    val isTransfer = mode == Mode.BANK_TO_CASH || mode == Mode.CASH_TO_BANK

    ScreenScaffold("Nuevo movimiento", onBack) { pad ->
        Column(
            Modifier.padding(pad).padding(horizontal = 24.dp, vertical = 22.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Mode.entries.take(2).forEach { FilterChip(mode == it, { mode = it }, { Text(it.label) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Mode.entries.drop(2).forEach { FilterChip(mode == it, { mode = it }, { Text(it.label) }) }
            }

            OutlinedTextField(
                value = amount, onValueChange = { amount = it }, label = { Text("Monto") },
                prefix = { Text("$") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            if (isTransfer) {
                Text("Es un movimiento interno: no cuenta como gasto.")
            } else {
                AccountChips(account) { account = it }
                OutlinedTextField(
                    category, { category = it }, label = { Text("Categoría") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(note, { note = it }, label = { Text("Nota") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Button(
                enabled = cents != null && cents > 0,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val c = cents ?: return@Button
                    when (mode) {
                        Mode.EXPENSE -> vm.addExpense(c, account, category, note)
                        Mode.INCOME -> vm.addIncome(c, account, category, note)
                        Mode.BANK_TO_CASH -> vm.transfer(c, AccountType.BANK, AccountType.CASH, note.ifBlank { "Retiro" })
                        Mode.CASH_TO_BANK -> vm.transfer(c, AccountType.CASH, AccountType.BANK, note.ifBlank { "Depósito" })
                    }
                    onBack()
                }
            ) { Text("Guardar") }
        }
    }
}
