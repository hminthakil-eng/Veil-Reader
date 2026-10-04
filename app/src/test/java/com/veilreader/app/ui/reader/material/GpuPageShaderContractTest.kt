package com.veilreader.app.ui.reader.material

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GpuPageShaderContractTest {

    @Test
    fun `vertex shader has unique and declared uniforms`() {
        assertShaderUniformContract(
            shader = GpuMaterialPageCurlView.VERTEX_SHADER
        )
    }

    @Test
    fun `fragment shader has unique and declared uniforms`() {
        assertShaderUniformContract(
            shader = GpuMaterialPageCurlView.FRAGMENT_SHADER
        )
    }

    @Test
    fun `vertex and fragment varyings match exactly`() {
        val varyingRegex =
            Regex("""\bvarying\s+(\w+)\s+(v[A-Za-z0-9_]+)\s*;""")
        fun varyings(shader: String): Map<String, String> =
            varyingRegex.findAll(shader)
                .associate { match ->
                    match.groupValues[2] to match.groupValues[1]
                }

        assertEquals(
            varyings(GpuMaterialPageCurlView.VERTEX_SHADER),
            varyings(GpuMaterialPageCurlView.FRAGMENT_SHADER)
        )
    }

    @Test
    fun `renderer critical uniforms stay present`() {
        val source =
            GpuMaterialPageCurlView.VERTEX_SHADER +
                "\n" +
                GpuMaterialPageCurlView.FRAGMENT_SHADER
        val required = setOf(
            "uCylinderPosition",
            "uCylinderTilt",
            "uCylinderRadius",
            "uPageAspect",
            "uSideSign",
            "uShadowPass",
            "uFrontTexture",
            "uBackTexture",
            "uHasBackTexture",
            "uTexelSize",
            "uFrontTint",
            "uBackTint",
            "uEdgeTint",
            "uRoughness",
            "uSpecular",
            "uTranslucency",
            "uVisualAlpha"
        )

        required.forEach { name ->
            assertTrue(
                "Missing critical GPU page uniform: $name",
                source.contains(name)
            )
        }
    }

    private fun assertShaderUniformContract(shader: String) {
        val declarationRegex =
            Regex("""\buniform\s+\w+\s+(u[A-Za-z0-9_]+)\s*;""")
        val tokenRegex =
            Regex("""\b(u[A-Z][A-Za-z0-9_]*)\b""")

        val declarations =
            declarationRegex.findAll(shader)
                .map { it.groupValues[1] }
                .toList()
        assertEquals(
            "Duplicate uniform declaration in shader",
            declarations.size,
            declarations.toSet().size
        )

        val declared = declarations.toSet()
        val used =
            tokenRegex.findAll(shader)
                .map { it.groupValues[1] }
                .toSet()

        val undeclared = used - declared
        assertTrue(
            "Undeclared GPU shader uniforms: $undeclared",
            undeclared.isEmpty()
        )
    }
}
