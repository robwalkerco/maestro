package maestro.orchestra

import maestro.js.JsEngine
import maestro.orchestra.util.Env.evaluateScripts

// Note: The appId config is only a yaml concept for now. It'll be a larger migration to get to a point
// where appId is part of MaestroConfig (and factored out of MaestroCommands - eg: LaunchAppCommand).
data class MaestroConfig(
    val appId: String? = null,
    val name: String? = null,
    val tags: List<String>? = emptyList(),
    val initFlow: MaestroInitFlow? = null,
    val ext: Map<String, Any?> = emptyMap(),
) {

    fun evaluateScripts(jsEngine: JsEngine): MaestroConfig {
        return copy(
            appId = appId?.evaluateScripts(jsEngine),
            name = name?.evaluateScripts(jsEngine),
            initFlow = initFlow?.evaluateScripts(jsEngine),
        )
    }

}

data class MaestroInitFlow(
    val appId: String,
    val commands: List<MaestroCommand>,
) {

    fun evaluateScripts(jsEngine: JsEngine): MaestroInitFlow {
        return copy(
            appId = appId.evaluateScripts(jsEngine),
        )
    }

}
