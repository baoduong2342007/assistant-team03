@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package vn.team03.bepnha

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable fun AssistantScreen(vm: PlannerViewModel) {
    val scroll = rememberLazyListState()
    val canSend = !vm.assistantBusy && vm.assistantProposal == null
    LaunchedEffect(vm.assistantHistory.size, vm.assistantProposal, vm.assistantError) {
        scroll.animateScrollToItem((scroll.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
    }
    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp).testTag("assistant-screen")) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = scroll,
            contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("${vm.state.meal.title} · ${PlannerEngine.fullDate(vm.state.date)} · ${vm.state.profile.people} người", style = MaterialTheme.typography.bodyMedium)
                vm.assistantRecipeId?.let { Text("Đang hỏi về: ${vm.engine.recipes[it]?.name}") }
                Text("Hỏi về 14 công thức và nhờ Bếp Nhà chuẩn bị bữa ăn. Bạn sẽ xem lại trước khi thay đổi.", style = MaterialTheme.typography.bodyMedium)
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vm.assistantSuggestions().forEach { suggestion ->
                        AssistChip(onClick = { vm.askAssistant(suggestion) }, enabled = canSend,
                            label = { Text(suggestion) }, modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
            }
            items(vm.assistantHistory) { item ->
                Surface(color = if (item.role == "user") MaterialTheme.colorScheme.surface else KitchenSoft,
                    shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (item.role == "user") "Bạn" else "Bếp Nhà", style = MaterialTheme.typography.labelLarge)
                        Text(item.text, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        item.recipeIds.forEach { id ->
                            TextButton(onClick = { vm.openRecipe(id) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Xem ${vm.engine.recipes[id]?.name}") }
                        }
                    }
                }
            }
            if (vm.assistantBusy) item { Text("Đang tìm gợi ý…", Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            vm.assistantProposal?.let { proposal -> item {
                KitchenCard(Modifier.testTag("assistant-preview")) {
                    Text("Xem lại thay đổi", style = MaterialTheme.typography.titleMedium)
                    proposal.descriptions.forEach { Text(it) }
                    Button(onClick = vm::confirmAssistantProposal, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("assistant-confirm")) { Text("Xác nhận") }
                    OutlinedButton(onClick = vm::cancelAssistantProposal, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Hủy") }
                }
            } }
            vm.assistantError?.let { error -> item {
                KitchenCard {
                    Text(error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    Button(onClick = { vm.askAssistant() }, enabled = canSend && vm.assistantQuestion.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Thử lại") }
                }
            } }
        }
        OutlinedTextField(value = vm.assistantQuestion, onValueChange = { vm.assistantQuestion = it.take(1000) },
            label = { Text("Bạn cần Bếp Nhà giúp gì?") }, modifier = Modifier.fillMaxWidth().testTag("assistant-input"),
            minLines = 1, maxLines = 3, enabled = canSend,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { vm.askAssistant() }))
        FlowRow(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.askAssistant() }, enabled = canSend && vm.assistantQuestion.isNotBlank(), modifier = Modifier.heightIn(min = 48.dp)) { Text("Gửi câu hỏi") }
            OutlinedButton(onClick = vm::lookupOffline, enabled = canSend && vm.assistantQuestion.isNotBlank(), modifier = Modifier.heightIn(min = 48.dp)) { Text("Tra cứu offline") }
        }
    }
}
