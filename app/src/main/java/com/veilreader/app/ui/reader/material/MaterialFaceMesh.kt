package com.veilreader.app.ui.reader.material

/** Explicit face ownership in source space. Clipping a projected *whole* mesh cannot separate
 * overlapping front/back triangles. Split triangles at the normal horizon before drawing.
 * Storage is bounded and reused; the four-vertex polygon scratch never escapes a frame.
 */
internal class MaterialFaceMesh(columns: Int, rows: Int) {
    private val capacity = columns * rows * 2 * 6
    val vertices = FloatArray(capacity * 2)
    val texture = FloatArray(capacity * 2)
    val colors = IntArray(capacity)
    var count = 0
        private set
    private val polygon = FloatArray(4 * 4)
    private val polygonColors = IntArray(4)
    private var polygonCount = 0

    fun update(geometry: MaterialPageGeometry, sourceWidth: Float, sourceHeight: Float,
               vertexColors: IntArray, front: Boolean) {
        count = 0
        val stride = geometry.columns + 1
        for (row in 0 until geometry.rows) for (column in 0 until geometry.columns) {
            val a = row * stride + column
            val b = a + 1
            val d = a + stride
            val c = d + 1
            triangle(geometry, a, b, c, sourceWidth, sourceHeight, vertexColors, front)
            triangle(geometry, a, c, d, sourceWidth, sourceHeight, vertexColors, front)
        }
    }

    private fun triangle(g: MaterialPageGeometry, a: Int, b: Int, c: Int,
                         width: Float, height: Float, shade: IntArray, front: Boolean) {
        polygonCount = 0
        clipEdge(g, c, a, width, height, shade, front)
        clipEdge(g, a, b, width, height, shade, front)
        clipEdge(g, b, c, width, height, shade, front)
        for (i in 1 until polygonCount - 1) {
            append(0); append(i); append(i + 1)
        }
    }

    private fun clipEdge(g: MaterialPageGeometry, from: Int, to: Int,
                         width: Float, height: Float, shade: IntArray, front: Boolean) {
        val n0 = g.normals[from]
        val n1 = g.normals[to]
        val inside0 = if (front) n0 >= 0f else n0 < 0f
        val inside1 = if (front) n1 >= 0f else n1 < 0f
        if (inside0 != inside1) {
            val t = n0 / (n0 - n1)
            point(g, from, to, t, width, height, interpolateColor(shade[from], shade[to], t))
        }
        if (inside1) point(g, to, to, 0f, width, height, shade[to])
    }

    private fun point(g: MaterialPageGeometry, a: Int, b: Int, t: Float,
                      width: Float, height: Float, color: Int) {
        val offset = polygonCount * 4
        val stride = g.columns + 1
        polygon[offset] = g.vertices[a * 2] + (g.vertices[b * 2] - g.vertices[a * 2]) * t
        polygon[offset + 1] = g.vertices[a * 2 + 1] + (g.vertices[b * 2 + 1] - g.vertices[a * 2 + 1]) * t
        polygon[offset + 2] = ((a % stride) + ((b % stride) - (a % stride)) * t) * width / g.columns
        polygon[offset + 3] = ((a / stride) + ((b / stride) - (a / stride)) * t) * height / g.rows
        polygonColors[polygonCount++] = color
    }

    private fun append(index: Int) {
        val offset = index * 4
        vertices[count * 2] = polygon[offset]
        vertices[count * 2 + 1] = polygon[offset + 1]
        texture[count * 2] = polygon[offset + 2]
        texture[count * 2 + 1] = polygon[offset + 3]
        colors[count++] = polygonColors[index]
    }

    private fun interpolateColor(a: Int, b: Int, t: Float): Int {
        var result = 0
        for (shift in 0..24 step 8) {
            val first = (a ushr shift) and 255
            val second = (b ushr shift) and 255
            result = result or ((first + (second - first) * t).toInt().coerceIn(0, 255) shl shift)
        }
        return result
    }
}
