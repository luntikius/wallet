package com.luntikius.wallet.ui.components.pass.pkpass

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luntikius.wallet.ui.components.HtmlText

/**
 * Info block for displaying back field with HTML content.
 */
@Composable
internal fun InfoBlock(title: String, htmlContent: String, textColor: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Block title/label (only if not blank)
            if (title.isNotBlank()) {
                SelectionContainer {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black.copy(alpha = 0.6f),
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // HTML content with clickable links - selectable
            SelectionContainer {
                HtmlText(
                    html = htmlContent,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                )
            }
        }
    }
}
