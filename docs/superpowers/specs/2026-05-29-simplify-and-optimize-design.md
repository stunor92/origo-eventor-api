# Design: Forenkling og ytelsesoptimalisering av origo-eventor-api

**Dato:** 2026-05-29
**Status:** Godkjent

## Bakgrunn

Appen er en Ktor-basert proxy mot Eventor-API-et. Enkeltforespørsler (særlig kalender-endepunktet) tar unødvendig lang tid fordi:

1. Eventor-kall gjøres med blokkerende Java `HttpClient` i en coroutine-kontekst — blokkerer tråder i stedet for å suspendere
2. `JAXBContext` opprettes på nytt for hvert XML-parsing-kall (dyrt)
3. Koin DI og `CompletableFuture`/`ThreadPoolExecutor` legger til kompleksitet uten tilsvarende verdi

Målet er en enklest mulig Ktor-app med coroutine-native HTTP og god concurrency-kontroll mot Eventor, uten å endre endepunktenes oppførsel.

## Hva endres

### 1. HTTP-klient: Java HttpClient → Ktor HttpClient

`EventorService` bytter fra `java.net.http.HttpClient` (blokkerende) til Ktor's `HttpClient` med CIO-engine (coroutine-native).

- Alle Eventor-kall suspenderer i stedet for å blokkere tråder
- `ThreadPoolExecutor` og `CompletableFuture` fjernes
- Parallelle Eventor-kall skrives om til `coroutineScope { async { ... } + awaitAll() }`
- Timeout konfigureres ett sted: `requestTimeout` i `HttpClient`-oppsettet
- En `Semaphore` (standard: 10) begrenser antall samtidige Eventor-kall for å unngå overbelastning

### 2. Dependency injection: Koin → enkel Kotlin-klasse

`AppModule.kt` og Koin-avhengigheten fjernes. Erstattes med en `Dependencies`-klasse instansiert i `Application.kt`:

```kotlin
class Dependencies(config: ApplicationConfig) {
    private val semaphore = Semaphore(config.property("app.eventor.maxConcurrentRequests").getString().toInt())
    private val httpClient = HttpClient(CIO) { ... }
    val eventorService = EventorService(httpClient, semaphore)
    val eventService = EventService(eventorService, ...)
    // osv.
}
```

Ingen annotations, ingen rammeverk — bare Kotlin-initialisering.

### 3. Caching: forenklet oppsett

Fire Caffeine-cacher beholdes (to med 30 min TTL, to med 5 min TTL) men initialiseres direkte som felt i `Dependencies` uten Koin-named-bindings. TTL-verdier flyttes til `application.conf`.

### 4. JAXBContext-caching

`unmarshal`-funksjonen i `EventorService` lager i dag ny `JAXBContext` per kall. Erstattes med et statisk `ConcurrentHashMap<Class<*>, JAXBContext>` som populeres én gang per type:

```kotlin
private val jaxbContextCache = ConcurrentHashMap<Class<*>, JAXBContext>()

private inline fun <reified T> unmarshal(xml: String): T {
    val ctx = jaxbContextCache.getOrPut(T::class.java) { JAXBContext.newInstance(T::class.java) }
    return ctx.createUnmarshaller().unmarshal(xml.reader()) as T
}
```

### 5. Opprydding av støy

- `getGetPersonalStarts()` → `getPersonalStarts()`
- `getGetPersonalResults()` → `getPersonalResults()`
- Hardkodede timeouts (`20_000`, `30`) flyttes til `application.conf`
- Redundant `connectTimeout` på Java-klienten fjernes (var duplisert med request-timeout)

## Hva endres ikke

| Komponent | Begrunnelse |
|-----------|-------------|
| `services/converter/` | Domenekode, ingen forenkling nødvendig |
| Auth-plugin | Fungerer, JWT-validering beholdes uendret |
| Exposed-repositories | Fungerer, ingen forenkling nødvendig |
| Exception-hierarki | Ryddig som det er |
| Mappestruktur (`api/`, `services/`, `data/`, `model/`, `plugins/`) | Beholdes |
| Endepunktenes oppførsel og responser | Uendret utad |

## Mappestruktur etter endringer

```
src/main/kotlin/no/stunor/origo/eventorapi/
├── Application.kt          ← initialiserer Dependencies, registrerer plugins
├── Dependencies.kt         ← ny: enkel klasse med alle singletons
├── api/
│   └── EventorService.kt   ← Ktor HttpClient, Semaphore, JAXBContext-cache
├── services/
│   ├── EventService.kt     ← coroutines i stedet for CompletableFuture
│   ├── CalendarService.kt  ← coroutines i stedet for CompletableFuture
│   ├── PersonService.kt
│   └── converter/
├── data/                   ← uendret
├── model/                  ← uendret
├── plugins/                ← uendret (minus Koin-initialisering)
└── exception/              ← uendret
```

## Konfigurasjon (application.conf)

Nye verdier:

```hocon
app.eventor.maxConcurrentRequests = 10
app.eventor.requestTimeoutMs = 20000
app.eventor.batchTimeoutMs = 30000
app.cache.eventListTtlMinutes = 30
app.cache.competitorCountTtlMinutes = 5
app.cache.eventClassesTtlMinutes = 30
app.cache.organisationEntriesTtlMinutes = 5
```

## Avhengigheter som endres

| Avhengighet | Endring |
|-------------|---------|
| `koin-ktor`, `koin-core` | Fjernes |
| `ktor-client-core`, `ktor-client-cio` | Legges til |
| `ktor-client-content-negotiation` | Legges til (for auth-headers) |
| Øvrige avhengigheter | Uendret |

## Forventet effekt

- Parallelle Eventor-kall suspenderer coroutinen i stedet for å blokkere en tråd — bedre utnyttelse av Ktor's tråd-pool
- Semaphore sørger for at ikke for mange kall mot Eventor skjer samtidig (backpressure)
- JAXBContext-caching eliminerer gjentatt dyr initialisering per kall
- Kodebasen mister ett rammeverk (Koin) og ett concurrency-lag (CompletableFuture/ThreadPoolExecutor)
