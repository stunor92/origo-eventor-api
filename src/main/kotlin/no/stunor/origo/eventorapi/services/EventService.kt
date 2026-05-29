package no.stunor.origo.eventorapi.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import no.stunor.origo.eventorapi.api.EventorService
import no.stunor.origo.eventorapi.data.EventClassRepository
import no.stunor.origo.eventorapi.data.EventRepository
import no.stunor.origo.eventorapi.data.EventorRepository
import no.stunor.origo.eventorapi.data.FeeRepository
import no.stunor.origo.eventorapi.exception.EventNotFoundException
import no.stunor.origo.eventorapi.exception.EventorNotFoundException
import no.stunor.origo.eventorapi.model.Eventor
import no.stunor.origo.eventorapi.model.event.Event
import no.stunor.origo.eventorapi.model.event.Fee
import no.stunor.origo.eventorapi.model.event.PunchingUnit
import no.stunor.origo.eventorapi.model.event.entry.Entry
import no.stunor.origo.eventorapi.model.event.entry.EntryStatus
import no.stunor.origo.eventorapi.model.event.entry.PersonEntry
import no.stunor.origo.eventorapi.model.event.entry.TeamEntry
import no.stunor.origo.eventorapi.services.converter.*
import org.slf4j.LoggerFactory

class EventService(
    private val eventorRepository: EventorRepository,
    private val eventRepository: EventRepository,
    private val eventConverter: EventConverter,
    private val feeRepository: FeeRepository,
    private val eventClassRepository: EventClassRepository,
    private val eventorService: EventorService,
    private val organisationConverter: OrganisationConverter,
    private val entryListConverter: EntryListConverter,
    private val startListConverter: StartListConverter,
    private val resultListConverter: ResultListConverter,
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    suspend fun getEvent(eventorId: String, eventorRef: String): Event {
        val eventor = withContext(Dispatchers.IO) {
            eventorRepository.findById(eventorId)
        } ?: throw EventorNotFoundException()

        val (eventorEvent, eventClassList, documentList) = coroutineScope {
            val eventDef = async { eventorService.getEvent(eventor.baseUrl, eventor.eventorApiKey, eventorRef) }
            val classDef = async { eventorService.getEventClasses(eventor, eventorRef) }
            val docDef   = async { eventorService.getEventDocuments(eventor.baseUrl, eventor.eventorApiKey, eventorRef) }
            Triple(eventDef.await() ?: throw EventNotFoundException(), classDef.await(), docDef.await())
        }

        val existingEvent = withContext(Dispatchers.IO) {
            eventRepository.findByEventorIdAndEventorRef(eventor.id, eventorEvent.eventId.content)
        }

        val organisers = withContext(Dispatchers.IO) {
            organisationConverter.convertOrganisations(
                organisations = eventorEvent.organiser.organisationIdOrOrganisation,
                eventorId     = eventorId
            )
        }
        val updatedOrNewEvent = eventConverter.convertEvent(
            existingEvent = existingEvent,
            eventorEvent  = eventorEvent,
            classes       = eventClassList,
            documents     = documentList,
            organisations = organisers,
            eventor       = eventor
        )

        val event = withContext(Dispatchers.IO) { eventRepository.save(updatedOrNewEvent) }
        val savedClasses = withContext(Dispatchers.IO) { eventClassRepository.findByEventId(event.id) }
        val savedClassesByRef = savedClasses.associateBy { it.eventorRef }

        val entryFees = eventorService.getEventEntryFees(eventor, eventorRef)
        val convertedFees: List<Fee> = FeeConverter.convertEntryFees(entryFees, event, eventClassList?.eventClass ?: listOf())

        convertedFees.forEach { fee ->
            fee.classes = fee.classes.mapNotNull { feeClass ->
                savedClassesByRef[feeClass.eventorRef]
            }.toMutableList()
        }

        val existingFees = withContext(Dispatchers.IO) { feeRepository.findAllByEventId(event.id) }
        val existingByRef = existingFees.associateBy { it.eventorRef }

        val mergedFees = convertedFees.map { fee ->
            val match = existingByRef[fee.eventorRef]
            if (match != null) {
                match.name                = fee.name
                match.currency            = fee.currency
                match.amount              = fee.amount
                match.externalFee         = fee.externalFee
                match.percentageSurcharge = fee.percentageSurcharge
                match.validFrom           = fee.validFrom
                match.validTo             = fee.validTo
                match.fromBirthYear       = fee.fromBirthYear
                match.toBirthYear         = fee.toBirthYear
                match.taxIncluded         = fee.taxIncluded
                match.classes.clear()
                match.classes.addAll(fee.classes)
                match
            } else {
                fee
            }
        }

        val incomingRefs = convertedFees.map { it.eventorRef }.toSet()
        val obsolete = existingFees.filter { it.eventorRef !in incomingRefs }
        withContext(Dispatchers.IO) {
            if (obsolete.isNotEmpty()) feeRepository.deleteAll(obsolete)
            feeRepository.saveAll(mergedFees)
        }
        return event
    }

    // ── Entry fetching ────────────────────────────────────────────────────────

    private suspend fun fetchResultEntries(eventor: Eventor, eventId: String): List<Entry> {
        val resultList = eventorService.getEventResultList(eventor.baseUrl, eventor.eventorApiKey, eventId)
        return resultList?.let { withContext(Dispatchers.IO) { resultListConverter.convertEventResultList(eventor, it) } } ?: emptyList()
    }

    private suspend fun fetchStartEntries(eventor: Eventor, eventId: String): List<Entry> {
        val startList = eventorService.getEventStartList(eventor.baseUrl, eventor.eventorApiKey, eventId)
        return startList?.let { withContext(Dispatchers.IO) { startListConverter.convertEventStartList(eventor, it) } } ?: emptyList()
    }

    private suspend fun fetchEntryEntries(eventor: Eventor, eventId: String): List<Entry> {
        val entryList = eventorService.getEventEntryList(eventor.baseUrl, eventor.eventorApiKey, eventId)
            ?: return emptyList()
        return if (!entryList.entry.isNullOrEmpty())
            withContext(Dispatchers.IO) { entryListConverter.convertEventEntryList(eventor, entryList) }
        else emptyList()
    }

    suspend fun getEntryList(eventorId: String, eventId: String): List<Entry> {
        val eventor = withContext(Dispatchers.IO) {
            eventorRepository.findById(eventorId)
        } ?: throw EventorNotFoundException()

        val (entryEntries, startEntries, resultEntries) = coroutineScope {
            val entryDef  = async {
                runCatching { fetchEntryEntries(eventor, eventId) }
                    .onFailure { log.warn("Failed to fetch entry entries for event {}: {}", eventId, it.message) }
                    .getOrDefault(emptyList())
            }
            val startDef  = async {
                runCatching { fetchStartEntries(eventor, eventId) }
                    .onFailure { log.warn("Failed to fetch start entries for event {}: {}", eventId, it.message) }
                    .getOrDefault(emptyList())
            }
            val resultDef = async {
                runCatching { fetchResultEntries(eventor, eventId) }
                    .onFailure { log.warn("Failed to fetch result entries for event {}: {}", eventId, it.message) }
                    .getOrDefault(emptyList())
            }
            Triple(entryDef.await(), startDef.await(), resultDef.await())
        }

        if (entryEntries.isEmpty() && startEntries.isEmpty() && resultEntries.isEmpty()) return emptyList()
        return mergeAllEntryLists(entryEntries, startEntries, resultEntries)
    }

    // ── Entry key generation ──────────────────────────────────────────────────

    private fun generatePrimaryEntryKey(entry: Entry): String? = when (entry) {
        is PersonEntry -> entry.personEventorRef?.takeIf { it.isNotBlank() }?.let { "PERSON:$it" }
        is TeamEntry   -> entry.name.takeIf { it.isNotBlank() }?.let { "TEAM:$it" }
        else           -> null
    }

    private fun generateCompositeEntryKey(entry: Entry): String? = when (entry) {
        is PersonEntry -> buildPersonCompositeKey(entry)
        is TeamEntry   -> buildTeamCompositeKey(entry)
        else           -> null
    }

    private fun buildPersonCompositeKey(entry: PersonEntry): String? {
        if (!entry.personEventorRef.isNullOrBlank()) return null
        val given = entry.name.given.trim().lowercase()
        val family = entry.name.family.trim().lowercase()
        if (given.isEmpty() && family.isEmpty()) return null
        val orgRef = entry.organisation?.eventorRef?.trim()?.lowercase() ?: ""
        return "P|$given|$family|$orgRef|${entry.classEventorRef}|${entry.raceEventorRef}"
    }

    private fun buildTeamCompositeKey(entry: TeamEntry): String? {
        if (entry.name.isNotBlank()) return null
        val orgs = entry.organisations.joinToString("+") { it.eventorRef.lowercase() }
        if (orgs.isEmpty()) return null
        return "T|$orgs|${entry.classEventorRef}|${entry.raceEventorRef}"
    }

    // ── Entry merging ─────────────────────────────────────────────────────────

    private enum class EntrySource { ENTRY_LIST, START_LIST, RESULT_LIST }

    private fun mergeEntryData(existing: Entry, incoming: Entry, existingSource: EntrySource, incomingSource: EntrySource) {
        val incomingHasPriority = incomingSource.ordinal > existingSource.ordinal
        if (incomingHasPriority && incoming.classEventorRef.isNotBlank()) {
            existing.classEventorRef = incoming.classEventorRef
        } else if (existing.classEventorRef.isBlank() && incoming.classEventorRef.isNotBlank()) {
            existing.classEventorRef = incoming.classEventorRef
        }
        if (incomingHasPriority) incoming.bib?.let { existing.bib = it }
        else if (existing.bib == null) incoming.bib?.let { existing.bib = it }
        if (incomingHasPriority) incoming.startTime?.let { existing.startTime = it }
        else if (existing.startTime == null) incoming.startTime?.let { existing.startTime = it }
        if (incomingHasPriority) incoming.finishTime?.let { existing.finishTime = it }
        else if (existing.finishTime == null) incoming.finishTime?.let { existing.finishTime = it }
        incoming.result?.let { existing.result = it }
        if (incoming.status.ordinal > existing.status.ordinal) existing.status = incoming.status
        when {
            existing is PersonEntry && incoming is PersonEntry -> mergePersonEntryData(existing, incoming, incomingHasPriority)
            existing is TeamEntry   && incoming is TeamEntry   -> mergeTeamEntryData(existing, incoming, incomingHasPriority)
        }
    }

    private fun mergePersonEntryData(existing: PersonEntry, incoming: PersonEntry, incomingHasPriority: Boolean) {
        mergePunchingUnits(existing.punchingUnits, incoming.punchingUnits, incomingHasPriority)
        replaceListWhenIncomingPresent(existing.splitTimes, incoming.splitTimes)
        mergePersonIdentityFields(existing, incoming, incomingHasPriority)
    }

    private fun mergePersonIdentityFields(existing: PersonEntry, incoming: PersonEntry, incomingHasPriority: Boolean) {
        if (incomingHasPriority) {
            incoming.competitorEventorRef?.let { existing.competitorEventorRef = it }
            incoming.nationality?.let { existing.nationality = it }
            incoming.birthYear?.let { existing.birthYear = it }
            return
        }
        if (existing.competitorEventorRef == null) incoming.competitorEventorRef?.let { existing.competitorEventorRef = it }
        if (existing.nationality == null) incoming.nationality?.let { existing.nationality = it }
        if (existing.birthYear == null) incoming.birthYear?.let { existing.birthYear = it }
    }

    private fun mergePunchingUnits(existingUnits: MutableList<PunchingUnit>, incomingUnits: List<PunchingUnit>, incomingHasPriority: Boolean) {
        if (incomingUnits.isEmpty()) return
        if (incomingHasPriority) {
            existingUnits.clear()
            existingUnits.addAll(incomingUnits)
            return
        }
        val existingKeys = existingUnits.map { it.id to it.type }.toSet()
        incomingUnits.filter { (it.id to it.type) !in existingKeys }.forEach { existingUnits.add(it) }
    }

    private fun <T> replaceListWhenIncomingPresent(existing: MutableList<T>, incoming: List<T>) {
        if (incoming.isEmpty()) return
        existing.clear()
        existing.addAll(incoming)
    }

    private fun mergeTeamEntryData(existing: TeamEntry, incoming: TeamEntry, incomingHasPriority: Boolean) {
        if (incoming.teamMembers.isEmpty()) return
        val membersByPersonId = existing.teamMembers
            .filter { !it.personEventorRef.isNullOrBlank() }
            .associateBy { it.personEventorRef!! }
        incoming.teamMembers.forEach { incomingMember ->
            val personId = incomingMember.personEventorRef ?: return@forEach
            val existingMember = membersByPersonId[personId] ?: return@forEach
            mergePunchingUnits(existingMember.punchingUnits, incomingMember.punchingUnits, incomingHasPriority)
        }
    }

    private fun mergeEntriesIntoMaps(entries: List<Entry>, entriesByKey: MutableMap<String, Entry>, keylessEntries: MutableMap<String, Entry>, entrySourceMap: MutableMap<String, EntrySource>, source: EntrySource) {
        entries.forEach { entry ->
            val primaryKey = generatePrimaryEntryKey(entry)
            if (primaryKey != null) mergeEntryByPrimaryKey(entry, primaryKey, entriesByKey, entrySourceMap, source)
            else mergeEntryByCompositeKey(entry, keylessEntries, entrySourceMap, source)
        }
    }

    private fun mergeEntryByPrimaryKey(entry: Entry, key: String, entriesByKey: MutableMap<String, Entry>, entrySourceMap: MutableMap<String, EntrySource>, source: EntrySource) {
        val existing = entriesByKey[key]
        if (existing != null) {
            mergeEntryData(existing, entry, entrySourceMap[key] ?: EntrySource.ENTRY_LIST, source)
            if (source.ordinal > (entrySourceMap[key]?.ordinal ?: 0)) entrySourceMap[key] = source
        } else {
            entriesByKey[key] = entry
            entrySourceMap[key] = source
        }
    }

    private fun mergeEntryByCompositeKey(entry: Entry, keylessEntries: MutableMap<String, Entry>, entrySourceMap: MutableMap<String, EntrySource>, source: EntrySource) {
        val compositeKey = generateCompositeEntryKey(entry) ?: return
        val existing = keylessEntries[compositeKey]
        if (existing != null) {
            mergeEntryData(existing, entry, entrySourceMap[compositeKey] ?: EntrySource.ENTRY_LIST, source)
            if (source.ordinal > (entrySourceMap[compositeKey]?.ordinal ?: 0)) entrySourceMap[compositeKey] = source
        } else {
            keylessEntries[compositeKey] = entry
            entrySourceMap[compositeKey] = source
        }
    }

    private fun mergeAllEntryLists(entryEntries: List<Entry>, startEntries: List<Entry>, resultEntries: List<Entry>): List<Entry> {
        val entriesByKey = LinkedHashMap<String, Entry>()
        val keylessEntries = LinkedHashMap<String, Entry>()
        val entrySourceMap = mutableMapOf<String, EntrySource>()
        if (entryEntries.isNotEmpty()) mergeEntriesIntoMaps(entryEntries, entriesByKey, keylessEntries, entrySourceMap, EntrySource.ENTRY_LIST)
        if (startEntries.isNotEmpty()) mergeEntriesIntoMaps(startEntries, entriesByKey, keylessEntries, entrySourceMap, EntrySource.START_LIST)
        if (resultEntries.isNotEmpty()) mergeResultEntriesAndMarkDeregistered(resultEntries, entriesByKey, keylessEntries, entrySourceMap)
        return ArrayList<Entry>(entriesByKey.size + keylessEntries.size).apply {
            addAll(entriesByKey.values)
            addAll(keylessEntries.values)
        }
    }

    private fun mergeResultEntriesAndMarkDeregistered(resultEntries: List<Entry>, entriesByKey: MutableMap<String, Entry>, keylessEntries: MutableMap<String, Entry>, entrySourceMap: MutableMap<String, EntrySource>) {
        val foundKeys = mutableSetOf<String>()
        val foundCompositeKeys = mutableSetOf<String>()
        resultEntries.forEach { resultEntry ->
            val primaryKey = generatePrimaryEntryKey(resultEntry)
            if (primaryKey != null) {
                foundKeys.add(primaryKey)
                val existing = entriesByKey[primaryKey]
                if (existing != null) {
                    mergeEntryData(existing, resultEntry, entrySourceMap[primaryKey] ?: EntrySource.ENTRY_LIST, EntrySource.RESULT_LIST)
                    entrySourceMap[primaryKey] = EntrySource.RESULT_LIST
                } else {
                    entriesByKey[primaryKey] = resultEntry
                    entrySourceMap[primaryKey] = EntrySource.RESULT_LIST
                }
            } else {
                val compositeKey = generateCompositeEntryKey(resultEntry) ?: return@forEach
                foundCompositeKeys.add(compositeKey)
                val existing = keylessEntries[compositeKey]
                if (existing != null) {
                    mergeEntryData(existing, resultEntry, entrySourceMap[compositeKey] ?: EntrySource.ENTRY_LIST, EntrySource.RESULT_LIST)
                    entrySourceMap[compositeKey] = EntrySource.RESULT_LIST
                } else {
                    keylessEntries[compositeKey] = resultEntry
                    entrySourceMap[compositeKey] = EntrySource.RESULT_LIST
                }
            }
        }
        entriesByKey.values.forEach { entry ->
            val key = generatePrimaryEntryKey(entry)
            if (key != null && key !in foundKeys) entry.status = EntryStatus.Deregistered
        }
        keylessEntries.values.forEach { entry ->
            val key = generateCompositeEntryKey(entry)
            if (key != null && key !in foundCompositeKeys) entry.status = EntryStatus.Deregistered
        }
    }
}
