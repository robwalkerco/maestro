/*
 *
 *  Copyright (c) 2022 mobile.dev inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package maestro.drivers

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.expect
import com.github.michaelbull.result.getOrThrow
import com.github.michaelbull.result.onSuccess
import hierarchy.IdbElementNode
import hierarchy.XCUIElement
import hierarchy.XCUIElementNode
import ios.IOSDevice
import maestro.Capability
import maestro.DeviceInfo
import maestro.Driver
import maestro.Filters
import maestro.KeyCode
import maestro.MaestroException
import maestro.Platform
import maestro.Point
import maestro.ScreenRecording
import maestro.SwipeDirection
import maestro.TreeNode
import maestro.UiElement.Companion.toUiElement
import maestro.UiElement.Companion.toUiElementOrNull
import maestro.ViewHierarchy
import maestro.utils.FileUtils
import maestro.utils.MaestroTimer
import maestro.utils.ScreenshotUtils
import okio.Sink
import org.slf4j.LoggerFactory
import util.XCRunnerCLIUtils
import java.io.File
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.set

class IOSDriver(
    private val iosDevice: IOSDevice,
) : Driver {

    private val executor by lazy { Executors.newSingleThreadExecutor() }
    private val deviceInfo by lazy {
        iosDevice.deviceInfo().expect {}
    }

    private val widthPoints by lazy {
        deviceInfo.widthPoints
    }
    private val heightPoints by lazy {
        deviceInfo.heightPoints
    }

    private var appId: String? = null
    private var proxySet = false

    override fun name(): String {
        return NAME
    }

    override fun open() {
        iosDevice.open()
    }

    override fun close() {
        if (proxySet) {
            resetProxy()
        }
        iosDevice.close()
        appId = null
    }

    override fun deviceInfo(): DeviceInfo {
        return DeviceInfo(
            platform = Platform.IOS,
            widthPixels = deviceInfo.widthPixels,
            heightPixels = deviceInfo.heightPixels,
            widthGrid = deviceInfo.widthPoints,
            heightGrid = deviceInfo.heightPoints,
        )
    }

    override fun launchApp(
        appId: String,
        launchArguments: Map<String, Any>,
        sessionId: UUID?,
    ) {
        iosDevice.launch(appId, launchArguments, sessionId)
            .onSuccess { this.appId = appId }
            .getOrThrow {
                MaestroException.UnableToLaunchApp("Unable to launch app $appId ${it.message}")
            }
    }

    override fun stopApp(appId: String) {
        iosDevice.stop(appId)
    }

    override fun clearAppState(appId: String) {
        iosDevice.clearAppState(appId)
    }

    override fun clearKeychain() {
        iosDevice.clearKeychain().expect {}
    }

    override fun pullAppState(appId: String, outFile: File) {
        if (!outFile.exists()) outFile.createNewFile()
        val tmpDir = Files.createTempDirectory("maestro_state_")

        iosDevice.pullAppState(appId, tmpDir.toFile()).getOrThrow {
            MaestroException.UnableToPullState("Unable to pull state for $appId. ${it.message}")
        }

        FileUtils.zipDir(tmpDir, outFile.toPath())
        FileUtils.deleteDir(tmpDir)
    }

    override fun pushAppState(appId: String, stateFile: File) {
        val tmpDir = Files.createTempDirectory("maestro_state_")
        FileUtils.unzip(stateFile.toPath(), tmpDir)

        iosDevice.pushAppState(appId, tmpDir.toFile()).getOrThrow {
            MaestroException.UnableToPushState("Unable to push state for $appId. ${it.message}")
        }

        FileUtils.deleteDir(tmpDir)
    }

    override fun tap(point: Point) {
        iosDevice.tap(point.x, point.y).expect {}
    }

    override fun longPress(point: Point) {
        iosDevice.longPress(point.x, point.y, 3000).expect {}
    }

    override fun pressKey(code: KeyCode) {
        val keyCodeNameMap = mapOf(
            KeyCode.BACKSPACE to "delete",
            KeyCode.ENTER to "return",
            // Supported by iOS but not yet by maestro:
//        KeyCode.RETURN to "return",
//        KeyCode.TAP to "tab",
//        KeyCode.SPACE to "space",
//        KeyCode.ESCAPE to "escape",
        )

        val buttonNameMap = mapOf(
            KeyCode.HOME to "home",
            KeyCode.LOCK to "lock",
        )

        keyCodeNameMap[code]?.let { name ->
            iosDevice.pressKey(name)
        }

        buttonNameMap[code]?.let { name ->
            iosDevice.pressButton(name)
        }
    }

    override fun contentDescriptor(): TreeNode {
        return when (val contentDescriptorResult = iosDevice.contentDescriptor()) {
            is Ok -> mapHierarchy(contentDescriptorResult.value)
            is Err -> TreeNode()
        }
    }

    override fun isUnicodeInputSupported(): Boolean {
        return true
    }

    private fun mapHierarchy(xcUiElement: XCUIElement): TreeNode {
        return when (xcUiElement) {
            is XCUIElementNode -> parseXCUIElementNode(xcUiElement)
            is IdbElementNode -> parseIdbElementNode(xcUiElement)
            else -> error("Illegal instance for parsing hierarchy")
        }
    }

    private fun parseIdbElementNode(xcUiElement: IdbElementNode) = TreeNode(
        children = xcUiElement.children.map {
            val attributes = mutableMapOf<String, String>()

            (it.title
                ?: it.axLabel
                ?: it.axValue
                )?.let { title ->
                    attributes["text"] = title
                }

            (it.axUniqueId)?.let { resourceId ->
                attributes["resource-id"] = resourceId
            }

            it.frame.let { frame ->
                val left = frame.x.toInt()
                val top = frame.y.toInt()
                val right = left + frame.width.toInt()
                val bottom = top + frame.height.toInt()

                attributes["bounds"] = "[$left,$top][$right,$bottom]"
            }

            TreeNode(
                attributes = attributes,
                enabled = it.enabled,
            )
        }
    )

    private fun parseXCUIElementNode(xcUiElement: XCUIElementNode): TreeNode {
        val attributes = mutableMapOf<String, String>()
        val text = xcUiElement.title?.ifEmpty {
            xcUiElement.value
        }
        attributes["accessibilityText"] = xcUiElement.label
        attributes["text"] = text ?: ""
        attributes["hintText"] = xcUiElement.placeholderValue ?: ""
        attributes["resource-id"] = xcUiElement.identifier
        val right = xcUiElement.frame.x + xcUiElement.frame.width
        val bottom = xcUiElement.frame.y + xcUiElement.frame.height
        attributes["bounds"] = "[${xcUiElement.frame.x.toInt()},${xcUiElement.frame.y.toInt()}][${right.toInt()},${bottom.toInt()}]"
        attributes["enabled"] = xcUiElement.enabled.toString()
        attributes["focused"] = xcUiElement.hasFocus.toString()
        attributes["selected"] = xcUiElement.selected.toString()

        val checked = xcUiElement.elementType in CHECKABLE_ELEMENTS && xcUiElement.value == "1"
        attributes["checked"] = checked.toString()

        val children = mutableListOf<TreeNode>()
        val childNodes = xcUiElement.children
        if (childNodes != null) {
            (0 until childNodes.size).forEach { i ->
                children += mapHierarchy(childNodes[i])
            }
        }

        return TreeNode(
            attributes = attributes,
            children = children,
            enabled = xcUiElement.enabled,
            focused = xcUiElement.hasFocus,
            selected = xcUiElement.selected,
            checked = checked,
        )
    }

    override fun scrollVertical() {
        swipe(
            start = Point(widthPercentToPoint(0.5), heightPercentToPoint(0.5)),
            end = Point(widthPercentToPoint(0.5), heightPercentToPoint(0.1)),
            durationMs = 333,
        )
    }

    private fun validate(start: Point, end: Point) {
        val screenWidth = widthPoints
        val screenHeight = heightPoints

        if (start.x < 0 || start.x > screenWidth) {
            error("x value of start point (${start.x}) needs to be between 0 and $screenWidth")
        }
        if (end.x < 0 || end.x > screenWidth) {
            error("x value of end point (${end.x}) needs to be between 0 and $screenWidth")
        }

        if (start.y < 0 || start.y > screenHeight) {
            error("y value of start point (${start.y}) needs to be between 0 and $screenHeight")
        }
        if (end.y < 0 || end.y > screenHeight) {
            error("y value of end point (${end.y}) needs to be between 0 and $screenHeight")
        }
    }

    override fun swipe(
        start: Point,
        end: Point,
        durationMs: Long
    ) {
        validate(start, end)

        waitForAppToSettle(null, null)
        iosDevice.scroll(
            xStart = start.x.toDouble(),
            yStart = start.y.toDouble(),
            xEnd = end.x.toDouble(),
            yEnd = end.y.toDouble(),
            duration = durationMs.toDouble() / 1000
        ).expect {}
    }

    override fun swipe(swipeDirection: SwipeDirection, durationMs: Long) {
        val startPoint: Point
        val endPoint: Point

        when (swipeDirection) {
            SwipeDirection.UP -> {
                startPoint = Point(
                    x = widthPercentToPoint(0.5),
                    y = heightPercentToPoint(0.9),
                )
                endPoint = Point(
                    x = widthPercentToPoint(0.5),
                    y = heightPercentToPoint(0.1),
                )
            }
            SwipeDirection.DOWN -> {
                startPoint = Point(
                    x = widthPercentToPoint(0.5),
                    y = heightPercentToPoint(0.2),
                )
                endPoint = Point(
                    x = widthPercentToPoint(0.5),
                    y = heightPercentToPoint(0.9),
                )
            }
            SwipeDirection.RIGHT -> {
                startPoint = Point(
                    x = widthPercentToPoint(0.1),
                    y = heightPercentToPoint(0.5),
                )
                endPoint = Point(
                    x = widthPercentToPoint(0.9),
                    y = heightPercentToPoint(0.5),
                )
            }
            SwipeDirection.LEFT -> {
                startPoint = Point(
                    x = widthPercentToPoint(0.9),
                    y = heightPercentToPoint(0.5),
                )
                endPoint = Point(
                    x = widthPercentToPoint(0.1),
                    y = heightPercentToPoint(0.5),
                )
            }
        }
        swipe(startPoint, endPoint, durationMs)
    }

    override fun swipe(elementPoint: Point, direction: SwipeDirection, durationMs: Long) {
        when (direction) {
            SwipeDirection.UP -> {
                val end = Point(x = elementPoint.x, y = heightPercentToPoint(0.1))
                swipe(elementPoint, end, durationMs)
            }
            SwipeDirection.DOWN -> {
                val end = Point(x = elementPoint.x, y = heightPercentToPoint(0.9))
                swipe(elementPoint, end, durationMs)
            }
            SwipeDirection.RIGHT -> {
                val end = Point(x = widthPercentToPoint(0.9), y = elementPoint.y)
                swipe(elementPoint, end, durationMs)
            }
            SwipeDirection.LEFT -> {
                val end = Point(x = widthPercentToPoint(0.1), y = elementPoint.y)
                swipe(elementPoint, end, durationMs)
            }
        }
    }

    override fun backPress() {}

    override fun hideKeyboard() {
        val future = executor.submit {
            dismissKeyboardIntroduction()

            if (isKeyboardHidden()) return@submit

            swipe(
                start = Point(widthPercentToPoint(0.5), heightPercentToPoint(0.5)),
                end = Point(widthPercentToPoint(0.5), heightPercentToPoint(0.47)),
                durationMs = 50,
            )

            if (isKeyboardHidden()) return@submit

            swipe(
                start = Point(widthPercentToPoint(0.5), heightPercentToPoint(0.5)),
                end = Point(widthPercentToPoint(0.47), heightPercentToPoint(0.5)),
                durationMs = 50,
            )

            waitForAppToSettle(null, null)
        }
        try {
            future.get(5, TimeUnit.SECONDS)
        } catch (_: Exception) {
            // swallow the timeout and exceptions
            future.cancel(true)
        }
    }

    private fun isKeyboardHidden(): Boolean {
        val filter = Filters.idMatches("delete".toRegex())
        val element = MaestroTimer.withTimeout(2000) {
            filter(contentDescriptor().aggregate()).firstOrNull()
        }?.toUiElementOrNull()

        return element == null
    }

    private fun dismissKeyboardIntroduction() {
        val fastTypingInstruction = "Speed up your typing by sliding your finger across the letters to compose a word.*".toRegex()
        val instructionTextFilter = Filters.textMatches(fastTypingInstruction)
        val instructionText = MaestroTimer.withTimeout(2000) {
            instructionTextFilter(contentDescriptor().aggregate()).firstOrNull()
        }?.toUiElementOrNull()
        if (instructionText != null && instructionText.bounds.center().y in heightPoints / 2..heightPoints) {
            val continueElementFilter = Filters.textMatches("Continue".toRegex())
            val continueElement = MaestroTimer.withTimeout(2000) {
                continueElementFilter(contentDescriptor().aggregate()).find {
                    it.toUiElement().bounds.center().y > instructionText.bounds.center().y
                }
            }?.toUiElementOrNull()
            if (continueElement != null && continueElement.bounds.center().y > instructionText.bounds.center().y) {
                tap(continueElement.bounds.center())
            }
        }
    }

    override fun takeScreenshot(out: Sink, compressed: Boolean) {
        iosDevice.takeScreenshot(out, compressed).expect {}
    }

    override fun startScreenRecording(out: Sink): ScreenRecording {
        val iosScreenRecording = iosDevice.startScreenRecording(out).expect {}
        return object : ScreenRecording {
            override fun close() = iosScreenRecording.close()
        }
    }

    override fun inputText(text: String) {
        // silently fail if no XCUIElement has focus
        iosDevice.input(
            text = text,
        )
    }

    override fun inputTextV2(text: String, point: Point) {
        if (text.length > INPUT_TEXT_MAX_LENGTH) {
            inputText(text)
        } else {
            iosDevice.copyText(text)
            iosDevice.doubleTap(point.x, point.y)
        }
    }

    override fun openLink(link: String, appId: String?, autoVerify: Boolean, browser: Boolean) {
        iosDevice.openLink(link).expect {}
    }

    override fun setLocation(latitude: Double, longitude: Double) {
        iosDevice.setLocation(latitude, longitude).expect {}
    }

    override fun eraseText(charactersToErase: Int) {
        iosDevice.eraseText(charactersToErase)
    }

    override fun setProxy(host: String, port: Int) {
        XCRunnerCLIUtils.setProxy(host, port)
        proxySet = true
    }

    override fun resetProxy() {
        XCRunnerCLIUtils.resetProxy()
    }

    override fun isShutdown(): Boolean {
        return iosDevice.isShutdown()
    }

    override fun waitUntilScreenIsStatic(timeoutMs: Long): Boolean {
        return MaestroTimer.retryUntilTrue(timeoutMs) {
            val isScreenStatic = isScreenStatic()

            LOGGER.info("screen static = $isScreenStatic")
            return@retryUntilTrue isScreenStatic
        }
    }

    override fun waitForAppToSettle(initialHierarchy: ViewHierarchy?, appId: String?): ViewHierarchy? {
        LOGGER.info("Waiting for animation to end with timeout $SCREEN_SETTLE_TIMEOUT_MS")
        val didFinishOnTime = waitUntilScreenIsStatic(SCREEN_SETTLE_TIMEOUT_MS)

        return if (didFinishOnTime) null else ScreenshotUtils.waitForAppToSettle(initialHierarchy, this)
    }

    override fun capabilities(): List<Capability> {
        return emptyList()
    }

    override fun setPermissions(appId: String, permissions: Map<String, String>) {
        iosDevice.setPermissions(appId, permissions)
    }

    private fun isScreenStatic(): Boolean {
        return iosDevice.isScreenStatic().expect {}
    }

    private fun heightPercentToPoint(percent: Double): Int {
        return (percent * heightPoints).toInt()
    }

    private fun widthPercentToPoint(percent: Double): Int {
        return (percent * widthPoints).toInt()
    }

    companion object {
        const val NAME = "iOS Simulator"

        private val LOGGER = LoggerFactory.getLogger(IOSDevice::class.java)

        private const val ELEMENT_TYPE_CHECKBOX = 12
        private const val ELEMENT_TYPE_SWITCH = 40
        private const val ELEMENT_TYPE_TOGGLE = 41

        private val CHECKABLE_ELEMENTS = setOf(
            ELEMENT_TYPE_CHECKBOX,
            ELEMENT_TYPE_SWITCH,
            ELEMENT_TYPE_TOGGLE,
        )

        private const val SCREEN_SETTLE_TIMEOUT_MS: Long = 3000

        // in case of >50 symbols copy-paste approach will be used
        private const val INPUT_TEXT_MAX_LENGTH = 50
    }
}
