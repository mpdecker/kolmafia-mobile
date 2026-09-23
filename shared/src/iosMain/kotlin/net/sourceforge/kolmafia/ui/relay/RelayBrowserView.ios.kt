package net.sourceforge.kolmafia.ui.relay

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

/**
 * iOS WKWebView bound to the local Relay loopback URL.
 * RelayServerPlatform still stubs the accept socket; decorate/proxy work headlessly.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun RelayBrowserView(url: String, modifier: Modifier) {
    UIKitView(
        factory = {
            val config = WKWebViewConfiguration()
            WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = config).apply {
                allowsBackForwardNavigationGestures = true
                NSURL.URLWithString(url)?.let { loadRequest(NSURLRequest.requestWithURL(it)) }
            }
        },
        modifier = modifier,
        update = { view ->
            val current = view.URL?.absoluteString
            if (current != url) {
                NSURL.URLWithString(url)?.let { view.loadRequest(NSURLRequest.requestWithURL(it)) }
            }
        },
    )
}
