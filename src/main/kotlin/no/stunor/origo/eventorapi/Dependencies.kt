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

    // ── Eventor HTTP client ───────────────────────────────────────────────────
    private val eventorConfig = config.config("app.eventor")

    private val semaphore = Semaphore(
        eventorConfig.propertyOrNull("maxConcurrentRequests")?.getString()?.toInt() ?: 10
    )

    private val requestTimeoutMs = eventorConfig.propertyOrNull("requestTimeoutMs")?.getString()?.toLong() ?: 15_000L

    private val httpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = requestTimeoutMs
            socketTimeoutMillis  = requestTimeoutMs
        }
    }

    val eventorService = EventorService(
        httpClient           = httpClient,
        semaphore            = semaphore,
        eventListCache       = eventListCache,
        competitorCountCache = competitorCountCache,
        eventClassCache      = eventClassCache
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
    private val batchTimeoutMs = eventorConfig.propertyOrNull("batchTimeoutMs")?.getString()?.toLong() ?: 8_000L
    private val calendarCallTimeoutMs = eventorConfig.propertyOrNull("calendarCallTimeoutMs")?.getString()?.toLong() ?: 6_000L

    val eventService = EventService(
        eventorRepository     = eventorRepository,
        eventRepository       = eventRepository,
        eventConverter        = eventConverter,
        feeRepository         = feeRepository,
        eventClassRepository  = eventClassRepository,
        eventorService        = eventorService,
        organisationConverter = organisationConverter,
        organisationRepository = organisationRepository,
        entryListConverter    = entryListConverter,
        startListConverter    = startListConverter,
        resultListConverter   = resultListConverter
    )

    val calendarService = CalendarService(
        personRepository      = personRepository,
        eventorRepository     = eventorRepository,
        eventorService        = eventorService,
        calendarConverter     = calendarConverter,
        batchTimeoutMs        = batchTimeoutMs,
        calendarCallTimeoutMs = calendarCallTimeoutMs
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
