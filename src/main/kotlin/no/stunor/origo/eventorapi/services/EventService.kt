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
import no.stunor.origo.eventorapi.model.event.entry.Entry
import no.stunor.origo.eventorapi.model.event.entry.EntryStatus
import no.stunor.origo.eventorapi.model.event.entry.PersonEntry
import no.stunor.origo.eventorapi.model.event.entry.TeamEntry
import no.stunor.origo.eventorapi.services.converter.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import jakarta.annotation.PreDestroy

@Service
class EventService {

    private val log = LoggerFactory.getLogger(this.javaClass)

    // I/O-bound thread pool for parallel API calls
    private val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4)
    private val apiTimeoutSeconds = 30L

    @PreDestroy
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

    @Autowired
    private lateinit var eventorRepository: EventorRepository
    @Autowired
    private lateinit var eventRepository: EventRepository
    @Autowired
    private lateinit var eventConverter: EventConverter
    @Autowired
    private lateinit var feeRepository: FeeRepository
    @Autowired
    private lateinit var eventClassRepository: EventClassRepository
    @Autowired
    private lateinit var eventorService: EventorService
    @Autowired
    private lateinit var organisationConverter: OrganisationConverter
    @Autowired
    private lateinit var entryListConverter: EntryListConverter
    @Autowired
    private lateinit var startListConverter: StartListConverter
    @Autowired
    private lateinit var resultListConverter: ResultListConverter


    @Transactional
    fun getEvent(eventorId: String, eventorRef: String): Event {
        val eventor = eventorRepository.findById(eventorId) ?: throw EventorNotFoundException()

        // Parallelize independent API calls for better performance
        val eventFuture = CompletableFuture.supplyAsync({
            eventorService.getEvent(eventor.baseUrl, eventor.eventorApiKey, eventorRef)
        }, executor)

        val classListFuture = CompletableFuture.supplyAsync({
            eventorService.getEventClasses(eventor, eventorRef)
        }, executor)

        val documentListFuture = CompletableFuture.supplyAsync({
            eventorService.getEventDocuments(eventor.baseUrl, eventor.eventorApiKey, eventorRef)
        }, executor)

        // Wait for all to complete
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
        val updatedOrNewEvent =  eventConverter.convertEvent(
            existingEvent = existingEvent,
            eventorEvent = eventorEvent,
            classes = eventClassList,
            documents = documentList,
            organisations = organisers,
            eventor = eventor
        )

        val event = eventRepository.save(updatedOrNewEvent)

        // Query the saved event from database to get classes with actual database IDs
        val savedClasses = eventClassRepository.findByEventId(event.id)

        // Create a map of saved classes by eventor_ref for quick lookup
        val savedClassesByRef = savedClasses.associateBy { it.eventorRef }

        // Merge fees
        val entryFees = eventorService.getEventEntryFees(eventor, eventorRef)
        val convertedFees: List<Fee> = FeeConverter.convertEntryFees(entryFees, event, eventClassList?.eventClass ?: listOf())

        // Update fee.classes to reference the actual saved classes with correct IDs from database
        convertedFees.forEach { fee ->
            fee.classes = fee.classes.mapNotNull { feeClass ->
                savedClassesByRef[feeClass.eventorRef]
            }.toMutableList()
        }

        val existingFees = feeRepository.findAllByEventId(event.id)
        val existingByRef = existingFees.associateBy { it.eventorRef }

        // Use map operation instead of loop for better performance
        val mergedFees = convertedFees.map { fee ->
            val match = existingByRef[fee.eventorRef]
            if (match != null) {
                // Update all fee properties
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
                // Replace classes entirely (don't append, replace with saved classes)
                match.classes.clear()
                match.classes.addAll(fee.classes)
                match
            } else {
                fee
            }
        }

        // Remove obsolete fees
        val incomingRefs = convertedFees.map { it.eventorRef }.toSet()
        val obsolete = existingFees.filter { it.eventorRef !in incomingRefs }
        if (obsolete.isNotEmpty()) feeRepository.deleteAll(obsolete)
        feeRepository.saveAll(mergedFees)
        return event
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

    /**
     * Generates a unique key for entries with a personId or team name.
     * Returns null for entries without sufficient identification.
     */
    private fun generatePrimaryEntryKey(entry: Entry): String? = when (entry) {
        is PersonEntry -> entry.personEventorRef?.takeIf { it.isNotBlank() }?.let { "PERSON:$it" }
        is TeamEntry -> entry.name.takeIf { it.isNotBlank() }?.let { "TEAM:$it" }
        else -> null
    }

    /**
     * Generates a composite key for entries without a primary identifier.
     * Uses name, organisation, class, and race to create a unique key.
     */
    private fun generateCompositeEntryKey(entry: Entry): String? {
        return when (entry) {
            is PersonEntry -> buildPersonCompositeKey(entry)
            is TeamEntry -> buildTeamCompositeKey(entry)
            else -> null
        }
    }

    private fun buildPersonCompositeKey(entry: PersonEntry): String? {
        if (!entry.personEventorRef.isNullOrBlank()) return null // Has primary key

        val given = entry.name.given.trim().lowercase()
        val family = entry.name.family.trim().lowercase()

        if (given.isEmpty() && family.isEmpty()) return null // Insufficient data

        val orgRef = entry.organisation?.eventorRef?.trim()?.lowercase() ?: ""
        return "P|$given|$family|$orgRef|${entry.classEventorRef}|${entry.raceEventorRef}"
    }

    private fun buildTeamCompositeKey(entry: TeamEntry): String? {
        if (entry.name.isNotBlank()) return null // Has primary key

        val orgs = entry.organisations.joinToString("+") { it.eventorRef.lowercase() }
        if (orgs.isEmpty()) return null // Insufficient data

        return "T|$orgs|${entry.classEventorRef}|${entry.raceEventorRef}"
    }

    // ========================================
    // Entry Data Merging Methods
    // ========================================

    /**
     * Defines the source priority for entry data.
     * Higher ordinal = higher priority when merging data.
     */
    private enum class EntrySource {
        ENTRY_LIST,      // Påmeldingslisten (lavest prioritet)
        START_LIST,      // Startlisten
        RESULT_LIST      // Resultatlisten (høyest prioritet)
    }

    /**
     * Merges data from incoming entry into existing entry.
     * Handles both PersonEntry and TeamEntry types.
     *
     * Priority rules (in order of importance):
     * 1. Result list (what actually happened)
     * 2. Start list (what was planned at start)
     * 3. Entry list (original registration)
     *
     * Fields affected by priority:
     * - Class (classEventorRef): Result > Start > Entry
     * - Bib number: Result > Start > Entry
     * - Punching units: Result > Start > Entry
     * - Start/Finish times: Result > Start > Entry
     * - Result data: Always from result list
     * - Status: Use most complete status (highest ordinal)
     */
    private fun mergeEntryData(
        existing: Entry,
        incoming: Entry,
        existingSource: EntrySource,
        incomingSource: EntrySource
    ) {
        val incomingHasPriority = incomingSource.ordinal > existingSource.ordinal

        // Merge fields based on priority
        // Class: Incoming overwrites if it has higher priority
        if (incomingHasPriority && incoming.classEventorRef.isNotBlank()) {
            existing.classEventorRef = incoming.classEventorRef
        } else if (existing.classEventorRef.isBlank() && incoming.classEventorRef.isNotBlank()) {
            existing.classEventorRef = incoming.classEventorRef
        }

        // Bib: Incoming overwrites if it has higher priority
        if (incomingHasPriority) {
            incoming.bib?.let { existing.bib = it }
        } else if (existing.bib == null) {
            incoming.bib?.let { existing.bib = it }
        }

        // Start time: Incoming overwrites if it has higher priority or existing is null
        if (incomingHasPriority) {
            incoming.startTime?.let { existing.startTime = it }
        } else if (existing.startTime == null) {
            incoming.startTime?.let { existing.startTime = it }
        }

        // Finish time: Incoming overwrites if it has higher priority or existing is null
        if (incomingHasPriority) {
            incoming.finishTime?.let { existing.finishTime = it }
        } else if (existing.finishTime == null) {
            incoming.finishTime?.let { existing.finishTime = it }
        }

        // Result: Always from result list (only result list has this data)
        incoming.result?.let { existing.result = it }

        // Status: Use most complete status (highest ordinal)
        if (incoming.status.ordinal > existing.status.ordinal) {
            existing.status = incoming.status
        }

        // Merge type-specific fields with priority awareness
        when {
            existing is PersonEntry && incoming is PersonEntry ->
                mergePersonEntryData(existing, incoming, incomingHasPriority)
            existing is TeamEntry && incoming is TeamEntry ->
                mergeTeamEntryData(existing, incoming, incomingHasPriority)
        }
    }

    /**
     * Merges PersonEntry-specific data including punching units and split times.
     * Punching units are replaced (not merged) if incoming has higher priority.
     */
    private fun mergePersonEntryData(existing: PersonEntry, incoming: PersonEntry, incomingHasPriority: Boolean) {
        // Punching units: Replace if incoming has priority, otherwise merge
        if (incoming.punchingUnits.isNotEmpty()) {
            if (incomingHasPriority) {
                // Higher priority source: replace entirely
                existing.punchingUnits.clear()
                existing.punchingUnits.addAll(incoming.punchingUnits)
            } else {
                // Lower priority source: only add missing units
                val existingKeys = existing.punchingUnits.map { it.id to it.type }.toSet()
                incoming.punchingUnits
                    .filter { (it.id to it.type) !in existingKeys }
                    .forEach { existing.punchingUnits.add(it) }
            }
        }

        // Split times: Always prefer result list data (only result list has this)
        if (incoming.splitTimes.isNotEmpty()) {
            existing.splitTimes.clear()
            existing.splitTimes.addAll(incoming.splitTimes)
        }

        // Update other fields based on priority
        if (incomingHasPriority) {
            incoming.competitorEventorRef?.let { existing.competitorEventorRef = it }
            incoming.nationality?.let { existing.nationality = it }
            if (incoming.birthYear != null) existing.birthYear = incoming.birthYear
        } else {
            // Only fill in if missing
            if (existing.competitorEventorRef == null) {
                incoming.competitorEventorRef?.let { existing.competitorEventorRef = it }
            }
            if (existing.nationality == null) {
                incoming.nationality?.let { existing.nationality = it }
            }
            if (existing.birthYear == null && incoming.birthYear != null) {
                existing.birthYear = incoming.birthYear
            }
        }
    }

    /**
     * Merges TeamEntry-specific data including punching units for team members.
     * Punching units are replaced per member if incoming has higher priority.
     */
    private fun mergeTeamEntryData(existing: TeamEntry, incoming: TeamEntry, incomingHasPriority: Boolean) {
        if (incoming.teamMembers.isEmpty()) return

        val membersByPersonId = existing.teamMembers
            .filter { !it.personEventorRef.isNullOrBlank() }
            .associateBy { it.personEventorRef!! }

        incoming.teamMembers.forEach { incomingMember ->
            val personId = incomingMember.personEventorRef ?: return@forEach
            val existingMember = membersByPersonId[personId] ?: return@forEach

            if (incomingMember.punchingUnits.isEmpty()) return@forEach

            if (incomingHasPriority) {
                // Higher priority source: replace entirely
                existingMember.punchingUnits.clear()
                existingMember.punchingUnits.addAll(incomingMember.punchingUnits)
            } else {
                // Lower priority source: only add missing units
                val existingKeys = existingMember.punchingUnits.map { it.id to it.type }.toSet()
                incomingMember.punchingUnits
                    .filter { (it.id to it.type) !in existingKeys }
                    .forEach { existingMember.punchingUnits.add(it) }
            }
        }
    }

    // ========================================
    // Entry Merging Logic
    // ========================================

    /**
     * Merges a list of entries into the provided maps.
     * Entries with primary keys go into entriesByKey, others into keylessEntries.
     * Also tracks the source of each entry for priority handling.
     */
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
            // Update source to the higher priority
            if (source.ordinal > existingSource.ordinal) {
                entrySourceMap[key] = source
            }
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
            // Update source to the higher priority
            if (source.ordinal > existingSource.ordinal) {
                entrySourceMap[compositeKey] = source
            }
        } else {
            keylessEntries[compositeKey] = entry
            entrySourceMap[compositeKey] = source
        }
    }

    /**
     * Combines entries from multiple sources with intelligent merging strategy.
     *
     * Priority strategy (in order):
     * 1. Entry list (påmeldingslisten) - base registration, lowest priority
     * 2. Start list (startlisten) - planned start with possible class changes, medium priority
     * 3. Result list (resultatlisten) - actual race results with final class/bib, highest priority
     *
     * When a participant changes class or bib after registration:
     * - Result list data always wins (what actually happened)
     * - Start list overwrites entry list (planned start trumps registration)
     * - Entry list provides base data (original registration)
     *
     * @param entryEntries Base entries from entry list (original registration)
     * @param startEntries Entries from start list (planned start, may include class changes)
     * @param resultEntries Result entries with race results (final actual data)
     * @return Deduplicated and merged list of entries with correct priority
     */
    private fun mergeAllEntryLists(
        entryEntries: List<Entry>,
        startEntries: List<Entry>,
        resultEntries: List<Entry>
    ): List<Entry> {
        val entriesByKey = LinkedHashMap<String, Entry>()
        val keylessEntries = LinkedHashMap<String, Entry>()
        val entrySourceMap = mutableMapOf<String, EntrySource>()

        // Step 1: Start with entry list as base (lowest priority)
        if (entryEntries.isNotEmpty()) {
            mergeEntriesIntoMaps(entryEntries, entriesByKey, keylessEntries, entrySourceMap, EntrySource.ENTRY_LIST)
        }

        // Step 2: Merge start list (medium priority - overwrites entry list data)
        if (startEntries.isNotEmpty()) {
            mergeEntriesIntoMaps(startEntries, entriesByKey, keylessEntries, entrySourceMap, EntrySource.START_LIST)
        }

        // Step 3: Merge result list (highest priority - overwrites all other data)
        if (resultEntries.isNotEmpty()) {
            mergeResultEntriesAndMarkDeregistered(resultEntries, entriesByKey, keylessEntries, entrySourceMap)
        }

        return buildFinalEntryList(entriesByKey, keylessEntries)
    }

    /**
     * Merges result entries into existing entry maps and marks entries not in results as Deregistered.
     *
     * @param resultEntries Entries from result list
     * @param entriesByKey Map of entries with primary keys
     * @param keylessEntries Map of entries without primary keys (composite key based)
     * @param entrySourceMap Map tracking the source of each entry for priority handling
     */
    private fun mergeResultEntriesAndMarkDeregistered(
        resultEntries: List<Entry>,
        entriesByKey: MutableMap<String, Entry>,
        keylessEntries: MutableMap<String, Entry>,
        entrySourceMap: MutableMap<String, EntrySource>
    ) {
        val foundKeys = mutableSetOf<String>()
        val foundCompositeKeys = mutableSetOf<String>()

        // Merge result entries into existing entries
        resultEntries.forEach { resultEntry ->
            mergeResultEntry(resultEntry, entriesByKey, keylessEntries, entrySourceMap, foundKeys, foundCompositeKeys)
        }

        // Mark entries not found in result list as Deregistered
        markMissingEntriesAsDeregistered(entriesByKey, keylessEntries, foundKeys, foundCompositeKeys)
    }

    /**
     * Merges a single result entry into the appropriate map and tracks it as found.
     */
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

    /**
     * Merges or adds a result entry using its primary key.
     */
    private fun mergeOrAddResultEntryByPrimaryKey(
        resultEntry: Entry,
        primaryKey: String,
        entriesByKey: MutableMap<String, Entry>,
        entrySourceMap: MutableMap<String, EntrySource>
    ) {
        val existing = entriesByKey[primaryKey]
        if (existing != null) {
            val existingSource = entrySourceMap[primaryKey] ?: EntrySource.ENTRY_LIST
            mergeEntryData(existing, resultEntry, existingSource, EntrySource.RESULT_LIST)
            entrySourceMap[primaryKey] = EntrySource.RESULT_LIST
        } else {
            entriesByKey[primaryKey] = resultEntry
            entrySourceMap[primaryKey] = EntrySource.RESULT_LIST
        }
    }

    /**
     * Merges or adds a result entry using its composite key.
     */
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
            val existingSource = entrySourceMap[compositeKey] ?: EntrySource.ENTRY_LIST
            mergeEntryData(existing, resultEntry, existingSource, EntrySource.RESULT_LIST)
            entrySourceMap[compositeKey] = EntrySource.RESULT_LIST
        } else {
            keylessEntries[compositeKey] = resultEntry
            entrySourceMap[compositeKey] = EntrySource.RESULT_LIST
        }
    }

    /**
     * Marks entries not found in result list as Deregistered.
     */
    private fun markMissingEntriesAsDeregistered(
        entriesByKey: Map<String, Entry>,
        keylessEntries: Map<String, Entry>,
        foundKeys: Set<String>,
        foundCompositeKeys: Set<String>
    ) {
        entriesByKey.values.forEach { entry ->
            val key = generatePrimaryEntryKey(entry)
            if (key != null && key !in foundKeys) {
                entry.status = EntryStatus.Deregistered
            }
        }

        keylessEntries.values.forEach { entry ->
            val key = generateCompositeEntryKey(entry)
            if (key != null && key !in foundCompositeKeys) {
                entry.status = EntryStatus.Deregistered
            }
        }
    }

    private fun buildFinalEntryList(
        entriesByKey: Map<String, Entry>,
        keylessEntries: Map<String, Entry>
    ): List<Entry> {
        val totalSize = entriesByKey.size + keylessEntries.size
        return ArrayList<Entry>(totalSize).apply {
            addAll(entriesByKey.values)
            addAll(keylessEntries.values)
        }
    }

    // ========================================
    // Main Entry List Method
    // ========================================

    /**
     * Retrieves and merges entry lists from Eventor API.
     *
     * Fetching and merging strategy:
     * 1. Fetch entry list and result list in parallel for performance
     * 2. Fetch start list in parallel as well (always, not just as fallback)
     * 3. Merge in priority order: Entry (base) → Start (medium) → Result (highest)
     * 4. When participant changes class/bib: Result > Start > Entry
     * 5. Mark entries not in result list as Deregistered
     *
     * Performance: Parallel API calls reduce total time significantly
     * Data accuracy: All three lists ensure we catch class changes and deregistrations
     */
    fun getEntryList(eventorId: String, eventId: String): List<Entry> {
        val eventor = eventorRepository.findById(eventorId)
            ?: throw EventorNotFoundException()

        // Fetch all three lists in parallel for best performance
        val entryFuture = CompletableFuture.supplyAsync({
            fetchEntryEntries(eventor, eventId)
        }, executor).exceptionally { ex ->
            log.warn("Failed to fetch entry entries for event {}: {}", eventId, ex.message)
            emptyList()
        }

        val startFuture = CompletableFuture.supplyAsync({
            fetchStartEntries(eventor, eventId)
        }, executor).exceptionally { ex ->
            log.warn("Failed to fetch start entries for event {}: {}", eventId, ex.message)
            emptyList()
        }

        val resultFuture = CompletableFuture.supplyAsync({
            fetchResultEntries(eventor, eventId)
        }, executor).exceptionally { ex ->
            log.warn("Failed to fetch result entries for event {}: {}", eventId, ex.message)
            emptyList()
        }

        // Wait for all three to complete
        CompletableFuture.allOf(entryFuture, startFuture, resultFuture).get(apiTimeoutSeconds, TimeUnit.SECONDS)

        val entryEntries = entryFuture.join()
        val startEntries = startFuture.join()
        val resultEntries = resultFuture.join()

        // If we have any data at all, merge with proper priority
        if (entryEntries.isNotEmpty() || startEntries.isNotEmpty() || resultEntries.isNotEmpty()) {
            return mergeAllEntryLists(entryEntries, startEntries, resultEntries)
        }

        // No data available at all
        return emptyList()
    }
}