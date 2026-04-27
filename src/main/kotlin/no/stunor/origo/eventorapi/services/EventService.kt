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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EventService {

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
        val eventorEvent = eventorService.getEvent(eventor.baseUrl, eventor.eventorApiKey, eventorRef) ?: throw EventNotFoundException()
        val eventClassList = eventorService.getEventClasses(eventor, eventorRef)
        val documentList = eventorService.getEventDocuments(eventor.baseUrl, eventor.eventorApiKey, eventorRef)
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
        val mergedFees = mutableListOf<Fee>()
        for (fee in convertedFees) {
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
                mergedFees.add(match)
            } else {
                mergedFees.add(fee)
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
     * Merges data from incoming entry into existing entry.
     * Handles both PersonEntry and TeamEntry types.
     * Result list data takes priority over entry/start list data.
     *
     * Priority rules:
     * - Start time: Always prefer result list (most accurate actual start time)
     * - Finish time: Always prefer result list
     * - Result: Always prefer result list
     * - Bib: Prefer result list if available
     * - Status: Use most complete status (highest ordinal)
     */
    private fun mergeEntryData(existing: Entry, incoming: Entry, isFromResultList: Boolean = false) {
        // Merge common fields - incoming data takes priority
        incoming.bib?.let { existing.bib = it }
        incoming.finishTime?.let { existing.finishTime = it }
        incoming.result?.let { existing.result = it }

        // Start time: If from result list, always override. Otherwise, only set if missing.
        if (isFromResultList) {
            incoming.startTime?.let { existing.startTime = it }
        } else {
            if (existing.startTime == null) {
                incoming.startTime?.let { existing.startTime = it }
            }
        }

        // Status: only update if incoming has more complete status
        if (incoming.status.ordinal > existing.status.ordinal) {
            existing.status = incoming.status
        }

        // Merge type-specific fields
        when {
            existing is PersonEntry && incoming is PersonEntry -> mergePersonEntryData(existing, incoming)
            existing is TeamEntry && incoming is TeamEntry -> mergeTeamEntryData(existing, incoming)
        }
    }

    /**
     * Merges PersonEntry-specific data including punching units and split times.
     */
    private fun mergePersonEntryData(existing: PersonEntry, incoming: PersonEntry) {
        // Merge punching units
        if (incoming.punchingUnits.isNotEmpty()) {
            val existingKeys = existing.punchingUnits.map { it.id to it.type }.toSet()
            incoming.punchingUnits
                .filter { (it.id to it.type) !in existingKeys }
                .forEach { existing.punchingUnits.add(it) }
        }

        // Merge split times (result list has priority)
        if (incoming.splitTimes.isNotEmpty()) {
            existing.splitTimes.clear()
            existing.splitTimes.addAll(incoming.splitTimes)
        }

        // Update other fields if they're more complete in incoming
        incoming.competitorEventorRef?.let { existing.competitorEventorRef = it }
        incoming.nationality?.let { existing.nationality = it }
        if (incoming.birthYear != null) existing.birthYear = incoming.birthYear
    }

    /**
     * Merges TeamEntry-specific data including punching units for team members.
     */
    private fun mergeTeamEntryData(existing: TeamEntry, incoming: TeamEntry) {
        if (incoming.teamMembers.isEmpty()) return

        val membersByPersonId = existing.teamMembers
            .filter { !it.personEventorRef.isNullOrBlank() }
            .associateBy { it.personEventorRef!! }

        incoming.teamMembers.forEach { incomingMember ->
            val personId = incomingMember.personEventorRef ?: return@forEach
            val existingMember = membersByPersonId[personId] ?: return@forEach

            if (incomingMember.punchingUnits.isEmpty()) return@forEach

            val existingKeys = existingMember.punchingUnits.map { it.id to it.type }.toSet()

            incomingMember.punchingUnits
                .filter { (it.id to it.type) !in existingKeys }
                .forEach { existingMember.punchingUnits.add(it) }
        }
    }

    // ========================================
    // Entry Merging Logic
    // ========================================

    /**
     * Merges a list of entries into the provided maps.
     * Entries with primary keys go into entriesByKey, others into keylessEntries.
     */
    private fun mergeEntriesIntoMaps(
        entries: List<Entry>,
        entriesByKey: MutableMap<String, Entry>,
        keylessEntries: MutableMap<String, Entry>
    ) {
        entries.forEach { entry ->
            val primaryKey = generatePrimaryEntryKey(entry)

            if (primaryKey != null) {
                mergeEntryByPrimaryKey(entry, primaryKey, entriesByKey)
            } else {
                mergeEntryByCompositeKey(entry, keylessEntries)
            }
        }
    }

    private fun mergeEntryByPrimaryKey(
        entry: Entry,
        key: String,
        entriesByKey: MutableMap<String, Entry>
    ) {
        val existing = entriesByKey[key]
        if (existing != null) {
            mergeEntryData(existing, entry, isFromResultList = false)
        } else {
            entriesByKey[key] = entry
        }
    }

    private fun mergeEntryByCompositeKey(
        entry: Entry,
        keylessEntries: MutableMap<String, Entry>
    ) {
        val compositeKey = generateCompositeEntryKey(entry) ?: return

        val existing = keylessEntries[compositeKey]
        if (existing != null) {
            mergeEntryData(existing, entry, isFromResultList = false)
        } else {
            keylessEntries[compositeKey] = entry
        }
    }

    /**
     * Combines entries from multiple sources with intelligent merging strategy.
     *
     * Strategy:
     * 1. Start with entry list (or start list if entry list is empty) as the base
     * 2. Merge result list data into existing entries (result data takes priority)
     * 3. Mark entries not found in result list as Deregistered (if result list exists)
     *
     * @param entryEntries Base entries from entry list
     * @param startEntries Fallback entries from start list (used if entry list is empty)
     * @param resultEntries Result entries with race results and status updates
     * @return Deduplicated and merged list of entries
     */
    private fun mergeAllEntryLists(
        entryEntries: List<Entry>,
        startEntries: List<Entry>,
        resultEntries: List<Entry>
    ): List<Entry> {
        val entriesByKey = LinkedHashMap<String, Entry>()
        val keylessEntries = LinkedHashMap<String, Entry>()

        // Step 1: Use entry list as base, fall back to start list if entry list is empty
        val baseEntries = if (entryEntries.isNotEmpty()) entryEntries else startEntries
        mergeEntriesIntoMaps(baseEntries, entriesByKey, keylessEntries)

        // Step 2: If we have result list, merge it and mark missing entries as deregistered
        if (resultEntries.isNotEmpty()) {
            mergeResultEntriesAndMarkDeregistered(resultEntries, entriesByKey, keylessEntries)
        }

        return buildFinalEntryList(entriesByKey, keylessEntries)
    }

    /**
     * Merges result entries into existing entry maps and marks entries not in results as Deregistered.
     *
     * @param resultEntries Entries from result list
     * @param entriesByKey Map of entries with primary keys
     * @param keylessEntries Map of entries without primary keys (composite key based)
     */
    private fun mergeResultEntriesAndMarkDeregistered(
        resultEntries: List<Entry>,
        entriesByKey: MutableMap<String, Entry>,
        keylessEntries: MutableMap<String, Entry>
    ) {
        val foundKeys = mutableSetOf<String>()
        val foundCompositeKeys = mutableSetOf<String>()

        // Merge result entries into existing entries
        resultEntries.forEach { resultEntry ->
            mergeResultEntry(resultEntry, entriesByKey, keylessEntries, foundKeys, foundCompositeKeys)
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
        foundKeys: MutableSet<String>,
        foundCompositeKeys: MutableSet<String>
    ) {
        val primaryKey = generatePrimaryEntryKey(resultEntry)

        if (primaryKey != null) {
            foundKeys.add(primaryKey)
            mergeOrAddResultEntryByPrimaryKey(resultEntry, primaryKey, entriesByKey)
        } else {
            mergeOrAddResultEntryByCompositeKey(resultEntry, keylessEntries, foundCompositeKeys)
        }
    }

    /**
     * Merges or adds a result entry using its primary key.
     */
    private fun mergeOrAddResultEntryByPrimaryKey(
        resultEntry: Entry,
        primaryKey: String,
        entriesByKey: MutableMap<String, Entry>
    ) {
        val existing = entriesByKey[primaryKey]
        if (existing != null) {
            mergeEntryData(existing, resultEntry, isFromResultList = true)
        } else {
            entriesByKey[primaryKey] = resultEntry
        }
    }

    /**
     * Merges or adds a result entry using its composite key.
     */
    private fun mergeOrAddResultEntryByCompositeKey(
        resultEntry: Entry,
        keylessEntries: MutableMap<String, Entry>,
        foundCompositeKeys: MutableSet<String>
    ) {
        val compositeKey = generateCompositeEntryKey(resultEntry) ?: return

        foundCompositeKeys.add(compositeKey)
        val existing = keylessEntries[compositeKey]
        if (existing != null) {
            mergeEntryData(existing, resultEntry, isFromResultList = true)
        } else {
            keylessEntries[compositeKey] = resultEntry
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
     * Fetching strategy:
     * 1. Always fetch entry list (contains registered participants)
     * 2. Fetch start list as fallback if entry list is empty
     * 3. Always fetch result list to get race results and actual participants
     * 4. Merge all lists with result data taking priority
     * 5. Mark entries not in result list as Deregistered
     *
     * Performance note: While this makes 3 API calls, it ensures data accuracy
     * by identifying participants who registered but didn't participate.
     */
    fun getEntryList(eventorId: String, eventId: String): List<Entry> {
        val eventor = eventorRepository.findById(eventorId)
            ?: throw EventorNotFoundException()

        // Fetch all available entry sources
        val entryEntries = fetchEntryEntries(eventor, eventId)
        val startEntries = if (entryEntries.isEmpty()) fetchStartEntries(eventor, eventId) else emptyList()
        val resultEntries = fetchResultEntries(eventor, eventId)

        // If we have results, merge everything together
        if (resultEntries.isNotEmpty() || entryEntries.isNotEmpty() || startEntries.isNotEmpty()) {
            return mergeAllEntryLists(entryEntries, startEntries, resultEntries)
        }

        // No data available at all
        return emptyList()
    }
}