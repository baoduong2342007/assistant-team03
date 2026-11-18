package vn.team03.bepnha

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun KitchenButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = 56.dp), enabled = enabled,
        shape = RoundedCornerShape(12.dp), content = content)
}

@Composable fun KitchenOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 56.dp), enabled = enabled,
        shape = RoundedCornerShape(12.dp), content = content)
}

@Composable fun KitchenTonalButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.heightIn(min = 56.dp), enabled = enabled,
        shape = RoundedCornerShape(12.dp), content = content)
}
