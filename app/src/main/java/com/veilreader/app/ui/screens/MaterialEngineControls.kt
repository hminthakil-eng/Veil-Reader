package com.veilreader.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.veilreader.app.R
import com.veilreader.app.domain.PageMaterial
import com.veilreader.app.domain.ReaderAppearance

@Composable
internal fun MaterialEngineControls(appearance: ReaderAppearance, onChange: (ReaderAppearance) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
            value = appearance.materialEngineEnabled, role = Role.Switch,
            onValueChange = { onChange(appearance.copy(materialEngineEnabled = it)) }
        ), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.material_engine_enable), modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium)
            Switch(appearance.materialEngineEnabled, onCheckedChange = null)
        }
        if (appearance.materialEngineEnabled) {
            Column(Modifier.selectableGroup()) {
                PageMaterial.entries.forEach { material ->
                    val selected = material == appearance.pageMaterial
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected,
                        role = Role.RadioButton,
                        onClick = { onChange(appearance.copy(pageMaterial = material)) }),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected, onClick = null)
                        Text(stringResource(when (material) {
                            PageMaterial.GLOSSY -> R.string.material_glossy
                            PageMaterial.MATTE -> R.string.material_matte
                            PageMaterial.PARCHMENT -> R.string.material_parchment
                            PageMaterial.PAPYRUS -> R.string.material_papyrus
                        }))
                    }
                }
            }
        }
    }
}
