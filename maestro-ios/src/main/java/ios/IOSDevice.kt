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

package ios

import com.github.michaelbull.result.Result
import hierarchy.XCUIElement
import ios.device.DeviceInfo
import okio.Sink
import java.io.File
import java.io.InputStream
import java.util.UUID

interface IOSDevice : AutoCloseable {

    val deviceId: String?

    fun open()

    fun deviceInfo(): Result<DeviceInfo, Throwable>

    fun contentDescriptor(): Result<XCUIElement, Throwable>

    fun tap(x: Int, y: Int): Result<Unit, Throwable>

    fun longPress(x: Int, y: Int, durationMs: Long): Result<Unit, Throwable>

    fun scroll(
        xStart: Double,
        yStart: Double,
        xEnd: Double,
        yEnd: Double,
        duration: Double,
    ): Result<Unit, Throwable>

    /**
     * Inputs text into the currently focused element.
     */
    fun input(
        text: String,
    ): Result<Unit, Throwable>

    /**
     * Installs application on the device.
     *
     * @param stream - input stream of zipped .app bundle
     */
    fun install(stream: InputStream): Result<Unit, Throwable>

    /**
     * Uninstalls the app.
     *
     * Idempotent. Operation succeeds if app is not installed.
     *
     * @param id = bundle id of the app to uninstall
     */
    fun uninstall(id: String): Result<Unit, Throwable>

    /**
     * Pulls files from app container
     *
     * @param id bundle id of the app
     * @param file output directory
     */
    fun pullAppState(id: String, file: File): Result<Unit, Throwable>

    /**
     * Pushes files to app container
     *
     * @param id bundle id of the app
     * @param file file or directory (if directory, it pushes contents of that directory)
     */
    fun pushAppState(id: String, file: File): Result<Unit, Throwable>

    /**
     * Clears state of a given application.
     *
     * @param id = bundle id of the app to clear
     */
    fun clearAppState(id: String): Result<Unit, Throwable>

    /**
     * Clears device keychain.
     */
    fun clearKeychain(): Result<Unit, Throwable>

    /**
     * Launches the app.
     *
     * @param id - bundle id of the app to launch
     * @param isWarmup - in case it is warmup and we're not waiting for the logs, the app can be launched from foreground
     */
    fun launch(
        id: String,
        launchArguments: Map<String, Any>,
        maestroSessionId: UUID?,
    ): Result<Unit, Throwable>

    /**
     * Terminates the app.
     *
     * @param id - bundle id of the app to terminate
     */
    fun stop(id: String): Result<Unit, Throwable>

    /**
     * Opens a link
     *
     * @param link - link to open
     */
    fun openLink(link: String): Result<Unit, Throwable>

    /**
     * Takes a screenshot and writes it into output sink
     *
     * @param out - output sink
     */
    fun takeScreenshot(out: Sink, compressed: Boolean): Result<Unit, Throwable>

    /**
     * Start a screen recording
     *
     * @param out - output sink
     */
    fun startScreenRecording(out: Sink): Result<IOSScreenRecording, Throwable>

    /**
     * Sets the geolocation
     *
     * @param lat - latitude
     * @param long - longitude
     */
    fun setLocation(latitude: Double, longitude: Double): Result<Unit, Throwable>

    /**
     * @return true if the connection to the device (not device itself) is shut down
     */
    fun isShutdown(): Boolean

    /**
     * @return false if 2 consequent screenshots are equal, true if screen is static
     */
    fun isScreenStatic(): Result<Boolean, Throwable>

    fun setPermissions(id: String, permissions: Map<String, String>): Result<Unit, Throwable>

    fun pressKey(name: String)

    fun pressButton(name: String)

    fun eraseText(charactersToErase: Int)

    fun doubleTap(x: Int, y: Int): Result<Unit, Throwable>
}

interface IOSScreenRecording : AutoCloseable
