package net.sourceforge.kolmafia.ash

class AshRuntime(private val library: RuntimeLibrary) : AshRuntimeContext {

    enum class ControlFlow { NORMAL, RETURN, BREAK, CONTINUE, EXIT }

    var controlFlow = ControlFlow.NORMAL
    var returnValue: AshValue = AshValue.VOID
    val output = StringBuilder()

    private val globalScope = AshScope().also { library.registerAll(it) }

    fun execute(script: List<ParseTreeNode>): AshValue {
        controlFlow = ControlFlow.NORMAL
        returnValue = AshValue.VOID
        output.clear()
        // Register user-defined functions in globalScope so recursive calls can find them
        for (node in script) when (node) {
            is FunctionDefNode -> globalScope.declareFunction(node.function)
            else -> { }
        }
        val scope = globalScope.child()
        for (node in script) {
            if (node is FunctionDefNode || node is RecordDefNode) continue
            executeNode(node, scope)
            if (controlFlow != ControlFlow.NORMAL) break
        }
        return returnValue
    }

    fun executeNode(node: ParseTreeNode, scope: AshScope): AshValue = when (node) {
        is ExprStatement -> { evalExpr(node.expr, scope); AshValue.VOID }
        is VarDeclNode -> {
            val init = node.init?.let { evalExpr(it, scope) } ?: node.type.defaultValue()
            scope.declareVar(node.varName, node.type, init.coerceTo(node.type))
            AshValue.VOID
        }
        is IfNode -> executeIf(node, scope)
        is WhileNode -> executeWhile(node, scope)
        is RepeatNode -> executeRepeat(node, scope)
        is ForNode -> executeFor(node, scope)
        is ForEachNode -> executeForeach(node, scope)
        is TryNode -> executeTry(node, scope)
        is ReturnNode -> {
            returnValue = node.value?.let { evalExpr(it, scope) } ?: AshValue.VOID
            controlFlow = ControlFlow.RETURN
            returnValue
        }
        is BreakNode -> { controlFlow = ControlFlow.BREAK; AshValue.VOID }
        is ContinueNode -> { controlFlow = ControlFlow.CONTINUE; AshValue.VOID }
        is ExitNode -> { controlFlow = ControlFlow.EXIT; AshValue.VOID }
        is FunctionDefNode, is RecordDefNode -> AshValue.VOID
        else -> AshValue.VOID
    }

    private fun executeBlock(block: List<ParseTreeNode>, scope: AshScope): AshValue {
        val child = scope.child()
        var last = AshValue.VOID
        for (node in block) {
            last = executeNode(node, child)
            if (controlFlow != ControlFlow.NORMAL) break
        }
        return last
    }

    private fun executeIf(node: IfNode, scope: AshScope): AshValue {
        if (evalExpr(node.condition, scope).toBoolean()) return executeBlock(node.thenBlock, scope)
        for ((cond, block) in node.elseIfClauses) {
            if (evalExpr(cond, scope).toBoolean()) return executeBlock(block, scope)
        }
        return node.elseBlock?.let { executeBlock(it, scope) } ?: AshValue.VOID
    }

    private fun executeWhile(node: WhileNode, scope: AshScope): AshValue {
        while (evalExpr(node.condition, scope).toBoolean()) {
            executeBlock(node.body, scope)
            when (controlFlow) {
                ControlFlow.BREAK -> { controlFlow = ControlFlow.NORMAL; break }
                ControlFlow.CONTINUE -> controlFlow = ControlFlow.NORMAL
                ControlFlow.NORMAL -> { }
                else -> break
            }
        }
        return AshValue.VOID
    }

    private fun executeRepeat(node: RepeatNode, scope: AshScope): AshValue {
        do {
            executeBlock(node.body, scope)
            when (controlFlow) {
                ControlFlow.BREAK -> { controlFlow = ControlFlow.NORMAL; return AshValue.VOID }
                ControlFlow.CONTINUE -> controlFlow = ControlFlow.NORMAL
                ControlFlow.NORMAL -> { }
                else -> return AshValue.VOID
            }
        } while (!evalExpr(node.condition, scope).toBoolean())
        return AshValue.VOID
    }

    private fun executeFor(node: ForNode, scope: AshScope): AshValue {
        val loopScope = scope.child()
        var cur = evalExpr(node.start, scope).toLong()
        val end = evalExpr(node.end, scope).toLong()
        val step = node.step?.let { evalExpr(it, scope).toLong() }
            ?: if (node.ascending) 1L else -1L
        loopScope.declareVar(node.varName, AshType.INT, AshValue.of(cur))
        while (if (node.ascending) cur <= end else cur >= end) {
            loopScope.setVar(node.varName, AshValue.of(cur))
            executeBlock(node.body, loopScope)
            when (controlFlow) {
                ControlFlow.BREAK -> { controlFlow = ControlFlow.NORMAL; break }
                ControlFlow.CONTINUE -> controlFlow = ControlFlow.NORMAL
                ControlFlow.NORMAL -> { }
                else -> break
            }
            cur += step
        }
        return AshValue.VOID
    }

    private fun executeForeach(node: ForEachNode, scope: AshScope): AshValue {
        val agg = evalExpr(node.aggregate, scope) as? AggregateValue
            ?: throw ScriptException("foreach requires an aggregate")
        val loopScope = scope.child()
        for ((key, value) in agg.map) {
            when (node.keyNames.size) {
                1 -> loopScope.declareVar(node.keyNames[0], agg.type.indexType, key)
                2 -> {
                    loopScope.declareVar(node.keyNames[0], agg.type.indexType, key)
                    loopScope.declareVar(node.keyNames[1], agg.type.dataType, value)
                }
                else -> throw ScriptException("foreach supports at most 2 key variables")
            }
            executeBlock(node.body, loopScope)
            when (controlFlow) {
                ControlFlow.BREAK -> { controlFlow = ControlFlow.NORMAL; break }
                ControlFlow.CONTINUE -> controlFlow = ControlFlow.NORMAL
                ControlFlow.NORMAL -> { }
                else -> break
            }
        }
        return AshValue.VOID
    }

    private fun executeTry(node: TryNode, scope: AshScope): AshValue = try {
        executeBlock(node.body, scope)
    } catch (e: ScriptException) {
        node.catchBlock?.let { executeBlock(it, scope) } ?: AshValue.VOID
    }

    fun evalExpr(expr: ExprNode, scope: AshScope): AshValue = when (expr) {
        is LiteralNode -> expr.value
        is VarRefNode -> scope.getVar(expr.name)?.value
            ?: throw ScriptException("Undefined variable '${expr.name}'")
        is AssignNode -> {
            val v = evalExpr(expr.value, scope)
            applyAssign(expr.target, v, scope)
            v
        }
        is BinOpNode -> expr.op.apply(evalExpr(expr.left, scope), evalExpr(expr.right, scope))
        is UnaryOpNode -> evalUnary(expr, scope)
        is TernaryNode -> if (evalExpr(expr.condition, scope).toBoolean())
            evalExpr(expr.thenExpr, scope) else evalExpr(expr.elseExpr, scope)
        is FunctionCallNode -> callFunction(expr.name, expr.args.map { evalExpr(it, scope) }, scope)
        is IndexNode -> {
            val agg = evalExpr(expr.aggregate, scope) as? AggregateValue
                ?: throw ScriptException("Cannot index non-aggregate")
            agg[evalExpr(expr.index, scope)]
        }
        is FieldAccessNode -> {
            val obj = evalExpr(expr.record, scope)
            when (obj) {
                is RecordValue -> obj.getField(expr.fieldName)
                is AggregateValue -> when (expr.fieldName) {
                    "length", "size", "count" -> obj.size()
                    else -> throw ScriptException("Aggregate has no field '${expr.fieldName}'")
                }
                else -> throw ScriptException("Cannot access field '${expr.fieldName}' on ${obj.type.name}")
            }
        }
        is AggregateLiteralNode -> {
            val result = AggregateValue(expr.aggType)
            for ((keyExpr, valueExpr) in expr.entries) {
                val key = keyExpr?.let { evalExpr(it, scope) } ?: AshValue.of(result.map.size)
                result[key] = evalExpr(valueExpr, scope)
            }
            result
        }
        is RecordLiteralNode -> {
            val result = RecordValue(expr.recordType)
            expr.fieldExprs.forEachIndexed { i, fe -> fe?.let { result.setField(i, evalExpr(it, scope)) } }
            result
        }
    }

    private fun evalUnary(expr: UnaryOpNode, scope: AshScope): AshValue {
        val operand = evalExpr(expr.operand, scope)
        val result = expr.op.applyUnary(operand)
        val target: LvalueNode? = when (expr.operand) {
            is VarRefNode -> VarLvalue(expr.operand.name)
            is IndexNode -> IndexLvalue(expr.operand.aggregate, expr.operand.index)
            else -> null
        }
        if (target != null && (expr.op == AshOperator.PRE_INC || expr.op == AshOperator.PRE_DEC ||
                               expr.op == AshOperator.POST_INC || expr.op == AshOperator.POST_DEC)) {
            applyAssign(target, result, scope)
        }
        return if (expr.op == AshOperator.POST_INC || expr.op == AshOperator.POST_DEC) operand else result
    }

    private fun applyAssign(target: LvalueNode, value: AshValue, scope: AshScope) {
        when (target) {
            is VarLvalue -> scope.setVar(target.name, value)
            is IndexLvalue -> {
                val agg = evalExpr(target.aggregate, scope) as? AggregateValue
                    ?: throw ScriptException("Cannot index-assign non-aggregate")
                agg[evalExpr(target.index, scope)] = value
            }
            is FieldLvalue -> {
                val rec = evalExpr(target.record, scope) as? RecordValue
                    ?: throw ScriptException("Cannot field-assign non-record")
                rec.setField(target.fieldName, value)
            }
        }
    }

    private fun callFunction(name: String, args: List<AshValue>, scope: AshScope): AshValue {
        val fn = scope.resolveFunction(name, args.map { it.type })
            ?: throw ScriptException(
                "No matching overload of '$name' for (${args.joinToString { it.type.name }})"
            )
        return if (fn.libraryImpl != null) {
            fn.libraryImpl.invoke(this, args)
        } else {
            val fnScope = globalScope.child()
            fn.params.forEachIndexed { i, (pName, pType) ->
                fnScope.declareVar(pName, pType, args[i].coerceTo(pType))
            }
            val savedCF = controlFlow; val savedRV = returnValue
            controlFlow = ControlFlow.NORMAL
            returnValue = fn.returnType.defaultValue()
            executeBlock(fn.body!!, fnScope)
            val result = returnValue
            if (controlFlow == ControlFlow.RETURN) controlFlow = ControlFlow.NORMAL
            returnValue = savedRV
            result
        }
    }

    override fun print(msg: String) { output.append(msg).append('\n') }
}
