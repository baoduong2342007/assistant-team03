package vn.team03.bepnha

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

private val photoResources = mapOf(
    "bread" to R.drawable.recipe_bread,
    "porridge" to R.drawable.recipe_porridge,
    "noodles" to R.drawable.recipe_noodles,
    "phoga" to R.drawable.recipe_phoga,
    "xoi" to R.drawable.recipe_xoi,
    "friedrice" to R.drawable.recipe_friedrice,
    "chicken" to R.drawable.recipe_chicken,
    "tofu" to R.drawable.recipe_tofu,
    "squash" to R.drawable.recipe_squash,
    "cabbage" to R.drawable.recipe_cabbage,
    "broccoli" to R.drawable.recipe_broccoli,
    "cucumber" to R.drawable.recipe_cucumber,
)

/** Normalized bounds of the three dishes in the original family meal photo. */
private val familyPhotoRegions = linkedMapOf(
    "pork" to Rect(0.20f, 0.36f, 0.78f, 0.98f),
    "soup" to Rect(0.00f, 0.00f, 0.48f, 0.49f),
    "greens" to Rect(0.48f, 0.00f, 0.96f, 0.49f),
)

fun hasRecipePhoto(recipeId: String) = recipeId in photoResources || recipeId in familyPhotoRegions

@Composable fun RecipePhoto(recipe: Recipe, modifier: Modifier = Modifier, description: String? = null) {
    val resource = photoResources[recipe.id]
    val painter = if (resource != null) {
        painterResource(resource)
    } else {
        val region = familyPhotoRegions[recipe.id] ?: return
        val bitmap = LocalMealPhoto.current
        remember(bitmap, region) {
            BitmapPainter(
                bitmap,
                srcOffset = IntOffset((bitmap.width * region.left).toInt(), (bitmap.height * region.top).toInt()),
                srcSize = IntSize((bitmap.width * region.width).toInt(), (bitmap.height * region.height).toInt()),
            )
        }
    }
    Image(painter, description, modifier, contentScale = ContentScale.Crop)
}

/** Show the full tray only for the matching menu. Other meals can show their first dish. */
@Composable fun MealPhoto(menu: List<Recipe>, modifier: Modifier = Modifier) {
    if (menu.map(Recipe::id) == familyPhotoRegions.keys.toList()) {
        Image(
            LocalMealPhoto.current,
            "Ảnh minh họa mâm cơm Việt với thịt kho, canh và rau",
            modifier.testTag("meal-photo-family"),
            contentScale = ContentScale.Crop,
        )
    } else {
        val first = menu.firstOrNull() ?: return
        RecipePhoto(first, modifier.testTag("meal-photo-${first.id}"), "Ảnh minh họa ${first.name}")
    }
}
