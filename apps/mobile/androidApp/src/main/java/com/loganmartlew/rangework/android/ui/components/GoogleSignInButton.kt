package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.R
import com.loganmartlew.rangework.android.ui.theme.RangeworkTheme

// Google Identity branding constants — intentionally off-theme per Google's spec.
private val GoogleButtonContainerColor = Color.White
private val GoogleButtonContentColor = Color(0xFF1F1F1F)
private val GoogleButtonBorderColor = Color(0xFF747775)

@Composable
internal fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = GoogleButtonContainerColor,
            contentColor = GoogleButtonContentColor,
            disabledContainerColor = GoogleButtonContainerColor.copy(alpha = 0.38f),
            disabledContentColor = GoogleButtonContentColor.copy(alpha = 0.38f),
        ),
        border = BorderStroke(1.dp, GoogleButtonBorderColor),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_google_logo),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Sign in with Google")
    }
}

@Preview(showBackground = true)
@Composable
private fun GoogleSignInButtonPreview() {
    RangeworkTheme {
        GoogleSignInButton(
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "Disabled")
@Composable
private fun GoogleSignInButtonDisabledPreview() {
    RangeworkTheme {
        GoogleSignInButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.padding(16.dp),
        )
    }
}
