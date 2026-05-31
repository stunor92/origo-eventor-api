# Eventor API

This Ktor web application converts Eventor IOF-XML data to JSON format for use in OriGo apps.
It acts as a bridge between Eventor's IOF-XML format and OriGo applications, providing REST endpoints to fetch and convert event data from multiple Eventor federations.

## Features

- **Multi-source entry list merging**: Combines entry, start, and result lists with intelligent deduplication and status tracking
- **Calendar view**: Fetches event calendars across all configured federations in parallel, with per-federation timeouts
- **Competitor counts**: Enriches calendar events with entry counts per class
- **Organisation pre-fetching**: Eliminates N+1 queries by loading all organisations for a federation in one query before conversion
- **In-memory caching**: Caffeine caches for event lists, event classes, and competitor counts
- **Multi-federation support**: Works with any Eventor instance (Norway, Sweden, etc.)

## Technology Stack

- **Language**: Kotlin
- **Framework**: Ktor 3.x
- **Build Tool**: Maven
- **Java Version**: Java 21
- **Database**: PostgreSQL with Jetbrains Exposed (DSL)
- **XML Processing**: JAXB for IOF-XML schema
- **Authentication**: JWT (Supabase)

## Prerequisites

- Java 21 or higher
- Maven 3.6.0 or higher
- PostgreSQL database

## Project Structure

```
src/
├── main/
│   ├── kotlin/no/stunor/origo/eventorapi/
│   │   ├── api/            # Eventor HTTP client (EventorService)
│   │   ├── data/           # Exposed-based repositories
│   │   ├── exception/      # Custom exceptions
│   │   ├── model/          # Domain models
│   │   │   ├── calendar/   # Calendar / race models
│   │   │   ├── event/      # Event-related models
│   │   │   ├── organisation/
│   │   │   └── person/
│   │   ├── plugins/        # Ktor plugin wiring (routing, auth, serialisation)
│   │   ├── services/       # Business logic
│   │   │   └── converter/  # IOF-XML → domain model converters
│   │   └── validation/     # Input validation
│   └── resources/
│       ├── IOF.xsd         # IOF-XML schema (JAXB source)
│       └── application.conf # HOCON configuration
└── test/
    └── kotlin/             # Tests
```

## Architecture

### Data Flow

1. **HTTP Request** → Ktor routing (`plugins/Routing.kt`)
2. **Service Layer** → `EventService` / `CalendarService` / `PersonService`
3. **Eventor API** → `EventorService` calls external Eventor HTTP API
4. **Converters** → Transform IOF-XML objects to domain models
5. **Repository** → Persist/retrieve supporting data (persons, organisations, events) from PostgreSQL
6. **Response** → Return JSON to client

### Key Components

- **EventService**: Fetches event details and entry lists from Eventor, merges entry/start/result data
- **CalendarService**: Fetches event calendars across all federations in parallel with timeout handling
- **PersonService**: Authenticates an Eventor person and persists the association to the app user
- **EventorService**: Thin HTTP client for the Eventor REST API with Caffeine caching and semaphore-based rate limiting
- **Repositories**: Exposed DSL-based access to PostgreSQL (persons, organisations, events, fees, etc.)
- **Converters**: Stateless transformers from IOF-XML JAXB objects to domain models; accept an `orgCache` map to avoid per-entry DB lookups

## Configuration

The application uses HOCON (`application.conf`). All secrets are supplied via environment variables; the file ships safe defaults for local development.

### Environment Variables

| Variable | Default (local) | Description |
|---|---|---|
| `PORT` | `8080` | HTTP listen port |
| `SUPABASE_URL` | `http://127.0.0.1:54321` | Supabase URL (used for JWT verification) |
| `POSTGRES_DB` | `jdbc:postgresql://127.0.0.1:54322/postgres` | JDBC connection URL |
| `POSTGRES_USER` | `postgres` | Database username |
| `POSTGRES_PASSWORD` | `postgres` | Database password |

### Eventor tuning (in `application.conf`)

```hocon
app.eventor {
    maxConcurrentRequests = 10   # semaphore permits for outbound HTTP
    requestTimeoutMs      = 20000
    batchTimeoutMs        = 30000  # per-federation timeout in CalendarService
}
```

### Cache TTLs (in `application.conf`)

```hocon
app.cache {
    eventListTtlMinutes          = 30
    competitorCountTtlMinutes    = 5
    eventClassesTtlMinutes       = 30
}
```

## Build & Run

```bash
# Generate JAXB classes from IOF.xsd + calendar models from OpenAPI spec, then compile
mvn generate-sources compile

# Full build with tests
mvn clean verify

# Build runnable fat jar
mvn clean package

# Run via Maven
mvn exec:java

# Run fat jar
java -jar target/eventor-api-*.jar
```

## API Documentation

The API contract is defined in [`src/main/resources/openapi/openapi.yaml`](src/main/resources/openapi/openapi.yaml).
Swagger UI is served at **`/swagger-ui`** when the application is running — endpoints, parameters, request/response schemas and authentication requirements are all documented there.

```
http://localhost:8080/swagger-ui
```

## Development Notes

### Generated sources

The project has two code generation steps, both triggered by `mvn generate-sources`:

**JAXB** — Java classes from `IOF.xsd` (IOF-XML parsing):
- Output: `target/generated-sources/jaxb/`
- Re-run after schema changes

**OpenAPI** — Kotlin calendar response models from `src/main/resources/openapi/openapi.yaml`:
- Output: `target/generated-sources/openapi/`
- Re-run after spec changes
- Do not manually edit either set of generated classes

### Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=EventClassRepositoryTest
```

### Repository Pattern

Repositories use Jetbrains Exposed `transaction {}` blocks with the DSL API:
- Upsert via `Table.upsert {}` with natural-key conflict targets
- Batch queries use `inList` predicates to avoid N+1 patterns
- `OrganisationRepository.findAllByEventorId()` does a single LEFT JOIN with `RegionTable` and returns a `Map<eventorRef, Organisation>` used as an in-memory cache throughout conversion

## Contributing

This project follows [Conventional Commits](https://www.conventionalcommits.org/) for commit messages and PR titles.

**Allowed types:** `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`

### Contribution Steps

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Make changes with appropriate tests
4. Ensure the build passes: `mvn clean verify`
5. Commit using conventional commit format
6. Push and open a Pull Request

## Release Process

This project uses [Release Please](https://github.com/googleapis/release-please) to automate releases:

- Release Please opens PRs titled `chore(main): release X.Y.Z`
- Release PRs ending with **SNAPSHOT** are automatically merged once checks pass
- After merge, a GitHub release is created and Docker images are published to GHCR
- Version numbers follow [Semantic Versioning](https://semver.org/)

## Resources

- [OpenAPI Spec](src/main/resources/openapi/openapi.yaml) — API contract (also browsable via Swagger UI at `/swagger-ui`)
- [Eventor API Documentation](https://eventor.orienteering.org/api) — Official Eventor API docs
- [IOF XML Schema](https://github.com/international-orienteering-federation/datastandard-v3) — IOF data standard
- [Ktor Documentation](https://ktor.io/docs) — Ktor framework reference
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html) — Kotlin language reference
- [Bruno](https://www.usebruno.com/) — API testing client (collection in `bruno/` directory)

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
