package com.luntikius.wallet.ui.components

import android.content.Intent
import android.net.Uri
import android.text.Html
import android.text.Spanned
import android.text.style.URLSpan
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/**
 * Composable that renders HTML text with clickable links.
 * Supports basic HTML tags: <a>, <b>, <i>, <br>
 */
@Composable
fun HtmlText(
    html: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    linkColor: Color = Color(0xFF2196F3),
) {
    val context = LocalContext.current
    val annotatedString = remember(html, color, linkColor) {
        parseHtmlToAnnotatedString(html, color, linkColor)
    }

    ClickableText(
        text = annotatedString,
        style = style.copy(color = color),
        modifier = modifier,
        softWrap = true,
        onClick = { offset ->
            // Handle link clicks
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Ignore invalid URLs
                    }
                }
        },
    )
}

/**
 * Parse HTML string to AnnotatedString with formatting and clickable links.
 */
private fun parseHtmlToAnnotatedString(html: String, baseColor: Color, linkColor: Color): AnnotatedString {
    // Convert plain newlines to <br> tags before parsing
    // This preserves line breaks that aren't already in HTML format
    val processedHtml = html.replace("\n", "<br>")

    // Use Android's HTML parser with LEGACY mode to preserve more whitespace
    val spanned: Spanned = Html.fromHtml(processedHtml, Html.FROM_HTML_MODE_LEGACY)

    return buildAnnotatedString {
        append(spanned.toString())

        // Apply spans from HTML
        spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)

            when (span) {
                is URLSpan -> {
                    // Add clickable link annotation
                    addStringAnnotation(
                        tag = "URL",
                        annotation = span.url,
                        start = start,
                        end = end,
                    )
                    // Style links with the provided link colour and underline
                    addStyle(
                        style = SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline,
                        ),
                        start = start,
                        end = end,
                    )
                }
                is android.text.style.StyleSpan -> {
                    when (span.style) {
                        android.graphics.Typeface.BOLD -> {
                            addStyle(
                                style = SpanStyle(fontWeight = FontWeight.Bold),
                                start = start,
                                end = end,
                            )
                        }
                        android.graphics.Typeface.ITALIC -> {
                            addStyle(
                                style = SpanStyle(fontStyle = FontStyle.Italic),
                                start = start,
                                end = end,
                            )
                        }
                        android.graphics.Typeface.BOLD_ITALIC -> {
                            addStyle(
                                style = SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontStyle = FontStyle.Italic,
                                ),
                                start = start,
                                end = end,
                            )
                        }
                    }
                }
            }
        }
    }
}
