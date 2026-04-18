package codes.shubham.grocerymanagement.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import codes.shubham.grocerymanagement.ui.screens.addedit.AddEditProductScreen
import codes.shubham.grocerymanagement.ui.screens.home.HomeScreen
import codes.shubham.grocerymanagement.ui.screens.product.ProductDetailScreen
import codes.shubham.grocerymanagement.ui.screens.scan.ScanScreen
import codes.shubham.grocerymanagement.ui.screens.settings.SettingsScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToScan = { navController.navigate(Screen.Scan.route) },
                onNavigateToProduct = { id -> navController.navigate(Screen.ProductDetail.createRoute(id)) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToAddEdit = { navController.navigate(Screen.AddEditProduct.createRoute()) }
            )
        }

        composable(Screen.Scan.route) {
            ScanScreen(
                onNavigateToProduct = { id -> navController.navigate(Screen.ProductDetail.createRoute(id)) },
                onNavigateToAddEdit = {
                    navController.navigate(Screen.AddEditProduct.createRoute())
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.LongType })
        ) { backStack ->
            val productId = backStack.arguments!!.getLong("productId")
            ProductDetailScreen(
                productId = productId,
                onNavigateToEdit = { id -> navController.navigate(Screen.AddEditProduct.createRoute(productId = id)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AddEditProduct.route,
            arguments = listOf(
                navArgument("productId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStack ->
            val productId = backStack.arguments!!.getLong("productId").takeIf { it != -1L }
            AddEditProductScreen(
                productId = productId,
                onSaved = { navController.popBackStack(Screen.Home.route, inclusive = false) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
