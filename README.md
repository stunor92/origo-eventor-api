# Eventor API Converter

This Spring Boot web application converts Eventor IOF-XML files to JSON format, which is used in OriGo apps.
The application provides REST API endpoints to fetch and convert event data from various Eventor federations.

## Overview

The OriGo EventorApi acts as a bridge between Eventor's IOF-XML format and OriGo applications, providing:
- Event information (details, classes, documents)
- Entry lists with intelligent merging from multiple sources
- Start lists with timing information
- Result lists with split times
- Fee structures with class associations
- Organization and participant data

## Features

- **Multi-source data merging**: Intelligently combines entry lists, start lists, and result lists
- **Event management**: Supports single race events, multi-race events, and relay events
- **Fee handling**: Manages entry fees with class-specific pricing
- **Database persistence**: Caches event data for improved performance
- **RESTful API**: Clean JSON responses for easy integration
- **Multi-federation support**: Works with different Eventor federations (Norway, Sweden, etc.)

## Technology Stack

- **Language**: Kotlin (primary) with Java (JAXB-generated classes)
- **Framework**: Spring Boot 4.0.0
- **Build Tool**: Maven
- **Java Version**: Java 21
- **Database**: PostgreSQL with JDBC (migrated from JPA/Hibernate)
- **API Documentation**: SpringDoc OpenAPI (Swagger)
- **XML Processing**: JAXB for IOF-XML schema
- **Authentication**: JWT tokens

## Prerequisites

- Java 21 or higher
- Maven 3.6.0 or higher
- PostgreSQL database

## Project Structure

```
src/
├── main/
│   ├── kotlin/no/stunor/origo/eventorapi/
│   │   ├── api/                    # External API clients (Eventor API)
│   │   ├── controller/             # REST controllers
│   │   ├── data/                   # JDBC repositories
│   │   ├── exception/              # Custom exceptions
│   │   ├── model/                  # Data models and domain objects
│   │   │   ├── event/              # Event-related models
│   │   │   ├── organisation/       # Organization models
│   │   │   └── person/             # Person and membership models
│   │   ├── persistence/            # Custom Hibernate types
│   │   │   └── hibernate/          # Array and enum type handlers
│   │   ├── security/               # JWT and authentication
│   │   └── services/               # Business logic
│   │       └── converter/          # IOF-XML to JSON converters
│   └── resources/
│       ├── IOF.xsd                 # IOF-XML schema (JAXB source)
│       ├── application.yml         # Production configuration
│       └── application-local.yml   # Local development config (not in git)
└── test/
    └── kotlin/                     # Unit and integration tests
```

## Architecture

### Data Flow

1. **API Request** → REST Controller receives request
2. **Service Layer** → EventService orchestrates data fetching
3. **Eventor API** → EventorService calls external Eventor API
4. **Converters** → Transform IOF-XML to domain models
5. **Repository** → Persist/retrieve data from PostgreSQL
6. **Response** → Return JSON to client

### Key Components

- **EventService**: Main business logic for event data management
  - Fetches events from Eventor API
  - Merges entry lists from multiple sources (entry/start/result)
  - Handles fee and class associations
  - Intelligent deduplication and status tracking

- **Repositories**: JDBC-based data access layer
  - EventRepository: Event CRUD operations
  - EventClassRepository: Event class management
  - FeeRepository: Fee structure management
  - PersonRepository, OrganisationRepository, etc.

- **Converters**: Transform Eventor IOF-XML to JSON
  - EventConverter: Event details
  - EntryListConverter, StartListConverter, ResultListConverter
  - FeeConverter: Entry fee structures

### Database Schema

The application uses PostgreSQL with custom enum types:
- `event_type`, `event_classification`, `event_status`
- `class_type`, `class_gender`
- Array types for disciplines, punching units, timestamps

Key tables:
- `event`: Event information
- `class`: Event classes
- `fee`: Entry fees with many-to-many to classes
- `person`, `organisation`: Participant data
- `user`: Application users with JWT authentication

## Configuration

The application uses Spring profiles for configuration:
- `application.yml` - Production configuration using environment variables
- `application-local.yml` - Local development configuration (not in version control)
- `application-local.yml.example` - Template for local development configuration

### Local Development Setup

1. Copy the example configuration file:
   ```bash
   cp src/main/resources/application-staging.yml src/main/resources/application-staging.yml
   ```

2. Update `application-local.yml` with your local values:
   - Set your local PostgreSQL password
   - Set a JWT secret (minimum 32 characters)

3. The `application-local.yml` file is ignored by git to prevent committing credentials

### Required Environment Variables (Production)

- `POSTGRES_DB` - PostgreSQL database connection URL
- `POSTGRES_USER` - Database username
- `POSTGRES_PASSWORD` - Database password
- `JWT_SECRET` - Secret key for JWT token signing (minimum 32 characters)

### Security Notes

⚠️ **Important**: Never commit actual secrets or production credentials to version control.

- Use the `application-local.yml.example` template for local development
- The actual `application-local.yml` file is ignored by git
- Always use environment variables for production deployments
- Never commit credentials, API keys, or secrets to the repository

## Recent Changes

### Spring Boot 4.0.0 Migration (December 2025)

The project has been upgraded to Spring Boot 4.0.0 and migrated from JPA/Hibernate to native JDBC:

**Database Layer Changes:**
- Migrated from JPA entities to JDBC-based repositories using `JdbcTemplate`
- Implemented custom Hibernate types for PostgreSQL arrays and enums
- Added `EventClassRepository` for managing event classes
- Improved query performance with direct SQL

**Benefits:**
- Better control over SQL queries and performance
- Simplified array and enum handling with PostgreSQL
- Reduced complexity and boilerplate code
- More predictable transaction behavior

**Breaking Changes:**
- Removed JPA annotations from models
- Repository interfaces now use custom JDBC implementations
- Some test annotations changed (e.g., `@DataJpaTest` no longer used)

### Entry List Merging

The `EventService` now implements intelligent entry list merging:

**Strategy:**
1. Fetch entry list (registered participants)
2. Fetch start list as fallback (if entry list is empty)
3. Fetch result list (actual participants with results)
4. Merge data with result taking priority
5. Mark registered but non-participating entries as "Deregistered"

**Deduplication:**
- Primary key: Person ID or Team name
- Fallback: Composite key (name + organization + class + race)
- Handles split times, punching units, and team members

## Development Notes

### Working with JAXB

The project generates Java classes from `IOF.xsd`:
- Generated classes are in `target/generated-sources/jaxb/`
- Don't manually edit generated classes
- Run `mvn generate-sources` after schema changes

### Database Enums

PostgreSQL enum types must match Kotlin enum values:
- `event_classification` → `EventClassificationEnum`
- `event_status` → `EventStatusEnum`
- `event_type` → `EventFormEnum`
- `class_type` → `EventClassTypeEnum`
- `class_gender` → `ClassGender`

### Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=EventClassRepositoryTest

# Run with debug output
mvn test -X
```

### Common Issues

**Issue**: `Unresolved reference 'WebMvcTest'`
- Some old test files use deprecated Spring Boot test annotations
- These are being gradually updated to `@SpringBootTest`


## Build the project
```bash
# Clean and compile
mvn clean compile

# Generate JAXB classes from IOF.xsd
mvn generate-sources

# Full build with tests
mvn clean install
```

## Run the application
```bash
# Run with local profile
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Run tests
mvn test
```

## API Endpoints

### Event Endpoints

- `GET /api/eventor/{eventorId}/event/{eventId}` - Get event details with classes and fees
- `GET /api/eventor/{eventorId}/event/{eventId}/entries` - Get entry list with merged data
- `GET /api/eventor/{eventorId}/event/{eventId}/starts` - Get start list
- `GET /api/eventor/{eventorId}/event/{eventId}/results` - Get result list

### Person Endpoints

- `GET /api/eventor/{eventorId}/person/{personId}/events` - Get person's events

### Organization Endpoints

- `GET /api/eventor/{eventorId}/organisation/{organisationId}` - Get organization details

### Authentication Endpoints

- `POST /api/auth/login` - Authenticate and receive JWT token
- `POST /api/users` - Register new user

### API Documentation

When running locally, access the interactive API documentation at:
- Swagger UI: `http://localhost:8080/rest/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/rest/v3/api-docs`

## Usage Examples

### Using Bruno (Recommended)

The project includes a Bruno collection in the `bruno/` directory with pre-configured requests for all endpoints.

### Using curl

```bash
# Get event details
curl -X GET "http://localhost:8080/rest/api/eventor/NOR/event/12345"

# Get entry list
curl -X GET "http://localhost:8080/rest/api/eventor/NOR/event/12345/entries"

# Authenticate
curl -X POST "http://localhost:8080/rest/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"password"}'
```

## Contributing

This project follows [Conventional Commits](https://www.conventionalcommits.org/) specification for commit messages and PR titles.

### Code Conventions

**Kotlin Style:**
- Use Kotlin idioms and conventions
- Prefer data classes for models
- Use nullable types (`?`) appropriately
- Use `when` expressions over `if-else` chains
- Follow Spring Boot annotation conventions

**Testing:**
- Use JUnit 5 for tests
- Use MockK for Kotlin mocking
- Test class names should end with `Test`
- Use descriptive test names with backticks: `` `test description` ``
- Mock external dependencies (repositories, services)

**Repository Pattern:**
- Use `JdbcTemplate` for database operations
- Implement upsert logic with `ON CONFLICT DO UPDATE`
- Create custom `RowMapper` for entity mapping
- Handle nullable fields appropriately

### Commit Message Format

All PR titles must follow the Conventional Commits format:

```
<type>: <description>
```

or with optional scope:

```
<type>(<scope>): <description>
```

**Allowed types:**
- `feat`: A new feature
- `fix`: A bug fix
- `docs`: Documentation only changes
- `style`: Changes that do not affect the meaning of the code (white-space, formatting, etc)
- `refactor`: A code change that neither fixes a bug nor adds a feature
- `perf`: A code change that improves performance
- `test`: Adding missing tests or correcting existing tests
- `build`: Changes that affect the build system or external dependencies
- `ci`: Changes to CI configuration files and scripts
- `chore`: Other changes that don't modify src or test files
- `revert`: Reverts a previous commit

**Examples:**
- `feat: add support for relay events`
- `fix: correct time calculation in results`
- `docs: update API documentation`
- `chore(deps): update spring boot to 4.0.0`

### Contribution Steps

1. Fork the repository
2. Create a new branch (`git checkout -b feature-branch`)
3. Make your changes with appropriate tests
4. Ensure the build passes: `mvn clean install`
5. Commit your changes using conventional commit format
6. Push to the branch (`git push origin feature-branch`)
7. Create a new Pull Request with a title following the conventional commit format

## Release Process

This project uses [Release Please](https://github.com/googleapis/release-please) to automate releases:

- Release Please creates PRs with titles like `chore(main): release X.Y.Z`
- Release PRs ending with **SNAPSHOT** are **automatically merged** once all required checks pass
- After merge, a new release is created and Docker images are published to GHCR
- Version numbers follow [Semantic Versioning](https://semver.org/)

## Resources

### Documentation
- [GitHub Copilot Instructions](.github/copilot-instructions.md) - Detailed development guidelines
- [Contributing Guide](CONTRIBUTING.md) - Contribution guidelines
- [Security Policy](SECURITY.md) - Security and vulnerability reporting
- [Changelog](CHANGELOG.md) - Version history and changes

### External Resources
- [Eventor API Documentation](https://eventor.orienteering.org/api) - Official Eventor API docs
- [IOF XML Schema](https://github.com/international-orienteering-federation/datastandard-v3) - IOF data standard
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/) - Spring Boot reference
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html) - Kotlin language reference

### Tools
- [Bruno](https://www.usebruno.com/) - API testing client (collection included in `bruno/` directory)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/) - Recommended IDE for Kotlin/Spring development

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
