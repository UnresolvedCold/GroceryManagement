package codes.shubham.grocerymanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import codes.shubham.grocerymanagement.domain.model.Product
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysUntilExpiry = product.expiryDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
    val isExpired = daysUntilExpiry != null && daysUntilExpiry < 0
    val isExpiringSoon = daysUntilExpiry != null && daysUntilExpiry in 0..7
    val isLowStock = product.quantity <= product.lowQuantityThreshold

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProductImage(
                imagePath = product.imagePath,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!product.brand.isNullOrBlank()) {
                    Text(
                        text = product.brand,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuantityBadge(quantity = product.quantity, unit = product.unit, isLow = isLowStock)
                    if (isExpired) ExpiryBadge("Expired", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                    else if (isExpiringSoon) ExpiryBadge(
                        if (daysUntilExpiry == 0L) "Expires today"
                        else "Expires in ${daysUntilExpiry}d",
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            if (isLowStock || isExpired) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CompactProductCard(product: Product, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val daysUntilExpiry = product.expiryDate?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
    val isLowStock = product.quantity <= product.lowQuantityThreshold

    Card(
        onClick = onClick,
        modifier = modifier.width(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ProductImage(
                imagePath = product.imagePath,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Text(
                text = product.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            QuantityBadge(quantity = product.quantity, unit = product.unit, isLow = isLowStock)
            daysUntilExpiry?.let { days ->
                if (days <= 7) {
                    val label = if (days < 0) "Expired" else if (days == 0L) "Today" else "${days}d left"
                    ExpiryBadge(
                        label = label,
                        bg = if (days < 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                        fg = if (days < 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun QuantityBadge(quantity: Double, unit: String, isLow: Boolean) {
    val qty = if (quantity == quantity.toLong().toDouble()) quantity.toLong().toString() else "%.1f".format(quantity)
    Surface(
        color = if (isLow) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = "$qty $unit",
            style = MaterialTheme.typography.labelSmall,
            color = if (isLow) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun ExpiryBadge(label: String, bg: Color, fg: Color) {
    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ProductImage(imagePath: String?, modifier: Modifier = Modifier) {
    if (!imagePath.isNullOrBlank() && File(imagePath).exists()) {
        SubcomposeAsyncImage(
            model = File(imagePath),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            loading = { Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) },
            error = { CategoryPlaceholder(modifier) }
        )
    } else {
        CategoryPlaceholder(modifier)
    }
}

@Composable
private fun CategoryPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.BrokenImage,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}
