package com.zhiligit.voicefirstdrivingassistant.data

import com.zhiligit.voicefirstdrivingassistant.model.PlannedAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentRepositoryTest {
    @Test
    fun parsesCreateNotePlan() {
        val plan = AgentRepository("").parsePlan(
            """{
              "summary":"Create a project note",
              "requires_confirmation":true,
              "actions":[{"type":"CREATE_NOTE","title":"Project Idea","content":"Voice-first assistant"}]
            }"""
        )

        assertTrue(plan.requiresConfirmation)
        assertEquals("Create a project note", plan.summary)
        assertTrue(plan.actions.single() is PlannedAction.CreateNote)
    }
}
