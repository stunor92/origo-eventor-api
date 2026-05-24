package no.stunor.origo.eventorapi.services

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
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CompletableFuture
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

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
    private val transactionTemplate: TransactionTemplate
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    private val executor = ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors() * 4,
        500,
        60L, TimeUnit.SECONDS,
        SynchronousQueue()
    )
    private val apiTimeoutSeconds = 30L

    fun shutdownExecutor() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate within 60 seconds, forcing shutdown")
                executor.shutdownNow()
            }
        } catch (_: InterruptedException) {
            log.warn("Shutdown interrupted, forcing shutdown")
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    fun getEvent(eventorId: String, eventorRef: String): Event {
        val eventor = eventorRepository.findById(eventorId) ?: throw EventorNotFoundException()

        val eventFuture = CompletableFuture.supplyAsync({
            eventorService.getEvent(eventor.baseUrl, eventor.eventorApiKey, eventorRef)
        }, executor)

        val classListFuture = CompletableFuture.supplyAsync({
            eventorService.getEventClasses(eventor, eventorRef)
        }, executor)

        val documentListFuture = CompletableFuture.supplyAsync({
            eventorService.getEventDocuments(eventor.baseUrl, eventor.eventorApiKey, eventorRef)
        }, executor)

        CompletableFuture.allOf(eventFuture, classListFuture, documentListFuture)
            .get(apiTimeoutSeconds, TimeUnit.SECONDS)

        val eventorEvent = eventFuture.join() ?: throw EventNotFoundException()
        val eventClassList = classListFuture.join()
        val documentList = documentListFuture.join()

        val existingEvent = eventRepository.findByEventorIdAndEventorRef(eventor.id, eventorEvent.eventId.content)

        val organisers = organisationConverter.convertOrganisations(
            organisations = eventorEvent.organiser.organisationIdOrOrganisation,
            eventorId = eventorId
        )
        val updatedOrNewEvent = eventConverter.convertEvent(
            existingEvent = existingEvent,
            eventorEvent = eventorEvent,
            classes = eventClassList,
            documents = documentList,
            organisations = organisers,
            eventor = eventor
        )

        return transactionTemplate.execute {
            val event = eventRepository.save(updatedOrNewEvent)
            val savedClasses = eventClassRepository.findByEventId(event.id)
            val savedClassesByRef = savedClasses.associateBy { it.eventorRef }

            val entryFees = eventorService.getEventEntryFees(eventor, eventorRef)
            val convertedFees: List<Fee> = FeeConverter.convertEntryFees(entryFees, event, eventClassList?.eventClass ?: listOf())

            convertedFees.forEach { fee ->
                fee.classes = fee.classes.mapNotNull { feeClass ->
                    savedClassesByRef[feeClass.eventorRef]
                }.toMutableList()
            }

            val existingFees = feeRepository.findAllByEventId(event.id)
            val existingByRef = existingFees.associateBy { it.eventorRef }

            val mergedFees = convertedFees.map { fee ->
                val match = existingByRef[fee.eventorRef]
                if (match != null) {
                    match.name = fee.name
                    match.currency = fee.currency
                    match.amount = fee.amount
                    match.externalFee = fee.externalFee
                    match.percentageSurcharge = fee.percentageSurcharge
                    match.validFrom = fee.validFrom
                    match.validTo = fee.validTo
                    match.fromBirthYear = fee.fromBirthYear
                    match.toBirthYear = fee.toBirthYear
                    match.taxIncluded = fee.taxIncluded
                    match.classes.clear()
                    match.classes.addAll(fee.classes)
                    match
                } else {
                    fee
                }
            }

            val incomingRefs = convertedFees.map { it.eventorRef }.toSet()
            val obsolete = existingFees.filter { it.eventorRef !in incomingRefs }
            if (obsolete.isNotEmpty()) feeRepository.deleteAll(obsolete)
            feeRepository.saveAll(mergedFees)
            event
        }!!
    }

    // ========================================
    // Entry Fetching Methods
    // ========================================

    private fun fetchResultEntries(eventor: Eventor, eventId: String): List<Entry> {
        val resultList = eventorService.getEventResultList(eventor.baseUrl, eventor.eventorApiKey, eventId)
        return resultList?.let { resultListConverter.convertEventResultList(eventor, it) } ?: emptyList()
    }

    private fun fetchStartEntries(eventor: Eventor, eventId: String): List<Entry> {
        val startList = eventorService.getEventStartList(eventor.baseUrl, eventor.eventorApiKey, eventId)
        return startList?.let { startListConverter.convertEventStartList(eventor, it) } ?: emptyList()
    }

    private fun fetchEntryEntries(eventor: Eventor, eventId: String): List<Entry> {
        val entryList = eventorService.getEventEntryList(eventor.baseUrl, eventor.eventorApiKey, eventId)
            ?: return emptyList()
        return if (!entryList.entry.isNullOrEmpty()) entryListConverter.convertEventEntryList(eventor, entryList) else emptyList()
    }

    // ========================================
    // Entry Key Generation Methods
    // ========================================

    private fun generatePrimaryEntryKey(entry: Entry): String? = when (entry) {
        is PersonEntry -> entry.personEventorRef?.takeIf { it.isNotBlank() }?.let { "PERSON:$it" }
        is TeamEntry -> entry.name.takeIf { it.isNotBlank() }?.let { "TEAM:$it" }
        else -> null
    }

    private fun generateCompositeEntryKey(entry: Entry): String? {
        return when (entry) {
            is PersonEntry -> buildPersonCompositeKey(entry)
            is TeamEntry -> buildTeamCompositeKey(entry)
            else -> null
        }
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

    // ========================================
    // Entry Data Merging Methods
    // ========================================

    private enum class EntrySource {
        ENTRY_LIST,
        START_LIST,
        RESULT_LIST
    }

    private fun mergeEntryData(
        existing: Entry,
        incoming: Entry,
        existingSource: EntrySource,
        incomingSource: EntrySource
    ) {
        val incomingHasPriority = incomingSource.ordinal > existingSource.ordinal

        if (incomingHasPriority && incoming.classEventorRef.isNotBlank()) {
            existing.classEventorRef = incoming.classEventorRef
        } else if (existing.classEventorRef.isBlank() && incoming.classEventorRef.isNotBlank()) {
            existing.classEventorRef = incoming.classEventorRef
        }

        if (incomingHasPriority) { incoming.bib?.let { existing.bib = it } }
        else if (existing.bib == null) { incoming.bib?.let { existing.bib = it } }

        if (incomingHasPriority) { incoming.startTime?.let { existing.startTime = it } }
        else if (existing.startTime == null) { incoming.startTime?.let { existing.startTime = it } }

        if (incomingHasPriority) { incoming.finishTime?.let { existing.finishTime = it } }
        else if (existing.finishTime == null) { incoming.finishTime?.let { existing.finishTime = it } }

        incoming.result?.let { existing.result = it }

        if (incoming.status.ordinal > existing.status.ordinal) {
            existing.status = incoming.status
        }

        when {
            existing is PersonEntry && incoming is PersonEntry ->
                mergePersonEntryData(existing, incoming, incomingHasPriority)
            existing is TeamEntry && incoming is TeamEntry ->
                mergeTeamEntryData(existing, incoming, incomingHasPriority)
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

    private fun mergePunchingUnits(
        existingUnits: MutableList<PunchingUnit>,
        incomingUnits: List<PunchingUnit>,
        incomingHasPriority: Boolean
    ) {
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

    // ========================================
    // Entry Merging Logic
    // ========================================

    private fun mergeEntriesIntoMaps(
        entries: List<Entry>,
        entriesByKey: MutableMap<String, Entry>,
        keylessEntries: MutableMap<String, Entry>,
        entrySourceMap: MutableMap<String, EntrySource>,
        source: EntrySource
    ) {
        entries.forEach { entry ->
            val primaryKey = generatePrimaryEntryKey(entry)
            if (primaryKey != null) {
                mergeEntryByPrimaryKey(entry, primaryKey, entriesByKey, entrySourceMap, source)
            } else {
                mergeEntryByCompositeKey(entry, keylessEntries, entrySourceMap, source)
            }
        }
    }

    private fun mergeEntryByPrimaryKey(
        entry: Entry,
        key: String,
        entriesByKey: MutableMap<String, Entry>,
        entrySourceMap: MutableMap<String, EntrySource>,
        source: EntrySource
    ) {
        val existing = entriesByKey[key]
        if (existing != null) {
            val existingSource = entrySourceMap[key] ?: EntrySource.ENTRY_LIST
            mergeEntryData(existing, entry, existingSource, source)
            if (source.ordinal > existingSource.ordinal) entrySourceMap[key] = source
        } else {
            entriesByKey[key] = entry
            entrySourceMap[key] = source
        }
    }

    private fun mergeEntryByCompositeKey(
        entry: Entry,
        keylessEntries: MutableMap<String, Entry>,
        entrySourceMap: MutableMap<String, EntrySource>,
        source: EntrySource
    ) {
        val compositeKey = generateCompositeEntryKey(entry) ?: return
        val existing = keylessEntries[compositeKey]
        if (existing != null) {
            val existingSource = entrySourceMap[compositeKey] ?: EntrySource.ENTRY_LIST
            mergeEntryData(existing, entry, existingSource, source)
            if (source.ordinal > existingSource.ordinal) entrySourceMap[compositeKey] = source
        } else {
            keylessEntries[compositeKey] = entry
            entrySourceMap[compositeKey] = source
        }
    }

    private fun mergeAllEntryLists(
        entryEntries: List<Entry>,
        startEntries: List<Entry>,
        resultEntries: List<Entry>
    ): List<Entry> {
        val entriesByKey = LinkedHashMap<String, Entry>()
        val keylessEntries = LinkedHashMap<String, Entry>()
        val entrySourceMap = mutableMapOf<String, EntrySource>()

        if (entryEntries.isNotEmpty()) mergeEntriesIntoMaps(entryEntries, entriesByKey, keylessEntries, entrySourceMap, EntrySource.ENTRY_LIST)
        if (startEntries.isNotEmpty()) mergeEntriesIntoMaps(startEntries, entriesByKey, keylessEntries, entrySourceMap, EntrySource.START_LIST)
        if (resultEntries.isNotEmpty()) mergeResultEntriesAndMarkDeregistered(resultEntries, entriesByKey, keylessEntries, entrySourceMap)

        return buildFinalEntryList(entriesByKey, keylessEntries)
    }

    private fun mergeResultEntriesAndMarkDeregistered(
        resultEntries: List<Entry>,
        entriesByKey: MutableMap<String, Entry>,
        keylessEntries: MutableMap<String, Entry>,
        entrySourceMap: MutableMap<String, EntrySource>
    ) {
        val foundKeys = mutableSetOf<String>()
        val foundCompositeKeys = mutableSetOf<String>()
        resultEntries.forEach { resultEntry ->
            mergeResultEntry(resultEntry, entriesByKey, keylessEntries, entrySourceMap, foundKeys, foundCompositeKeys)
        }
        markMissingEntriesAsDeregistered(entriesByKey, keylessEntries, foundKeys, foundCompositeKeys)
    }

    private fun mergeResultEntry(
        resultEntry: Entry,
        entriesByKey: MutableMap<String, Entry>,
        keylessEntries: MutableMap<String, Entry>,
        entrySourceMap: MutableMap<String, EntrySource>,
        foundKeys: MutableSet<String>,
        foundCompositeKeys: MutableSet<String>
    ) {
        val primaryKey = generatePrimaryEntryKey(resultEntry)
        if (primaryKey != null) {
            foundKeys.add(primaryKey)
            mergeOrAddResultEntryByPrimaryKey(resultEntry, primaryKey, entriesByKey, entrySourceMap)
        } else {
            mergeOrAddResultEntryByCompositeKey(resultEntry, keylessEntries, entrySourceMap, foundCompositeKeys)
        }
    }

    private fun mergeOrAddResultEntryByPrimaryKey(
        resultEntry: Entry,
        primaryKey: String,
        entriesByKey: MutableMap<String, Entry>,
        entrySourceMap: MutableMap<String, EntrySource>
    ) {
        val existing = entriesByKey[primaryKey]
        if (existing != null) {
            mergeEntryData(existing, resultEntry, entrySourceMap[primaryKey] ?: EntrySource.ENTRY_LIST, EntrySource.RESULT_LIST)
            entrySourceMap[primaryKey] = EntrySource.RESULT_LIST
        } else {
            entriesByKey[primaryKey] = resultEntry
            entrySourceMap[primaryKey] = EntrySource.RESULT_LIST
        }
    }

    private fun mergeOrAddResultEntryByCompositeKey(
        resultEntry: Entry,
        keylessEntries: MutableMap<String, Entry>,
        entrySourceMap: MutableMap<String, EntrySource>,
        foundCompositeKeys: MutableSet<String>
    ) {
        val compositeKey = generateCompositeEntryKey(resultEntry) ?: return
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

    private fun markMissingEntriesAsDeregistered(
        entriesByKey: Map<String, Entry>,
        keylessEntries: Map<String, Entry>,
        foundKeys: Set<String>,
        foundCompositeKeys: Set<String>
    ) {
        entriesByKey.values.forEach { entry ->
            val key = generatePrimaryEntryKey(entry)
            if (key != null && key !in foundKeys) entry.status = EntryStatus.Deregistered
        }
        keylessEntries.values.forEach { entry ->
            val key = generateCompositeEntryKey(entry)
            if (key != null && key !in foundCompositeKeys) entry.status = EntryStatus.Deregistered
        }
    }

    private fun buildFinalEntryList(entriesByKey: Map<String, Entry>, keylessEntries: Map<String, Entry>): List<Entry> {
        return ArrayList<Entry>(entriesByKey.size + keylessEntries.size).apply {
            addAll(entriesByKey.values)
            addAll(keylessEntries.values)
        }
    }

    // ========================================
    // Main Entry List Method
    // ========================================

    fun getEntryList(eventorId: String, eventId: String): List<Entry> {
        val eventor = eventorRepository.findById(eventorId) ?: throw EventorNotFoundException()

        val entryFuture = CompletableFuture.supplyAsync({ fetchEntryEntries(eventor, eventId) }, executor)
            .exceptionally { ex -> log.warn("Failed to fetch entry entries for event {}: {}", eventId, ex.message); emptyList() }

        val startFuture = CompletableFuture.supplyAsync({ fetchStartEntries(eventor, eventId) }, executor)
            .exceptionally { ex -> log.warn("Failed to fetch start entries for event {}: {}", eventId, ex.message); emptyList() }

        val resultFuture = CompletableFuture.supplyAsync({ fetchResultEntries(eventor, eventId) }, executor)
            .exceptionally { ex -> log.warn("Failed to fetch result entries for event {}: {}", eventId, ex.message); emptyList() }

        CompletableFuture.allOf(entryFuture, startFuture, resultFuture).get(apiTimeoutSeconds, TimeUnit.SECONDS)

        val entryEntries = entryFuture.join()
        val startEntries = startFuture.join()
        val resultEntries = resultFuture.join()

        if (entryEntries.isNotEmpty() || startEntries.isNotEmpty() || resultEntries.isNotEmpty()) {
            return mergeAllEntryLists(entryEntries, startEntries, resultEntries)
        }

        return emptyList()
    }
}
