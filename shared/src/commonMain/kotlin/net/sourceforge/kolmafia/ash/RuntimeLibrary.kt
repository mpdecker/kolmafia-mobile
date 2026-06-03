package net.sourceforge.kolmafia.ash

open class RuntimeLibrary {

    open fun registerAll(scope: AshScope) {
        register(scope, "print", AshType.VOID, listOf("value" to AshType.STRING)) { runtime, args ->
            runtime.print(args[0].toString())
            AshValue.VOID
        }
        register(scope, "to_string", AshType.STRING, listOf("value" to AshType.INT)) { _, args ->
            AshValue.of(args[0].toString())
        }
        register(scope, "to_string", AshType.STRING, listOf("value" to AshType.FLOAT)) { _, args ->
            AshValue.of(args[0].toString())
        }
        register(scope, "to_string", AshType.STRING, listOf("value" to AshType.BOOLEAN)) { _, args ->
            AshValue.of(args[0].toString())
        }
        register(scope, "to_string", AshType.STRING, listOf("value" to AshType.STRING)) { _, args ->
            args[0]
        }
    }

    protected fun register(
        scope: AshScope,
        name: String,
        returnType: AshType,
        params: List<Pair<String, AshType>>,
        impl: (AshRuntimeContext, List<AshValue>) -> AshValue
    ) {
        scope.declareFunction(
            AshFunction(name, returnType, params, body = null, libraryImpl = impl)
        )
    }
}
