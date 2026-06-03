package net.sourceforge.kolmafia.adventure.choice

class ChoiceHandlerRegistry {

    private val handlers = mutableMapOf<Int, ChoiceHandler>()

    fun register(choiceId: Int, handler: ChoiceHandler) { handlers[choiceId] = handler }

    fun register(vararg choiceIds: Int, handler: ChoiceHandler) {
        choiceIds.forEach { handlers[it] = handler }
    }

    /**
     * Dispatch order:
     *   1. Registered handler present → return handler's decision verbatim.
     *      null from a handler means "go manual regardless of preference" (e.g., the
     *      desired option is not present on this page). The preference is NOT applied.
     *   2. No handler registered → apply raw user preference if > 0.
     *   3. null (manual browser control).
     */
    fun dispatch(ctx: ChoiceContext): Int? {
        val handler = handlers[ctx.choiceId]
        if (handler != null) return handler.decide(ctx)
        return ctx.preference.takeIf { it > 0 }
    }
}
