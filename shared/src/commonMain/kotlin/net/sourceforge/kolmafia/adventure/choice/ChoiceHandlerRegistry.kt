package net.sourceforge.kolmafia.adventure.choice

class ChoiceHandlerRegistry {

    private val handlers = mutableMapOf<Int, ChoiceHandler>()

    fun register(choiceId: Int, handler: ChoiceHandler) { handlers[choiceId] = handler }

    fun register(vararg choiceIds: Int, handler: ChoiceHandler) {
        choiceIds.forEach { handlers[it] = handler }
    }

    /**
     * Dispatch order:
     *   1. Registered handler (if any) — handler returning null means "fall through"
     *   2. Raw user preference if > 0
     *   3. null (manual browser control)
     */
    fun dispatch(ctx: ChoiceContext): Int? {
        val handlerResult = handlers[ctx.choiceId]?.decide(ctx)
        if (handlerResult != null) return handlerResult
        return ctx.preference.takeIf { it > 0 }
    }
}
