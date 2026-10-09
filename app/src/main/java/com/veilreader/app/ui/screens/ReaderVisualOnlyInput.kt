package com.veilreader.app.ui.screens

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize

/**
 * Share hit testing at the overlay's outer layout, not only its AndroidView child.
 * AndroidView interop sharing applies to siblings at its own layout level; without this
 * parent policy a nested GPU host excludes the publication's separate sibling subtree.
 * Observe/consume nothing: Readium remains the sole native gesture/event producer.
 */
internal fun Modifier.readerVisualOnlyInput(): Modifier = then(ReaderVisualOnlyInputElement)

private object ReaderVisualOnlyInputElement : ModifierNodeElement<ReaderVisualOnlyInputNode>() {
    override fun equals(other: Any?) = other === this
    override fun hashCode() = javaClass.hashCode()
    override fun create() = ReaderVisualOnlyInputNode()
    override fun update(node: ReaderVisualOnlyInputNode) = Unit
    override fun InspectorInfo.inspectableProperties() { name = "readerVisualOnlyInput" }
}

private class ReaderVisualOnlyInputNode : Modifier.Node(), PointerInputModifierNode {
    override fun sharePointerInputWithSiblings() = true
    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) = Unit
    override fun onCancelPointerInput() = Unit
}
