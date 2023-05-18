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

package maestro.orchestra

import maestro.KeyCode
import maestro.Point
import maestro.ScrollDirection
import maestro.SwipeDirection
import maestro.js.JsEngine
import maestro.orchestra.util.Env.evaluateScripts
import maestro.orchestra.util.InputRandomTextHelper

sealed interface Command {

    fun description(): String

    fun evaluateScripts(jsEngine: JsEngine): Command

    fun visible(): Boolean = true

}

sealed interface CompositeCommand : Command {

    fun subCommands(): List<MaestroCommand>

}

data class SwipeCommand(
    val direction: SwipeDirection? = null,
    val startPoint: Point? = null,
    val endPoint: Point? = null,
    val elementSelector: ElementSelector? = null,
    val startRelative: String? = null,
    val endRelative: String? = null,
    val duration: Long = DEFAULT_DURATION_IN_MILLIS
) : Command {

    override fun description(): String {
        return when {
            elementSelector != null && direction != null -> {
                "Swiping in $direction direction on ${elementSelector.description()}"
            }
            direction != null -> {
                "Swiping in $direction direction in $duration ms"
            }
            startPoint != null && endPoint != null -> {
                "Swipe from (${startPoint.x},${startPoint.y}) to (${endPoint.x},${endPoint.y}) in $duration ms"
            }
            startRelative != null && endRelative != null -> {
                "Swipe from ($startRelative) to ($endRelative) in $duration ms"
            }
            else -> "Invalid input to swipe command"
        }
    }

    override fun evaluateScripts(jsEngine: JsEngine): SwipeCommand {
        return copy(
            elementSelector = elementSelector?.evaluateScripts(jsEngine),
            startRelative = startRelative?.evaluateScripts(jsEngine),
            endRelative = endRelative?.evaluateScripts(jsEngine)
        )
    }

    companion object {
        private const val DEFAULT_DURATION_IN_MILLIS = 400L
    }
}

/**
 * @param visibilityPercentage 0-1 Visibility within viewport bounds. 0 not within viewport and 1 fully visible within viewport.
 */
data class ScrollUntilVisibleCommand(
    val selector: ElementSelector,
    val direction: ScrollDirection,
    val scrollDuration: Long,
    val visibilityPercentage: Int,
    val timeout: Long = DEFAULT_TIMEOUT_IN_MILLIS
) : Command {

    val visibilityPercentageNormalized = (visibilityPercentage / 100).toDouble()

    override fun description(): String {
        return "Scrolling $direction until ${selector.description()} is visible."
    }

    override fun evaluateScripts(jsEngine: JsEngine): ScrollUntilVisibleCommand {
        return copy(
            selector = selector.evaluateScripts(jsEngine),
        )
    }

    companion object {
        const val DEFAULT_TIMEOUT_IN_MILLIS = 20 * 1000L
        const val DEFAULT_SCROLL_DURATION = 40
        const val DEFAULT_ELEMENT_VISIBILITY_PERCENTAGE = 100
    }
}

class ScrollCommand : Command {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun toString(): String {
        return "ScrollCommand()"
    }

    override fun description(): String {
        return "Scroll vertically"
    }

    override fun evaluateScripts(jsEngine: JsEngine): ScrollCommand {
        return this
    }
}

class BackPressCommand : Command {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun toString(): String {
        return "BackPressCommand()"
    }

    override fun description(): String {
        return "Press back"
    }

    override fun evaluateScripts(jsEngine: JsEngine): BackPressCommand {
        return this
    }
}

class HideKeyboardCommand : Command {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun toString(): String {
        return "HideKeyboardCommand()"
    }

    override fun description(): String {
        return "Hide Keyboard"
    }

    override fun evaluateScripts(jsEngine: JsEngine): HideKeyboardCommand {
        return this
    }
}

data class CopyTextFromCommand(
    val selector: ElementSelector,
) : Command {

    override fun description(): String {
        return "Copy text from element with ${selector.description()}"
    }

    override fun evaluateScripts(jsEngine: JsEngine): CopyTextFromCommand {
        return copy(
            selector = selector.evaluateScripts(jsEngine)
        )
    }
}

class PasteTextCommand : Command {

    override fun description(): String {
        return "Paste text"
    }

    override fun evaluateScripts(jsEngine: JsEngine): PasteTextCommand {
        return this
    }
}

data class TapOnElementCommand(
    val selector: ElementSelector,
    val retryIfNoChange: Boolean? = null,
    val waitUntilVisible: Boolean? = null,
    val longPress: Boolean? = null,
) : Command {

    override fun description(): String {
        return "${tapOrLong(longPress)} on ${selector.description()}"
    }

    override fun evaluateScripts(jsEngine: JsEngine): TapOnElementCommand {
        return copy(
            selector = selector.evaluateScripts(jsEngine),
        )
    }
}

@Deprecated("Use TapOnPointV2Command instead")
data class TapOnPointCommand(
    val x: Int,
    val y: Int,
    val retryIfNoChange: Boolean? = null,
    val waitUntilVisible: Boolean? = null,
    val longPress: Boolean? = null,
) : Command {

    override fun description(): String {
        return "${tapOrLong(longPress)} on point ($x, $y)"
    }

    override fun evaluateScripts(jsEngine: JsEngine): TapOnPointCommand {
        return this
    }
}

data class TapOnPointV2Command(
    val point: String,
    val retryIfNoChange: Boolean? = null,
    val longPress: Boolean? = null,
) : Command {

    override fun description(): String {
        return "${tapOrLong(longPress)} on point ($point)"
    }

    override fun evaluateScripts(jsEngine: JsEngine): TapOnPointV2Command {
        return copy(
            point = point.evaluateScripts(jsEngine),
        )
    }
}

// Do not delete this class. It might have been already serialized in the past and stored in DB.
@Deprecated("Use AssertConditionCommand instead")
data class AssertCommand(
    val visible: ElementSelector? = null,
    val notVisible: ElementSelector? = null,
    val timeout: Long? = null,
) : Command {

    override fun description(): String {
        val timeoutStr = timeout?.let { " within $timeout ms" } ?: ""
        if (visible != null) {
            return "Assert visible ${visible.description()}" + timeoutStr
        }

        if (notVisible != null) {
            return "Assert not visible ${notVisible.description()}" + timeoutStr
        }

        return "No op"
    }

    override fun evaluateScripts(jsEngine: JsEngine): AssertCommand {
        return copy(
            visible = visible?.evaluateScripts(jsEngine),
            notVisible = notVisible?.evaluateScripts(jsEngine),
        )
    }

    fun toAssertConditionCommand(): AssertConditionCommand {
        return AssertConditionCommand(
            condition = Condition(
                visible = visible,
                notVisible = notVisible,
            ),
            timeout = timeout?.toString(),
        )
    }

}

data class AssertConditionCommand(
    val condition: Condition,
    private val timeout: String? = null,
) : Command {

    fun timeoutMs() = timeout?.toLong()

    override fun description(): String {
        return "Assert that ${condition.description()}"
    }

    override fun evaluateScripts(jsEngine: JsEngine): Command {
        return copy(
            condition = condition.evaluateScripts(jsEngine),
            timeout = timeout?.evaluateScripts(jsEngine)
        )
    }
}

data class InputTextCommandV1(
    val text: String
) : Command {

    override fun description(): String {
        return "Input text $text"
    }

    override fun evaluateScripts(jsEngine: JsEngine): InputTextCommandV1 {
        return copy(
            text = text.evaluateScripts(jsEngine)
        )
    }
}

data class InputTextCommandV2(
    val text: String,
    val accessibilityText: String? = null,
    val id: String? = null,
    val point: String? = null,
) : Command {
    override fun description(): String {
        return "Input text v2 $text"
    }

    override fun evaluateScripts(jsEngine: JsEngine): InputTextCommandV2 {
        return copy(
            text = text.evaluateScripts(jsEngine),
            accessibilityText = accessibilityText,
            id = id,
            point = point,
        )
    }
}

data class LaunchAppCommand(
    val appId: String,
    val clearState: Boolean? = null,
    val clearKeychain: Boolean? = null,
    val stopApp: Boolean? = null,
    var permissions: Map<String, String>? = null,
    val launchArguments: Map<String, Any>? = null,
) : Command {

    override fun description(): String {
        var result = if (clearState != true) {
            "Launch app \"$appId\""
        } else {
            "Launch app \"$appId\" with clear state"
        }

        if (clearKeychain == true) {
            result += " and clear keychain"
        }

        if (stopApp == false) {
            result += " without stopping app"
        }

        if (launchArguments != null) {
            result += " (launch arguments: ${launchArguments})"
        }

        return result
    }

    override fun evaluateScripts(jsEngine: JsEngine): LaunchAppCommand {
        return copy(
            appId = appId.evaluateScripts(jsEngine),
            launchArguments = launchArguments?.entries?.associate {
                val value = it.value
                it.key.evaluateScripts(jsEngine) to if (value is String) value.evaluateScripts(jsEngine) else it.value
            },
        )
    }
}

data class ApplyConfigurationCommand(
    val config: MaestroConfig,
) : Command {

    override fun description(): String {
        return "Apply configuration"
    }

    override fun evaluateScripts(jsEngine: JsEngine): ApplyConfigurationCommand {
        return copy(
            config = config.evaluateScripts(jsEngine),
        )
    }

    override fun visible(): Boolean = false
}

data class OpenLinkCommand(
    val link: String,
    val autoVerify: Boolean? = null,
    val browser: Boolean? = null
) : Command {

    override fun description(): String {
        return if (browser == true) {
            if (autoVerify == true) "Open $link with auto verification in browser" else "Open $link in browser"
        } else {
            if (autoVerify == true) "Open $link with auto verification" else "Open $link"
        }
    }

    override fun evaluateScripts(jsEngine: JsEngine): OpenLinkCommand {
        return copy(
            link = link.evaluateScripts(jsEngine),
        )
    }
}

data class PressKeyCommand(
    val code: KeyCode,
) : Command {
    override fun description(): String {
        return "Press ${code.description} key"
    }

    override fun evaluateScripts(jsEngine: JsEngine): PressKeyCommand {
        return this
    }

}

data class EraseTextCommand(
    val charactersToErase: Int?,
) : Command {

    override fun description(): String {
        return if (charactersToErase != null) {
            "Erase $charactersToErase characters"
        } else {
            "Erase text"
        }
    }

    override fun evaluateScripts(jsEngine: JsEngine): EraseTextCommand {
        return this
    }

}

data class TakeScreenshotCommand(
    val path: String,
) : Command {

    override fun description(): String {
        return "Take screenshot $path"
    }

    override fun evaluateScripts(jsEngine: JsEngine): TakeScreenshotCommand {
        return copy(
            path = path.evaluateScripts(jsEngine),
        )
    }
}

data class StopAppCommand(
    val appId: String,
) : Command {

    override fun description(): String {
        return "Stop $appId"
    }

    override fun evaluateScripts(jsEngine: JsEngine): Command {
        return copy(
            appId = appId.evaluateScripts(jsEngine),
        )
    }
}

data class ClearStateCommand(
    val appId: String,
) : Command {

    override fun description(): String {
        return "Clear state of $appId"
    }

    override fun evaluateScripts(jsEngine: JsEngine): Command {
        return copy(
            appId = appId.evaluateScripts(jsEngine),
        )
    }
}

class ClearKeychainCommand : Command {

    override fun description(): String {
        return "Clear keychain"
    }

    override fun evaluateScripts(jsEngine: JsEngine): Command {
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

}

enum class InputRandomType {
    NUMBER, TEXT, TEXT_EMAIL_ADDRESS, TEXT_PERSON_NAME,
}

data class InputRandomCommand(
    val inputType: InputRandomType? = InputRandomType.TEXT,
    val length: Int? = 8,
) : Command {

    fun genRandomString(): String {
        val lengthNonNull = length ?: 8
        val finalLength = if (lengthNonNull <= 0) 8 else lengthNonNull

        return when (inputType) {
            InputRandomType.NUMBER -> InputRandomTextHelper.getRandomNumber(finalLength)
            InputRandomType.TEXT -> InputRandomTextHelper.getRandomText(finalLength)
            InputRandomType.TEXT_EMAIL_ADDRESS -> InputRandomTextHelper.randomEmail()
            InputRandomType.TEXT_PERSON_NAME -> InputRandomTextHelper.randomPersonName()
            else -> InputRandomTextHelper.getRandomText(finalLength)
        }
    }

    override fun description(): String {
        return "Input text random $inputType"
    }

    override fun evaluateScripts(jsEngine: JsEngine): InputRandomCommand {
        return this
    }
}

data class RunFlowCommand(
    val commands: List<MaestroCommand>,
    val condition: Condition? = null,
    val sourceDescription: String? = null,
) : CompositeCommand {

    override fun subCommands(): List<MaestroCommand> {
        return commands
    }

    override fun description(): String {
        val runDescription = if (sourceDescription != null) {
            "Run $sourceDescription"
        } else {
            "Run flow"
        }

        return if (condition == null) {
            runDescription
        } else {
            "$runDescription when ${condition.description()}"
        }
    }

    override fun evaluateScripts(jsEngine: JsEngine): Command {
        return copy(
            condition = condition?.evaluateScripts(jsEngine),
        )
    }

}

data class SetLocationCommand(
    val latitude: Double,
    val longitude: Double,
) : Command {

    override fun description(): String {
        return "Set location (${latitude}, ${longitude})"
    }

    override fun evaluateScripts(jsEngine: JsEngine): SetLocationCommand {
        return this
    }
}

data class RepeatCommand(
    val times: String? = null,
    val condition: Condition? = null,
    val commands: List<MaestroCommand>,
) : CompositeCommand {

    override fun subCommands(): List<MaestroCommand> {
        return commands
    }

    override fun description(): String {
        val timesInt = times?.toIntOrNull() ?: 1

        return when {
            condition != null && timesInt > 1 -> {
                "Repeat while ${condition.description()} (up to $timesInt times)"
            }
            condition != null -> {
                "Repeat while ${condition.description()}"
            }
            timesInt > 1 -> "Repeat $timesInt times"
            else -> "Repeat indefinitely"
        }
    }

    override fun evaluateScripts(jsEngine: JsEngine): Command {
        return copy(
            times = times?.evaluateScripts(jsEngine),
        )
    }

}

data class DefineVariablesCommand(
    val env: Map<String, String>,
) : Command {

    override fun description(): String {
        return "Define variables"
    }

    override fun evaluateScripts(jsEngine: JsEngine): DefineVariablesCommand {
        return copy(
            env = env.mapValues { (_, value) ->
                value.evaluateScripts(jsEngine)
            }
        )
    }

    override fun visible(): Boolean = false

}

data class RunScriptCommand(
    val script: String,
    val env: Map<String, String> = emptyMap(),
    val sourceDescription: String,
    val condition: Condition?
) : Command {

    override fun description(): String {
        return if (condition == null) {
            "Run $sourceDescription"
        } else {
            "Run $sourceDescription when ${condition.description()}"
        }
    }

    override fun evaluateScripts(jsEngine: JsEngine): Command {
        return copy(
            env = env.mapValues { (_, value) ->
                value.evaluateScripts(jsEngine)
            },
            condition = condition?.evaluateScripts(jsEngine),
        )
    }

}

data class WaitForAnimationToEndCommand(
    val timeout: Long?
) : Command {
    override fun description(): String {
        return "Wait for animation to end"
    }

    override fun evaluateScripts(jsEngine: JsEngine): Command {
        return this
    }
}

data class EvalScriptCommand(
    val scriptString: String,
) : Command {

    override fun description(): String {
        return "Run $scriptString"
    }

    override fun evaluateScripts(jsEngine: JsEngine): Command {
        return this
    }

}

data class MockNetworkCommand(
    val path: String,
) : Command {

    override fun description(): String {
        return "Mock network using $path"
    }

    override fun evaluateScripts(jsEngine: JsEngine): Command {
        return copy(
            path = path.evaluateScripts(jsEngine),
        )
    }

}

data class TravelCommand(
    val points: List<GeoPoint>,
    val speedMPS: Double? = null,
) : Command {

    data class GeoPoint(
        val latitude: Double,
        val longitude: Double,
    ) {

        fun getDistanceInMeters(another: GeoPoint): Double {
            val earthRadius = 6371 // in kilometers
            val dLat = Math.toRadians(another.latitude - latitude)
            val dLon = Math.toRadians(another.longitude - longitude)

            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(another.latitude)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            val distance = earthRadius * c * 1000 // convert to meters

            return distance
        }

    }

    override fun description(): String {
        return "Travel path ${points.joinToString { "(${it.latitude}, ${it.longitude})" }}"
    }

    override fun evaluateScripts(jsEngine: JsEngine): Command {
        return this
    }

}

data class AssertOutgoingRequestsCommand(
    val path: String? = null,
    val headersPresent: List<String> = emptyList(),
    val headersAndValues: Map<String, String> = emptyMap(),
    val httpMethodIs: String? = null,
    val requestBodyContains: String? = null,
) : Command {
    override fun description(): String {
        return "Assert outgoing requests to $path"
    }

    override fun evaluateScripts(jsEngine: JsEngine): Command {
        return this
    }
}

internal fun tapOrLong(isLongPress: Boolean?): String = if (isLongPress == true) "Long press" else "Tap"


