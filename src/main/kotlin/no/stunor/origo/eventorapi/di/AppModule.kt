package no.stunor.origo.eventorapi.di

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
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
import org.koin.dsl.module
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

fun appModule(config: ApplicationConfig) = module {

    // ─── DataSource ────────────────────────────────────────────────────────────
    single<DataSource> {
        val db = config.config("app.database")
        val hk = HikariConfig()
        hk.jdbcUrl             = db.property("url").getString()
        hk.username            = db.property("username").getString()
        hk.password            = db.property("password").getString()
        hk.driverClassName     = "org.postgresql.Driver"
        hk.maximumPoolSize     = db.propertyOrNull("maximumPoolSize")?.getString()?.toInt() ?: 20
        hk.minimumIdle         = db.propertyOrNull("minimumIdle")?.getString()?.toInt() ?: 5
        hk.connectionTimeout   = db.propertyOrNull("connectionTimeout")?.getString()?.toLong() ?: 30_000L
        hk.idleTimeout         = db.propertyOrNull("idleTimeout")?.getString()?.toLong() ?: 600_000L
        hk.maxLifetime         = db.propertyOrNull("maxLifetime")?.getString()?.toLong() ?: 1_800_000L
        hk.addDataSourceProperty("stringtype", "unspecified")
        HikariDataSource(hk)
    }

    // ─── Spring JDBC helpers (used standalone, no Spring container) ────────────
    single { JdbcTemplate(get()) }
    single { NamedParameterJdbcTemplate(get<DataSource>()) }
    single { DataSourceTransactionManager(get()) }
    single { TransactionTemplate(get<DataSourceTransactionManager>()) }

    // ─── Repositories ─────────────────────────────────────────────────────────
    single { RegionRepository(get()) }
    single { OrganisationRepository(get(), get()) }
    single { MembershipRepository(get(), get()) }
    single { UserPersonRepository(get()) }
    single { UserRepository(get()) }
    single { PersonRepository(get(), get(), get(), get()) }
    single { EventClassRepository(get()) }
    single { FeeRepository(get()) }
    single { EventRepository(get(), get()) }
    single { EventorRepository(get()) }

    // ─── Caffeine caches ──────────────────────────────────────────────────────
    single<Cache<String, EventList>>(qualifier = org.koin.core.qualifier.named("event-lists")) {
        Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build()
    }
    single<Cache<String, org.iof.eventor.CompetitorCountList>>(qualifier = org.koin.core.qualifier.named("competitor-counts")) {
        Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build()
    }
    single<Cache<String, EventClassList>>(qualifier = org.koin.core.qualifier.named("event-classes")) {
        Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build()
    }
    single<Cache<String, EntryList>>(qualifier = org.koin.core.qualifier.named("organisation-entries")) {
        Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build()
    }

    // ─── External API service ─────────────────────────────────────────────────
    single {
        EventorService(
            eventListCache       = get(qualifier = org.koin.core.qualifier.named("event-lists")),
            competitorCountCache = get(qualifier = org.koin.core.qualifier.named("competitor-counts")),
            eventClassCache      = get(qualifier = org.koin.core.qualifier.named("event-classes")),
            orgEntriesCache      = get(qualifier = org.koin.core.qualifier.named("organisation-entries"))
        )
    }

    // ─── Converters ───────────────────────────────────────────────────────────
    single { OrganisationConverter(get(), get()) }
    single { PersonConverter(get()) }
    single { EntryListConverter(get(), get()) }
    single { StartListConverter(get(), get()) }
    single { ResultListConverter(get(), get(), get()) }
    single { EventConverter(get()) }
    single { CalendarConverter(get(), get()) }

    // ─── Services ─────────────────────────────────────────────────────────────
    single {
        PersonService(
            eventorRepository   = get(),
            personRepository    = get(),
            membershipRepository = get(),
            userPersonRepository = get(),
            eventorService      = get(),
            personConverter     = get()
        )
    }
    single {
        EventService(
            eventorRepository    = get(),
            eventRepository      = get(),
            eventConverter       = get(),
            feeRepository        = get(),
            eventClassRepository = get(),
            eventorService       = get(),
            organisationConverter = get(),
            entryListConverter   = get(),
            startListConverter   = get(),
            resultListConverter  = get(),
            transactionTemplate  = get()
        )
    }
    single {
        CalendarService(
            personRepository      = get(),
            eventorRepository     = get(),
            organisationRepository = get(),
            regionRepository      = get(),
            eventorService        = get(),
            calendarConverter     = get()
        )
    }

    // ─── Utilities ────────────────────────────────────────────────────────────
    single { InputValidator() }
}
