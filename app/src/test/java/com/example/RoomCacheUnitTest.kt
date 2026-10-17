package com.example

import com.example.data.local.TeamRosterMapper
import com.example.model.PlayerLineup
import com.example.model.TeamLineup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RoomCacheUnitTest {

    @Test
    fun testRosterSerializationAndDeserialization() {
        val originalPlayers = listOf(
            PlayerLineup(1, "أليسون بيكر", "GK", 8.2f, false, 0.5f, 0.9f),
            PlayerLineup(4, "فيرجيل فان دايك", "CB", 8.8f, true, 0.6f, 0.75f),
            PlayerLineup(11, "محمد صلاح", "RW", 9.4f, false, 0.8f, 0.2f)
        )
        val serialized = TeamRosterMapper.serializePlayers(originalPlayers)
        val deserialized = TeamRosterMapper.deserializePlayers(serialized)

        assertEquals(3, deserialized.size)
        assertEquals("أليسون بيكر", deserialized[0].name)
        assertEquals(8.8f, deserialized[1].rating, 0.01f)
        assertEquals(true, deserialized[1].isCaptain)
        assertEquals("محمد صلاح", deserialized[2].name)
        assertEquals(11, deserialized[2].number)
    }

    @Test
    fun testTeamLineupEntityMapping() {
        val lineup = TeamLineup(
            formation = "4-3-3",
            coach = "آرني سلوت",
            starters = listOf(
                PlayerLineup(1, "الحارس", "GK", 8.0f, false, 0.5f, 0.9f),
                PlayerLineup(10, "صانع الألعاب", "CAM", 8.5f, false, 0.5f, 0.35f)
            ),
            subs = emptyList()
        )
        val entity = TeamRosterMapper.toEntity("ليفربول", lineup, "الدوري الإنجليزي", "كرة القدم")
        assertEquals("ليفربول", entity.teamName)
        assertEquals("4-3-3", entity.formation)
        assertEquals("آرني سلوت", entity.coach)

        val restored = TeamRosterMapper.toDomainModel(entity)
        assertEquals(lineup.formation, restored.formation)
        assertEquals(lineup.coach, restored.coach)
        assertEquals(2, restored.starters.size)
        assertEquals("الحارس", restored.starters[0].name)
    }
}
