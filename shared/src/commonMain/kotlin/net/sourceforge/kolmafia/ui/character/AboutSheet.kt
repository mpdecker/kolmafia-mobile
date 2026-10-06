package net.sourceforge.kolmafia.ui.character

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import net.sourceforge.kolmafia.ui.AppAbout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uriHandler = LocalUriHandler.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(AppAbout.APP_NAME, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(AppAbout.revisionLine(), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Text(AppAbout.GPL_BLURB, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Text("Privacy policy", style = MaterialTheme.typography.labelLarge)
            LinkLine(
                url = AppAbout.PRIVACY_POLICY_URL,
                label = AppAbout.privacyPolicyDisplay(),
                onOpen = { uriHandler.openUri(it) },
            )
            Spacer(Modifier.height(12.dp))
            Text("Source", style = MaterialTheme.typography.labelLarge)
            LinkLine(
                url = AppAbout.SOURCE_URL,
                label = AppAbout.sourceDisplay(),
                onOpen = { uriHandler.openUri(it) },
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onDismiss) { Text("Close") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LinkLine(
    url: String,
    label: String,
    onOpen: (String) -> Unit,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = buildAnnotatedString {
        withLink(
            LinkAnnotation.Url(
                url = url,
                linkInteractionListener = { onOpen(url) },
            ),
        ) {
            withStyle(
                SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                append(label)
            }
        }
    }
    Text(text = annotated, style = MaterialTheme.typography.bodySmall)
}
