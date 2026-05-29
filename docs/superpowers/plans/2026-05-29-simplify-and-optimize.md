# Simplify and Optimize Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Koin DI and Java's blocking HttpClient with a simple Dependencies class and Ktor's coroutine-native HttpClient, eliminating thread blocking and unnecessary complexity.

**Architecture:** A `Dependencies` class instantiates all singletons directly. `EventorService` uses Ktor `HttpClient(CIO)` with suspend functions and a `Semaphore` to cap concurrent Eventor calls. `EventService` and `CalendarService` replace `CompletableFuture`/`ThreadPoolExecutor` with `coroutineScope { async { } }`.

**Tech Stack:** Ktor 3.1.3, kotlinx-coroutines (Ktor transitive), Ktor CIO client, Caffeine cache, Exposed + HikariCP, MockK + JUnit 5, kotlinx-coroutines-test 1.8.1

---

## File Map

| Action | File |
|--------|------|
| Modify | `pom.xml` |
| Modify | `src/main/resources/application.conf` |
| Rewrite | `src/main/kotlin/.../api/EventorService.kt` |
| Create  | `src/main/kotlin/.../Dependencies.kt` |
| Rewrite | `src/main/kotlin/.../services/EventService.kt` |
| Rewrite | `src/main/kotlin/.../services/CalendarService.kt` |
| Rewrite | `src/main/kotlin/.../services/PersonService.kt` |
| Rewrite | `src/main/kotlin/.../Application.kt` |
| Rewrite | `src/main/kotlin/.../plugins/Routing.kt` |
| Rewrite | `src/main/kotlin/.../plugins/Database.kt` |
| Delete  | `src/main/kotlin/.../di/AppModule.kt` |
| Modify  | `src/test/kotlin/.../services/EventServiceTest.kt` |
| Modify  | `src/test/kotlin/.../services/PersonServiceTest.kt` |

All paths are under `src/main/kotlin/no/stunor/origo/eventorapi/` unless noted.

---

### Task 1: Update pom.xml — add Ktor client, coroutines-test; remove Koin

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Remove `<koin.version>` property (line 20)**

Remove the line:
```xml
        <koin.version>4.0.4</koin.version>
```

- [ ] **Step 2: Replace the Koin dependencies block with Ktor client deps**

Find and replace the entire `<!-- Koin -->` block:
```xml
        <!-- Koin -->
        <dependency>
            <groupId>io.insert-koin</groupId>
            <artifactId>koin-ktor</artifactId>
            <version>${koin.version}</version>
        </dependency>
        <dependency>
            <groupId>io.insert-koin</groupId>
            <artifactId>koin-logger-slf4j</artifactId>
            <version>${koin.version}</version>
        </dependency>
```

Replace with:
```xml
        <!-- Ktor client -->
        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-client-core-jvm</artifactId>
            <version>${ktor.version}</version>
        </dependency>
        <dependency>
            <groupId>io.ktor</groupId>
            <artifactId>ktor-client-cio-jvm</artifactId>
            <version>${ktor.version}</version>
        </dependency>
```

- [ ] **Step 3: Add `kotlinx-coroutines-test` to the test dependencies block**

Find the `<!-- Tests -->` section and add after the existing `ktor-server-test-host-jvm` dependency:
```xml
        <dependency>
            <groupId>org.jetbrains.kotlinx</groupId>
            <artifactId>kotlinx-coroutines-test</artifactId>
            <version>1.8.1</version>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 4: Verify the build compiles (ignore test failures for now)**

```bash
cd /Users/stunor/IdeaProjects/origo-eventor-api && ./mvnw compile -q
```
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add pom.xml
git commit -m "build: replace Koin with Ktor client deps, add coroutines-test"
```

---

### Task 2: Add Eventor config to application.conf

**Files:**
- Modify: `src/main/resources/application.conf`

- [ ] **Step 1: Add the `eventor` and `cache` config blocks**

Find the existing `app { ... }` block and add after the `database { ... }` block:

```hocon
    eventor {
        maxConcurrentRequests = 10
        requestTimeoutMs = 20000
        batchTimeoutMs = 30000
    }

    cache {
        eventListTtlMinutes = 30
        competitorCountTtlMinutes = 5
        eventClassesTtlMinutes = 30
        organisationEntriesTtlMinutes = 5
    }
```

The full `app { }` block should now end as:
```hocon
app {
    supabaseUrl = "http://127.0.0.1:54321"
    supabaseUrl = ${?SUPABASE_URL}

    database {
        url = "jdbc:postgresql://127.0.0.1:54322/postgres"
        url = ${?POSTGRES_DB}
        username = "postgres"
        username = ${?POSTGRES_USER}
        password = "postgres"
        password = ${?POSTGRES_PASSWORD}
        maximumPoolSize = 20
        minimumIdle = 5
        connectionTimeout = 30000
        idleTimeout = 600000
        maxLifetime = 1800000
        leakDetectionThreshold = 60000
    }

    eventor {
        maxConcurrentRequests = 10
        requestTimeoutMs = 20000
        batchTimeoutMs = 30000
    }

    cache {
        eventListTtlMinutes = 30
        competitorCountTtlMinutes = 5
        eventClassesTtlMinutes = 30
        organisationEntriesTtlMinutes = 5
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/application.conf
git commit -m "config: add eventor concurrency and cache TTL settings"
```

---

### Task 3: Rewrite EventorService

**Files:**
- Rewrite: `src/main/kotlin/no/stunor/origo/eventorapi/api/EventorService.kt`

- [ ] **Step 1: Replace the entire file with the following**

```kotlin
package no.stunor.origo.eventorapi.api

import com.github.benmanes.caffeine.cache.Cache
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import no.stunor.origo.eventorapi.exception.EventorAuthException
import no.stunor.origo.eventorapi.exception.EventorConnectionException
import no.stunor.origo.eventorapi.model.Eventor
import no.stunor.origo.eventorapi.model.event.EventClassificationEnum
import org.iof.eventor.*
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import javax.xml.bind.JAXBContext

class EventorService(
    private val httpClient: HttpClient,
    private val semaphore: Semaphore,
    private val eventListCache:       Cache<String, EventList>,
    private val competitorCountCache: Cache<String, CompetitorCountList>,
    private val eventClassCache:      Cache<String, EventClassList>,
    private val orgEntriesCache:      Cache<String, EntryList>
) {
    private val log = LoggerFactory.getLogger(this.javaClass)
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    companion object {
        private val jaxbContextCache = ConcurrentHashMap<Class<*>, JAXBContext>()
    }

    private fun <T> jaxbContext(clazz: Class<T>): JAXBContext =
        jaxbContextCache.getOrPut(clazz) { JAXBContext.newInstance(clazz) }

    private inline fun <reified T> unmarshal(xml: String): T =
        jaxbContext(T::class.java).createUnmarshaller().unmarshal(xml.reader()) as T

    private suspend fun get(
        url: String,
        apiKey: String? = null,
        username: String? = null,
        password: String? = null
    ): String = semaphore.withPermit {
        val response = httpClient.get(url) {
            apiKey?.let   { v -> header("ApiKey", v) }
            username?.let { v -> header("Username", v) }
            password?.let { v -> header("Password", v) }
        }
        when (response.status) {
            HttpStatusCode.OK           -> response.bodyAsText()
            HttpStatusCode.Unauthorized -> throw EventorAuthException()
            else -> {
                log.warn("Eventor API error: HTTP ${response.status} for $url")
                throw EventorConnectionException()
            }
        }
    }

    suspend fun authenticatePerson(eventor: Eventor, username: String?, password: String?): org.iof.eventor.Person {
        val xml = get(eventor.baseUrl + "api/authenticatePerson", username = username, password = password)
        return unmarshal(xml)
    }

    suspend fun getEventList(
        eventor: Eventor,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        organisationIds: List<String?>?,
        classifications: List<EventClassificationEnum?>?
    ): EventList? {
        val classificationIds = (classifications ?: emptyList()).mapNotNull { c ->
            when (c) {
                EventClassificationEnum.Championship -> "1"
                EventClassificationEnum.National     -> "2"
                EventClassificationEnum.Regional     -> "3"
                EventClassificationEnum.Local        -> "4"
                else                                 -> "5"
            }
        }
        val orgPart = if (!organisationIds.isNullOrEmpty())
            "&organisationIds=${organisationIds.filterNotNull().joinToString()}" else ""
        val url = ("${eventor.baseUrl}api/events" +
                "?fromDate=${if (fromDate == null) "" else dateFormat.format(fromDate)}" +
                "&toDate=${if (toDate == null) "" else dateFormat.format(toDate)}" +
                orgPart +
                "&classificationIds=${classificationIds.joinToString()}" +
                "&includeEntryBreaks=true").replace("\n", "").replace(" ", "")

        val cacheKey = "${eventor.id}:$url"
        eventListCache.getIfPresent(cacheKey)?.let { return it }
        val xml = get(url, apiKey = eventor.eventorApiKey)
        val result: EventList = unmarshal(xml)
        eventListCache.put(cacheKey, result)
        return result
    }

    suspend fun getCompetitorCounts(
        eventor: Eventor,
        events: List<String?>?,
        organisations: List<String?>?,
        persons: List<String?>?
    ): CompetitorCountList {
        val url = eventor.baseUrl + "api/competitorcount" +
                "?eventIds=" + events?.joinToString(",") +
                "&organisationIds=" + organisations?.joinToString(",") +
                "&personIds=" + persons?.joinToString(",")
        val cacheKey = "${eventor.id}:$url"
        competitorCountCache.getIfPresent(cacheKey)?.let { return it }
        val xml = get(url, apiKey = eventor.eventorApiKey)
        val result: CompetitorCountList = unmarshal(xml)
        competitorCountCache.put(cacheKey, result)
        return result
    }

    suspend fun getPersonalStarts(
        eventor: Eventor,
        personId: String,
        eventId: String?,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): StartListList? {
        val url = eventor.baseUrl + "api/starts/person" +
                "?personId=$personId" +
                "&fromDate=${if (fromDate == null) "" else dateFormat.format(fromDate)}" +
                "&toDate=${if (toDate == null) "" else dateFormat.format(toDate)}" +
                "&eventIds=${eventId ?: ""}"
        val xml = get(url, apiKey = eventor.eventorApiKey)
        return unmarshal(xml)
    }

    suspend fun getPersonalResults(
        eventor: Eventor,
        personId: String,
        eventId: String?,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): ResultListList? {
        val url = eventor.baseUrl + "api/results/person" +
                "?personId=$personId" +
                "&fromDate=${if (fromDate == null) "" else dateFormat.format(fromDate)}" +
                "&toDate=${if (toDate == null) "" else dateFormat.format(toDate)}" +
                "&eventIds=${eventId ?: ""}"
        val xml = get(url, apiKey = eventor.eventorApiKey)
        return unmarshal(xml)
    }

    suspend fun getOrganisationEntries(
        eventor: Eventor,
        organisations: List<String>,
        eventId: String?,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): EntryList {
        val url = eventor.baseUrl + "api/entries" +
                "?organisationIds=${organisations.joinToString(",")}" +
                "&fromEventDate=${if (fromDate == null) "" else dateFormat.format(fromDate)}" +
                "&toEventDate=${if (toDate == null) "" else dateFormat.format(toDate)}" +
                "&includeEventElement=true&eventIds=${eventId ?: ""}"
        val cacheKey = "${eventor.id}:$url"
        orgEntriesCache.getIfPresent(cacheKey)?.let { return it }
        val xml = get(url, apiKey = eventor.eventorApiKey)
        val result: EntryList = unmarshal(xml)
        orgEntriesCache.put(cacheKey, result)
        return result
    }

    suspend fun getEvent(baseUrl: String, apiKey: String?, eventId: String): Event? {
        val xml = get("${baseUrl}api/event/$eventId", apiKey = apiKey)
        return unmarshal(xml)
    }

    suspend fun getEventClasses(eventor: Eventor, eventId: String): EventClassList? {
        val url = "${eventor.baseUrl}api/eventclasses?includeEntryFees=true&eventId=$eventId"
        val cacheKey = "${eventor.id}:$url"
        eventClassCache.getIfPresent(cacheKey)?.let { return it }
        val xml = get(url, apiKey = eventor.eventorApiKey)
        val result: EventClassList = unmarshal(xml)
        eventClassCache.put(cacheKey, result)
        return result
    }

    suspend fun getEventDocuments(baseUrl: String, apiKey: String?, eventId: String): DocumentList? {
        val xml = get("${baseUrl}api/events/documents?eventIds=$eventId", apiKey = apiKey)
        return unmarshal(xml)
    }

    suspend fun getEventEntryList(baseUrl: String, apiKey: String?, eventId: String): EntryList? {
        val xml = get(
            "${baseUrl}api/entries?includePersonElement=true&includeEntryFees=true&eventIds=$eventId",
            apiKey = apiKey
        )
        return unmarshal(xml)
    }

    suspend fun getEventStartList(baseUrl: String, apiKey: String?, eventId: String): StartList? {
        val xml = get("${baseUrl}api/starts/event?eventId=$eventId", apiKey = apiKey)
        return unmarshal(xml)
    }

    suspend fun getEventResultList(baseUrl: String, apiKey: String?, eventId: String): ResultList? {
        val xml = get("${baseUrl}api/results/event?eventId=$eventId&includeSplitTimes=true", apiKey = apiKey)
        return unmarshal(xml)
    }

    suspend fun getEventEntryFees(eventor: Eventor, eventId: String): EntryFeeList? {
        val xml = get("${eventor.baseUrl}api/entryfees/events/$eventId", apiKey = eventor.eventorApiKey)
        return unmarshal(xml)
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd /Users/stunor/IdeaProjects/origo-eventor-api && ./mvnw compile -q 2>&1 | head -30
```
Expected: errors only about callers that still use the old API (will be fixed in later tasks)

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/no/stunor/origo/eventorapi/api/EventorService.kt
git commit -m "refactor: replace Java HttpClient with Ktor CIO client in EventorService"
```

---

### Task 4: Create Dependencies.kt

**Files:**
- Create: `src/main/kotlin/no/stunor/origo/eventorapi/Dependencies.kt`

- [ ] **Step 1: Create the file**

```kotlin
package no.stunor.origo.eventorapi

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.server.config.*
import kotlinx.coroutines.sync.Semaphore
import no.stunor.origo.eventorapi.api.EventorService
import no.stunor.origo.eventorapi.data.*
import no.stunor.origo.eventorapi.services.CalendarService
import no.stunor.origo.eventorapi.services.EventService
import no.stunor.origo.eventorapi.services.PersonService
import no.stunor.origo.eventorapi.services.converter.*
import no.stunor.origo.eventorapi.validation.InputValidator
import org.iof.eventor.CompetitorCountList
import org.iof.eventor.EntryList
import org.iof.eventor.EventClassList
import org.iof.eventor.EventList
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

class Dependencies(config: ApplicationConfig) {

    val dataSource: DataSource = run {
        val db = config.config("app.database")
        val hk = HikariConfig()
        hk.jdbcUrl           = db.property("url").getString()
        hk.username          = db.property("username").getString()
        hk.password          = db.property("password").getString()
        hk.driverClassName   = "org.postgresql.Driver"
        hk.maximumPoolSize   = db.propertyOrNull("maximumPoolSize")?.getString()?.toInt() ?: 20
        hk.minimumIdle       = db.propertyOrNull("minimumIdle")?.getString()?.toInt() ?: 5
        hk.connectionTimeout = db.propertyOrNull("connectionTimeout")?.getString()?.toLong() ?: 30_000L
        hk.idleTimeout       = db.propertyOrNull("idleTimeout")?.getString()?.toLong() ?: 600_000L
        hk.maxLifetime       = db.propertyOrNull("maxLifetime")?.getString()?.toLong() ?: 1_800_000L
        hk.addDataSourceProperty("stringtype", "unspecified")
        HikariDataSource(hk)
    }

    // ── Repositories ──────────────────────────────────────────────────────────
    val regionRepository       = RegionRepository(dataSource)
    val organisationRepository = OrganisationRepository(dataSource, regionRepository)
    val userPersonRepository   = UserPersonRepository(dataSource)
    val membershipRepository   = MembershipRepository(dataSource, organisationRepository)
    val personRepository       = PersonRepository(dataSource, membershipRepository, userPersonRepository)
    val eventClassRepository   = EventClassRepository(dataSource)
    val feeRepository          = FeeRepository(dataSource)
    val eventRepository        = EventRepository(dataSource, organisationRepository)
    val eventorRepository      = EventorRepository(dataSource)

    // ── Caches ────────────────────────────────────────────────────────────────
    private val cacheConfig = config.config("app.cache")

    private val eventListCache: Cache<String, EventList> = Caffeine.newBuilder()
        .maximumSize(500)
        .expireAfterWrite(cacheConfig.propertyOrNull("eventListTtlMinutes")?.getString()?.toLong() ?: 30, TimeUnit.MINUTES)
        .build()

    private val competitorCountCache: Cache<String, CompetitorCountList> = Caffeine.newBuilder()
        .maximumSize(500)
        .expireAfterWrite(cacheConfig.propertyOrNull("competitorCountTtlMinutes")?.getString()?.toLong() ?: 5, TimeUnit.MINUTES)
        .build()

    private val eventClassCache: Cache<String, EventClassList> = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(cacheConfig.propertyOrNull("eventClassesTtlMinutes")?.getString()?.toLong() ?: 30, TimeUnit.MINUTES)
        .build()

    private val orgEntriesCache: Cache<String, EntryList> = Caffeine.newBuilder()
        .maximumSize(500)
        .expireAfterWrite(cacheConfig.propertyOrNull("organisationEntriesTtlMinutes")?.getString()?.toLong() ?: 5, TimeUnit.MINUTES)
        .build()

    // ── Eventor HTTP client ───────────────────────────────────────────────────
    private val eventorConfig = config.config("app.eventor")

    private val semaphore = Semaphore(
        eventorConfig.propertyOrNull("maxConcurrentRequests")?.getString()?.toInt() ?: 10
    )

    private val httpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = eventorConfig.propertyOrNull("requestTimeoutMs")?.getString()?.toLong() ?: 20_000L
        }
    }

    val eventorService = EventorService(
        httpClient           = httpClient,
        semaphore            = semaphore,
        eventListCache       = eventListCache,
        competitorCountCache = competitorCountCache,
        eventClassCache      = eventClassCache,
        orgEntriesCache      = orgEntriesCache
    )

    // ── Converters ────────────────────────────────────────────────────────────
    val organisationConverter = OrganisationConverter(organisationRepository, regionRepository)
    val personConverter       = PersonConverter(organisationRepository)
    val entryListConverter    = EntryListConverter(organisationConverter, personConverter)
    val startListConverter    = StartListConverter(organisationConverter, personConverter)
    val resultListConverter   = ResultListConverter(organisationConverter, personConverter, entryListConverter)
    val eventConverter        = EventConverter(entryListConverter)
    val calendarConverter     = CalendarConverter(eventConverter, organisationConverter)

    // ── Services ──────────────────────────────────────────────────────────────
    private val batchTimeoutMs = eventorConfig.propertyOrNull("batchTimeoutMs")?.getString()?.toLong() ?: 30_000L

    val eventService = EventService(
        eventorRepository     = eventorRepository,
        eventRepository       = eventRepository,
        eventConverter        = eventConverter,
        feeRepository         = feeRepository,
        eventClassRepository  = eventClassRepository,
        eventorService        = eventorService,
        organisationConverter = organisationConverter,
        entryListConverter    = entryListConverter,
        startListConverter    = startListConverter,
        resultListConverter   = resultListConverter,
        batchTimeoutMs        = batchTimeoutMs
    )

    val calendarService = CalendarService(
        personRepository       = personRepository,
        eventorRepository      = eventorRepository,
        organisationRepository = organisationRepository,
        regionRepository       = regionRepository,
        eventorService         = eventorService,
        calendarConverter      = calendarConverter,
        batchTimeoutMs         = batchTimeoutMs
    )

    val personService = PersonService(
        eventorRepository    = eventorRepository,
        personRepository     = personRepository,
        membershipRepository = membershipRepository,
        userPersonRepository = userPersonRepository,
        eventorService       = eventorService,
        personConverter      = personConverter
    )

    val inputValidator = InputValidator()

    fun close() {
        httpClient.close()
        (dataSource as? HikariDataSource)?.close()
    }
}
```

- [ ] **Step 2: Commit (will fail to compile until services are updated — commit source only)**

```bash
git add src/main/kotlin/no/stunor/origo/eventorapi/Dependencies.kt
git commit -m "feat: add Dependencies class to replace Koin"
```

---

### Task 5: Rewrite EventService

**Files:**
- Rewrite: `src/main/kotlin/no/stunor/origo/eventorapi/services/EventService.kt`

- [ ] **Step 1: Replace the entire file with the following**

```kotlin
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
    private val batchTimeoutMs: Long = 30_000L
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

        val organisers = organisationConverter.convertOrganisations(
            organisations = eventorEvent.organiser.organisationIdOrOrganisation,
            eventorId     = eventorId
        )
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
        return resultList?.let { resultListConverter.convertEventResultList(eventor, it) } ?: emptyList()
    }

    private suspend fun fetchStartEntries(eventor: Eventor, eventId: String): List<Entry> {
        val startList = eventorService.getEventStartList(eventor.baseUrl, eventor.eventorApiKey, eventId)
        return startList?.let { startListConverter.convertEventStartList(eventor, it) } ?: emptyList()
    }

    private suspend fun fetchEntryEntries(eventor: Eventor, eventId: String): List<Entry> {
        val entryList = eventorService.getEventEntryList(eventor.baseUrl, eventor.eventorApiKey, eventId)
            ?: return emptyList()
        return if (!entryList.entry.isNullOrEmpty()) entryListConverter.convertEventEntryList(eventor, entryList) else emptyList()
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
```

- [ ] **Step 2: Verify compilation**

```bash
cd /Users/stunor/IdeaProjects/origo-eventor-api && ./mvnw compile -q 2>&1 | head -30
```
Expected: errors only from CalendarService, PersonService, Routing, Application (not yet updated)

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/no/stunor/origo/eventorapi/services/EventService.kt
git commit -m "refactor: replace CompletableFuture with coroutines in EventService"
```

---

### Task 6: Rewrite CalendarService

**Files:**
- Rewrite: `src/main/kotlin/no/stunor/origo/eventorapi/services/CalendarService.kt`

- [ ] **Step 1: Replace the entire file with the following**

```kotlin
package no.stunor.origo.eventorapi.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import no.stunor.origo.eventorapi.api.EventorService
import no.stunor.origo.eventorapi.data.EventorRepository
import no.stunor.origo.eventorapi.data.OrganisationRepository
import no.stunor.origo.eventorapi.data.PersonRepository
import no.stunor.origo.eventorapi.data.RegionRepository
import no.stunor.origo.eventorapi.exception.EventorNotFoundException
import no.stunor.origo.eventorapi.model.Eventor
import no.stunor.origo.eventorapi.model.PartialResult
import no.stunor.origo.eventorapi.model.calendar.CalendarRace
import no.stunor.origo.eventorapi.model.event.EventClassificationEnum
import no.stunor.origo.eventorapi.model.person.Person
import no.stunor.origo.eventorapi.services.converter.CalendarConverter
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

class CalendarService(
    private val personRepository: PersonRepository,
    private val eventorRepository: EventorRepository,
    private val organisationRepository: OrganisationRepository,
    private val regionRepository: RegionRepository,
    private val eventorService: EventorService,
    private val calendarConverter: CalendarConverter,
    private val batchTimeoutMs: Long = 30_000L
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    suspend fun getEventList(
        from: LocalDate,
        to: LocalDate,
        classifications: List<EventClassificationEnum>?,
        userId: UUID?
    ): PartialResult<List<CalendarRace>> {
        val eventorList = withContext(Dispatchers.IO) { eventorRepository.findAll() }

        val results = coroutineScope {
            eventorList.map { eventor ->
                async {
                    withTimeoutOrNull(batchTimeoutMs) {
                        try {
                            val persons = resolvePersonsForEventor(eventor.id, userId)
                            getEventListInternal(eventor, from, to, null, classifications, persons)
                        } catch (e: Exception) {
                            log.warn("Failed to fetch events for eventor {}: {}", eventor.id, e.message)
                            PartialResult(emptyList<CalendarRace>(), isPartial = false)
                        }
                    } ?: run {
                        log.warn("Timeout fetching events from eventor {} after {} ms", eventor.id, batchTimeoutMs)
                        PartialResult(emptyList<CalendarRace>(), isPartial = true)
                    }
                }
            }.awaitAll()
        }

        val allRaces = results.flatMap { it.data }
        val isPartial = results.any { it.isPartial }
        return PartialResult(filterRacesByDateRange(allRaces, from, to), isPartial)
    }

    suspend fun getEventList(
        eventorId: String,
        from: LocalDate,
        to: LocalDate,
        organisations: List<String>?,
        classifications: List<EventClassificationEnum>?,
        userId: UUID?
    ): PartialResult<List<CalendarRace>> {
        val eventor = withContext(Dispatchers.IO) { eventorRepository.findById(eventorId) }
            ?: throw EventorNotFoundException()
        val persons = resolvePersonsForEventor(eventor.id, userId)
        val races = getEventListInternal(eventor, from, to, organisations, classifications, persons)
        return PartialResult(filterRacesByDateRange(races.data, from, to), isPartial = races.isPartial)
    }

    private suspend fun resolvePersonsForEventor(eventorId: String, userId: UUID?): List<Person> {
        return if (userId != null) {
            withContext(Dispatchers.IO) {
                personRepository.findAllByUsersAndEventorId(userId = userId, eventorId = eventorId)
            }
        } else {
            emptyList()
        }
    }

    private suspend fun getEventListInternal(
        eventor: Eventor,
        from: LocalDate,
        to: LocalDate,
        organisations: List<String>?,
        classifications: List<EventClassificationEnum>?,
        persons: List<Person>
    ): PartialResult<List<CalendarRace>> {
        val eventList = eventorService.getEventList(eventor, from, to, organisations, classifications)
            ?: return PartialResult(emptyList(), isPartial = false)
        val events = eventList.event.map { it.eventId.content }

        val personIds = persons.map { it.eventorRef }
        val organisationIds = persons.flatMap { person ->
            person.memberships.mapNotNull { it.organisation?.eventorRef }
        }.distinct()

        log.info("Fetching competitor-count for persons {} and organisations {}.", personIds, organisationIds)
        val competitorCountList = eventorService.getCompetitorCounts(eventor, events, organisationIds, personIds)

        log.info("Fetching event classes for {} events", events.size)
        val eventClassesMap = coroutineScope {
            events.map { eventId ->
                async {
                    runCatching { eventId to eventorService.getEventClasses(eventor, eventId) }
                        .onFailure { log.warn("Failed to fetch event classes for event {}: {}", eventId, it.message) }
                        .getOrNull() ?: (eventId to null)
                }
            }.awaitAll()
        }.toMap()

        val races = calendarConverter.convertEvents(eventList, eventor, competitorCountList, eventClassesMap)
        return PartialResult(races, isPartial = false)
    }

    private fun filterRacesByDateRange(races: List<CalendarRace>, from: LocalDate, to: LocalDate): List<CalendarRace> {
        return races.filter { race ->
            val raceLocalDate = race.raceDate.toLocalDateTime().toLocalDate()
            !raceLocalDate.isBefore(from) && !raceLocalDate.isAfter(to)
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd /Users/stunor/IdeaProjects/origo-eventor-api && ./mvnw compile -q 2>&1 | head -30
```

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/no/stunor/origo/eventorapi/services/CalendarService.kt
git commit -m "refactor: replace CompletableFuture with coroutines in CalendarService"
```

---

### Task 7: Rewrite PersonService

**Files:**
- Rewrite: `src/main/kotlin/no/stunor/origo/eventorapi/services/PersonService.kt`

- [ ] **Step 1: Replace the entire file with the following**

```kotlin
package no.stunor.origo.eventorapi.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.stunor.origo.eventorapi.api.EventorService
import no.stunor.origo.eventorapi.data.EventorRepository
import no.stunor.origo.eventorapi.data.MembershipRepository
import no.stunor.origo.eventorapi.data.PersonRepository
import no.stunor.origo.eventorapi.data.UserPersonRepository
import no.stunor.origo.eventorapi.exception.EventorAuthException
import no.stunor.origo.eventorapi.exception.EventorConnectionException
import no.stunor.origo.eventorapi.exception.EventorNotFoundException
import no.stunor.origo.eventorapi.model.person.Person
import no.stunor.origo.eventorapi.model.person.UserPerson
import no.stunor.origo.eventorapi.model.person.UserPersonKey
import no.stunor.origo.eventorapi.services.converter.PersonConverter
import java.util.UUID

class PersonService(
    private val eventorRepository: EventorRepository,
    private val personRepository: PersonRepository,
    private val membershipRepository: MembershipRepository,
    private val userPersonRepository: UserPersonRepository,
    private val eventorService: EventorService,
    private val personConverter: PersonConverter
) {

    suspend fun authenticate(eventorId: String, username: String, password: String, userId: UUID): Person {
        val eventor = withContext(Dispatchers.IO) {
            eventorRepository.findById(eventorId)
        } ?: throw EventorNotFoundException()

        val eventorPerson = try {
            eventorService.authenticatePerson(eventor, username, password)
        } catch (e: EventorAuthException) {
            throw e
        } catch (e: EventorConnectionException) {
            throw e
        } catch (e: Exception) {
            throw EventorConnectionException()
        }

        val person = personConverter.convertPerson(eventorPerson, eventor)
        val existingPerson = withContext(Dispatchers.IO) {
            personRepository.findByEventorIdAndEventorRef(eventorId, person.eventorRef)
        }
        if (existingPerson != null) {
            person.id = existingPerson.id
            withContext(Dispatchers.IO) { membershipRepository.deleteByPersonId(existingPerson.id) }
        }

        val userPerson = UserPerson(id = UserPersonKey(userId = userId, personId = person.id), person = person)
        person.users.add(userPerson)
        withContext(Dispatchers.IO) { personRepository.save(person) }
        return person
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/kotlin/no/stunor/origo/eventorapi/services/PersonService.kt
git commit -m "refactor: make PersonService.authenticate suspend"
```

---

### Task 8: Update Application.kt, Routing.kt, Database.kt — remove Koin

**Files:**
- Rewrite: `src/main/kotlin/no/stunor/origo/eventorapi/Application.kt`
- Rewrite: `src/main/kotlin/no/stunor/origo/eventorapi/plugins/Routing.kt`
- Rewrite: `src/main/kotlin/no/stunor/origo/eventorapi/plugins/Database.kt`

- [ ] **Step 1: Replace Application.kt**

```kotlin
package no.stunor.origo.eventorapi

import io.ktor.server.netty.*
import io.ktor.server.application.*
import no.stunor.origo.eventorapi.plugins.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    val deps = Dependencies(environment.config)
    configureDatabase(deps)
    configureSerialization()
    configureAuth(environment.config)
    configureStatusPages()
    configureRouting(deps)
    configureShutdownHook(deps)
}
```

- [ ] **Step 2: Replace Routing.kt**

```kotlin
package no.stunor.origo.eventorapi.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.stunor.origo.eventorapi.Dependencies
import no.stunor.origo.eventorapi.model.event.EventClassificationEnum
import java.time.LocalDate
import java.util.UUID

fun Application.configureRouting(deps: Dependencies) {
    routing {
        // ── Health ─────────────────────────────────────────────────────────────
        get("/actuator/health") {
            call.respond(mapOf("status" to "UP"))
        }

        route("/rest") {
            authenticate("jwt-optional", optional = true) {
                get("/event/{eventorId}/{eventorRef}") {
                    val eventorId  = deps.inputValidator.validateEventorId(call.parameters["eventorId"]!!)
                    val eventorRef = deps.inputValidator.validateEventId(call.parameters["eventorRef"]!!)
                    call.respond(deps.eventService.getEvent(eventorId, eventorRef))
                }

                get("/event/{eventorId}/{eventId}/entry-list") {
                    val eventorId = deps.inputValidator.validateEventorId(call.parameters["eventorId"]!!)
                    val eventId   = deps.inputValidator.validateEventId(call.parameters["eventId"]!!)
                    call.respond(deps.eventService.getEntryList(eventorId, eventId))
                }

                get("/event-list/{eventorId}") {
                    val eventorId       = deps.inputValidator.validateEventorId(call.parameters["eventorId"]!!)
                    val from            = LocalDate.parse(call.request.queryParameters["from"]!!)
                    val to              = LocalDate.parse(call.request.queryParameters["to"]!!)
                    val organisations   = call.request.queryParameters.getAll("organisations")
                    val classifications = call.request.queryParameters.getAll("classifications")
                        ?.flatMap { it.split(",") }
                        ?.mapNotNull { runCatching { EventClassificationEnum.valueOf(it.trim()) }.getOrNull() }
                        ?: listOf(
                            EventClassificationEnum.Championship,
                            EventClassificationEnum.National,
                            EventClassificationEnum.Regional,
                            EventClassificationEnum.Local
                        )
                    val uid = call.principal<JWTPrincipal>()?.subject?.let { UUID.fromString(it) }
                    val validatedOrgs = deps.inputValidator.validateOrganisationIds(organisations)
                    val result = deps.calendarService.getEventList(
                        eventorId       = eventorId,
                        from            = from,
                        to              = to,
                        organisations   = validatedOrgs,
                        classifications = classifications,
                        userId          = uid
                    )
                    call.respond(if (result.isPartial) HttpStatusCode.PartialContent else HttpStatusCode.OK, result.data)
                }

                get("/event-list") {
                    val from            = LocalDate.parse(call.request.queryParameters["from"]!!)
                    val to              = LocalDate.parse(call.request.queryParameters["to"]!!)
                    val classifications = call.request.queryParameters.getAll("classifications")
                        ?.flatMap { it.split(",") }
                        ?.mapNotNull { runCatching { EventClassificationEnum.valueOf(it.trim()) }.getOrNull() }
                        ?: listOf(
                            EventClassificationEnum.Championship,
                            EventClassificationEnum.National,
                            EventClassificationEnum.Regional,
                            EventClassificationEnum.Local
                        )
                    val uid = call.principal<JWTPrincipal>()?.subject?.let { UUID.fromString(it) }
                    val result = deps.calendarService.getEventList(
                        from            = from,
                        to              = to,
                        classifications = classifications,
                        userId          = uid
                    )
                    call.respond(if (result.isPartial) HttpStatusCode.PartialContent else HttpStatusCode.OK, result.data)
                }
            }

            authenticate("jwt-required") {
                post("/person/{eventorId}") {
                    val eventorId = deps.inputValidator.validateEventorId(call.parameters["eventorId"]!!)
                    val username  = deps.inputValidator.validateUsername(
                        call.request.headers["username"] ?: throw IllegalArgumentException("Missing username header")
                    )
                    val password  = call.request.headers["password"]
                        ?: throw IllegalArgumentException("Missing password header")
                    val uid = UUID.fromString(
                        call.principal<JWTPrincipal>()?.subject
                            ?: throw IllegalStateException("Authentication required")
                    )
                    call.respond(deps.personService.authenticate(eventorId, username, password, uid))
                }
            }
        }
    }
}
```

- [ ] **Step 3: Replace Database.kt**

```kotlin
package no.stunor.origo.eventorapi.plugins

import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import no.stunor.origo.eventorapi.Dependencies
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("DatabasePlugin")

fun Application.configureDatabase(deps: Dependencies) {
    log.info("Database connection pool initialized: ${(deps.dataSource as? HikariDataSource)?.jdbcUrl}")
}

fun Application.configureShutdownHook(deps: Dependencies) {
    monitor.subscribe(ApplicationStopped) {
        log.info("Application stopping – closing HTTP client and DB pool")
        deps.close()
    }
}
```

- [ ] **Step 4: Verify full compilation**

```bash
cd /Users/stunor/IdeaProjects/origo-eventor-api && ./mvnw compile -q 2>&1
```
Expected: `BUILD SUCCESS` with no errors

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/no/stunor/origo/eventorapi/Application.kt \
        src/main/kotlin/no/stunor/origo/eventorapi/plugins/Routing.kt \
        src/main/kotlin/no/stunor/origo/eventorapi/plugins/Database.kt
git commit -m "refactor: remove Koin, wire dependencies directly in Application"
```

---

### Task 9: Delete AppModule.kt

**Files:**
- Delete: `src/main/kotlin/no/stunor/origo/eventorapi/di/AppModule.kt`

- [ ] **Step 1: Delete the file**

```bash
rm src/main/kotlin/no/stunor/origo/eventorapi/di/AppModule.kt
```

- [ ] **Step 2: Remove the now-empty di/ directory if it's empty**

```bash
rmdir src/main/kotlin/no/stunor/origo/eventorapi/di 2>/dev/null || true
```

- [ ] **Step 3: Verify compilation**

```bash
cd /Users/stunor/IdeaProjects/origo-eventor-api && ./mvnw compile -q
```
Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add -A src/main/kotlin/no/stunor/origo/eventorapi/di/
git commit -m "chore: delete Koin AppModule"
```

---

### Task 10: Update tests for suspend functions

**Files:**
- Modify: `src/test/kotlin/no/stunor/origo/eventorapi/services/EventServiceTest.kt`
- Modify: `src/test/kotlin/no/stunor/origo/eventorapi/services/PersonServiceTest.kt`

- [ ] **Step 1: Update EventServiceTest.kt imports and all `every`/`verify` on suspend methods**

Add these imports at the top of the file (replacing existing mockk imports):
```kotlin
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
```

Change the `EventService` constructor call in `@BeforeEach` to add `batchTimeoutMs`:
```kotlin
eventService = EventService(
    eventorRepository     = eventorRepository,
    eventRepository       = eventRepository,
    eventConverter        = eventConverter,
    feeRepository         = feeRepository,
    eventClassRepository  = eventClassRepository,
    eventorService        = eventorService,
    organisationConverter = organisationConverter,
    entryListConverter    = entryListConverter,
    startListConverter    = startListConverter,
    resultListConverter   = resultListConverter,
    batchTimeoutMs        = 5_000L
)
```

For each test method that mocks `EventorService` suspend functions, change `every` → `coEvery` and `verify` → `coVerify`. For example:

```kotlin
// Before:
every { eventorService.getEvent(eventor.baseUrl, eventor.eventorApiKey, eventId) } returns oneDayEvent
// After:
coEvery { eventorService.getEvent(eventor.baseUrl, eventor.eventorApiKey, eventId) } returns oneDayEvent
```

For each test method that calls `eventService.getEvent(...)` or `eventService.getEntryList(...)`, wrap the call in `runTest { }`:

```kotlin
// Before:
@Test
fun `getEvent should retrieve and convert one-day event successfully`() {
    // ... setup mocks ...
    val result = eventService.getEvent(eventorId, eventId)
    assertNotNull(result)
    verify { eventorService.getEvent(...) }
}

// After:
@Test
fun `getEvent should retrieve and convert one-day event successfully`() = runTest {
    // ... setup mocks using coEvery ...
    val result = eventService.getEvent(eventorId, eventId)
    assertNotNull(result)
    coVerify { eventorService.getEvent(...) }
}
```

Apply this pattern to ALL test methods in the file.

The complete list of `EventorService` methods that become suspend (change to `coEvery`/`coVerify`):
- `getEvent`
- `getEventClasses`
- `getEventDocuments`
- `getEventEntryFees`
- `getEventEntryList`
- `getEventStartList`
- `getEventResultList`

- [ ] **Step 2: Update PersonServiceTest.kt**

Add imports:
```kotlin
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
```

Change all `every { eventorService.authenticatePerson(...) }` → `coEvery { eventorService.authenticatePerson(...) }`

Change all `verify { eventorService.authenticatePerson(...) }` → `coVerify { eventorService.authenticatePerson(...) }`

Wrap all test method bodies with `= runTest { ... }`:
```kotlin
@Test
fun `authenticate should return person when credentials are valid`() = runTest {
    // test body unchanged
}
```

Also update the `PersonService` constructor call in `@BeforeEach` (no new params needed — PersonService constructor is unchanged).

- [ ] **Step 3: Run the tests**

```bash
cd /Users/stunor/IdeaProjects/origo-eventor-api && ./mvnw test -pl . -Dtest="EventServiceTest,PersonServiceTest,FeeConverterTest" 2>&1 | tail -30
```
Expected: all tests pass

- [ ] **Step 4: Run the full test suite**

```bash
cd /Users/stunor/IdeaProjects/origo-eventor-api && ./mvnw test 2>&1 | tail -20
```
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add src/test/kotlin/no/stunor/origo/eventorapi/services/EventServiceTest.kt \
        src/test/kotlin/no/stunor/origo/eventorapi/services/PersonServiceTest.kt
git commit -m "test: update mocks and tests for suspend EventorService methods"
```

---

### Task 11: Final build verification

- [ ] **Step 1: Clean build**

```bash
cd /Users/stunor/IdeaProjects/origo-eventor-api && ./mvnw clean package -q 2>&1 | tail -10
```
Expected: `BUILD SUCCESS`

- [ ] **Step 2: Verify no Koin imports remain in main sources**

```bash
grep -r "koin\|inject()" src/main/kotlin --include="*.kt"
```
Expected: no output

- [ ] **Step 3: Verify no Java HttpClient imports remain**

```bash
grep -r "java.net.http\|HttpClient.newBuilder\|CompletableFuture\|ThreadPoolExecutor\|SynchronousQueue" src/main/kotlin --include="*.kt"
```
Expected: no output

- [ ] **Step 4: Commit**

```bash
git commit --allow-empty -m "chore: verified clean build after simplification"
```
