package vn.team03.bepnha

import kotlinx.coroutines.launch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable fun TodayDashboard(vm: PlannerViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val catalog = remember(vm.engine) { vm.engine.recipes.values.toList() }
    var showAll by rememberSaveable { mutableStateOf(false) }
    var filters by remember { mutableStateOf(false) }
    var mealMenu by remember { mutableStateOf(false) }
    val browsing = showAll || query.isNotBlank() || category != null
    val results = remember(catalog, query, category) {
        val words = PlannerEngine.words(query).split(' ').filter(String::isNotBlank)
        catalog.filter { recipe ->
            (category == null || recipe.kind.name == category) && words.all {
                it in PlannerEngine.words(recipe.name + " " + recipe.ingredients.joinToString { ingredient -> ingredient.name })
            }
        }
    }
    val keyboard = LocalSoftwareKeyboardController.current
    val searchFocus = remember { FocusRequester() }
    val scroll = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()
    val columns = if (LocalDensity.current.fontScale > 1.3f) 1 else 2
    val selectCategory: (String?) -> Unit = { value -> category = value; keyboard?.hide(); scope.launch { scroll.scrollToItem(0) } }
    val browseAll = { showAll = true; category = null; keyboard?.hide(); scope.launch { scroll.scrollToItem(0) }; Unit }
    LazyColumn(Modifier.fillMaxSize().testTag("today-list"), state = scroll, contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item("intro") {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = query, onValueChange = { query = it.take(100) },
                    modifier = Modifier.fillMaxWidth().focusRequester(searchFocus).testTag("today-search"), singleLine = true,
                    shape = RoundedCornerShape(14.dp), placeholder = { Text("Tìm món hoặc nguyên liệu…", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = KitchenMuted) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Xóa tìm kiếm") }
                            Box {
                                FilledTonalIconButton(onClick = { filters = true },
                                    modifier = Modifier.padding(end = 6.dp).testTag("today-filters"), shape = RoundedCornerShape(10.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color(0xFFD7EAB7), contentColor = KitchenGreen)) {
                                    Icon(Icons.Outlined.Settings, "Lọc nhóm món")
                                }
                                DropdownMenu(expanded = filters, onDismissRequest = { filters = false }) {
                                    DropdownMenuItem(text = { Text("Tất cả công thức") }, onClick = { browseAll(); filters = false })
                                    RecipeKind.entries.forEach { kind ->
                                        DropdownMenuItem(text = { Text(kind.label) }, onClick = { selectCategory(kind.name); filters = false })
                                    }
                                }
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = KitchenLine.copy(alpha = 0.6f),
                        focusedBorderColor = KitchenGreen, unfocusedContainerColor = KitchenSurface, focusedContainerColor = KitchenSurface),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }))
            }
        }
        if (browsing) {
            item("categories") { TodayCategories(catalog, category, selectCategory) }
            item("results-heading") {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${results.size} công thức", style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() })
                    if (results.isEmpty()) Text("Chưa tìm thấy món phù hợp. Bạn thử tên món hoặc nguyên liệu khác nhé.", color = KitchenMuted)
                    TextButton(onClick = { query = ""; category = null; showAll = false; keyboard?.hide(); scope.launch { scroll.scrollToItem(0) } }) { Text("Về thực đơn hôm nay") }
                }
            }
            items(results, key = { "result-${it.id}" }) { recipe ->
                Box(Modifier.padding(horizontal = 20.dp)) { RecipeCard(vm, recipe) }
            }
        } else {
            item("plan-heading") {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            TextButton(onClick = { mealMenu = true }, contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.testTag("today-meal-picker")) {
                                Text("Thực đơn ${vm.state.meal.title.lowercase(PlannerEngine.locale)}", style = MaterialTheme.typography.titleMedium, color = KitchenGreen)
                                Text(" ▾", color = KitchenGreen)
                            }
                            DropdownMenu(expanded = mealMenu, onDismissRequest = { mealMenu = false }) {
                                Meal.entries.forEach { meal ->
                                    DropdownMenuItem(text = { Text(meal.title) }, onClick = { vm.selectMeal(meal); mealMenu = false },
                                        modifier = Modifier.testTag("meal-${meal.value}"))
                                }
                            }
                        }
                        TextButton(onClick = { vm.navigate(Screen.PLAN) }, contentPadding = PaddingValues(4.dp)) {
                            Text("Cả tuần ›", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                TodayDateStrip(vm)
            }
            items(vm.menu.withIndex().toList(), key = { it.value.id }) { (index, recipe) ->
                Box(Modifier.padding(horizontal = 20.dp)) { TodayMealRow(vm, recipe, index) }
            }
            item("explore-heading") {
                Box(Modifier.padding(horizontal = 20.dp)) { KitchenSection("Khám phá nhóm món", "Tất cả") { browseAll() } }
            }
            item("categories") { TodayCategories(catalog, category, selectCategory) }
            item("recipes-heading") {
                Box(Modifier.padding(horizontal = 20.dp)) { KitchenSection("Khám phá công thức", "Tất cả") { browseAll() } }
            }
            val recommendations = catalog.filterNot { recipe -> vm.menu.any { it.id == recipe.id } }.take(6)
            items(recommendations.chunked(columns), key = { "discover-${it.first().id}" }) { row ->
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { recipe -> Box(Modifier.weight(1f)) { TodayDiscoveryCard(vm, recipe) } }
                    if (row.size < columns) Spacer(Modifier.weight(1f))
                }
            }
            item("overview-heading") { Box(Modifier.padding(horizontal = 20.dp)) { KitchenSection("Tổng quan bữa ăn") } }
            item("overview") { MealOverview(vm, Modifier.padding(horizontal = 20.dp)) }
            item("fridge") { Box(Modifier.padding(horizontal = 20.dp)) { FridgePromo(vm) } }
        }
    }
}

@Composable private fun TodayDateStrip(vm: PlannerViewModel) {
    val scroll = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(vm.state.date) {
        val index = vm.days.indexOf(vm.state.date)
        if (index >= 0) scroll.animateScrollToItem(index)
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val large = LocalDensity.current.fontScale > 1.3f
        val width = ((maxWidth - 76.dp) / 7).coerceAtLeast(if (large) 64.dp else 40.dp)
        LazyRow(state = scroll, contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(vm.days, key = { it.toString() }) { day ->
                val active = vm.state.date == day
                Surface(onClick = { vm.selectDate(day) }, shape = RoundedCornerShape(12.dp),
                    color = if (active) KitchenGreen else Color(0xFFD7EAB7),
                    contentColor = if (active) Color.White else KitchenGreen,
                    modifier = Modifier.width(width).heightIn(min = 64.dp).testTag("date-$day").semantics { selected = active }) {
                    Column(Modifier.padding(horizontal = 4.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(PlannerEngine.weekdayShort(day), style = MaterialTheme.typography.labelSmall)
                        Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable private fun TodayMealRow(vm: PlannerViewModel, recipe: Recipe, index: Int) {
    val saved = recipe.id in vm.state.favorites
    Column(Modifier.testTag("recipe-${recipe.id}")) {
        Surface(onClick = { vm.openRecipe(recipe.id) }, color = Color.White, shape = RoundedCornerShape(14.dp),
            shadowElevation = 2.dp, border = BorderStroke(1.dp, KitchenLine.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth().testTag("open-${recipe.id}")) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RecipePhoto(recipe, Modifier.size(76.dp).clip(RoundedCornerShape(10.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(recipe.name, style = MaterialTheme.typography.titleSmall, color = KitchenGreen)
                    Surface(color = Color(0xFFEBF4DC), shape = RoundedCornerShape(5.dp)) {
                        Text(recipe.kind.label, Modifier.padding(horizontal = 7.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = KitchenGreen)
                    }
                    Text("${recipe.minutes} phút · ${vm.state.profile.people} người", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                }
                Column {
                    IconButton(onClick = { vm.toggleFavorite(recipe.id) }) {
                        Icon(if (saved) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                            if (saved) "Bỏ lưu ${recipe.name}" else "Lưu ${recipe.name}", Modifier.size(20.dp), tint = if (saved) KitchenTerracotta else KitchenMuted)
                    }
                    IconButton(onClick = { vm.openSwap(index) }, modifier = Modifier.testTag("swap-${recipe.id}")) {
                        Icon(Icons.Outlined.Refresh, "Đổi món ${recipe.name}", Modifier.size(20.dp), tint = KitchenGreen)
                    }
                }
            }
        }
    }
}

@Composable private fun TodayCategories(catalog: List<Recipe>, category: String?, onCategory: (String?) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val kinds = listOf<RecipeKind?>(null) + RecipeKind.entries
        items(kinds, key = { it?.name ?: "all" }) { kind ->
            val active = category == kind?.name
            Surface(onClick = { onCategory(kind?.name) }, shape = RoundedCornerShape(10.dp), shadowElevation = 2.dp,
                color = Color.White, border = BorderStroke(1.dp, if (active) KitchenGreen else KitchenLine.copy(alpha = 0.5f)),
                modifier = Modifier.width(if (LocalDensity.current.fontScale > 1.3f) 110.dp else 82.dp)
                    .testTag("today-category-${kind?.name ?: "all"}").semantics { selected = active }) {
                Column(Modifier.padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    catalog.firstOrNull { kind == null || it.kind == kind }?.let { RecipePhoto(it, Modifier.fillMaxWidth().height(74.dp).clip(RoundedCornerShape(7.dp))) }
                    Text(kind?.label ?: "Tất cả", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, color = KitchenGreen)
                }
            }
        }
    }
}

@Composable private fun TodayDiscoveryCard(vm: PlannerViewModel, recipe: Recipe) {
    val saved = recipe.id in vm.state.favorites
    Surface(onClick = { vm.openRecipe(recipe.id) }, color = Color.White, shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp, border = BorderStroke(1.dp, KitchenLine.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                RecipePhoto(recipe, Modifier.fillMaxWidth().aspectRatio(1.05f).clip(RoundedCornerShape(12.dp)))
                FilledIconButton(onClick = { vm.toggleFavorite(recipe.id) }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = KitchenTerracotta)) {
                    Icon(if (saved) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        if (saved) "Bỏ lưu ${recipe.name}" else "Lưu ${recipe.name}", Modifier.size(20.dp))
                }
            }
            Text(recipe.name, style = MaterialTheme.typography.titleSmall, color = KitchenGreen)
            Surface(color = Color(0xFFEBF4DC), shape = RoundedCornerShape(5.dp)) {
                Text(recipe.kind.label, Modifier.padding(horizontal = 7.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = KitchenGreen)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                KitchenIcon(KitchenGlyph.CLOCK, Modifier.size(14.dp), KitchenMuted)
                Text("${recipe.minutes} phút", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
            }
        }
    }
}
