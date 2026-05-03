package no.stunor.origo.eventorapi.services.converter

import io.mockk.every
import io.mockk.mockk
import no.stunor.origo.eventorapi.model.event.Event
import no.stunor.origo.eventorapi.model.event.EventClass
import org.iof.eventor.Amount
import org.iof.eventor.EntryFee
import org.iof.eventor.EntryFeeId
import org.iof.eventor.EntryFeeList
import org.iof.eventor.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.UUID

class FeeConverterTest {

    @Test
    fun `convertEntryFees should handle comma as decimal separator`() {
        // Arrange
        val event = mockk<Event>(relaxed = true)
        every { event.id } returns UUID.randomUUID()
        every { event.eventorId } returns "test-eventor"
        every { event.classes } returns mutableListOf<EventClass>()

        val entryFeeList = EntryFeeList().apply {
            entryFee.add(EntryFee().apply {
                entryFeeId = EntryFeeId().apply { content = "fee-1" }
                name = Name().apply { content = "Test Fee" }
                valueOperator = "fixed"
                amount = Amount().apply {
                    content = "12,5"  // European format with comma
                    currency = "NOK"
                }
                taxIncluded = "Y"
            })
        }

        val eventClassList = emptyList<org.iof.eventor.EventClass>()

        // Act
        val result = FeeConverter.convertEntryFees(entryFeeList, event, eventClassList)

        // Assert
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals(12.5, result[0].amount)
        assertEquals("NOK", result[0].currency)
    }

    @Test
    fun `convertEntryFees should handle period as decimal separator`() {
        // Arrange
        val event = mockk<Event>(relaxed = true)
        every { event.id } returns UUID.randomUUID()
        every { event.eventorId } returns "test-eventor"
        every { event.classes } returns mutableListOf<EventClass>()

        val entryFeeList = EntryFeeList().apply {
            entryFee.add(EntryFee().apply {
                entryFeeId = EntryFeeId().apply { content = "fee-2" }
                name = Name().apply { content = "Test Fee 2" }
                valueOperator = "fixed"
                amount = Amount().apply {
                    content = "15.75"  // US/English format with period
                    currency = "SEK"
                }
                taxIncluded = "N"
            })
        }

        val eventClassList = emptyList<org.iof.eventor.EventClass>()

        // Act
        val result = FeeConverter.convertEntryFees(entryFeeList, event, eventClassList)

        // Assert
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals(15.75, result[0].amount)
        assertEquals("SEK", result[0].currency)
    }

    @Test
    fun `convertEntryFees should handle externalFee with comma decimal separator`() {
        // Arrange
        val event = mockk<Event>(relaxed = true)
        every { event.id } returns UUID.randomUUID()
        every { event.eventorId } returns "test-eventor"
        every { event.classes } returns mutableListOf<EventClass>()

        val externalFee = org.iof.eventor.ExternalFee().apply {
            content = "3,50"  // European format with comma
            currency = "EUR"
        }

        val entryFeeList = EntryFeeList().apply {
            entryFee.add(EntryFee().apply {
                entryFeeId = EntryFeeId().apply { content = "fee-3" }
                name = Name().apply { content = "Test Fee with External Fee" }
                valueOperator = "fixed"
                amount = Amount().apply {
                    content = "100"
                    currency = "EUR"
                }
                this.externalFee = externalFee
                taxIncluded = "Y"
            })
        }

        val eventClassList = emptyList<org.iof.eventor.EventClass>()

        // Act
        val result = FeeConverter.convertEntryFees(entryFeeList, event, eventClassList)

        // Assert
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals(3.5, result[0].externalFee)
    }

    @Test
    fun `convertEntryFees should return empty list when entryFees is null`() {
        // Arrange
        val event = mockk<Event>(relaxed = true)
        val eventClassList = emptyList<org.iof.eventor.EventClass>()

        // Act
        val result = FeeConverter.convertEntryFees(null, event, eventClassList)

        // Assert
        assertNotNull(result)
        assertEquals(0, result.size)
    }
}