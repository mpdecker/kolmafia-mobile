package net.sourceforge.kolmafia.ash

sealed class ParseTreeNode

// --- Statements ---

data class IfNode(
    val condition: ExprNode,
    val thenBlock: List<ParseTreeNode>,
    val elseIfClauses: List<Pair<ExprNode, List<ParseTreeNode>>>,
    val elseBlock: List<ParseTreeNode>?
) : ParseTreeNode()

data class WhileNode(val condition: ExprNode, val body: List<ParseTreeNode>) : ParseTreeNode()

data class RepeatNode(val body: List<ParseTreeNode>, val condition: ExprNode) : ParseTreeNode()

data class ForNode(
    val varName: String,
    val start: ExprNode,
    val end: ExprNode,
    val step: ExprNode?,
    val ascending: Boolean
) : ParseTreeNode()

data class ForEachNode(
    val keyNames: List<String>,
    val aggregate: ExprNode,
    val body: List<ParseTreeNode>
) : ParseTreeNode()

data class TryNode(
    val body: List<ParseTreeNode>,
    val catchBlock: List<ParseTreeNode>?
) : ParseTreeNode()

data class ReturnNode(val value: ExprNode?) : ParseTreeNode()
object BreakNode : ParseTreeNode()
object ContinueNode : ParseTreeNode()
object ExitNode : ParseTreeNode()

data class VarDeclNode(
    val varName: String,
    val type: AshType,
    val init: ExprNode?
) : ParseTreeNode()

data class FunctionDefNode(val function: AshFunction) : ParseTreeNode()
data class RecordDefNode(val recordType: RecordType) : ParseTreeNode()

data class ExprStatement(val expr: ExprNode) : ParseTreeNode()

// --- Expressions ---

sealed class ExprNode : ParseTreeNode()

data class LiteralNode(val value: AshValue) : ExprNode()
data class VarRefNode(val name: String) : ExprNode()
data class AssignNode(val target: LvalueNode, val value: ExprNode) : ExprNode()
data class BinOpNode(val op: AshOperator, val left: ExprNode, val right: ExprNode) : ExprNode()
data class UnaryOpNode(val op: AshOperator, val operand: ExprNode) : ExprNode()
data class TernaryNode(val condition: ExprNode, val thenExpr: ExprNode, val elseExpr: ExprNode) : ExprNode()
data class FunctionCallNode(val name: String, val args: List<ExprNode>) : ExprNode()
data class IndexNode(val aggregate: ExprNode, val index: ExprNode) : ExprNode()
data class FieldAccessNode(val record: ExprNode, val fieldName: String) : ExprNode()
data class AggregateLiteralNode(
    val aggType: AggregateType,
    val entries: List<Pair<ExprNode?, ExprNode>>
) : ExprNode()
data class RecordLiteralNode(
    val recordType: RecordType,
    val fieldExprs: List<ExprNode?>
) : ExprNode()

// --- Lvalues (assignment targets) ---

sealed class LvalueNode : ParseTreeNode()
data class VarLvalue(val name: String) : LvalueNode()
data class IndexLvalue(val aggregate: ExprNode, val index: ExprNode) : LvalueNode()
data class FieldLvalue(val record: ExprNode, val fieldName: String) : LvalueNode()
