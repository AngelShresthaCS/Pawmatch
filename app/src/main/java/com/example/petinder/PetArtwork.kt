package com.example.petinder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*

/** Original pet illustrations, available completely offline. */
@Composable
fun PetArtwork(pet: Pet, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = "Illustration of ${pet.name}, ${pet.breed}" }) {
        val artScale = maxOf(size.width / 400f, size.height / 480f)
        val left = (size.width - 400f * artScale) / 2f
        val top = (size.height - 480f * artScale) / 2f
        withTransform({ translate(left, top); scale(artScale, artScale, Offset.Zero) }) {
            val bg = when (pet.species) { "Cat" -> Color(0xFFEBDDCB); "Rabbit" -> Color(0xFFE2DCEB); else -> Color(0xFFD6E2CF) }
            drawRect(bg, size = Size(400f, 480f))
            drawCircle(Color.White.copy(alpha = .24f), 145f, Offset(295f, 125f))
            drawCircle(Color.White.copy(alpha = .18f), 95f, Offset(32f, 295f))
            fun leaf(x: Float, y: Float, rotation: Float, color: Color) { rotate(rotation, Offset(x, y)) { drawOval(color, Offset(x - 12, y - 42), Size(24f, 84f)) } }
            val green = Color(0xFF7C9B78).copy(alpha = .35f)
            drawLine(green, Offset(14f, 400f), Offset(60f, 165f), 3f)
            leaf(37f, 300f, -45f, green); leaf(46f, 245f, 30f, green); leaf(53f, 193f, -20f, green)
            drawLine(green, Offset(385f, 410f), Offset(340f, 240f), 3f)
            leaf(363f, 315f, 40f, green); leaf(345f, 263f, -30f, green)
            drawOval(Color(0xFF547558).copy(alpha = .15f), Offset(67f, 422f), Size(270f, 32f))
            if (pet.species == "Dog") drawDog(pet.id == "milo") else if (pet.species == "Cat") drawCat() else drawRabbit()
            drawCircle(Color.White.copy(alpha = .45f), 3f, Offset(96f, 107f)); drawCircle(Color.White.copy(alpha = .45f), 4f, Offset(330f, 196f))
            drawLine(Color.White.copy(alpha = .6f), Offset(302f, 67f), Offset(302f, 83f), 2f)
            drawLine(Color.White.copy(alpha = .6f), Offset(294f, 75f), Offset(310f, 75f), 2f)
        }
    }
}

private fun DrawScope.drawDog(dark: Boolean) {
    val fur = if (dark) Color(0xFFAF8158) else Color(0xFFEAC17C)
    val shadow = if (dark) Color(0xFF795539) else Color(0xFFC48E4B)
    val light = if (dark) Color(0xFFE0BC90) else Color(0xFFF8DDA7)
    drawOval(fur, Offset(100f, 270f), Size(205f, 245f)); drawOval(light, Offset(150f, 292f), Size(107f, 181f))
    rotate(-14f, Offset(112f, 210f)) { drawOval(shadow, Offset(61f, 117f), Size(94f, 187f)) }
    rotate(14f, Offset(291f, 210f)) { drawOval(shadow, Offset(249f, 117f), Size(94f, 187f)) }
    drawOval(fur, Offset(103f, 100f), Size(198f, 222f)); drawOval(light, Offset(134f, 201f), Size(136f, 102f))
    drawOval(fur.copy(alpha = .7f), Offset(175f, 119f), Size(49f, 115f))
    drawOval(Color(0xFF342C24), Offset(143f, 183f), Size(17f, 22f)); drawOval(Color(0xFF342C24), Offset(244f, 183f), Size(17f, 22f))
    drawCircle(Color.White, 3f, Offset(148f, 188f)); drawCircle(Color.White, 3f, Offset(249f, 188f))
    drawOval(Color(0xFF3D3028), Offset(183f, 224f), Size(38f, 26f)); drawOval(Color(0xFF5E3B2E), Offset(179f, 254f), Size(47f, 34f))
    drawRoundRect(Color(0xFFE69587), Offset(190f, 263f), Size(26f, 37f), CornerRadius(13f)); drawLine(Color(0xFFCB7A70), Offset(203f, 269f), Offset(203f, 289f), 2f)
    drawArc(Color(0xFF4E7967), 0f, 180f, false, Offset(111f, 258f), Size(182f, 70f), style = Stroke(15f))
    drawCircle(Color(0xFFF4CE76), 13f, Offset(202f, 324f)); drawCircle(Color(0xFFD4A352), 8f, Offset(202f, 324f), style = Stroke(1.5f))
    drawOval(fur, Offset(99f, 382f), Size(72f, 83f)); drawOval(fur, Offset(235f, 382f), Size(72f, 83f))
}

private fun DrawScope.drawCat() {
    val fur = Color(0xFFAA9D8E); val light = Color(0xFFE8DED0)
    drawOval(fur, Offset(117f, 271f), Size(179f, 232f)); drawOval(light, Offset(167f, 294f), Size(78f, 191f))
    drawPath(Path().apply { moveTo(107f, 182f); lineTo(102f, 75f); lineTo(181f, 128f); lineTo(236f, 128f); lineTo(304f, 75f); lineTo(298f, 192f); close() }, fur)
    drawPath(Path().apply { moveTo(119f, 142f); lineTo(118f, 99f); lineTo(155f, 133f); close() }, Color(0xFFD4ACA4))
    drawPath(Path().apply { moveTo(263f, 135f); lineTo(290f, 99f); lineTo(286f, 150f); close() }, Color(0xFFD4ACA4))
    drawOval(fur, Offset(100f, 125f), Size(207f, 185f)); drawOval(light, Offset(151f, 222f), Size(107f, 74f))
    drawOval(Color(0xFF6A8063), Offset(135f, 185f), Size(36f, 24f)); drawOval(Color(0xFF6A8063), Offset(239f, 185f), Size(36f, 24f))
    drawOval(Ink, Offset(150f, 186f), Size(7f, 21f)); drawOval(Ink, Offset(253f, 186f), Size(7f, 21f))
    drawPath(Path().apply { moveTo(193f, 239f); lineTo(216f, 239f); lineTo(205f, 253f); close() }, Color(0xFFB4817E))
    for (i in 0..2) { drawLine(Color(0xFF786C60), Offset(165f, 253f + i * 7), Offset(91f, 243f + i * 15), 1.5f); drawLine(Color(0xFF786C60), Offset(245f, 253f + i * 7), Offset(316f, 243f + i * 15), 1.5f) }
    drawArc(Coral, 0f, 180f, false, Offset(131f, 272f), Size(150f, 49f), style = Stroke(12f)); drawCircle(Color(0xFFE5BD67), 10f, Offset(205f, 321f))
    drawOval(light, Offset(131f, 411f), Size(66f, 53f)); drawOval(light, Offset(221f, 411f), Size(66f, 53f))
}

private fun DrawScope.drawRabbit() {
    val fur = Color(0xFFF2EADF); val shadow = Color(0xFFDACCBF)
    drawOval(shadow, Offset(98f, 287f), Size(218f, 180f))
    rotate(-14f, Offset(161f, 200f)) { drawOval(fur, Offset(122f, 37f), Size(65f, 209f)); drawOval(Color(0xFFE4BFB8), Offset(138f, 58f), Size(32f, 157f)) }
    rotate(15f, Offset(248f, 200f)) { drawOval(fur, Offset(220f, 37f), Size(65f, 209f)); drawOval(Color(0xFFE4BFB8), Offset(237f, 58f), Size(32f, 157f)) }
    drawOval(fur, Offset(103f, 168f), Size(207f, 190f)); drawCircle(Ink, 9f, Offset(150f, 250f)); drawCircle(Ink, 9f, Offset(265f, 250f))
    drawCircle(Color.White, 2.5f, Offset(147f, 247f)); drawCircle(Color.White, 2.5f, Offset(262f, 247f))
    drawOval(Color(0xFFDFB1AC), Offset(194f, 280f), Size(25f, 17f)); drawLine(shadow, Offset(206f, 297f), Offset(206f, 311f), 2f)
    drawOval(fur, Offset(111f, 408f), Size(78f, 51f)); drawOval(fur, Offset(230f, 408f), Size(78f, 51f))
}

@Composable
fun AppIcon(name: String, color: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val width = this.size.width; val height = this.size.height
        withTransform({ scale(width / 24f, height / 24f, Offset.Zero) }) {
            val stroke = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            fun line(x: Float, y: Float, x2: Float, y2: Float) = drawLine(color, Offset(x,y), Offset(x2,y2), 1.8f, StrokeCap.Round)
            when (name) {
                "paw", "dog", "cat", "rabbit" -> {
                    if (name == "rabbit") { drawOval(color, Offset(6f, 1f), Size(4f, 11f)); drawOval(color, Offset(14f, 1f), Size(4f, 11f)); drawOval(color, Offset(5f, 11f), Size(14f, 11f)) }
                    else { drawOval(color, Offset(6f, 12f), Size(12f, 9f)); drawOval(color, Offset(2f, 7f), Size(5f, 6f)); drawOval(color, Offset(7f, 2f), Size(5f, 7f)); drawOval(color, Offset(13f, 2f), Size(5f, 7f)); drawOval(color, Offset(18f, 7f), Size(5f, 6f)) }
                }
                "heart" -> drawPath(Path().apply { moveTo(12f,21f); cubicTo(9f,18f,2f,13f,2f,8f); cubicTo(2f,1f,10f,1f,12f,6f); cubicTo(15f,1f,22f,1f,22f,8f); cubicTo(22f,13f,15f,18f,12f,21f); close() }, color, style = stroke)
                "pin" -> { drawPath(Path().apply { moveTo(12f,22f); cubicTo(9f,18f,4f,12f,4f,8f); cubicTo(4f,-1f,20f,-1f,20f,8f); cubicTo(20f,12f,15f,18f,12f,22f); close() }, color, style = stroke); drawCircle(color, 2.7f, Offset(12f,8f), style = stroke) }
                "close" -> { line(6f,6f,18f,18f); line(18f,6f,6f,18f) }
                "filter" -> { line(3f,6f,21f,6f); line(3f,12f,21f,12f); line(3f,18f,21f,18f); drawCircle(Cream,3f,Offset(9f,6f)); drawCircle(color,2.5f,Offset(9f,6f),style=stroke); drawCircle(Cream,3f,Offset(16f,12f)); drawCircle(color,2.5f,Offset(16f,12f),style=stroke); drawCircle(Cream,3f,Offset(8f,18f)); drawCircle(color,2.5f,Offset(8f,18f),style=stroke) }
                "person" -> { drawCircle(color, 4f, Offset(12f,6f), style = stroke); drawArc(color,180f,180f,false,Offset(4f,13f),Size(16f,16f),style=stroke); line(4f,21f,20f,21f) }
                "undo" -> { drawArc(color,210f,280f,false,Offset(5f,5f),Size(15f,15f),style=stroke); line(4f,3f,4f,10f); line(4f,10f,11f,10f) }
                "male" -> { drawCircle(color,6f,Offset(9f,15f),style=stroke); line(13f,11f,21f,3f); line(15f,3f,21f,3f); line(21f,3f,21f,9f) }
                "female" -> { drawCircle(color,6f,Offset(12f,8f),style=stroke); line(12f,14f,12f,23f); line(8f,19f,16f,19f) }
                else -> { drawCircle(color,9f,Offset(12f,12f),style=stroke); line(12f,11f,12f,17f); drawCircle(color,1f,Offset(12f,7f)) }
            }
        }
    }
}
