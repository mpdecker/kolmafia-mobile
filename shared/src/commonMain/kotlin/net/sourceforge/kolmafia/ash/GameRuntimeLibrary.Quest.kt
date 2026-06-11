package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

internal fun GameRuntimeLibrary.registerQuestQueries(scope: AshScope) {
    fun resolveQuest(query: String): Quest? {
        val trimmed = query.trim()
        Quest.entries.find { it.name.equals(trimmed, ignoreCase = true) }?.let { return it }
        Quest.entries.find { it.prefKey.equals(trimmed, ignoreCase = true) }?.let { return it }
        return Quest.entries.find {
            it.prefKey.equals("quest$trimmed", ignoreCase = true) ||
                it.prefKey.endsWith(trimmed, ignoreCase = true)
        }
    }

    fun db(): QuestDatabase? = questDatabase

    regFn(scope, "quest_status", AshType.STRING, listOf("quest" to AshType.STRING)) { _, args ->
        val quest = resolveQuest(args[0].toString()) ?: return@regFn AshValue.EMPTY_STRING
        AshValue.of(db()?.getProgress(quest) ?: QuestDatabase.UNSTARTED)
    }

    regFn(scope, "quest_step", AshType.INT, listOf("quest" to AshType.STRING)) { _, args ->
        val quest = resolveQuest(args[0].toString()) ?: return@regFn AshValue.of(-1L)
        val step = db()?.getProgress(quest) ?: QuestDatabase.UNSTARTED
        AshValue.of(QuestDatabase.stepOrdinal(step).toLong())
    }

    regFn(scope, "quest_finished", AshType.BOOLEAN, listOf("quest" to AshType.STRING)) { _, args ->
        val quest = resolveQuest(args[0].toString()) ?: return@regFn AshValue.FALSE
        AshValue.of(db()?.isQuestFinished(quest) ?: false)
    }

    regFn(scope, "set_quest_progress", AshType.VOID,
        listOf("quest" to AshType.STRING, "step" to AshType.STRING)) { _, args ->
        val quest = resolveQuest(args[0].toString()) ?: return@regFn AshValue.VOID
        db()?.setProgress(quest, QuestDatabase.validateStep(args[1].toString()))
        AshValue.VOID
    }
}
