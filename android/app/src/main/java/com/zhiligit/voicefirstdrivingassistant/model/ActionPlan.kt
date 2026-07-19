package com.zhiligit.voicefirstdrivingassistant.model

data class ActionPlan(
    val summary: String,
    val requiresConfirmation: Boolean,
    val actions: List<PlannedAction>
)

sealed interface PlannedAction {
    val type: String

    data class CreateNote(
        val title: String,
        val content: String
    ) : PlannedAction {
        override val type: String = "CREATE_NOTE"
    }

    data class CreateReminder(
        val title: String,
        val scheduledAt: String
    ) : PlannedAction {
        override val type: String = "CREATE_REMINDER"
    }

    data class NoAction(val reason: String) : PlannedAction {
        override val type: String = "NO_ACTION"
    }
}
