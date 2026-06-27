package codes.shubham.grocerymanagement.ui.screens.recipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import codes.shubham.grocerymanagement.data.repository.GroceryRepository
import codes.shubham.grocerymanagement.domain.model.Product
import codes.shubham.grocerymanagement.domain.model.Recipe
import codes.shubham.grocerymanagement.domain.model.RecipeIngredientInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecipeIngredientEditorState(
    val localId: Int,
    val productId: Long? = null,
    val quantity: String = ""
)

data class RecipeEditorState(
    val recipeId: Long? = null,
    val name: String = "",
    val notes: String = "",
    val ingredients: List<RecipeIngredientEditorState> = emptyList(),
    val errorMessage: String? = null
)

data class RecipesUiState(
    val recipes: List<Recipe> = emptyList(),
    val products: List<Product> = emptyList(),
    val editor: RecipeEditorState? = null,
    val isLoading: Boolean = true
)

class RecipesViewModel(
    private val groceryRepository: GroceryRepository
) : ViewModel() {
    private val _editor = MutableStateFlow<RecipeEditorState?>(null)
    private var nextIngredientId = 1

    val uiState: StateFlow<RecipesUiState> = combine(
        groceryRepository.getRecipes(),
        groceryRepository.getAllProducts(),
        _editor
    ) { recipes, products, editor ->
        RecipesUiState(
            recipes = recipes.sortedWith(compareByDescending<Recipe> { it.canMake }.thenBy { it.name.lowercase() }),
            products = products.sortedBy { it.name.lowercase() },
            editor = editor,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecipesUiState()
    )

    fun startAddRecipe() {
        _editor.value = RecipeEditorState(
            ingredients = listOf(newIngredient(uiState.value.products.firstOrNull()?.id))
        )
    }

    fun startEditRecipe(recipe: Recipe) {
        _editor.value = RecipeEditorState(
            recipeId = recipe.id,
            name = recipe.name,
            notes = recipe.notes.orEmpty(),
            ingredients = recipe.ingredients.map {
                RecipeIngredientEditorState(
                    localId = nextIngredientId++,
                    productId = it.productId,
                    quantity = it.requiredQuantity.formatQuantity()
                )
            }.ifEmpty { listOf(newIngredient(uiState.value.products.firstOrNull()?.id)) }
        )
    }

    fun dismissEditor() {
        _editor.value = null
    }

    fun onNameChange(name: String) {
        _editor.update { it?.copy(name = name, errorMessage = null) }
    }

    fun onNotesChange(notes: String) {
        _editor.update { it?.copy(notes = notes) }
    }

    fun addIngredient() {
        _editor.update { editor ->
            editor?.copy(
                ingredients = editor.ingredients + newIngredient(uiState.value.products.firstOrNull()?.id),
                errorMessage = null
            )
        }
    }

    fun removeIngredient(localId: Int) {
        _editor.update { editor ->
            editor?.copy(
                ingredients = editor.ingredients.filterNot { it.localId == localId },
                errorMessage = null
            )
        }
    }

    fun updateIngredientProduct(localId: Int, productId: Long) {
        _editor.update { editor ->
            editor?.copy(
                ingredients = editor.ingredients.map {
                    if (it.localId == localId) it.copy(productId = productId) else it
                },
                errorMessage = null
            )
        }
    }

    fun updateIngredientQuantity(localId: Int, quantity: String) {
        _editor.update { editor ->
            editor?.copy(
                ingredients = editor.ingredients.map {
                    if (it.localId == localId) it.copy(quantity = quantity) else it
                },
                errorMessage = null
            )
        }
    }

    fun saveRecipe() {
        val editor = _editor.value ?: return
        val cleanName = editor.name.trim()
        if (cleanName.isBlank()) {
            _editor.update { it?.copy(errorMessage = "Recipe name is required") }
            return
        }

        val ingredients = editor.ingredients.mapNotNull { row ->
            val productId = row.productId ?: return@mapNotNull null
            val quantity = row.quantity.toDoubleOrNull()?.takeIf { it > 0.0 } ?: return@mapNotNull null
            RecipeIngredientInput(productId = productId, quantity = quantity)
        }
        if (ingredients.isEmpty()) {
            _editor.update { it?.copy(errorMessage = "Add at least one ingredient with quantity") }
            return
        }

        viewModelScope.launch {
            groceryRepository.upsertRecipe(
                recipeId = editor.recipeId,
                name = cleanName,
                notes = editor.notes,
                ingredients = ingredients
            )
            _editor.value = null
        }
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            groceryRepository.deleteRecipe(recipe)
        }
    }

    fun consumeRecipe(recipe: Recipe) {
        if (!recipe.canMake) return
        viewModelScope.launch {
            groceryRepository.consumeRecipe(recipe.id)
        }
    }

    private fun newIngredient(productId: Long? = null): RecipeIngredientEditorState =
        RecipeIngredientEditorState(localId = nextIngredientId++, productId = productId)

    private fun Double.formatQuantity(): String =
        if (this == toLong().toDouble()) toLong().toString() else "%.2f".format(this)
}
