package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.event.Discipline
import no.stunor.origo.eventorapi.model.event.Event
import no.stunor.origo.eventorapi.model.event.EventClassificationEnum
import no.stunor.origo.eventorapi.model.event.EventFormEnum
import no.stunor.origo.eventorapi.model.event.EventStatusEnum
import no.stunor.origo.eventorapi.model.event.PunchingUnitType
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.sql.Timestamp
import java.util.*
import javax.sql.DataSource

internal object EventTable : Table("event") {
    val id = uuid("id")
    val eventorId = text("eventor_id")
    val eventorRef = text("eventor_ref")
    val name = text("name")
    val type = text("type")
    val classification = text("classification")
    val status = text("status")
    val disciplines = text("disciplines").nullable()
    val punchingUnitTypes = text("punching_unit_types").nullable()
    val startDate = timestamp("start_date").nullable()
    val finishDate = timestamp("finish_date").nullable()
    val entryBreaks = text("entry_breaks").nullable()
    val webUrls = text("web_urls").nullable()
    val message = text("message").nullable()
    val email = text("email").nullable()
    val phone = text("phone").nullable()
}

private object EventOrganiserTable : Table("event_organiser") {
    val eventId = uuid("event_id").references(EventTable.id)
    val organisationId = uuid("organisation_id").references(OrganisationTable.id)
}

internal object ClassTable : Table("class") {
    val id = uuid("id")
    val eventId = uuid("event_id").references(EventTable.id)
    val eventorRef = text("eventor_ref")
    val name = text("name")
    val shortName = text("short_name")
    val type = text("type")
    val minAge = integer("min_age").nullable()
    val maxAge = integer("max_age").nullable()
    val gender = text("gender")
    val presentTime = bool("present_time")
    val orderedResult = bool("ordered_result")
    val legs = integer("legs")
    val minAverageAge = integer("min_average_age").nullable()
    val maxAverageAge = integer("max_average_age").nullable()
}

private object DocumentTable : Table("document") {
    val id = uuid("id")
    val eventId = uuid("event_id").references(EventTable.id)
    val eventorRef = text("eventor_ref")
    val name = text("name")
    val url = text("url")
    val type = text("type")
}

private object RaceTable : Table("race") {
    val id = uuid("id")
    val eventId = uuid("event_id").references(EventTable.id)
    val eventorRef = text("eventor_ref")
    val name = text("name")
    val lightCondition = text("light_condition")
    val distance = text("distance")
    val date = timestamp("date").nullable()
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()
    val startList = bool("start_list")
    val resultList = bool("result_list")
    val livelox = bool("livelox")
}

class EventRepository(
    dataSource: DataSource,
    private val organisationRepository: OrganisationRepository
) {
    
    private val database = Database.connect(dataSource)

    private fun parseDisciplines(disciplinesStr: String?): Array<Discipline> {
        if (disciplinesStr.isNullOrEmpty()) return emptyArray()
        return disciplinesStr.split(",").map { Discipline.valueOf(it.trim()) }.toTypedArray()
    }

    private fun parsePunchingTypes(typesStr: String?): Array<PunchingUnitType> {
        if (typesStr.isNullOrEmpty()) return emptyArray()
        return typesStr.split(",").map { PunchingUnitType.valueOf(it.trim()) }.toTypedArray()
    }

    private fun parseWebUrls(urlsStr: String?): List<String> {
        if (urlsStr.isNullOrEmpty()) return emptyList()
        return urlsStr.split("\n").filter { it.isNotEmpty() }
    }

    private fun toEvent(row: ResultRow): Event {
        return Event(
            id = row[EventTable.id],
            eventorId = row[EventTable.eventorId],
            eventorRef = row[EventTable.eventorRef],
            name = row[EventTable.name],
            type = EventFormEnum.valueOf(row[EventTable.type]),
            classification = EventClassificationEnum.valueOf(row[EventTable.classification]),
            status = EventStatusEnum.valueOf(row[EventTable.status]),
            disciplines = parseDisciplines(row[EventTable.disciplines]),
            punchingUnitTypes = parsePunchingTypes(row[EventTable.punchingUnitTypes]),
            startDate = row[EventTable.startDate]?.let { Timestamp.from(it) },
            finishDate = row[EventTable.finishDate]?.let { Timestamp.from(it) },
            organisers = mutableListOf(), // Load separately
            classes = mutableListOf(), // Load separately
            documents = mutableListOf(), // Load separately
            entryBreaks = emptyArray(), // Could be parsed from entryBreaks column if needed
            races = mutableListOf(), // Load separately
            webUrls = parseWebUrls(row[EventTable.webUrls]),
            message = row[EventTable.message],
            email = row[EventTable.email],
            phone = row[EventTable.phone]
        )
    }
    
    fun findByEventorIdAndEventorRef(eventorId: String, eventorRef: String): Event? {
        return transaction(database) {
            EventTable
                .selectAll()
                .where { (EventTable.eventorId eq eventorId) and (EventTable.eventorRef eq eventorRef) }
                .limit(1)
                .map(::toEvent)
                .singleOrNull()
        }
    }
    
    fun save(event: Event): Event {
        transaction(database) {
            // Generate ID if not present
            if (event.id == null) {
                event.id = UUID.randomUUID()
            }

            // Convert disciplines and punching types to strings for storage
            val disciplinesStr = event.disciplines.joinToString(",") { it.name }
            val punchingTypesStr = event.punchingUnitTypes.joinToString(",") { it.name }
            val webUrlsStr = event.webUrls.joinToString("\n")

            // Upsert main event record
            EventTable.upsert {
                it[EventTable.id] = event.id!!
                it[EventTable.eventorId] = event.eventorId
                it[EventTable.eventorRef] = event.eventorRef
                it[EventTable.name] = event.name
                it[EventTable.type] = event.type.name
                it[EventTable.classification] = event.classification.name
                it[EventTable.status] = event.status.name
                it[EventTable.disciplines] = disciplinesStr
                it[EventTable.punchingUnitTypes] = punchingTypesStr
                it[EventTable.startDate] = event.startDate?.toInstant()
                it[EventTable.finishDate] = event.finishDate?.toInstant()
                it[EventTable.webUrls] = webUrlsStr
                it[EventTable.message] = event.message
                it[EventTable.email] = event.email
                it[EventTable.phone] = event.phone
            }

            // Retrieve the actual event ID from the database (in case of conflict, use existing ID)
            val actualEvent = findByEventorIdAndEventorRef(event.eventorId, event.eventorRef)
            if (actualEvent != null) {
                event.id = actualEvent.id
            }

            // Save organisers (many-to-many)
            event.organisers.forEach { org ->
                org.id?.let { orgId ->
                    organisationRepository.save(org)
                    EventOrganiserTable.upsert {
                        it[EventOrganiserTable.eventId] = event.id!!
                        it[EventOrganiserTable.organisationId] = orgId
                    }
                }
            }

            // Save event classes
            event.classes.forEach { eventClass ->
                ClassTable.upsert {
                    it[ClassTable.id] = eventClass.id
                    it[ClassTable.eventId] = event.id!!
                    it[ClassTable.eventorRef] = eventClass.eventorRef
                    it[ClassTable.name] = eventClass.name
                    it[ClassTable.shortName] = eventClass.shortName
                    it[ClassTable.type] = eventClass.type.name
                    it[ClassTable.minAge] = eventClass.minAge
                    it[ClassTable.maxAge] = eventClass.maxAge
                    it[ClassTable.gender] = eventClass.gender.name
                    it[ClassTable.presentTime] = eventClass.presentTime
                    it[ClassTable.orderedResult] = eventClass.orderedResult
                    it[ClassTable.legs] = eventClass.legs
                    it[ClassTable.minAverageAge] = eventClass.minAverageAge
                    it[ClassTable.maxAverageAge] = eventClass.maxAverageAge
                }
            }

            // Save documents
            event.documents.forEach { document ->
                if (document.id == null) {
                    document.id = UUID.randomUUID()
                }
                DocumentTable.upsert {
                    it[DocumentTable.id] = document.id!!
                    it[DocumentTable.eventId] = event.id!!
                    it[DocumentTable.eventorRef] = document.eventorRef
                    it[DocumentTable.name] = document.name
                    it[DocumentTable.url] = document.url
                    it[DocumentTable.type] = document.type
                }
            }

            // Save races
            event.races.forEach { race ->
                if (race.id == null) {
                    race.id = UUID.randomUUID()
                }
                RaceTable.upsert {
                    it[RaceTable.id] = race.id!!
                    it[RaceTable.eventId] = event.id!!
                    it[RaceTable.eventorRef] = race.eventorRef
                    it[RaceTable.name] = race.name
                    it[RaceTable.lightCondition] = race.lightCondition.name
                    it[RaceTable.distance] = race.distance.name
                    it[RaceTable.date] = race.date?.toInstant()
                    it[RaceTable.latitude] = race.position?.latitude
                    it[RaceTable.longitude] = race.position?.longitude
                    it[RaceTable.startList] = race.startList
                    it[RaceTable.resultList] = race.resultList
                    it[RaceTable.livelox] = race.livelox
                }
            }
        }

        return event
    }
}
