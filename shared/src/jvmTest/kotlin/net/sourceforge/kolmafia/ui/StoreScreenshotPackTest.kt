package net.sourceforge.kolmafia.ui

import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.di.platformModule
import net.sourceforge.kolmafia.di.sharedModule
import net.sourceforge.kolmafia.session.SessionState
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Phone screenshot pack for Play listing prep under `docs/store-assets/screenshots/`.
 *
 * - [storeScreenshots_arePhonePortraitPlaceholders] always asserts committed 1080×1920 PNGs.
 * - [captureStoreScreenshotPack] regenerates placeholders from offline Compose UI (scaled to
 *   1080×1920). Run via:
 *   `.\gradlew.bat :shared:jvmTest --tests "*StoreScreenshotPackTest.capture*"`
 *
 * Replace with device captures before production publish.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class StoreScreenshotPackTest {
    private val mainDispatcher = UnconfinedTestDispatcher()
    private val loginBehavior: suspend (String, String) -> SessionState =
        { _, _ -> SessionState.LoggedIn }

    private val packFiles = listOf(
        "01-login.png",
        "02-character.png",
        "03-adventure.png",
        "04-inventory.png",
        "05-scripts.png",
        "06-drawer.png",
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        stopKoinQuietly()
        startKoin {
            allowOverride(true)
            modules(
                sharedModule,
                platformModule,
                module {
                    single<HttpClient> {
                        HttpClient(
                            MockEngine { _ ->
                                respond(
                                    content = "{}",
                                    status = HttpStatusCode.OK,
                                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                                )
                            },
                        )
                    }
                },
            )
        }
        GlobalContext.get().get<KoLCharacter>().updateFromApiResponse(
            net.sourceforge.kolmafia.character.CharacterApiResponse(
                name = "JourneyTester",
                level = "5",
            ),
        )
    }

    @AfterTest
    fun tearDown() {
        stopKoinQuietly()
        Dispatchers.resetMain()
    }

    @Test
    fun storeScreenshots_arePhonePortraitPlaceholders() {
        val outDir = resolveScreenshotsDir()
        packFiles.forEach { name ->
            val file = File(outDir, name)
            assertTrue(file.isFile, "missing screenshot $name at ${file.absolutePath}")
            val image = ImageIO.read(file)
            assertTrue(image != null, "unreadable PNG $name")
            assertEquals(PHONE_WIDTH, image.width, "$name width")
            assertEquals(PHONE_HEIGHT, image.height, "$name height")
        }
    }

    @Test
    fun captureStoreScreenshotPack() {
        val outDir = resolveScreenshotsDir()
        outDir.mkdirs()

        capture("01-login.png", outDir) {
            setContent { App(loginOverride = loginBehavior) }
            waitForIdle()
            onNodeWithText("KoLmafia Mobile").assertIsDisplayed()
        }

        capture("02-character.png", outDir) {
            setContent { App(loginOverride = loginBehavior) }
            performLogin()
            onNodeWithText("JourneyTester").assertIsDisplayed()
        }

        capture("03-adventure.png", outDir) {
            setContent { App(loginOverride = loginBehavior) }
            performLogin()
            openDestination("Adventure")
            onNodeWithText("Browse").assertIsDisplayed()
        }

        capture("04-inventory.png", outDir) {
            setContent { App(loginOverride = loginBehavior) }
            performLogin()
            openDestination("Inventory")
            onAllNodesWithText("Inventory")[0].assertIsDisplayed()
        }

        capture("05-scripts.png", outDir) {
            setContent { App(loginOverride = loginBehavior) }
            performLogin()
            openDestination("Scripts")
            onNodeWithText("New Script").assertIsDisplayed()
        }

        capture("06-drawer.png", outDir) {
            setContent { App(loginOverride = loginBehavior) }
            performLogin()
            onNodeWithContentDescription("Open navigation").performClick()
            waitForIdle()
            onNodeWithContentDescription("Relay").assertIsDisplayed()
        }

        packFiles.forEach { name ->
            val file = File(outDir, name)
            assertTrue(file.isFile && file.length() > 0, "missing screenshot $name")
            val image = ImageIO.read(file)!!
            assertEquals(PHONE_WIDTH, image.width, "$name width after capture")
            assertEquals(PHONE_HEIGHT, image.height, "$name height after capture")
        }
    }

    private fun capture(
        fileName: String,
        outDir: File,
        body: androidx.compose.ui.test.ComposeUiTest.() -> Unit,
    ) {
        runComposeUiTest {
            body()
            waitForIdle()
            val skiaImage = Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap())
            val encoded =
                skiaImage.encodeToData(EncodedImageFormat.PNG)
                    ?: error("PNG encode failed for $fileName")
            val raw = ImageIO.read(encoded.bytes.inputStream())
                ?: error("Could not decode capture for $fileName")
            ImageIO.write(scaleToPhonePortrait(raw, fileName), "png", File(outDir, fileName))
        }
    }

    private fun scaleToPhonePortrait(source: BufferedImage, label: String): BufferedImage {
        val dest = BufferedImage(PHONE_WIDTH, PHONE_HEIGHT, BufferedImage.TYPE_INT_RGB)
        val g = dest.createGraphics()
        try {
            g.color = Color(0x12, 0x1A, 0x14)
            g.fillRect(0, 0, PHONE_WIDTH, PHONE_HEIGHT)
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            val scale = minOf(
                PHONE_WIDTH.toDouble() / source.width,
                PHONE_HEIGHT.toDouble() / source.height,
            )
            val w = (source.width * scale).toInt().coerceAtLeast(1)
            val h = (source.height * scale).toInt().coerceAtLeast(1)
            val x = (PHONE_WIDTH - w) / 2
            val y = (PHONE_HEIGHT - h) / 2
            g.drawImage(source, x, y, w, h, null)
            g.font = Font("SansSerif", Font.BOLD, 36)
            g.color = Color(0xA5, 0xD6, 0xA7)
            g.drawString(label.removeSuffix(".png"), 48, PHONE_HEIGHT - 64)
        } finally {
            g.dispose()
        }
        return dest
    }

    private fun androidx.compose.ui.test.ComposeUiTest.performLogin() {
        onNodeWithText("Username").performTextInput("player")
        onNodeWithText("Password").performTextInput("secret")
        onNodeWithText("Log In").performClick()
        waitForIdle()
    }

    private fun androidx.compose.ui.test.ComposeUiTest.openDestination(label: String) {
        onNodeWithContentDescription("Open navigation").performClick()
        waitForIdle()
        onNodeWithContentDescription(label).performClick()
        waitForIdle()
    }

    private fun resolveScreenshotsDir(): File {
        val userDir = File(System.getProperty("user.dir"))
        val fromRoot = File(userDir, "docs/store-assets/screenshots")
        if (File(userDir, "docs").isDirectory) {
            fromRoot.mkdirs()
            return fromRoot
        }
        val fromModule = File(userDir.parentFile, "docs/store-assets/screenshots")
        fromModule.mkdirs()
        return fromModule
    }

    private fun stopKoinQuietly() {
        try {
            stopKoin()
        } catch (_: Exception) {
            // Already stopped.
        }
    }

    companion object {
        private const val PHONE_WIDTH = 1080
        private const val PHONE_HEIGHT = 1920
    }
}
