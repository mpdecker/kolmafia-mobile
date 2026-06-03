package net.sourceforge.kolmafia.ash

// Task 5 (AshRuntime) will implement this interface
interface AshRuntimeContext {
    fun print(msg: String)
}

data class AshVariable(val name: String, val type: AshType, var value: AshValue)

data class AshFunction(
    val name: String,
    val returnType: AshType,
    val params: List<Pair<String, AshType>>,
    val body: List<ParseTreeNode>?,
    val libraryImpl: ((AshRuntimeContext, List<AshValue>) -> AshValue)? = null
)

class AshScope(val parent: AshScope? = null) {
    private val variables: MutableMap<String, AshVariable> = mutableMapOf()
    private val functions: MutableMap<String, MutableList<AshFunction>> = mutableMapOf()

    fun declareVar(name: String, type: AshType, value: AshValue? = null) {
        variables[name.lowercase()] = AshVariable(name, type, value ?: type.defaultValue())
    }

    fun getVar(name: String): AshVariable? =
        variables[name.lowercase()] ?: parent?.getVar(name)

    fun setVar(name: String, value: AshValue) {
        val key = name.lowercase()
        if (variables.containsKey(key)) { variables[key]!!.value = value; return }
        parent?.setVar(name, value) ?: throw ScriptException("Undefined variable: $name")
    }

    fun declareFunction(fn: AshFunction) {
        functions.getOrPut(fn.name.lowercase()) { mutableListOf() }.add(fn)
    }

    fun resolveFunction(name: String, argTypes: List<AshType>): AshFunction? {
        val candidates = functions[name.lowercase()]
        if (candidates != null) {
            val match = candidates.find { fn ->
                fn.params.size == argTypes.size &&
                fn.params.zip(argTypes).all { (param, arg) -> AshType.canCoerce(arg, param.second) }
            }
            if (match != null) return match
        }
        return parent?.resolveFunction(name, argTypes)
    }

    fun child(): AshScope = AshScope(this)
}
