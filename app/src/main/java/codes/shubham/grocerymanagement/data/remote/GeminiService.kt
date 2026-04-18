package codes.shubham.grocerymanagement.data.remote

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ProductScanResult(
    val name: String,
    val brand: String? = null,
    val category: String = "Other",
    val unit: String = "pcs",
    val estimatedExpiryDays: Int? = null
)

class GeminiService {

    suspend fun analyzeProductImage(bitmap: Bitmap, apiKey: String): ProductScanResult? {
        if (apiKey.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val model = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = apiKey)
                val prompt = """
                    Analyze this grocery/food product image and respond ONLY with this JSON:
                    {
                      "name": "product name",
                      "brand": "brand or null",
                      "category": "one of Dairy/Produce/Bakery/Beverages/Snacks/Frozen/Canned/Meat/Seafood/Condiments/Grains/Other",
                      "unit": "pcs/kg/g/L/ml/pack/box/bottle/can",
                      "estimatedExpiryDays": number_or_null
                    }
                """.trimIndent()
                val response = model.generateContent(content { image(bitmap); text(prompt) })
                val raw = response.text
                    ?.trim()
                    ?.removePrefix("```json")
                    ?.removePrefix("```")
                    ?.removeSuffix("```")
                    ?.trim() ?: return@withContext null
                parseJson(raw)
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun lookupBarcode(barcode: String): ProductScanResult? = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL("https://world.openfoodfacts.org/api/v0/product/$barcode.json")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.setRequestProperty("User-Agent", "GroceryManagementApp/1.0")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            if (json.optInt("status", 0) != 1) return@withContext null
            val p = json.optJSONObject("product") ?: return@withContext null
            val name = p.optString("product_name").takeIf { it.isNotBlank() } ?: return@withContext null
            ProductScanResult(
                name = name,
                brand = p.optString("brands").takeIf { it.isNotBlank() },
                category = "Other",
                unit = "pcs"
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseJson(json: String): ProductScanResult? = try {
        val obj = JSONObject(json)
        ProductScanResult(
            name = obj.optString("name", "Unknown Product"),
            brand = obj.optString("brand").takeIf { it.isNotBlank() && it != "null" },
            category = obj.optString("category", "Other"),
            unit = obj.optString("unit", "pcs"),
            estimatedExpiryDays = obj.optInt("estimatedExpiryDays", -1).takeIf { it > 0 }
        )
    } catch (_: Exception) {
        null
    }
}
