package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.event.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.*

@Repository
class EventRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val organisationRepository: OrganisationRepository
) {
    
    private val rowMapper = RowMapper { rs: ResultSet, _: Int ->
        val id = rs.getObject("id", UUID::class.java)
        
        // Parse array columns
        val disciplinesArray = rs.getArray("disciplines")
        val disciplines = if (disciplinesArray != null) {
            (disciplinesArray.array as Array<*>).map { Discipline.valueOf(it.toString()) }.toTypedArray()
        } else {
            emptyArray()
        }
        
        val punchingTypesArray = rs.getArray("punching_unit_types")
        val punchingUnitTypes = if (punchingTypesArray != null) {
            (punchingTypesArray.array as Array<*>).map { PunchingUnitType.valueOf(it.toString()) }.toTypedArray()
        } else {
            emptyArray()
        }
        
        val entryBreaksArray = rs.getArray("entry_breaks")
        val entryBreaks = if (entryBreaksArray != null) {
            (entryBreaksArray.array as Array<*>).map {
                when (it) {
                    is java.sql.Timestamp -> it
                    is Long -> java.sql.Timestamp(it)
                    else -> java.sql.Timestamp(it.toString().toLong())
                }
            }.toTypedArray()
        } else {
            emptyArray()
        }
        
        val webUrlsArray = rs.getArray("web_urls")
        val webUrls = if (webUrlsArray != null) {
            (webUrlsArray.array as Array<*>).map { it.toString() }
        } else {
            emptyList()
        }
        
        Event(
            id = id,
            eventorId = rs.getString("eventor_id"),
            eventorRef = rs.getString("eventor_ref"),
            name = rs.getString("name"),
            type = EventFormEnum.valueOf(rs.getString("type")),
            classification = EventClassificationEnum.valueOf(rs.getString("classification")),
            status = EventStatusEnum.valueOf(rs.getString("status")),
            disciplines = disciplines,
            punchingUnitTypes = punchingUnitTypes,
            startDate = rs.getTimestamp("start_date"),
            finishDate = rs.getTimestamp("finish_date"),
            organisers = mutableListOf(), // Load separately
            classes = mutableListOf(), // Load separately
            documents = mutableListOf(), // Load separately
            entryBreaks = entryBreaks,
            races = mutableListOf(), // Load separately
            webUrls = webUrls,
            message = rs.getString("message"),
            email = rs.getString("email"),
            phone = rs.getString("phone")
        )
    }
    
    fun findByEventorIdAndEventorRef(eventorId: String, eventorRef: String): Event? {
        return try {
            jdbcTemplate.queryForObject(
                "SELECT * FROM event WHERE eventor_id = ? AND eventor_ref = ?",
                rowMapper,
                eventorId, eventorRef
            )
        } catch (_: Exception) {
            null
        }
    }
    
    fun save(event: Event): Event {
        // Convert arrays to SQL arrays
        val disciplinesArray = jdbcTemplate.dataSource?.connection?.use { conn ->
            conn.createArrayOf("discipline", event.disciplines.map { it.name }.toTypedArray())
        }
        
        val punchingTypesArray = jdbcTemplate.dataSource?.connection?.use { conn ->
            conn.createArrayOf("punching_unit_type", event.punchingUnitTypes.map { it.name }.toTypedArray())
        }
        
        val entryBreaksArray = jdbcTemplate.dataSource?.connection?.use { conn ->
            conn.createArrayOf("timestamp", event.entryBreaks)
        }
        
        val webUrlsArray = jdbcTemplate.dataSource?.connection?.use { conn ->
            conn.createArrayOf("varchar", event.webUrls.toTypedArray())
        }
        
        // Generate ID if not present
        if (event.id == null) {
            event.id = UUID.randomUUID()
        }

        // Use ON CONFLICT with the unique constraint on (eventor_id, eventor_ref)
        // Note: We don't update the ID to preserve foreign key relationships
        jdbcTemplate.update(
            """
            INSERT INTO event (id, eventor_id, eventor_ref, name, type, classification, status,
                disciplines, punching_unit_types, start_date, finish_date, entry_breaks, web_urls,
                message, email, phone)
            VALUES (?, ?, ?, ?, ?::event_type, ?::event_classification, ?::event_status, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (eventor_id, eventor_ref) DO UPDATE SET
                name = EXCLUDED.name,
                type = EXCLUDED.type,
                classification = EXCLUDED.classification,
                status = EXCLUDED.status,
                disciplines = EXCLUDED.disciplines,
                punching_unit_types = EXCLUDED.punching_unit_types,
                start_date = EXCLUDED.start_date,
                finish_date = EXCLUDED.finish_date,
                entry_breaks = EXCLUDED.entry_breaks,
                web_urls = EXCLUDED.web_urls,
                message = EXCLUDED.message,
                email = EXCLUDED.email,
                phone = EXCLUDED.phone
            """,
            event.id, event.eventorId, event.eventorRef, event.name, event.type.name,
            event.classification.name, event.status.name, disciplinesArray, punchingTypesArray,
            event.startDate, event.finishDate, entryBreaksArray, webUrlsArray,
            event.message, event.email, event.phone
        )

        // Retrieve the actual event ID from the database (in case of conflict, use existing ID)
        // This MUST happen before saving related entities to ensure correct foreign keys
        val actualEvent = findByEventorIdAndEventorRef(event.eventorId, event.eventorRef)
        if (actualEvent != null) {
            event.id = actualEvent.id
        }

        // Save organisers (many-to-many)
        event.organisers.forEach { org ->
            org.id?.let { orgId ->
                organisationRepository.save(org)
                jdbcTemplate.update(
                    "INSERT INTO event_organiser (event_id, organisation_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                    event.id, orgId
                )
            }
        }
        
        // Save event classes - upsert based on (event_id, eventor_ref)
        event.classes.forEach { eventClass ->
            jdbcTemplate.update(
                """
                INSERT INTO class (id, event_id, eventor_ref, name, short_name, type, 
                    min_age, max_age, gender, present_time, ordered_result, legs, 
                    min_average_age, max_average_age)
                VALUES (?, ?, ?, ?, ?, ?::class_type, ?, ?, ?::class_gender, ?, ?, ?, ?, ?)
                ON CONFLICT (event_id, eventor_ref) DO UPDATE SET
                    name = EXCLUDED.name,
                    short_name = EXCLUDED.short_name,
                    type = EXCLUDED.type,
                    min_age = EXCLUDED.min_age,
                    max_age = EXCLUDED.max_age,
                    gender = EXCLUDED.gender,
                    present_time = EXCLUDED.present_time,
                    ordered_result = EXCLUDED.ordered_result,
                    legs = EXCLUDED.legs,
                    min_average_age = EXCLUDED.min_average_age,
                    max_average_age = EXCLUDED.max_average_age
                """,
                eventClass.id, event.id, eventClass.eventorRef, eventClass.name,
                eventClass.shortName, eventClass.type.name, eventClass.minAge, eventClass.maxAge,
                eventClass.gender.name, eventClass.presentTime, eventClass.orderedResult,
                eventClass.legs, eventClass.minAverageAge, eventClass.maxAverageAge
            )
        }

        // Save documents - upsert based on (event_id, eventor_ref)
        event.documents.forEach { document ->
            if (document.id == null) {
                document.id = UUID.randomUUID()
            }
            jdbcTemplate.update(
                """
                INSERT INTO document (id, event_id, eventor_ref, name, url, type)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (event_id, eventor_ref) DO UPDATE SET
                    name = EXCLUDED.name,
                    url = EXCLUDED.url,
                    type = EXCLUDED.type
                """,
                document.id, event.id, document.eventorRef, document.name,
                document.url, document.type
            )
        }

        // Save races - upsert based on (event_id, eventor_ref)
        event.races.forEach { race ->
            if (race.id == null) {
                race.id = UUID.randomUUID()
            }
            jdbcTemplate.update(
                """
                INSERT INTO race (id, event_id, eventor_ref, name, light_condition, distance, 
                    date, latitude, longitude, start_list, result_list, livelox)
                VALUES (?, ?, ?, ?, ?::light_condition, ?::distance, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (event_id, eventor_ref) DO UPDATE SET
                    name = EXCLUDED.name,
                    light_condition = EXCLUDED.light_condition,
                    distance = EXCLUDED.distance,
                    date = EXCLUDED.date,
                    latitude = EXCLUDED.latitude,
                    longitude = EXCLUDED.longitude,
                    start_list = EXCLUDED.start_list,
                    result_list = EXCLUDED.result_list,
                    livelox = EXCLUDED.livelox
                """,
                race.id, event.id, race.eventorRef, race.name, race.lightCondition.name,
                race.distance.name, race.date, race.position?.latitude, race.position?.longitude,
                race.startList, race.resultList, race.livelox
            )
        }

        return event
    }
}
