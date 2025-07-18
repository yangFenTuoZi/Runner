package yangfentuozi.runner.app.data

open class CommandInfo {
    var name: String? = null
    var command: String? = null
    var keepAlive: Boolean = false
    var reducePerm: Boolean = false
    var targetPerm: String? = null

    constructor()
}