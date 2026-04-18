package codes.shubham.grocerymanagement

import android.app.Application
import codes.shubham.grocerymanagement.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class GroceryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@GroceryApp)
            modules(appModule)
        }
    }
}
