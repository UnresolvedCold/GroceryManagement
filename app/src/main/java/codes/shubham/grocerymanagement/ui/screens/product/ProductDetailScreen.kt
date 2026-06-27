package codes.shubham.grocerymanagement.ui.screens.product

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import codes.shubham.grocerymanagement.domain.model.Transaction
import codes.shubham.grocerymanagement.domain.model.TransactionType
import codes.shubham.grocerymanagement.ui.components.ConsumptionEntryDialog
import codes.shubham.grocerymanagement.ui.components.ProductImage
import codes.shubham.grocerymanagement.ui.components.QuantityDialog
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    productId: Long,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ProductDetailViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var consumptionEntryToDelete by remember { mutableStateOf<Transaction?>(null) }

    LaunchedEffect(productId) { viewModel.loadProduct(productId) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onNavigateBack()
    }

    if (state.showAddDialog) {
        QuantityDialog(
            title = "Add to Pantry",
            unit = state.product?.unit ?: "pcs",
            onConfirm = { qty, notes -> viewModel.addQuantity(qty, notes) },
            onDismiss = viewModel::dismissDialogs
        )
    }

    if (state.showConsumeDialog) {
        ConsumptionEntryDialog(
            title = "Consume from Pantry",
            unit = state.product?.unit ?: "pcs",
            onConfirm = { qty, notes, date -> viewModel.consumeQuantity(qty, notes, date) },
            onDismiss = viewModel::dismissDialogs
        )
    }

    state.editingConsumptionEntry?.let { entry ->
        ConsumptionEntryDialog(
            title = "Edit Consumed Entry",
            unit = state.product?.unit ?: "pcs",
            entry = entry,
            onConfirm = { qty, notes, date -> viewModel.updateConsumptionEntry(qty, notes, date) },
            onDismiss = viewModel::dismissDialogs
        )
    }

    consumptionEntryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { consumptionEntryToDelete = null },
            title = { Text("Delete consumed entry?") },
            text = { Text("This will restore ${entry.quantity} ${state.product?.unit.orEmpty()} to the pantry.") },
            confirmButton = {
                Button(
                    onClick = {
                        consumptionEntryToDelete = null
                        viewModel.deleteConsumptionEntry(entry)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { consumptionEntryToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Product?") },
            text = { Text("This will permanently remove ${state.product?.name} and all its history.") },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; viewModel.deleteProduct() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.product?.name ?: "Product", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { state.product?.let { onNavigateToEdit(it.id) } }) {
                        Icon(Icons.Default.Edit, "Edit")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        val product = state.product
        if (product == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProductImage(
                    imagePath = product.imagePath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }

            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(product.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        if (!product.brand.isNullOrBlank()) {
                            Text(product.brand, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(product.category) },
                                icon = { Icon(Icons.Default.Category, null, Modifier.size(16.dp)) }
                            )
                            product.barcodes.forEach { barcode ->
                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            text = barcode,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    icon = { Icon(Icons.Default.QrCode, null, Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Quantity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val qty = if (product.quantity == product.quantity.toLong().toDouble()) product.quantity.toLong().toString() else "%.2f".format(product.quantity)
                            Text(
                                "$qty ${product.unit}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (product.quantity <= product.lowQuantityThreshold) {
                                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        "Low Stock",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilledTonalButton(onClick = viewModel::showAddDialog, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add")
                            }
                            OutlinedButton(onClick = viewModel::showConsumeDialog, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Remove, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Consume")
                            }
                        }
                    }
                }
            }

            product.expiryDate?.let { expiry ->
                item {
                    val days = ChronoUnit.DAYS.between(LocalDate.now(), expiry)
                    val expired = days < 0
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (expired) MaterialTheme.colorScheme.errorContainer
                            else if (days <= 7) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                if (expired || days <= 7) Icons.Default.Warning else Icons.Default.CalendarToday,
                                null,
                                tint = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text("Expiry Date", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    expiry.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    when {
                                        expired -> "Expired ${-days} day(s) ago"
                                        days == 0L -> "Expires today!"
                                        else -> "Expires in $days day(s)"
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            if (!product.notes.isNullOrBlank()) {
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Notes, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Column {
                                Text("Notes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(product.notes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = viewModel::showConsumeDialog) {
                        Icon(Icons.Default.Add, contentDescription = "Add consumed entry")
                    }
                }
            }
            if (state.transactions.isEmpty()) {
                item {
                    Text(
                        "No history entries yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.transactions, key = { it.id }) { tx ->
                    TransactionRow(
                        tx = tx,
                        unit = product.unit,
                        onEdit = { viewModel.editConsumptionEntry(tx) },
                        onDelete = { consumptionEntryToDelete = tx }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    tx: Transaction,
    unit: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val containerColor = when (tx.type) {
        TransactionType.ADD -> MaterialTheme.colorScheme.primaryContainer
        TransactionType.CONSUME -> MaterialTheme.colorScheme.secondaryContainer
        TransactionType.AUDIT -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when (tx.type) {
        TransactionType.ADD -> MaterialTheme.colorScheme.onPrimaryContainer
        TransactionType.CONSUME -> MaterialTheme.colorScheme.onSecondaryContainer
        TransactionType.AUDIT -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val icon = when (tx.type) {
        TransactionType.ADD -> Icons.Default.Add
        TransactionType.CONSUME -> Icons.Default.Remove
        TransactionType.AUDIT -> Icons.Default.FactCheck
    }
    val quantityLabel = when (tx.type) {
        TransactionType.ADD -> "+${tx.quantity} $unit"
        TransactionType.CONSUME -> "-${tx.quantity} $unit"
        TransactionType.AUDIT -> "Audited to ${tx.quantity} $unit"
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = containerColor,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                quantityLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                tx.date.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            tx.recipeName?.let {
                Text(
                    "Recipe: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            tx.notes?.takeIf { it.isNotBlank() && it != "Recipe: ${tx.recipeName}" }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (tx.type == TransactionType.CONSUME) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit consumed entry")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete consumed entry",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
