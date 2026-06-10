package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.event.Discipline
import no.stunor.origo.eventorapi.model.event.Event
import no.stunor.origo.eventorapi.model.event.EventClassificationEnum
import no.stunor.origo.eventorapi.model.event.EventFormEnum
import no.stunor.origo.eventorapi.model.event.EventStatusEnum
import no.stunor.origo.eventorapi.model.event.PunchingUnitType
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import java.sql.Timestamp
import java.util.*
import javax.sql.DataSource

internal object EventTable : Table("event") {
    val id = javaUUID("id")
    override val primaryKey = PrimaryKey(id)
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
    val eventId = javaUUID("event_id").references(EventTable.id)
    val organisationId = javaUUID("organisation_id").references(OrganisationTable.id)
    override val primaryKey = PrimaryKey(eventId, organisationId)
}

internal object ClassTable : Table("class") {
    val id = javaUUID("id")
    override val primaryKey = PrimaryKey(id)
    val eventId = javaUUID("event_id").references(EventTable.id)
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
    val id = javaUUID("id")
    override val primaryKey = PrimaryKey(id)
    val eventId = javaUUID("event_id").references(EventTable.id)
    val eventorRef = text("eventor_ref")
    val name = text("name")
    val url = text("url")
    val type = text("type")
}

private object RaceTable : Table("race") {
    val id = javaUUID("id")
    override val primaryKey = PrimaryKey(id)
    val eventId = javaUUID("event_id").references(EventTable.id)
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
        val normalized = disciplinesStr.trim().trimStart('{').trimEnd('}')
        if (normalized.isEmpty()) return emptyArray()
        return normalized.split(",").mapNotNull { token ->
            runCatching { Discipline.valueOf(token.trim()) }.getOrNull()
        }.toTypedArray()
    }

    private fun parsePunchingTypes(typesStr: String?): Array<PunchingUnitType> {
        if (typesStr.isNullOrEmpty()) return emptyArray()
        val normalized = typesStr.trim().trimStart('{').trimEnd('}')
        if (normalized.isEmpty()) return emptyArray()
        return normalized.split(",").mapNotNull { token ->
            runCatching { PunchingUnitType.valueOf(token.trim()) }.getOrNull()
        }.toTypedArray()
    }

    private fun parseWebUrls(urlsStr: String?): List<String> {
        if (urlsStr.isNullOrEmpty()) return emptyList()
        val normalized = urlsStr.trim().trimStart('{').trimEnd('}')
        if (normalized.isEmpty()) return emptyList()
        return normalized.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun parseEntryBreaks(entryBreaksStr: String?): Array<Timestamp> {
        if (entryBreaksStr.isNullOrEmpty()) return emptyArray()
        val normalized = entryBreaksStr.trim().trimStart('{').trimEnd('}')
        if (normalized.isEmpty()) return emptyArray()
        return normalized.split(",")
            .mapNotNull { token ->
                runCatching { Timestamp.valueOf(token.trim()) }.getOrNull()
            }
            .toTypedArray()
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
            entryBreaks = parseEntryBreaks(row[EventTable.entryBreaks]),
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
            val disciplinesStr = if (event.disciplines.isEmpty()) "{}"
                else "{${event.disciplines.joinToString(",") { d -> d.name }}}"
            val punchingTypesStr = if (event.punchingUnitTypes.isEmpty()) "{}"
                else "{${event.punchingUnitTypes.joinToString(",") { p -> p.name }}}"
            val webUrlsStr = if (event.webUrls.isEmpty()) "{}"
                else "{${event.webUrls.joinToString(",")}}"
            val entryBreaksStr = if (event.entryBreaks.isEmpty()) "{}"
                else "{${event.entryBreaks.joinToString(",") { eb -> eb.toString() }}}"

            EventTable.upsert(EventTable.eventorId, EventTable.eventorRef,
                onUpdateExclude = listOf(EventTable.id)
            ) {
                it[EventTable.id] = (event.id ?: UUID.randomUUID())
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
                it[EventTable.entryBreaks] = entryBreaksStr
                it[EventTable.webUrls] = webUrlsStr
                it[EventTable.message] = event.message
                it[EventTable.email] = event.email
                it[EventTable.phone] = event.phone
            }

            event.id = findByEventorIdAndEventorRef(event.eventorId, event.eventorRef)!!.id

            event.organisers.forEach { org ->
                org.id?.let { orgId ->
                    organisationRepository.save(org)
                    EventOrganiserTable.upsert {
                        it[EventOrganiserTable.eventId] = event.id!!
                        it[EventOrganiserTable.organisationId] = orgId
                    }
                }
            }

            event.classes.forEach { eventClass ->
                ClassTable.upsert(ClassTable.eventId, ClassTable.eventorRef,
                    onUpdateExclude = listOf(ClassTable.id)
                ) {
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

            event.documents.forEach { document ->
                DocumentTable.upsert(DocumentTable.eventId, DocumentTable.eventorRef,
                    onUpdateExclude = listOf(DocumentTable.id)
                ) {
                    it[DocumentTable.id] = (document.id ?: UUID.randomUUID())
                    it[DocumentTable.eventId] = event.id!!
                    it[DocumentTable.eventorRef] = document.eventorRef
                    it[DocumentTable.name] = document.name
                    it[DocumentTable.url] = document.url
                    it[DocumentTable.type] = document.type
                }
            }

            event.races.forEach { race ->
                RaceTable.upsert(RaceTable.eventId, RaceTable.eventorRef,
                    onUpdateExclude = listOf(RaceTable.id)
                ) {
                    it[RaceTable.id] = (race.id ?: UUID.randomUUID())
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
