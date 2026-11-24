package vn.team03.bepnha

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val FieldBackground = Color(0xFFF4F1EA)

/** First run only: a photo splash with two ways in, then a one-card family setup. No account is needed. */
@Composable fun WelcomeFlow(vm: PlannerViewModel) {
    var setup by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = setup) { setup = false }
    Box(Modifier.fillMaxSize().background(KitchenGreen).testTag("welcome")) {
        Image(painterResource(R.drawable.family_meal), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        AnimatedContent(setup, label = "welcome step", transitionSpec = {
            (fadeIn(tween(ComponentMotionMillis)) + slideInVertically(tween(ComponentMotionMillis)) { it / 8 }) togetherWith
                fadeOut(tween(ComponentMotionMillis))
        }) { setupStep ->
            if (setupStep) WelcomeSetup(vm, onBack = { setup = false })
            else WelcomeIntro(onStart = { setup = true }, onGuest = { vm.finishWelcome() })
        }
    }
}

@Composable private fun WelcomeLogo(size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
        KitchenIcon(KitchenGlyph.BOWL, Modifier.size(size * 0.5f), KitchenGreen)
    }
}

@Composable private fun WelcomeIntro(onStart: () -> Unit, onGuest: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
        0f to KitchenGreen.copy(alpha = 0.15f), 0.45f to KitchenGreen.copy(alpha = 0.55f), 1f to KitchenGreen.copy(alpha = 0.97f)))) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.weight(1f))
            WelcomeLogo(76.dp)
            Spacer(Modifier.height(16.dp))
            Text("Bếp Nhà", style = MaterialTheme.typography.headlineMedium, color = Color.White,
                modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(6.dp))
            Text("Lên thực đơn · Đi chợ · Vào bếp", style = MaterialTheme.typography.bodyLarge, color = KitchenOnGreen, textAlign = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            Button(onClick = onStart, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = KitchenGreen),
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("welcome-start")) {
                Text("Bắt đầu", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.3f))
                Text("hoặc", Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.bodySmall, color = KitchenOnGreen)
                HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.3f))
            }
            TextButton(onClick = onGuest, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("welcome-guest"),
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                Text("Dùng thử ngay", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable private fun WelcomeSetup(vm: PlannerViewModel, onBack: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var people by rememberSaveable { mutableIntStateOf(vm.state.profile.people) }
    Column(Modifier.fillMaxSize()) {
        // The photo stays visible above the card, tinted like the reference's header.
        Box(Modifier.fillMaxWidth().weight(0.32f).background(KitchenGreen.copy(alpha = 0.6f)).statusBarsPadding(),
            contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WelcomeLogo(40.dp)
                Text("Bếp Nhà", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            }
        }
        Column(Modifier.fillMaxWidth().weight(0.68f).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Color.White)
            .verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding().padding(24.dp).testTag("welcome-setup"),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Chào mừng bạn!", style = KitchenHeroTitle, color = KitchenInk, modifier = Modifier.semantics { heading() })
                Text("Cho Bếp Nhà biết một chút về gia đình để tính đúng định lượng và chi phí.",
                    style = MaterialTheme.typography.bodyMedium, color = KitchenMuted)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Tên gia đình", style = MaterialTheme.typography.labelLarge, color = KitchenInk)
                TextField(name, { name = it.take(35) }, Modifier.fillMaxWidth().testTag("welcome-name"), singleLine = true,
                    placeholder = { Text("Ví dụ: Nhà mình, Nhà bà Lan") }, shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    colors = TextFieldDefaults.colors(unfocusedContainerColor = FieldBackground, focusedContainerColor = FieldBackground,
                        unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Số người ăn", style = MaterialTheme.typography.labelLarge, color = KitchenInk)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FieldBackground).padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    StepButton("−", "Bớt một người", people > 1) { people-- }
                    Text("$people người", Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }.testTag("welcome-people"),
                        style = MaterialTheme.typography.titleMedium, color = KitchenInk, textAlign = TextAlign.Center)
                    StepButton("+", "Thêm một người", people < 12) { people++ }
                }
            }
            Text("Giờ ăn, khẩu vị và dị ứng có thể chỉnh sau trong mục Gia đình.",
                style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
            Spacer(Modifier.weight(1f, fill = false).heightIn(min = 8.dp))
            Button(onClick = { vm.finishWelcome(name, people) }, shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("welcome-finish")) {
                Text("Bắt đầu lên thực đơn", style = MaterialTheme.typography.titleSmall)
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Quay lại") }
        }
    }
}

@Composable private fun StepButton(label: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(10.dp), color = Color.White,
        border = BorderStroke(1.dp, KitchenCardBorder), modifier = Modifier.size(48.dp).semantics { contentDescription = description }) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.titleLarge, color = if (enabled) KitchenGreen else KitchenMuted.copy(alpha = 0.4f))
        }
    }
}
