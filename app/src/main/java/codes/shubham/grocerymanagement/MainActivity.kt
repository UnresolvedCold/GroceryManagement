package codes.shubham.grocerymanagement

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import codes.shubham.grocerymanagement.ui.navigation.NavGraph
import codes.shubham.grocerymanagement.ui.theme.GroceryManagementTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GroceryManagementTheme {
                NavGraph()
            }
        }
    }
}
