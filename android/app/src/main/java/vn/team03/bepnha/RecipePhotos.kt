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
    "amaranth" to R.drawable.recipe_amaranth,
    "bean_sprout" to R.drawable.recipe_bean_sprout,
    "beef_onion" to R.drawable.recipe_beef_onion,
    "beef_stew" to R.drawable.recipe_beef_stew,
    "beef_tomato" to R.drawable.recipe_beef_tomato,
    "bitter_melon" to R.drawable.recipe_bitter_melon,
    "bitter_melon_egg" to R.drawable.recipe_bitter_melon_egg,
    "boiled" to R.drawable.recipe_boiled,
    "boiled_chicken" to R.drawable.recipe_boiled_chicken,
    "boiled_pork" to R.drawable.recipe_boiled_pork,
    "boiled_spinach" to R.drawable.recipe_boiled_spinach,
    "bok_choy" to R.drawable.recipe_bok_choy,
    "cabbage_tomato" to R.drawable.recipe_cabbage_tomato,
    "chayote" to R.drawable.recipe_chayote,
    "chayote_soup" to R.drawable.recipe_chayote_soup,
    "chicken_mushroom" to R.drawable.recipe_chicken_mushroom,
    "choy_sum" to R.drawable.recipe_choy_sum,
    "egg_soup" to R.drawable.recipe_egg_soup,
    "egg_tomato" to R.drawable.recipe_egg_tomato,
    "eggplant_tofu" to R.drawable.recipe_eggplant_tofu,
    "eggs" to R.drawable.recipe_eggs,
    "fish" to R.drawable.recipe_fish,
    "fish_tomato_soup" to R.drawable.recipe_fish_tomato_soup,
    "gourd" to R.drawable.recipe_gourd,
    "gourd_egg" to R.drawable.recipe_gourd_egg,
    "green_beans" to R.drawable.recipe_green_beans,
    "katuk" to R.drawable.recipe_katuk,
    "kohlrabi" to R.drawable.recipe_kohlrabi,
    "lean_pork" to R.drawable.recipe_lean_pork,
    "luffa" to R.drawable.recipe_luffa,
    "luffa_stirfry" to R.drawable.recipe_luffa_stirfry,
    "mackerel" to R.drawable.recipe_mackerel,
    "malabar" to R.drawable.recipe_malabar,
    "mushroom_braise" to R.drawable.recipe_mushroom_braise,
    "mushroom_soup" to R.drawable.recipe_mushroom_soup,
    "napa_tofu" to R.drawable.recipe_napa_tofu,
    "pumpkin_stirfry" to R.drawable.recipe_pumpkin_stirfry,
    "radish_soup" to R.drawable.recipe_radish_soup,
    "rib_potato" to R.drawable.recipe_rib_potato,
    "ribs" to R.drawable.recipe_ribs,
    "shrimp" to R.drawable.recipe_shrimp,
    "snakehead" to R.drawable.recipe_snakehead,
    "sour" to R.drawable.recipe_sour,
    "steamed_broccoli" to R.drawable.recipe_steamed_broccoli,
    "steamed_egg" to R.drawable.recipe_steamed_egg,
    "steamed_fish" to R.drawable.recipe_steamed_fish,
    "steamed_shrimp" to R.drawable.recipe_steamed_shrimp,
    "stuffed_tofu" to R.drawable.recipe_stuffed_tofu,
    "sweet_potato_leaves" to R.drawable.recipe_sweet_potato_leaves,
    "tofu_mushroom" to R.drawable.recipe_tofu_mushroom,
    "winter_melon" to R.drawable.recipe_winter_melon,
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
