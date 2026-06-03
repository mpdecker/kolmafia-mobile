package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AshParserTest {

    private fun parse(src: String) = AshParser().parse(src)
    private fun parseExpr(src: String): ExprNode =
        (parse("$src;")[0] as ExprStatement).expr

    // --- Literals ---

    @Test fun literal_int() {
        val lit = parseExpr("42") as LiteralNode
        assertEquals(42L, lit.value.toLong())
    }

    @Test fun literal_float() {
        val lit = parseExpr("3.14") as LiteralNode
        assertEquals(3.14, lit.value.toDouble(), 0.001)
    }

    @Test fun literal_true() { assertTrue((parseExpr("true") as LiteralNode).value.toBoolean()) }

    @Test fun literal_false() { assertEquals(false, (parseExpr("false") as LiteralNode).value.toBoolean()) }

    @Test fun literal_string() { assertEquals("hello", (parseExpr("\"hello\"") as LiteralNode).value.toString()) }

    @Test fun literal_string_escapes() { assertEquals("a\nb", (parseExpr("\"a\\nb\"") as LiteralNode).value.toString()) }

    // --- Binary expressions and precedence ---

    @Test fun binaryExpr_add() { assertEquals(AshOperator.ADD, (parseExpr("1 + 2") as BinOpNode).op) }

    @Test fun precedence_mulOverAdd() {
        val node = parseExpr("1 + 2 * 3") as BinOpNode
        assertEquals(AshOperator.ADD, node.op)
        assertEquals(AshOperator.MUL, (node.right as BinOpNode).op)
    }

    @Test fun precedence_powOverMul() {
        val node = parseExpr("2 * 3 ** 4") as BinOpNode
        assertEquals(AshOperator.MUL, node.op)
        assertEquals(AshOperator.POW, (node.right as BinOpNode).op)
    }

    @Test fun precedence_andOverOr() {
        val node = parseExpr("a || b && c") as BinOpNode
        assertEquals(AshOperator.OR, node.op)
        assertEquals(AshOperator.AND, (node.right as BinOpNode).op)
    }

    @Test fun unaryNot() { assertEquals(AshOperator.NOT, (parseExpr("!true") as UnaryOpNode).op) }
    @Test fun unaryNegate() { assertEquals(AshOperator.NEGATE, (parseExpr("-5") as UnaryOpNode).op) }

    @Test fun ternary() {
        val node = parseExpr("true ? 1 : 2") as TernaryNode
        assertIs<LiteralNode>(node.condition)
        assertIs<LiteralNode>(node.thenExpr)
        assertIs<LiteralNode>(node.elseExpr)
    }

    // --- Statements ---

    @Test fun varDecl_withInit() {
        val node = parse("int x = 5;")[0] as VarDeclNode
        assertEquals("x", node.varName); assertEquals(AshType.INT, node.type); assertIs<LiteralNode>(node.init)
    }

    @Test fun varDecl_withoutInit() {
        val node = parse("string s;")[0] as VarDeclNode
        assertEquals("s", node.varName); assertNull(node.init)
    }

    @Test fun ifStatement_thenOnly() {
        val node = parse("if (true) { }")[0] as IfNode
        assertTrue(node.elseBlock == null); assertTrue(node.elseIfClauses.isEmpty())
    }

    @Test fun ifStatement_withElse() { assertTrue((parse("if (x) { } else { }")[0] as IfNode).elseBlock != null) }

    @Test fun ifStatement_elseIf() {
        val node = parse("if (a) { } else if (b) { } else { }")[0] as IfNode
        assertEquals(1, node.elseIfClauses.size); assertTrue(node.elseBlock != null)
    }

    @Test fun whileStatement() { assertIs<LiteralNode>((parse("while (true) { }")[0] as WhileNode).condition) }

    @Test fun repeatStatement() { assertIs<LiteralNode>((parse("repeat { } until (false);")[0] as RepeatNode).condition) }

    @Test fun forStatement() {
        val node = parse("for i from 1 to 10 { }")[0] as ForNode
        assertEquals("i", node.varName); assertTrue(node.ascending)
    }

    @Test fun forStatement_downto() { assertEquals(false, (parse("for i from 10 downto 1 { }")[0] as ForNode).ascending) }

    @Test fun foreachStatement() { assertEquals(listOf("k"), (parse("foreach k in myMap { }")[0] as ForEachNode).keyNames) }

    @Test fun foreachStatement_twoKeys() { assertEquals(listOf("k", "v"), (parse("foreach k, v in myMap { }")[0] as ForEachNode).keyNames) }

    @Test fun tryStatement_withCatch() { assertTrue((parse("try { } catch { }")[0] as TryNode).catchBlock != null) }

    @Test fun returnStatement_withValue() {
        val fn = parse("int f() { return 42; }")[0] as FunctionDefNode
        assertIs<LiteralNode>((fn.function.body!![0] as ReturnNode).value)
    }

    @Test fun breakStatement() { assertIs<BreakNode>((parse("void f() { break; }")[0] as FunctionDefNode).function.body!![0]) }

    @Test fun functionDef_params() {
        val fn = (parse("int add(int a, int b) { return a + b; }")[0] as FunctionDefNode).function
        assertEquals("add", fn.name); assertEquals(AshType.INT, fn.returnType); assertEquals(2, fn.params.size)
        assertEquals("a" to AshType.INT, fn.params[0]); assertEquals("b" to AshType.INT, fn.params[1])
    }

    @Test fun recordDef() {
        val node = parse("record Point { int x; int y; }")[0] as RecordDefNode
        assertEquals("Point", node.recordType.name); assertEquals(2, node.recordType.fields.size)
        assertEquals("x", node.recordType.fields[0].name); assertEquals(AshType.INT, node.recordType.fields[0].type)
    }

    @Test fun indexExpr() {
        val node = parseExpr("arr[0]") as IndexNode
        assertIs<VarRefNode>(node.aggregate); assertIs<LiteralNode>(node.index)
    }

    @Test fun fieldAccess() {
        val node = parseExpr("rec.hp") as FieldAccessNode
        assertEquals("rec", (node.record as VarRefNode).name); assertEquals("hp", node.fieldName)
    }

    @Test fun assignment_simple() { assertIs<VarLvalue>((parseExpr("x = 5") as AssignNode).target) }

    @Test fun assignment_indexed() { assertIs<IndexLvalue>((parseExpr("arr[0] = 5") as AssignNode).target) }

    @Test fun compoundAssignment_plusEq() {
        val node = parseExpr("x += 1") as AssignNode
        assertIs<VarLvalue>(node.target)
        assertEquals(AshOperator.ADD, (node.value as BinOpNode).op)
    }

    @Test fun functionCall_noArgs() {
        val node = parseExpr("foo()") as FunctionCallNode
        assertEquals("foo", node.name); assertTrue(node.args.isEmpty())
    }

    @Test fun functionCall_withArgs() { assertEquals(2, (parseExpr("bar(1, 2)") as FunctionCallNode).args.size) }

    @Test fun preIncrement() { assertEquals(AshOperator.PRE_INC, (parseExpr("++x") as UnaryOpNode).op) }
    @Test fun postIncrement() { assertEquals(AshOperator.POST_INC, (parseExpr("x++") as UnaryOpNode).op) }

    @Test fun lineComment_skipped() { assertEquals(1, parse("// comment\nint x = 1;").size) }
    @Test fun blockComment_skipped() { assertEquals(1, parse("/* block */ int x = 1;").size) }
    @Test fun hashComment_skipped() { assertEquals(1, parse("# hash\nint x = 1;").size) }
}
