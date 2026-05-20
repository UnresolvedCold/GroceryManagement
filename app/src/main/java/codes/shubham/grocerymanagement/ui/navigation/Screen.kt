package codes.shubham.grocerymanagement.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Scan : Screen("scan")
    data object Settings : Screen("settings")
    data object Audit : Screen("audit")

    data object ProductDetail : Screen("product/{productId}") {
        fun createRoute(productId: Long) = "product/$productId"
    }

    data object AddEditProduct : Screen("add_edit?productId={productId}") {
        fun createRoute(productId: Long? = null): String =
            if (productId != null) "add_edit?productId=$productId" else "add_edit"
    }
}
