package codes.shubham.grocerymanagement.di

import androidx.room.Room
import codes.shubham.grocerymanagement.data.db.GroceryDatabase
import codes.shubham.grocerymanagement.data.preferences.UserPreferencesRepository
import codes.shubham.grocerymanagement.data.remote.GeminiService
import codes.shubham.grocerymanagement.data.remote.ScanResultStore
import codes.shubham.grocerymanagement.data.repository.GroceryRepository
import codes.shubham.grocerymanagement.ui.screens.addedit.AddEditProductViewModel
import codes.shubham.grocerymanagement.ui.screens.home.HomeViewModel
import codes.shubham.grocerymanagement.ui.screens.product.ProductDetailViewModel
import codes.shubham.grocerymanagement.ui.screens.scan.ScanViewModel
import codes.shubham.grocerymanagement.ui.screens.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { Room.databaseBuilder(androidContext(), GroceryDatabase::class.java, "grocery.db").build() }
    single { get<GroceryDatabase>().productDao() }
    single { get<GroceryDatabase>().transactionDao() }
    single { GroceryRepository(get(), get()) }
    single { UserPreferencesRepository(androidContext()) }
    single { GeminiService() }
    single { ScanResultStore() }

    viewModel { HomeViewModel(get(), get()) }
    viewModel { ScanViewModel(get(), get(), get(), get()) }
    viewModel { AddEditProductViewModel(get(), get()) }
    viewModel { ProductDetailViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
}
