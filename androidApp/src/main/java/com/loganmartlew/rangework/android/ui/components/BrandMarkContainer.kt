package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.R

@Composable
internal fun BrandMarkContainer(
    size: Dp,
    markSize: Dp,
    twoColor: Boolean,
    contentDescription: String? = "Rangework mark",
) {
    Surface(
        modifier = Modifier.size(size),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        CenteredBox {
            Image(
                painter = painterResource(
                    if (twoColor) R.drawable.ic_rangework_mark_twocolor else R.drawable.ic_rangework_mark,
                ),
                contentDescription = contentDescription,
                modifier = Modifier.size(markSize),
            )
        }
    }
}

@Composable
internal fun BrandWordmark(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMarkContainer(size = 40.dp, markSize = 24.dp, twoColor = false)
        Text(
            text = "Rangework",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun CenteredBox(
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
