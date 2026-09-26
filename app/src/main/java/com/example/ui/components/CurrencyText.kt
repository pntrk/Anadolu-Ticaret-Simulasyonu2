package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em

@Composable
fun CurrencyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: androidx.compose.ui.text.style.TextOverflow = androidx.compose.ui.text.style.TextOverflow.Clip
) {
    if (!text.contains("₳")) {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign,
            style = style,
            maxLines = maxLines,
            overflow = overflow
        )
        return
    }

    val parts = text.split("₳")
    val annotatedString = buildAnnotatedString {
        parts.forEachIndexed { index, part ->
            append(part)
            if (index < parts.size - 1) {
                appendInlineContent("currencyIcon", "₳")
            }
        }
    }

    val inlineContent = mapOf(
        "currencyIcon" to InlineTextContent(
            Placeholder(
                width = 1.3.em,
                height = 1.3.em,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            AnadoluLiraIcon(modifier = Modifier.fillMaxSize())
        }
    )

    Text(
        text = annotatedString,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
        inlineContent = inlineContent,
        style = style,
        maxLines = maxLines,
        overflow = overflow
    )
}
