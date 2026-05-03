package no.stunor.origo.eventorapi.model.event

import com.fasterxml.jackson.annotation.JsonIgnore
import no.stunor.origo.eventorapi.model.organisation.Organisation
import java.sql.Timestamp
import java.util.*

data class Event(
    var id: UUID? = UUID.randomUUID(),
    @JsonIgnore var eventorId: String = "",
    var eventorRef: String = "",
    var name: String = "",
    var type: EventFormEnum = EventFormEnum.Individual,
    var classification: EventClassificationEnum = EventClassificationEnum.Club,
    var status: EventStatusEnum = EventStatusEnum.Created,
    var disciplines: Array<Discipline> = emptyArray(),
    var punchingUnitTypes: Array<PunchingUnitType> = emptyArray(),
    var startDate: Timestamp? = null,
    var finishDate: Timestamp? = null,
    var organisers: MutableList<Organisation> = mutableListOf(),
    var classes: MutableList<EventClass> = mutableListOf(),
    var documents: MutableList<Document> = mutableListOf(),
    var entryBreaks: Array<Timestamp> = emptyArray(),
    var races: MutableList<Race> = mutableListOf(),
    var webUrls: List<String> = listOf(),
    var message: String? = null,
    var email: String? = null,
    var phone: String? = null
) {
    private fun basicFieldsEqual(other: Event): Boolean {
        val checks = listOf(
            { id == other.id },
            { eventorId == other.eventorId },
            { eventorRef == other.eventorRef },
            { name == other.name },
            { type == other.type },
            { classification == other.classification },
            { status == other.status },
            { startDate == other.startDate },
            { finishDate == other.finishDate },
            { message == other.message },
            { email == other.email },
            { phone == other.phone }
        )
        return checks.all { it() }
    }

    private fun arraysEqual(other: Event): Boolean {
        return disciplines.contentEquals(other.disciplines) &&
                punchingUnitTypes.contentEquals(other.punchingUnitTypes) &&
                entryBreaks.contentEquals(other.entryBreaks)
    }

    private fun collectionsEqual(other: Event): Boolean {
        return organisers == other.organisers &&
                classes == other.classes &&
                documents == other.documents &&
                races == other.races &&
                webUrls == other.webUrls
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Event
        if (!basicFieldsEqual(other)) return false
        if (!arraysEqual(other)) return false
        if (!collectionsEqual(other)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + eventorId.hashCode()
        result = 31 * result + eventorRef.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + classification.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + disciplines.contentHashCode()
        result = 31 * result + punchingUnitTypes.contentHashCode()
        result = 31 * result + (startDate?.hashCode() ?: 0)
        result = 31 * result + (finishDate?.hashCode() ?: 0)
        result = 31 * result + organisers.hashCode()
        result = 31 * result + classes.hashCode()
        result = 31 * result + documents.hashCode()
        result = 31 * result + entryBreaks.contentHashCode()
        result = 31 * result + races.hashCode()
        result = 31 * result + webUrls.hashCode()
        result = 31 * result + (message?.hashCode() ?: 0)
        result = 31 * result + (email?.hashCode() ?: 0)
        result = 31 * result + (phone?.hashCode() ?: 0)
        return result
    }
}