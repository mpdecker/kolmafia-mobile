package net.sourceforge.kolmafia.ash

enum class TokenType {
    INT_LIT, FLOAT_LIT, BOOL_LIT, STRING_LIT,
    IDENT,
    IF, ELSE, WHILE, REPEAT, UNTIL,
    FOR, FROM, TO, UPTO, DOWNTO, BY,
    FOREACH, IN, TRY, CATCH,
    RETURN, BREAK, CONTINUE, EXIT, RECORD,
    PLUS, MINUS, STAR, SLASH, PERCENT, STAR_STAR,
    EQ_EQ, BANG_EQ, LT, LE, GT, GE,
    AMP_AMP, PIPE_PIPE, BANG,
    AMP, PIPE, CARET, TILDE,
    PLUS_PLUS, MINUS_MINUS,
    PLUS_EQ, MINUS_EQ, STAR_EQ, SLASH_EQ, PERCENT_EQ,
    EQ, QUESTION, COLON,
    LPAREN, RPAREN, LBRACE, RBRACE, LBRACKET, RBRACKET,
    SEMICOLON, COMMA, DOT,
    EOF
}

data class Token(val type: TokenType, val text: String, val line: Int)

class AshParser {
    private var tokens: List<Token> = emptyList()
    private var pos: Int = 0
    private val knownRecords: MutableMap<String, RecordType> = mutableMapOf()

    private companion object {
        val KEYWORDS: Map<String, TokenType> = mapOf(
            "if" to TokenType.IF, "else" to TokenType.ELSE,
            "while" to TokenType.WHILE, "repeat" to TokenType.REPEAT, "until" to TokenType.UNTIL,
            "for" to TokenType.FOR, "from" to TokenType.FROM, "to" to TokenType.TO,
            "upto" to TokenType.UPTO, "downto" to TokenType.DOWNTO, "by" to TokenType.BY,
            "foreach" to TokenType.FOREACH, "in" to TokenType.IN,
            "try" to TokenType.TRY, "catch" to TokenType.CATCH,
            "return" to TokenType.RETURN, "break" to TokenType.BREAK,
            "continue" to TokenType.CONTINUE, "exit" to TokenType.EXIT,
            "record" to TokenType.RECORD
        )
    }

    // ── Lexer ──────────────────────────────────────────────────────

    private fun tokenize(source: String): List<Token> {
        val result = mutableListOf<Token>()
        var i = 0
        var line = 1

        while (i < source.length) {
            while (i < source.length && source[i].isWhitespace()) {
                if (source[i] == '\n') line++
                i++
            }
            if (i >= source.length) break
            val c = source[i]

            if (c == '#' || (c == '/' && i + 1 < source.length && source[i + 1] == '/')) {
                while (i < source.length && source[i] != '\n') i++
                continue
            }
            if (c == '/' && i + 1 < source.length && source[i + 1] == '*') {
                i += 2
                while (i + 1 < source.length && !(source[i] == '*' && source[i + 1] == '/')) {
                    if (source[i] == '\n') line++
                    i++
                }
                if (i + 1 >= source.length) throw ScriptException("Unterminated block comment", line)
                i += 2
                continue
            }
            if (c == '"') {
                val sb = StringBuilder(); i++
                while (i < source.length && source[i] != '"') {
                    if (source[i] == '\\' && i + 1 < source.length) {
                        i++
                        sb.append(when (source[i]) {
                            'n' -> '\n'; 't' -> '\t'; 'r' -> '\r'; '"' -> '"'; '\\' -> '\\'
                            else -> source[i]
                        })
                    } else sb.append(source[i])
                    i++
                }
                i++
                result.add(Token(TokenType.STRING_LIT, sb.toString(), line))
                continue
            }
            if (c.isDigit() || (c == '.' && i + 1 < source.length && source[i + 1].isDigit())) {
                val start = i; var isFloat = false
                while (i < source.length && source[i].isDigit()) i++
                if (i < source.length && source[i] == '.') { isFloat = true; i++; while (i < source.length && source[i].isDigit()) i++ }
                result.add(Token(if (isFloat) TokenType.FLOAT_LIT else TokenType.INT_LIT, source.substring(start, i), line))
                continue
            }
            if (c.isLetter() || c == '_') {
                val start = i
                while (i < source.length && (source[i].isLetterOrDigit() || source[i] == '_')) i++
                val text = source.substring(start, i)
                when (text.lowercase()) {
                    "true", "false" -> result.add(Token(TokenType.BOOL_LIT, text, line))
                    else -> result.add(Token(KEYWORDS[text.lowercase()] ?: TokenType.IDENT, text, line))
                }
                continue
            }
            fun ch(offset: Int = 1): Char? = if (i + offset < source.length) source[i + offset] else null
            val tok = when {
                c == '+' && ch() == '+' -> Token(TokenType.PLUS_PLUS, "++", line).also { i += 2 }
                c == '+' && ch() == '=' -> Token(TokenType.PLUS_EQ, "+=", line).also { i += 2 }
                c == '+' -> Token(TokenType.PLUS, "+", line).also { i++ }
                c == '-' && ch() == '-' -> Token(TokenType.MINUS_MINUS, "--", line).also { i += 2 }
                c == '-' && ch() == '=' -> Token(TokenType.MINUS_EQ, "-=", line).also { i += 2 }
                c == '-' -> Token(TokenType.MINUS, "-", line).also { i++ }
                c == '*' && ch() == '*' -> Token(TokenType.STAR_STAR, "**", line).also { i += 2 }
                c == '*' && ch() == '=' -> Token(TokenType.STAR_EQ, "*=", line).also { i += 2 }
                c == '*' -> Token(TokenType.STAR, "*", line).also { i++ }
                c == '/' && ch() == '=' -> Token(TokenType.SLASH_EQ, "/=", line).also { i += 2 }
                c == '/' -> Token(TokenType.SLASH, "/", line).also { i++ }
                c == '%' && ch() == '=' -> Token(TokenType.PERCENT_EQ, "%=", line).also { i += 2 }
                c == '%' -> Token(TokenType.PERCENT, "%", line).also { i++ }
                c == '=' && ch() == '=' -> Token(TokenType.EQ_EQ, "==", line).also { i += 2 }
                c == '=' -> Token(TokenType.EQ, "=", line).also { i++ }
                c == '!' && ch() == '=' -> Token(TokenType.BANG_EQ, "!=", line).also { i += 2 }
                c == '!' -> Token(TokenType.BANG, "!", line).also { i++ }
                c == '<' && ch() == '=' -> Token(TokenType.LE, "<=", line).also { i += 2 }
                c == '<' -> Token(TokenType.LT, "<", line).also { i++ }
                c == '>' && ch() == '=' -> Token(TokenType.GE, ">=", line).also { i += 2 }
                c == '>' -> Token(TokenType.GT, ">", line).also { i++ }
                c == '&' && ch() == '&' -> Token(TokenType.AMP_AMP, "&&", line).also { i += 2 }
                c == '&' -> Token(TokenType.AMP, "&", line).also { i++ }
                c == '|' && ch() == '|' -> Token(TokenType.PIPE_PIPE, "||", line).also { i += 2 }
                c == '|' -> Token(TokenType.PIPE, "|", line).also { i++ }
                c == '^' -> Token(TokenType.CARET, "^", line).also { i++ }
                c == '~' -> Token(TokenType.TILDE, "~", line).also { i++ }
                c == '?' -> Token(TokenType.QUESTION, "?", line).also { i++ }
                c == ':' -> Token(TokenType.COLON, ":", line).also { i++ }
                c == '(' -> Token(TokenType.LPAREN, "(", line).also { i++ }
                c == ')' -> Token(TokenType.RPAREN, ")", line).also { i++ }
                c == '{' -> Token(TokenType.LBRACE, "{", line).also { i++ }
                c == '}' -> Token(TokenType.RBRACE, "}", line).also { i++ }
                c == '[' -> Token(TokenType.LBRACKET, "[", line).also { i++ }
                c == ']' -> Token(TokenType.RBRACKET, "]", line).also { i++ }
                c == ';' -> Token(TokenType.SEMICOLON, ";", line).also { i++ }
                c == ',' -> Token(TokenType.COMMA, ",", line).also { i++ }
                c == '.' -> Token(TokenType.DOT, ".", line).also { i++ }
                else -> throw ScriptException("Unexpected character '$c'", line)
            }
            result.add(tok)
        }
        result.add(Token(TokenType.EOF, "", line))
        return result
    }

    // ── Token utilities ────────────────────────────────────────────

    private fun current(): Token = tokens[pos]
    private fun check(type: TokenType): Boolean = current().type == type
    private fun checkIdent(text: String): Boolean = check(TokenType.IDENT) && current().text.equals(text, ignoreCase = true)
    private fun checkNext(type: TokenType): Boolean = pos + 1 < tokens.size && tokens[pos + 1].type == type
    private fun advance(): Token = tokens[pos++]
    private fun currentLine(): Int = current().line

    private fun expect(type: TokenType): Token {
        if (!check(type)) throw ScriptException("Expected $type but got ${current().type} ('${current().text}')", currentLine())
        return advance()
    }

    private fun expectIdent(): String {
        if (current().type != TokenType.IDENT) throw ScriptException("Expected identifier, got '${current().text}'", currentLine())
        return advance().text
    }

    // ── Type parsing ───────────────────────────────────────────────

    private fun isTypeName(text: String): Boolean = AshType.fromName(text, knownRecords) != null

    private fun parseTypeToken(): AshType {
        val name = current().text
        if (!isTypeName(name)) throw ScriptException("Unknown type '$name'", currentLine())
        advance()
        if (!check(TokenType.LBRACKET)) return AshType.fromName(name, knownRecords)!!
        advance()
        return if (tokens[pos].type == TokenType.INT_LIT) {
            val size = tokens[pos].text.toInt(); advance()
            expect(TokenType.RBRACKET)
            AggregateType(AshType.INT, AshType.fromName(name, knownRecords)!!, size)
        } else {
            val indexName = current().text; advance()
            expect(TokenType.RBRACKET)
            val base = AshType.fromName(name, knownRecords)!!
            val indexType = AshType.fromName(indexName, knownRecords)
                ?: throw ScriptException("Unknown index type '$indexName'", currentLine())
            AggregateType(indexType, base)
        }
    }

    // ── Expression parsing (precedence climbing) ───────────────────

    private fun parseExpr(): ExprNode {
        val left = parseTernary()
        return when {
            check(TokenType.EQ) -> { advance(); AssignNode(toTarget(left), parseTernary()) }
            check(TokenType.PLUS_EQ) -> { advance(); val t = toTarget(left); AssignNode(t, BinOpNode(AshOperator.ADD, left, parseTernary())) }
            check(TokenType.MINUS_EQ) -> { advance(); val t = toTarget(left); AssignNode(t, BinOpNode(AshOperator.SUB, left, parseTernary())) }
            check(TokenType.STAR_EQ) -> { advance(); val t = toTarget(left); AssignNode(t, BinOpNode(AshOperator.MUL, left, parseTernary())) }
            check(TokenType.SLASH_EQ) -> { advance(); val t = toTarget(left); AssignNode(t, BinOpNode(AshOperator.DIV, left, parseTernary())) }
            check(TokenType.PERCENT_EQ) -> { advance(); val t = toTarget(left); AssignNode(t, BinOpNode(AshOperator.REM, left, parseTernary())) }
            else -> left
        }
    }

    private fun toTarget(expr: ExprNode): LvalueNode = when (expr) {
        is VarRefNode -> VarLvalue(expr.name)
        is IndexNode -> IndexLvalue(expr.aggregate, expr.index)
        is FieldAccessNode -> FieldLvalue(expr.record, expr.fieldName)
        else -> throw ScriptException("Not an assignable expression", currentLine())
    }

    private fun parseTernary(): ExprNode {
        val cond = parseOr()
        if (!check(TokenType.QUESTION)) return cond
        advance()
        val then = parseOr()
        expect(TokenType.COLON)
        return TernaryNode(cond, then, parseOr())
    }

    private fun parseOr(): ExprNode {
        var l = parseAnd()
        while (check(TokenType.PIPE_PIPE)) { advance(); l = BinOpNode(AshOperator.OR, l, parseAnd()) }
        return l
    }

    private fun parseAnd(): ExprNode {
        var l = parseBitOr()
        while (check(TokenType.AMP_AMP)) { advance(); l = BinOpNode(AshOperator.AND, l, parseBitOr()) }
        return l
    }

    private fun parseBitOr(): ExprNode {
        var l = parseBitXor()
        while (check(TokenType.PIPE)) { advance(); l = BinOpNode(AshOperator.BOR, l, parseBitXor()) }
        return l
    }

    private fun parseBitXor(): ExprNode {
        var l = parseBitAnd()
        while (check(TokenType.CARET)) { advance(); l = BinOpNode(AshOperator.BXOR, l, parseBitAnd()) }
        return l
    }

    private fun parseBitAnd(): ExprNode {
        var l = parseEquality()
        while (check(TokenType.AMP)) { advance(); l = BinOpNode(AshOperator.BAND, l, parseEquality()) }
        return l
    }

    private fun parseEquality(): ExprNode {
        var l = parseComparison()
        while (true) {
            l = when {
                check(TokenType.EQ_EQ) -> { advance(); BinOpNode(AshOperator.EQ, l, parseComparison()) }
                check(TokenType.BANG_EQ) -> { advance(); BinOpNode(AshOperator.NEQ, l, parseComparison()) }
                else -> return l
            }
        }
    }

    private fun parseComparison(): ExprNode {
        var l = parseAddition()
        while (true) {
            l = when {
                check(TokenType.LT) -> { advance(); BinOpNode(AshOperator.LT, l, parseAddition()) }
                check(TokenType.LE) -> { advance(); BinOpNode(AshOperator.LE, l, parseAddition()) }
                check(TokenType.GT) -> { advance(); BinOpNode(AshOperator.GT, l, parseAddition()) }
                check(TokenType.GE) -> { advance(); BinOpNode(AshOperator.GE, l, parseAddition()) }
                else -> return l
            }
        }
    }

    private fun parseAddition(): ExprNode {
        var l = parseMultiply()
        while (true) {
            l = when {
                check(TokenType.PLUS) -> { advance(); BinOpNode(AshOperator.ADD, l, parseMultiply()) }
                check(TokenType.MINUS) -> { advance(); BinOpNode(AshOperator.SUB, l, parseMultiply()) }
                else -> return l
            }
        }
    }

    private fun parseMultiply(): ExprNode {
        var l = parsePower()
        while (true) {
            l = when {
                check(TokenType.STAR) -> { advance(); BinOpNode(AshOperator.MUL, l, parsePower()) }
                check(TokenType.SLASH) -> { advance(); BinOpNode(AshOperator.DIV, l, parsePower()) }
                check(TokenType.PERCENT) -> { advance(); BinOpNode(AshOperator.REM, l, parsePower()) }
                else -> return l
            }
        }
    }

    private fun parsePower(): ExprNode {
        val base = parseUnary()
        if (!check(TokenType.STAR_STAR)) return base
        advance()
        return BinOpNode(AshOperator.POW, base, parsePower())
    }

    private fun parseUnary(): ExprNode = when {
        check(TokenType.BANG) -> { advance(); UnaryOpNode(AshOperator.NOT, parseUnary()) }
        check(TokenType.MINUS) -> { advance(); UnaryOpNode(AshOperator.NEGATE, parseUnary()) }
        check(TokenType.TILDE) -> { advance(); UnaryOpNode(AshOperator.BNOT, parseUnary()) }
        check(TokenType.PLUS_PLUS) -> { advance(); UnaryOpNode(AshOperator.PRE_INC, parsePostfix()) }
        check(TokenType.MINUS_MINUS) -> { advance(); UnaryOpNode(AshOperator.PRE_DEC, parsePostfix()) }
        else -> parsePostfix()
    }

    private fun parsePostfix(): ExprNode {
        var expr = parsePrimary()
        while (true) {
            expr = when {
                check(TokenType.PLUS_PLUS) -> { advance(); UnaryOpNode(AshOperator.POST_INC, expr) }
                check(TokenType.MINUS_MINUS) -> { advance(); UnaryOpNode(AshOperator.POST_DEC, expr) }
                check(TokenType.LBRACKET) -> { advance(); val idx = parseExpr(); expect(TokenType.RBRACKET); IndexNode(expr, idx) }
                check(TokenType.DOT) -> { advance(); FieldAccessNode(expr, expectIdent()) }
                else -> return expr
            }
        }
    }

    private fun parsePrimary(): ExprNode = when {
        check(TokenType.LPAREN) -> { advance(); val e = parseExpr(); expect(TokenType.RPAREN); e }
        check(TokenType.INT_LIT) -> { val v = current().text.toLong(); advance(); LiteralNode(AshValue.of(v)) }
        check(TokenType.FLOAT_LIT) -> { val v = current().text.toDouble(); advance(); LiteralNode(AshValue.of(v)) }
        check(TokenType.BOOL_LIT) -> { val v = current().text == "true"; advance(); LiteralNode(AshValue.of(v)) }
        check(TokenType.STRING_LIT) -> { val v = current().text; advance(); LiteralNode(AshValue.of(v)) }
        check(TokenType.IDENT) -> {
            val name = current().text; advance()
            if (check(TokenType.LPAREN)) {
                advance(); val args = parseArgList(); expect(TokenType.RPAREN)
                FunctionCallNode(name, args)
            } else VarRefNode(name)
        }
        else -> throw ScriptException("Unexpected token '${current().text}'", currentLine())
    }

    private fun parseArgList(): List<ExprNode> {
        if (check(TokenType.RPAREN)) return emptyList()
        val args = mutableListOf(parseExpr())
        while (check(TokenType.COMMA)) { advance(); args.add(parseExpr()) }
        return args
    }

    // ── Statement parsing ──────────────────────────────────────────

    private fun parseStatement(): ParseTreeNode? = when {
        check(TokenType.IF) -> parseIf()
        check(TokenType.WHILE) -> parseWhile()
        check(TokenType.REPEAT) -> parseRepeat()
        check(TokenType.FOR) -> parseFor()
        check(TokenType.FOREACH) -> parseForeach()
        check(TokenType.TRY) -> parseTry()
        check(TokenType.RETURN) -> parseReturn()
        check(TokenType.BREAK) -> { advance(); expect(TokenType.SEMICOLON); BreakNode }
        check(TokenType.CONTINUE) -> { advance(); expect(TokenType.SEMICOLON); ContinueNode }
        check(TokenType.EXIT) -> { advance(); expect(TokenType.SEMICOLON); ExitNode }
        check(TokenType.RBRACE) || check(TokenType.EOF) -> null
        current().type == TokenType.IDENT && isTypeName(current().text) && checkNext(TokenType.IDENT) -> parseVarDecl()
        current().type == TokenType.IDENT && isTypeName(current().text) && checkNext(TokenType.LBRACKET) -> parseVarDecl()
        else -> { val e = parseExpr(); expect(TokenType.SEMICOLON); ExprStatement(e) }
    }

    private fun parseIf(): IfNode {
        expect(TokenType.IF); expect(TokenType.LPAREN)
        val cond = parseExpr(); expect(TokenType.RPAREN)
        val thenBlock = parseBlock()
        val elseIfs = mutableListOf<Pair<ExprNode, List<ParseTreeNode>>>()
        var elseBlock: List<ParseTreeNode>? = null
        while (check(TokenType.ELSE)) {
            advance()
            if (check(TokenType.IF)) { advance(); expect(TokenType.LPAREN); val ec = parseExpr(); expect(TokenType.RPAREN); elseIfs.add(ec to parseBlock()) }
            else { elseBlock = parseBlock(); break }
        }
        return IfNode(cond, thenBlock, elseIfs, elseBlock)
    }

    private fun parseWhile(): WhileNode {
        expect(TokenType.WHILE); expect(TokenType.LPAREN)
        val cond = parseExpr(); expect(TokenType.RPAREN)
        return WhileNode(cond, parseBlock())
    }

    private fun parseRepeat(): RepeatNode {
        expect(TokenType.REPEAT)
        val body = parseBlock()
        expect(TokenType.UNTIL); expect(TokenType.LPAREN)
        val cond = parseExpr(); expect(TokenType.RPAREN); expect(TokenType.SEMICOLON)
        return RepeatNode(body, cond)
    }

    private fun parseFor(): ForNode {
        expect(TokenType.FOR)
        val varName = expectIdent()
        // "from" is a keyword token (TokenType.FROM), not IDENT
        if (!check(TokenType.FROM)) throw ScriptException("Expected 'from' in for loop", currentLine())
        advance()
        val start = parseExpr()
        val ascending = when {
            check(TokenType.TO) || check(TokenType.UPTO) -> { advance(); true }
            check(TokenType.DOWNTO) -> { advance(); false }
            else -> throw ScriptException("Expected 'to', 'upto', or 'downto'", currentLine())
        }
        val end = parseExpr()
        // "by" is a keyword token (TokenType.BY), not IDENT
        val step = if (check(TokenType.BY)) { advance(); parseExpr() } else null
        return ForNode(varName, start, end, step, ascending, parseBlock())
    }

    private fun parseForeach(): ForEachNode {
        expect(TokenType.FOREACH)
        val keys = mutableListOf(expectIdent())
        while (check(TokenType.COMMA)) { advance(); keys.add(expectIdent()) }
        expect(TokenType.IN)
        val agg = parseExpr()
        return ForEachNode(keys, agg, parseBlock())
    }

    private fun parseTry(): TryNode {
        expect(TokenType.TRY)
        val body = parseBlock()
        val catchBlock = if (check(TokenType.CATCH)) { advance(); parseBlock() } else null
        return TryNode(body, catchBlock)
    }

    private fun parseReturn(): ReturnNode {
        expect(TokenType.RETURN)
        val value = if (!check(TokenType.SEMICOLON)) parseExpr() else null
        expect(TokenType.SEMICOLON)
        return ReturnNode(value)
    }

    private fun parseVarDecl(): VarDeclNode {
        val type = parseTypeToken()
        val name = expectIdent()
        val init = if (check(TokenType.EQ)) { advance(); parseExpr() } else null
        expect(TokenType.SEMICOLON)
        return VarDeclNode(name, type, init)
    }

    private fun parseBlock(): List<ParseTreeNode> {
        expect(TokenType.LBRACE)
        val stmts = mutableListOf<ParseTreeNode>()
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) { parseStatement()?.let { stmts.add(it) } }
        expect(TokenType.RBRACE)
        return stmts
    }

    // ── Top-level ──────────────────────────────────────────────────

    private fun isFunctionDef(): Boolean {
        if (current().type != TokenType.IDENT || !isTypeName(current().text)) return false
        var lookahead = pos + 1
        if (lookahead < tokens.size && tokens[lookahead].type == TokenType.LBRACKET) {
            lookahead++
            while (lookahead < tokens.size && tokens[lookahead].type != TokenType.RBRACKET) lookahead++
            lookahead++
        }
        if (lookahead >= tokens.size || tokens[lookahead].type != TokenType.IDENT) return false
        return lookahead + 1 < tokens.size && tokens[lookahead + 1].type == TokenType.LPAREN
    }

    private fun parseFunctionDef(): FunctionDefNode {
        val returnType = parseTypeToken()
        val name = expectIdent()
        expect(TokenType.LPAREN)
        val params = mutableListOf<Pair<String, AshType>>()
        if (!check(TokenType.RPAREN)) {
            do {
                val pType = parseTypeToken(); val pName = expectIdent()
                params.add(pName to pType)
            } while (check(TokenType.COMMA).also { if (it) advance() })
        }
        expect(TokenType.RPAREN)
        return FunctionDefNode(AshFunction(name, returnType, params, parseBlock()))
    }

    private fun parseRecordDef(): RecordDefNode {
        expect(TokenType.RECORD)
        val name = expectIdent()
        expect(TokenType.LBRACE)
        val fields = mutableListOf<RecordField>()
        var idx = 0
        while (!check(TokenType.RBRACE) && !check(TokenType.EOF)) {
            val fieldType = parseTypeToken(); val fieldName = expectIdent()
            expect(TokenType.SEMICOLON)
            fields.add(RecordField(fieldName, fieldType, idx++))
        }
        expect(TokenType.RBRACE)
        val rt = RecordType(name, fields)
        knownRecords[name.lowercase()] = rt
        return RecordDefNode(rt)
    }

    fun parse(source: String): List<ParseTreeNode> {
        tokens = tokenize(source)
        pos = 0
        val nodes = mutableListOf<ParseTreeNode>()
        while (!check(TokenType.EOF)) {
            when {
                check(TokenType.RECORD) -> nodes.add(parseRecordDef())
                isFunctionDef() -> nodes.add(parseFunctionDef())
                else -> parseStatement()?.let { nodes.add(it) }
            }
        }
        return nodes
    }
}
