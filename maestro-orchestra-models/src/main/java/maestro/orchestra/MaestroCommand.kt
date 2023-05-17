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

import maestro.js.JsEngine

/**
 * The Mobile.dev platform uses this class in the backend and hence the custom
 * serialization logic. The earlier implementation of this class had a nullable field for
 * each command. Sometime in the future we may move this serialization logic to the backend
 * itself, where it would be more relevant.
 */
data class MaestroCommand(
    val tapOnElement: TapOnElementCommand? = null,
    @Deprecated("Use tapOnPointV2Command") val tapOnPoint: TapOnPointCommand? = null,
    val tapOnPointV2Command: TapOnPointV2Command? = null,
    val scrollCommand: ScrollCommand? = null,
    val swipeCommand: SwipeCommand? = null,
    val backPressCommand: BackPressCommand? = null,
    @Deprecated("Use assertConditionCommand") val assertCommand: AssertCommand? = null,
    val assertConditionCommand: AssertConditionCommand? = null,
    val inputTextCommandV1: InputTextCommandV1? = null,
    val inputTextCommandV2: InputTextCommandV2? = null,
    val inputRandomTextCommand: InputRandomCommand? = null,
    val launchAppCommand: LaunchAppCommand? = null,
    val applyConfigurationCommand: ApplyConfigurationCommand? = null,
    val openLinkCommand: OpenLinkCommand? = null,
    val pressKeyCommand: PressKeyCommand? = null,
    val eraseTextCommand: EraseTextCommand? = null,
    val hideKeyboardCommand: HideKeyboardCommand? = null,
    val takeScreenshotCommand: TakeScreenshotCommand? = null,
    val stopAppCommand: StopAppCommand? = null,
    val clearStateCommand: ClearStateCommand? = null,
    val clearKeychainCommand: ClearKeychainCommand? = null,
    val runFlowCommand: RunFlowCommand? = null,
    val setLocationCommand: SetLocationCommand? = null,
    val repeatCommand: RepeatCommand? = null,
    val copyTextCommand: CopyTextFromCommand? = null,
    val pasteTextCommand: PasteTextCommand? = null,
    val defineVariablesCommand: DefineVariablesCommand? = null,
    val runScriptCommand: RunScriptCommand? = null,
    val waitForAnimationToEndCommand: WaitForAnimationToEndCommand? = null,
    val evalScriptCommand: EvalScriptCommand? = null,
    val mockNetworkCommand: MockNetworkCommand? = null,
    val scrollUntilVisible: ScrollUntilVisibleCommand? = null,
    val travelCommand: TravelCommand? = null,
    val assertOutgoingRequestsCommand: AssertOutgoingRequestsCommand? = null,
) {

    constructor(command: Command) : this(
        tapOnElement = command as? TapOnElementCommand,
        tapOnPoint = command as? TapOnPointCommand,
        tapOnPointV2Command = command as? TapOnPointV2Command,
        scrollCommand = command as? ScrollCommand,
        swipeCommand = command as? SwipeCommand,
        backPressCommand = command as? BackPressCommand,
        assertCommand = command as? AssertCommand,
        assertConditionCommand = command as? AssertConditionCommand,
        inputTextCommandV1 = command as? InputTextCommandV1,
        inputTextCommandV2 = command as? InputTextCommandV2,
        inputRandomTextCommand = command as? InputRandomCommand,
        launchAppCommand = command as? LaunchAppCommand,
        applyConfigurationCommand = command as? ApplyConfigurationCommand,
        openLinkCommand = command as? OpenLinkCommand,
        pressKeyCommand = command as? PressKeyCommand,
        eraseTextCommand = command as? EraseTextCommand,
        hideKeyboardCommand = command as? HideKeyboardCommand,
        takeScreenshotCommand = command as? TakeScreenshotCommand,
        stopAppCommand = command as? StopAppCommand,
        clearStateCommand = command as? ClearStateCommand,
        clearKeychainCommand = command as? ClearKeychainCommand,
        runFlowCommand = command as? RunFlowCommand,
        setLocationCommand = command as? SetLocationCommand,
        repeatCommand = command as? RepeatCommand,
        copyTextCommand = command as? CopyTextFromCommand,
        pasteTextCommand = command as? PasteTextCommand,
        defineVariablesCommand = command as? DefineVariablesCommand,
        runScriptCommand = command as? RunScriptCommand,
        waitForAnimationToEndCommand = command as? WaitForAnimationToEndCommand,
        evalScriptCommand = command as? EvalScriptCommand,
        mockNetworkCommand = command as? MockNetworkCommand,
        scrollUntilVisible = command as? ScrollUntilVisibleCommand,
        travelCommand = command as? TravelCommand,
        assertOutgoingRequestsCommand = command as? AssertOutgoingRequestsCommand,
    )

    fun asCommand(): Command? = when {
        tapOnElement != null -> tapOnElement
        tapOnPoint != null -> tapOnPoint
        tapOnPointV2Command != null -> tapOnPointV2Command
        scrollCommand != null -> scrollCommand
        swipeCommand != null -> swipeCommand
        backPressCommand != null -> backPressCommand
        assertCommand != null -> assertCommand
        assertConditionCommand != null -> assertConditionCommand
        inputTextCommandV1 != null -> inputTextCommandV1
        inputTextCommandV2 != null -> inputTextCommandV2
        inputRandomTextCommand != null -> inputRandomTextCommand
        launchAppCommand != null -> launchAppCommand
        applyConfigurationCommand != null -> applyConfigurationCommand
        openLinkCommand != null -> openLinkCommand
        pressKeyCommand != null -> pressKeyCommand
        eraseTextCommand != null -> eraseTextCommand
        hideKeyboardCommand != null -> hideKeyboardCommand
        takeScreenshotCommand != null -> takeScreenshotCommand
        stopAppCommand != null -> stopAppCommand
        clearStateCommand != null -> clearStateCommand
        clearKeychainCommand != null -> clearKeychainCommand
        runFlowCommand != null -> runFlowCommand
        setLocationCommand != null -> setLocationCommand
        repeatCommand != null -> repeatCommand
        copyTextCommand != null -> copyTextCommand
        pasteTextCommand != null -> pasteTextCommand
        defineVariablesCommand != null -> defineVariablesCommand
        runScriptCommand != null -> runScriptCommand
        waitForAnimationToEndCommand != null -> waitForAnimationToEndCommand
        evalScriptCommand != null -> evalScriptCommand
        mockNetworkCommand != null -> mockNetworkCommand
        scrollUntilVisible != null -> scrollUntilVisible
        travelCommand != null -> travelCommand
        assertOutgoingRequestsCommand != null -> assertOutgoingRequestsCommand
        else -> null
    }

    fun evaluateScripts(jsEngine: JsEngine): MaestroCommand {
        return asCommand()
            ?.let { MaestroCommand(it.evaluateScripts(jsEngine)) }
            ?: MaestroCommand()
    }

    fun description(): String {
        return asCommand()?.description() ?: "No op"
    }
}
