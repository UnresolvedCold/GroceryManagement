package codes.shubham.grocerymanagement.data.remote

data class PendingScan(
    val barcode: String? = null,
    val imagePath: String? = null,
    val scanResult: ProductScanResult? = null
)

class ScanResultStore {
    private var pending: PendingScan? = null

    fun store(pendingScan: PendingScan) { pending = pendingScan }

    fun consumeAndGet(): PendingScan? = pending.also { pending = null }
}
