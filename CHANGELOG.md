# Changelog

## [14.1.0](https://github.com/stunor92/origo-eventor-api/compare/v14.0.7...v14.1.0) (2026-05-31)


### Features

* add global personal event-list endpoint across all Eventors ([889fa83](https://github.com/stunor92/origo-eventor-api/commit/889fa83554b2ac3ec1f35f31468a12b1ec9b99f4))
* add personal event-list endpoint with user entries and results ([86a044f](https://github.com/stunor92/origo-eventor-api/commit/86a044f8caead433eec6e61edd469ebe7439a786))
* API-first setup with OpenAPI spec and Swagger UI ([8b39d70](https://github.com/stunor92/origo-eventor-api/commit/8b39d707bc0dff67784d4deb7926914d33b42d2c))
* return PersonalCalendarRace from personal event-list endpoint ([fae0d10](https://github.com/stunor92/origo-eventor-api/commit/fae0d1030ff0ef23532ef9f53c57bd75b89cc23e))


### Bug Fixes

* make competitor counts best-effort with separate short timeout ([9d5f1d1](https://github.com/stunor92/origo-eventor-api/commit/9d5f1d1a27bf4daef6353cba1e19ab8b4d59710a))


### Performance Improvements

* replace N+1 enrichment with person-based Eventor API calls ([88eebe3](https://github.com/stunor92/origo-eventor-api/commit/88eebe3a51823b81c83e56fafed49856be16ffe2))


### Documentation

* replace hardcoded endpoint table with Swagger UI reference ([2060d25](https://github.com/stunor92/origo-eventor-api/commit/2060d259328c978481ec54d8417d30a19e836241))

## [14.0.7](https://github.com/stunor92/origo-eventor-api/compare/v14.0.6...v14.0.7) (2026-05-30)


### Bug Fixes

* timeout import ([a24407f](https://github.com/stunor92/origo-eventor-api/commit/a24407fd2f66fdaaee767da2d0fcb7b1094cfe74))

## [14.0.6](https://github.com/stunor92/origo-eventor-api/compare/v14.0.5...v14.0.6) (2026-05-30)


### Performance Improvements

* per-request 6s timeout on calendar HTTP calls, batch limit 8s ([59ac95d](https://github.com/stunor92/origo-eventor-api/commit/59ac95dfbe1ff4275d04ba425978b042f321f2f5))

## [14.0.5](https://github.com/stunor92/origo-eventor-api/compare/v14.0.4...v14.0.5) (2026-05-30)


### Bug Fixes

* add batch timeout to single-eventor event-list and reduce to 15s ([ea89a81](https://github.com/stunor92/origo-eventor-api/commit/ea89a8119415a2a0983620d6ba404eab10cc2a0d))

## [14.0.4](https://github.com/stunor92/origo-eventor-api/compare/v14.0.3...v14.0.4) (2026-05-30)


### Bug Fixes

* Prometheus imports ([95ca41d](https://github.com/stunor92/origo-eventor-api/commit/95ca41d74bbb39bd0a6b518f40578a8543688b92))

## [14.0.3](https://github.com/stunor92/origo-eventor-api/compare/v14.0.2...v14.0.3) (2026-05-30)


### Performance Improvements

* add Micrometer/Prometheus metrics and fix HTTP timeout overlap ([c77d459](https://github.com/stunor92/origo-eventor-api/commit/c77d45958d4700b6c350d01856a2c2ac4def1476))

## [14.0.2](https://github.com/stunor92/origo-eventor-api/compare/v14.0.1...v14.0.2) (2026-05-30)


### Bug Fixes

* correct timeout handling and increase competitor-count cache TTL ([a697be8](https://github.com/stunor92/origo-eventor-api/commit/a697be80c191de4e983757865cdd97e30a0559a8))


### Performance Improvements

* reduce batchTimeoutMs default to 20s and add env var override ([ce2f98d](https://github.com/stunor92/origo-eventor-api/commit/ce2f98dd9362cf763b08a5da9917138848cd4dff))

## [14.0.1](https://github.com/stunor92/origo-eventor-api/compare/v14.0.0...v14.0.1) (2026-05-30)


### Performance Improvements

* remove per-event getEventClasses N+1 from event-list ([1cd8316](https://github.com/stunor92/origo-eventor-api/commit/1cd83161ad575e7bc79c9dca23ecddd04bed524c))


### Documentation

* rewrite README to reflect current Ktor/Exposed stack ([4cf2c96](https://github.com/stunor92/origo-eventor-api/commit/4cf2c96afd2cda265e224b7e5fda2aedaf9eb312))

## [14.0.0](https://github.com/stunor92/origo-eventor-api/compare/v13.1.0...v14.0.0) (2026-05-30)


### ⚠ BREAKING CHANGES

* migrate to Ktor and exposed ([#329](https://github.com/stunor92/origo-eventor-api/issues/329))

### Features

* add Dependencies class to replace Koin ([13f3a5f](https://github.com/stunor92/origo-eventor-api/commit/13f3a5fb426b2bbf5253d32bb7d82ce3720b8da1))
* load .env file at startup via dotenv-kotlin ([3d92c7a](https://github.com/stunor92/origo-eventor-api/commit/3d92c7ad25e2961f03e53045cf8b4499d6452a40))
* migrate to Ktor and exposed ([#329](https://github.com/stunor92/origo-eventor-api/issues/329)) ([d66fac2](https://github.com/stunor92/origo-eventor-api/commit/d66fac299b315ceed117d879266ef397262dee2e))


### Bug Fixes

* declare primaryKey on all Exposed Table objects ([b670359](https://github.com/stunor92/origo-eventor-api/commit/b670359cfb2ebdadcaf8655ed6f3b0cba1d53e00))
* nomalized tables ([4593187](https://github.com/stunor92/origo-eventor-api/commit/45931877e053da78a7f4a789946793e9a3eb3ecd))
* parse PostgreSQL array notation in disciplines and punching types ([a5cec54](https://github.com/stunor92/origo-eventor-api/commit/a5cec54dbce4a6d61c83daa644e7986ae4e322e9))
* read and write webUrls as PostgreSQL array literal ([193271f](https://github.com/stunor92/origo-eventor-api/commit/193271ff2dfab1d19225efff8a2968a19629b6cb))
* remove original JAR before copy ([#331](https://github.com/stunor92/origo-eventor-api/issues/331)) ([8951aca](https://github.com/stunor92/origo-eventor-api/commit/8951aca3a657549e5b35c3433ddafe3b68dd95c2))
* remove unused batchTimeoutMs from EventService, fix Health.kt type error ([360d2ea](https://github.com/stunor92/origo-eventor-api/commit/360d2ea8105afa727a6b7de610b6c822ca26cee4))
* use computeIfAbsent for atomic JAXBContext caching ([f64f61e](https://github.com/stunor92/origo-eventor-api/commit/f64f61eb8c645858c4c0168785b61f71fc698bdc))
* use http for localhost Bruno environment ([e7b2f52](https://github.com/stunor92/origo-eventor-api/commit/e7b2f52d8af0c1b07e58b56017b8e3aaffcc0ec7))
* use Maven wrapper in Dockerfile ([#330](https://github.com/stunor92/origo-eventor-api/issues/330)) ([65086df](https://github.com/stunor92/origo-eventor-api/commit/65086df4cab579a48f759e64e2b69aa018231054))
* use natural keys as upsert conflict targets instead of generated id ([1aed657](https://github.com/stunor92/origo-eventor-api/commit/1aed6579c090d6f3b7fceb594059769833336ff1))
* wrap blocking converter calls in Dispatchers.IO, align coroutines-test version ([4e68d2f](https://github.com/stunor92/origo-eventor-api/commit/4e68d2ffc90288d0556a78a6fd8e302570136f98))
* write disciplines and punching types as PostgreSQL array literals ([82badbe](https://github.com/stunor92/origo-eventor-api/commit/82badbeb693ffe0a43e75406595d255333852d3c))


### Performance Improvements

* eliminate N+1 DB queries in entry-list and remove dead code ([7aa1f9c](https://github.com/stunor92/origo-eventor-api/commit/7aa1f9c3208a4bdbe2cd3abee9b98b003dc00866))


### Documentation

* add design spec for simplification and Eventor concurrency optimization ([8150794](https://github.com/stunor92/origo-eventor-api/commit/8150794fc211cbcc14b86f1d2d0bc2e04a1c2c77))
* add implementation plan for simplification and Eventor optimization ([bd56168](https://github.com/stunor92/origo-eventor-api/commit/bd561685ccad241d2b9c507700e615f6a6ce24bb))

## [13.1.0](https://github.com/stunor92/origo-eventor-api/compare/v13.0.0...v13.1.0) (2026-05-05)


### Features

* remove personal-races service due to performance issue ([1355e56](https://github.com/stunor92/origo-eventor-api/commit/1355e56321c1b3065a8b3bb1aaa774ea2bda6394))
* send eventClass object in CalendarRace.kt ([88de931](https://github.com/stunor92/origo-eventor-api/commit/88de9317f8c8273e10eb968bf7670b3f7b9a86a6))
* send eventClass object in entry ([d79d119](https://github.com/stunor92/origo-eventor-api/commit/d79d119997e3826dbfa889d8524ef81f1a859ed5))


### Bug Fixes

* Fix cache config ([313bd2c](https://github.com/stunor92/origo-eventor-api/commit/313bd2c8c8d6e448ed7a265c09c17300a041c86b))
* fix jdk distro to liberica ([9f08a93](https://github.com/stunor92/origo-eventor-api/commit/9f08a931f37ea6bbaa54cd294823a6307f0499d5))
* handle decimals on eventClass ([941e02e](https://github.com/stunor92/origo-eventor-api/commit/941e02eecae1755a0ea6e01b3c186f1af4b978c1))
* remove not needed test for calendar-converting ([a110d96](https://github.com/stunor92/origo-eventor-api/commit/a110d96f54a98053b117177dc89cef7d16e6e03f))
* Revert LinkedBlockingQueue change ([578b93b](https://github.com/stunor92/origo-eventor-api/commit/578b93bb7ca507826118a0ab1dd0dea613088486))


### Performance Improvements

* Add logging of time for calender-requests ([916a9cb](https://github.com/stunor92/origo-eventor-api/commit/916a9cb0981d497a205446b16034b56aa4a02ed4))
* Implement caching of eventor-data and graceful return partial result instead of 500 error ([7eda3c8](https://github.com/stunor92/origo-eventor-api/commit/7eda3c8e824342a71c1513dd4c14a699ef2eb1a9))
* parallelize Eventor API calls for entry-list and event-list/me ([#325](https://github.com/stunor92/origo-eventor-api/issues/325)) ([b5cddac](https://github.com/stunor92/origo-eventor-api/commit/b5cddac1343b2ce9382cebdc7f528a80b8a74a2b))

## [2.0.0](https://github.com/stunor92/origo-eventor-api/compare/v12.0.10...v2.0.0) (2026-05-03)


### ⚠ BREAKING CHANGES

* delete delete endpoint that should be handled in supabase. ([#269](https://github.com/stunor92/origo-eventor-api/issues/269))
* migrate to jdk 25 ([#258](https://github.com/stunor92/origo-eventor-api/issues/258))
* migrate from JPA/Hibernate to Spring JDBC ([#239](https://github.com/stunor92/origo-eventor-api/issues/239))
* Use generated uuid as id on entities ([#192](https://github.com/stunor92/origo-eventor-api/issues/192))
* Fees is not relevant to return in responses for this application
* Use more inlined objects instead of references
* Migated the event-endpoint to supabase
* Migated the event-endpoint to supabase
* Migrate auth person to use supabase ([#107](https://github.com/stunor92/origo-eventor-api/issues/107))
* Rollback refactoring ([#88](https://github.com/stunor92/origo-eventor-api/issues/88))
* Deprecate one punchingUnit and use list instead ([#86](https://github.com/stunor92/origo-eventor-api/issues/86))
* try to fix the prod deploy pipeline
* upgrade to java 23

### Features

* Add api for delete person ([896f2c4](https://github.com/stunor92/origo-eventor-api/commit/896f2c4dbecc3e147498a2019214dc56c2967c4c))
* add auto-merge workflow for SNAPSHOT release PRs ([#219](https://github.com/stunor92/origo-eventor-api/issues/219)) ([67571d8](https://github.com/stunor92/origo-eventor-api/commit/67571d8556eea6aa56751514e23a7598fbbfac8d))
* Add automatic release via prof branch ([55ee347](https://github.com/stunor92/origo-eventor-api/commit/55ee34757593825f75f4a6ef3ef7ff942f4519d3))
* add release-please json files ([02193d8](https://github.com/stunor92/origo-eventor-api/commit/02193d81b9f04214ad35d024c6c9eacafaee0b53))
* also incliude org-ids on entrylist ([373c1fe](https://github.com/stunor92/origo-eventor-api/commit/373c1fec80dd33836b5b08640cffa57b4418a072))
* Automatic download fees when fetching event ([89e5bb1](https://github.com/stunor92/origo-eventor-api/commit/89e5bb19244e69c8c8f176f2a51491e835a15eaf))
* back to tag triggered deploy ([1895535](https://github.com/stunor92/origo-eventor-api/commit/189553522a810b244f712e723b37ff2033a96fcc))
* checkout code after release-please ([57bc13b](https://github.com/stunor92/origo-eventor-api/commit/57bc13b5651481342bbcd09936a5676a8676e970))
* checkout code before release-please ([6a5bd42](https://github.com/stunor92/origo-eventor-api/commit/6a5bd42c117d262f5704778a24ea2df3f0e88b58))
* Cleanup application config and connect to local db locally ([7c14526](https://github.com/stunor92/origo-eventor-api/commit/7c14526e5955b63a347e97d5212fe4a5bdf8bf1c))
* Codeql scan on all branch ([#181](https://github.com/stunor92/origo-eventor-api/issues/181)) ([3f432a7](https://github.com/stunor92/origo-eventor-api/commit/3f432a7fb03e07215eef9334c0d2f882a5df0864))
* Deprecate one punchingUnit and use list instead ([#86](https://github.com/stunor92/origo-eventor-api/issues/86)) ([da15428](https://github.com/stunor92/origo-eventor-api/commit/da15428501285062ca9be1e93056c26bda45f566))
* Fees is not relevant to return in responses for this application ([d33da27](https://github.com/stunor92/origo-eventor-api/commit/d33da2718db93638926f2d5a85a0588ef20e2ea1))
* Fees is updated in database when a event is fetched ([3ffaf8a](https://github.com/stunor92/origo-eventor-api/commit/3ffaf8a36cd75de7e109a1a2f0abf7e44ade1ede))
* fix release-please-config.json with package-name ([a8b7e8e](https://github.com/stunor92/origo-eventor-api/commit/a8b7e8e92c0bf91ec6f4c09fa2d911c9dd31efad))
* implemented disconnect eventor-person and delete user ([de7f7d6](https://github.com/stunor92/origo-eventor-api/commit/de7f7d6847d33b81c2974fb06879226c833ee1e5))
* Improve ci-pipeline ([#184](https://github.com/stunor92/origo-eventor-api/issues/184)) ([db39b0d](https://github.com/stunor92/origo-eventor-api/commit/db39b0d7d13161c5adff94150c5472cad7f0a4b2))
* Migated the event-endpoint to supabase ([ac04f3b](https://github.com/stunor92/origo-eventor-api/commit/ac04f3bb16fcc5a5aba4a8c3a737ca5ab19975d8))
* Migated the event-endpoint to supabase ([c29a9e3](https://github.com/stunor92/origo-eventor-api/commit/c29a9e3c651fd722fdab2cbbc445a1709776dc30))
* Migrate auth person to use supabase ([#107](https://github.com/stunor92/origo-eventor-api/issues/107)) ([2c998eb](https://github.com/stunor92/origo-eventor-api/commit/2c998eb10312fb5a7a76f40c36e02c9d8ec3c5a0))
* migrate from JPA/Hibernate to Spring JDBC ([#239](https://github.com/stunor92/origo-eventor-api/issues/239)) ([17bb95f](https://github.com/stunor92/origo-eventor-api/commit/17bb95f1766cca3be8735c266764259e645721fb))
* modify release.yml ([f2b0545](https://github.com/stunor92/origo-eventor-api/commit/f2b05457c7ded757dbebc53545281d49f05b5f4c))
* more pipeline stuff ([b0e2e50](https://github.com/stunor92/origo-eventor-api/commit/b0e2e50d2d7ea19a64b6c1e50da67a994607b257))
* Remove deploy with GHA ([#104](https://github.com/stunor92/origo-eventor-api/issues/104)) ([92f5a7d](https://github.com/stunor92/origo-eventor-api/commit/92f5a7d71369b9bd426a6bd007e37632fed0238c))
* send only organisation with id in response ([ee29d85](https://github.com/stunor92/origo-eventor-api/commit/ee29d858fcce6d56edc3a9e79ec5970df929ba04))
* support multiple punchingUnits in event ([ad405f7](https://github.com/stunor92/origo-eventor-api/commit/ad405f79b305567d2fa86f1f5e2499996382f3ac))
* support multiple punchingUnits in event ([4660031](https://github.com/stunor92/origo-eventor-api/commit/466003184f93aa993311b8faca303e07dc7c4fb6))
* testing release ([939ae57](https://github.com/stunor92/origo-eventor-api/commit/939ae57a7a6bd181fb63044ad4358cf9105f86d9))
* try again ([c90bc44](https://github.com/stunor92/origo-eventor-api/commit/c90bc44408c501763e8a85c9a35c43941485c1c0))
* update release-please-config.json ([650f7b7](https://github.com/stunor92/origo-eventor-api/commit/650f7b798b60fe6e17e8c054f1b7bc0d918aff28))
* upgrade to java 23 ([cedccf7](https://github.com/stunor92/origo-eventor-api/commit/cedccf73e393ef6d3a8222e63a9345336f592ac1))
* upgrade to java 23 also in dockerfile ([6bfaf6d](https://github.com/stunor92/origo-eventor-api/commit/6bfaf6d5ebe7e47440c6df6ff052f529537776ad))
* upgrade to java 23 also in dockerfile ([13431e5](https://github.com/stunor92/origo-eventor-api/commit/13431e507746c1bc627dce152f50860ca24cc1e2))
* Use generated uuid as id on entities ([#192](https://github.com/stunor92/origo-eventor-api/issues/192)) ([d38215b](https://github.com/stunor92/origo-eventor-api/commit/d38215b1479a0fea43313e6660f822ff3e104d1f))
* Use joining table for organisers ([b6e5e91](https://github.com/stunor92/origo-eventor-api/commit/b6e5e9128b564db074eaf668361e6a0e4c2b686b))
* Use more inlined objects instead of references ([611f581](https://github.com/stunor92/origo-eventor-api/commit/611f5810a59a6f5cf74b20b2a703cc498744e9d3))


### Bug Fixes

* add config file ([313d9d3](https://github.com/stunor92/origo-eventor-api/commit/313d9d38b4e06baa6be7cf347b684941becded01))
* Add hypersistence again ([#186](https://github.com/stunor92/origo-eventor-api/issues/186)) ([22e5e71](https://github.com/stunor92/origo-eventor-api/commit/22e5e71c89bd81cf72af53a1097b0c9c8edab0b6))
* add manifest file ([df2a1b6](https://github.com/stunor92/origo-eventor-api/commit/df2a1b6a3b271e4ddcb54c1f6a0c8701ca6ab02a))
* Add missing depencency ([838ed44](https://github.com/stunor92/origo-eventor-api/commit/838ed44969d30fd265090b78368c07cb1571be40))
* add name to release-please step ([ac0a488](https://github.com/stunor92/origo-eventor-api/commit/ac0a48879d14199af0f7fd438cbe719fac9fbf3b))
* Add punching-units on start and result list ([#200](https://github.com/stunor92/origo-eventor-api/issues/200)) ([023bdfe](https://github.com/stunor92/origo-eventor-api/commit/023bdfece95f5579ec67e2bd099b7bd7f7bda186))
* add spring-context framework ([56f401b](https://github.com/stunor92/origo-eventor-api/commit/56f401ba1a778436dc74e2b24578b7af3b038547))
* add timestamp for saved person ([#254](https://github.com/stunor92/origo-eventor-api/issues/254)) ([dabd972](https://github.com/stunor92/origo-eventor-api/commit/dabd972926f5602ef974dfbca9d688d82ab4b150))
* app handles invalid tokens with fallback to no auth ([#277](https://github.com/stunor92/origo-eventor-api/issues/277)) ([bbc387c](https://github.com/stunor92/origo-eventor-api/commit/bbc387cc9ec845fd77c7d75bcd174794545d12e1))
* back to simple setup ([9ce54b7](https://github.com/stunor92/origo-eventor-api/commit/9ce54b76016ce8ad44bef6e3b8be49a24ad1ba22))
* batch load memberships in sql query ([#265](https://github.com/stunor92/origo-eventor-api/issues/265)) ([8bb8844](https://github.com/stunor92/origo-eventor-api/commit/8bb8844a56d6ed3f82655f94c25d15745630732f))
* Cast uuid to string in select query ([d513912](https://github.com/stunor92/origo-eventor-api/commit/d51391287cd8ab1454e3824ddf7867fad65b0fef))
* Change from apiKey to eventorApiKey columns ([ac09a50](https://github.com/stunor92/origo-eventor-api/commit/ac09a50f0b3a1622c7f3b8bb9c9b0eea7481742b))
* Change from apiKey to eventorApiKey columns ([26e97b2](https://github.com/stunor92/origo-eventor-api/commit/26e97b226732f18942781cdba28a32c52f0944d8))
* debug release output ([d658161](https://github.com/stunor92/origo-eventor-api/commit/d6581616f48f204bd360cc6eeef1babee263eb86))
* delete delete endpoint that should be handled in supabase. ([#269](https://github.com/stunor92/origo-eventor-api/issues/269)) ([dc2c785](https://github.com/stunor92/origo-eventor-api/commit/dc2c785a7b4fecbfaf4689fad1d61d42aa9fdf98))
* deploy on all tags ([9a9efe5](https://github.com/stunor92/origo-eventor-api/commit/9a9efe5ca59b0272f29e1b4388a756b42ef21f0c))
* Distroless dockerimage ([f4a2bf8](https://github.com/stunor92/origo-eventor-api/commit/f4a2bf882632e034f7188e447a8d243c214a1033))
* do not log PojoBeanMapper warnings ([f437b73](https://github.com/stunor92/origo-eventor-api/commit/f437b73014da12794e7d08004f1aeb2e77c2c79c))
* Dump spring boot starter to 3.5.4 ([691fd72](https://github.com/stunor92/origo-eventor-api/commit/691fd72e1bfaf87381531c2b1890d373448ccd93))
* entry-fees with "," must be parsed correctly ([6e10085](https://github.com/stunor92/origo-eventor-api/commit/6e100853331521aeefdeb049c5d42f91075fcbbc))
* eventClassId is mandatory ([d817656](https://github.com/stunor92/origo-eventor-api/commit/d817656f609f26eb114d970aca645f0588380269))
* filter races outside requested date-range in calendar-list service ([#261](https://github.com/stunor92/origo-eventor-api/issues/261)) ([0f6e41c](https://github.com/stunor92/origo-eventor-api/commit/0f6e41ce13760733b3bfda088c657080bd0f8f66))
* fix error with entry-list if person has changed class after web-entry ([239e834](https://github.com/stunor92/origo-eventor-api/commit/239e834ffd72858081064e8eeac10a7d00c3c483))
* Fix nullpointer for team-entries ([9e61b40](https://github.com/stunor92/origo-eventor-api/commit/9e61b406cfc6872b813675d116019c4c77f1d05c))
* Fix some stuff with CalendarRace ([b823891](https://github.com/stunor92/origo-eventor-api/commit/b8238917c465325e1b517ab975342cb370024d57))
* fix sql-query to fetch persons from userId ([#242](https://github.com/stunor92/origo-eventor-api/issues/242)) ([c0f7cb2](https://github.com/stunor92/origo-eventor-api/commit/c0f7cb27630e1f599238333a521329a425636b46))
* fix the release-please action ([1a38ab1](https://github.com/stunor92/origo-eventor-api/commit/1a38ab110c95503f4eaa86ac7cb9d14461bcf440))
* fix user_person-mapping ([#251](https://github.com/stunor92/origo-eventor-api/issues/251)) ([bf8fdac](https://github.com/stunor92/origo-eventor-api/commit/bf8fdac4b91038a77b743ea88c27177af50a402c))
* fix-reference to manifest and config ([9b812e7](https://github.com/stunor92/origo-eventor-api/commit/9b812e7e822bd1d33096741786b0f62cde286663))
* Fixed OrganisationEntries with test ([157ed5e](https://github.com/stunor92/origo-eventor-api/commit/157ed5e9d094fe0079dee9e88460f452b6c4147c))
* Fixed problems with inserting events ([bdbfa0c](https://github.com/stunor92/origo-eventor-api/commit/bdbfa0cb7ee0efd10c97a01f847f5da545571836))
* Fixed the timestamp problem ([8c18748](https://github.com/stunor92/origo-eventor-api/commit/8c1874864a9c6872e23b1f8989ba30c3db5496d6))
* Improve the combination of result-, start- and entry-list ([4c035ba](https://github.com/stunor92/origo-eventor-api/commit/4c035bab0ef5bc47f880977511baeecf4b76d936))
* include patch tag in release job ([26df151](https://github.com/stunor92/origo-eventor-api/commit/26df15181742cda8b9bac6b66ebbd20b8237dbb9))
* Instant time in CalendarRace ([18ea38c](https://github.com/stunor92/origo-eventor-api/commit/18ea38ce6079f8e30a50639197f6aee3d9d1f7c8))
* make entries identifiable ([765a971](https://github.com/stunor92/origo-eventor-api/commit/765a97197c7cbdeb503c90e7088242c9f20a724f))
* manifest-file with a dot ([613d274](https://github.com/stunor92/origo-eventor-api/commit/613d274a55b96a82bb799f0fa843127bad88693a))
* map userId from String to UUID ([#248](https://github.com/stunor92/origo-eventor-api/issues/248)) ([aeaf8b3](https://github.com/stunor92/origo-eventor-api/commit/aeaf8b3751d81145584e64cedd404ccaba481cb2))
* migrate to modern and rotating jwt tokens with spring-boot-security ([#272](https://github.com/stunor92/origo-eventor-api/issues/272)) ([fe9abff](https://github.com/stunor92/origo-eventor-api/commit/fe9abffc7e9b1bd60567b62481c193a0084d4fc6))
* more release-please debuging ([fa2f96b](https://github.com/stunor92/origo-eventor-api/commit/fa2f96b4a2c2f36672e6d9a8504de32b7d06e21c))
* Nullpointer on OrganisationConverter ([80124c0](https://github.com/stunor92/origo-eventor-api/commit/80124c02c003ffb4a6a221b6e0417208a2827180))
* Nullpointer on OrganisationConverter ([#100](https://github.com/stunor92/origo-eventor-api/issues/100)) ([51188be](https://github.com/stunor92/origo-eventor-api/commit/51188bedbd8e7dbbfe03239f8ff8392ed3406887))
* Only codeQL on prs ([#182](https://github.com/stunor92/origo-eventor-api/issues/182)) ([d57f3d2](https://github.com/stunor92/origo-eventor-api/commit/d57f3d20a105635b19b6eccaa8617a8e1415daec))
* Optimize entry-lists from eventor ([#203](https://github.com/stunor92/origo-eventor-api/issues/203)) ([083da24](https://github.com/stunor92/origo-eventor-api/commit/083da24a6c25de51fc2d7e203418b9268a352cb7))
* print all outputs ([3bbf042](https://github.com/stunor92/origo-eventor-api/commit/3bbf0421edcd7c22daa28003981299bb084253e9))
* print output ([502ebf3](https://github.com/stunor92/origo-eventor-api/commit/502ebf38d32df40ae4252ddead8e7e7044d67ffd))
* Push image to github when releae is done ([5a1a9ea](https://github.com/stunor92/origo-eventor-api/commit/5a1a9ea4ea9e27b295fbb9379ae600df1e1d2dc6))
* refactor entrylist ([#231](https://github.com/stunor92/origo-eventor-api/issues/231)) ([1c13ae3](https://github.com/stunor92/origo-eventor-api/commit/1c13ae39ad6b7c4ab549757e6fcac1fee294d3bd))
* release-type: maven ([50afe15](https://github.com/stunor92/origo-eventor-api/commit/50afe155137d120b8fffb3523dbede45246fd63c))
* remove debugging print in release.yml ([5d84f47](https://github.com/stunor92/origo-eventor-api/commit/5d84f4740ca0c41bc822e062f8f59444228d3f4b))
* remove duplicated eventor date-format ([4c2610a](https://github.com/stunor92/origo-eventor-api/commit/4c2610af26e672862d63e6821832f533050d3322))
* Remove hypersistence depndency ([a365fdc](https://github.com/stunor92/origo-eventor-api/commit/a365fdc4d08d47eaaacdd82cdecdc7d6a1e556e8))
* remove legacy interceptor in test ([#275](https://github.com/stunor92/origo-eventor-api/issues/275)) ([b1da818](https://github.com/stunor92/origo-eventor-api/commit/b1da81893d81569f373613ce4e257087b755dfd9))
* remove organisation object for competitors ([2fedc9a](https://github.com/stunor92/origo-eventor-api/commit/2fedc9aab3c896b589ecba060610e0dc42af5454))
* remove whitespace ([f4f0eb0](https://github.com/stunor92/origo-eventor-api/commit/f4f0eb0301ae140238c143dfde2757cfced5576e))
* Rename eventClasses ([7a29178](https://github.com/stunor92/origo-eventor-api/commit/7a291783de2d663bf1d96fae98e3a6160f3a41cf))
* Rename eventClassId to classId ([17b3dbc](https://github.com/stunor92/origo-eventor-api/commit/17b3dbcdf710817ea3d1b53c6e5644b905f06255))
* rename releases_created check ([7e0c1a5](https://github.com/stunor92/origo-eventor-api/commit/7e0c1a59ceead12a0fadf243983f9bd826eaa7af))
* replace fixed thread pool with bounded SynchronousQueue pool to prevent nested parallelism deadlock ([#322](https://github.com/stunor92/origo-eventor-api/issues/322)) ([89eebd4](https://github.com/stunor92/origo-eventor-api/commit/89eebd4d106fa69448bbbcaa98814925f0823935))
* Return eventorId for person in json ([#197](https://github.com/stunor92/origo-eventor-api/issues/197)) ([7e2d77d](https://github.com/stunor92/origo-eventor-api/commit/7e2d77d4679b717dce446d867b78191088d6b4c8))
* Rollback refactoring ([#88](https://github.com/stunor92/origo-eventor-api/issues/88)) ([bc01759](https://github.com/stunor92/origo-eventor-api/commit/bc01759c945bffcc4edfe94a1bde372ebaea8d27))
* run deploy when release is created ([735b9f7](https://github.com/stunor92/origo-eventor-api/commit/735b9f72f4e31d611d7efe622b13ced5dea51a8d))
* set path to pom.xmm ([dfc91c5](https://github.com/stunor92/origo-eventor-api/commit/dfc91c501d86515756e2bfbc51abbf604d8e2b6a))
* set release-type to maven ([7731e4d](https://github.com/stunor92/origo-eventor-api/commit/7731e4da93f5ea50f8c8271852ac5451fd50c329))
* set target-branch to main ([ef9e9d7](https://github.com/stunor92/origo-eventor-api/commit/ef9e9d774ade4ee867755ce9018cce5ef03c5a5e))
* simple release type ([e3e579a](https://github.com/stunor92/origo-eventor-api/commit/e3e579a9fceb9d45806d74e56d8bcb7aa243d4b6))
* Skip major release tagging ([274c060](https://github.com/stunor92/origo-eventor-api/commit/274c060b544c2dd29d265b3bf3a5a695135518ca))
* Specify jpa hibernate dialect ([ed5049b](https://github.com/stunor92/origo-eventor-api/commit/ed5049be0a946da30d39d28b47b1d9c0cf12f661))
* Specify jwt algorithm used in supabase ([1211bd4](https://github.com/stunor92/origo-eventor-api/commit/1211bd4ad2ce01de34285ed1926e2dd211e8e9a3))
* specify target branch ([551c59d](https://github.com/stunor92/origo-eventor-api/commit/551c59def85b2bfd692c74672c5453d37c0bb2aa))
* spexify jws-algorithms to ES256 ([#276](https://github.com/stunor92/origo-eventor-api/issues/276)) ([8bb7dba](https://github.com/stunor92/origo-eventor-api/commit/8bb7dba40031581b38eb6d73b6a9904f1d9a8d6b))
* support competitors without valid eventor-organisation in result-list ([f14630c](https://github.com/stunor92/origo-eventor-api/commit/f14630cefbd5f4b7e7fde912cdc427e924704c9d))
* test paths_released output ([a3d3992](https://github.com/stunor92/origo-eventor-api/commit/a3d3992a244792f9c9b339244aaf630e2330f601))
* test production deploy trigger ([#282](https://github.com/stunor92/origo-eventor-api/issues/282)) ([9181c34](https://github.com/stunor92/origo-eventor-api/commit/9181c34dee2c6d7823faef479314e871b2d34f06))
* test triggering new deploy pipeline ([#279](https://github.com/stunor92/origo-eventor-api/issues/279)) ([a541c4d](https://github.com/stunor92/origo-eventor-api/commit/a541c4d5d3d180c49f2b69eef3fb2019f218fe84))
* testing auto merge of snapshot-release ([#223](https://github.com/stunor92/origo-eventor-api/issues/223)) ([6b7115a](https://github.com/stunor92/origo-eventor-api/commit/6b7115a7b5bbd0c69739ea28d1e004f0412593bb))
* try again ([5af581f](https://github.com/stunor92/origo-eventor-api/commit/5af581f869b193a93d6f8502d2267fbbdb2376a2))
* try again ([bdca6aa](https://github.com/stunor92/origo-eventor-api/commit/bdca6aafeb56cdcda1594bfe414a39438afc06b3))
* try again ([9881966](https://github.com/stunor92/origo-eventor-api/commit/98819665460c0862cb1c82b5c618a328075f11b1))
* try to create tag when release is created ([5b436a7](https://github.com/stunor92/origo-eventor-api/commit/5b436a7ce6c7854cf92bc6c8267af40e8fb40ec0))
* Try to fix competitor count in calendarserivce ([4daaf99](https://github.com/stunor92/origo-eventor-api/commit/4daaf999ab3b1b723f5dbb394ea7dade230df818))
* try to fix the prod deploy pipeline ([f11ef7f](https://github.com/stunor92/origo-eventor-api/commit/f11ef7f70d4a6d8c67781019620d9fbcb5de3f4e))
* Try to set Enumerated type in the array-types ([57b22a6](https://github.com/stunor92/origo-eventor-api/commit/57b22a68fd425838a51771ee071934a41b8cb7c3))
* try to true check release_created ([33370aa](https://github.com/stunor92/origo-eventor-api/commit/33370aae42119015220009eb462af1147b1f6dda))
* try to use default token ([3bd32b4](https://github.com/stunor92/origo-eventor-api/commit/3bd32b4362683aaba197559e080579cff07a2c55))
* try to use release_created on job level ([aba18ba](https://github.com/stunor92/origo-eventor-api/commit/aba18ba227e33b89545e57fa85ad19ebf0661a7c))
* update release.yml ([c012608](https://github.com/stunor92/origo-eventor-api/commit/c01260825a6904805921e2025e43f8e9ed033a54))
* use googleapis/release-please-action@v4.1.3 ([e541724](https://github.com/stunor92/origo-eventor-api/commit/e54172483ab630fa313ec852b347657bca61563e))
* Use instant for timestamp ([594cb32](https://github.com/stunor92/origo-eventor-api/commit/594cb327c8eafcf61fa30067daefbd73c55f4cde))
* use jdk23 on codeql ([e8c6797](https://github.com/stunor92/origo-eventor-api/commit/e8c679701ca423465d4df2d704c8096efaf1d4af))
* use supabase_url from env variables ([58ac3f3](https://github.com/stunor92/origo-eventor-api/commit/58ac3f3e0f4335255204387d26aa3b13af4a23a5))
* use supabase_url from env variables ([d8079b7](https://github.com/stunor92/origo-eventor-api/commit/d8079b71891389f8f7127d0304801c526eab6280))
* Use uid from token ([38a77f8](https://github.com/stunor92/origo-eventor-api/commit/38a77f81db28a175f4859ee1e07af7a3760674a9))
* userId as UUID ([#245](https://github.com/stunor92/origo-eventor-api/issues/245)) ([d0cfcc2](https://github.com/stunor92/origo-eventor-api/commit/d0cfcc2b58643ae0728b51cd84826d7f5611c173))
* wrong type of EntryBreak in CalendarRace ([9f00d20](https://github.com/stunor92/origo-eventor-api/commit/9f00d20fc59a937e4bd118df228187e7f9ade5ab))


### Performance Improvements

* improve performance on event and calendar services ([dc72f6f](https://github.com/stunor92/origo-eventor-api/commit/dc72f6ff2048d2ff0b74d534ab99440400a44e30))
* optimize entry list merging when results are available ([#216](https://github.com/stunor92/origo-eventor-api/issues/216)) ([a913e94](https://github.com/stunor92/origo-eventor-api/commit/a913e94f7f350e23e7594179ff646c1016aa88f3))
* parallelize Eventor API calls in /event-list/me to reduce response time ([#307](https://github.com/stunor92/origo-eventor-api/issues/307)) ([1a94103](https://github.com/stunor92/origo-eventor-api/commit/1a941035fade830c17e3719c9973c8677ba76511))


### Miscellaneous Chores

* migrate to jdk 25 ([#258](https://github.com/stunor92/origo-eventor-api/issues/258)) ([f34ec15](https://github.com/stunor92/origo-eventor-api/commit/f34ec15198dd07fbf54358388ea7d4f2b753bdbb))
* release 1.0.0 ([cc960f9](https://github.com/stunor92/origo-eventor-api/commit/cc960f9fd2a32247acb4aac36b91122c888166f5))
* release 2.0.0 ([c319cad](https://github.com/stunor92/origo-eventor-api/commit/c319cad36c484eeea372205ab00b5d9b3d804c24))

## [12.0.10](https://github.com/stunor92/origo-eventor-api/compare/v12.0.9...v12.0.10) (2026-05-03)


### Bug Fixes

* entry-fees with "," must be parsed correctly ([6e10085](https://github.com/stunor92/origo-eventor-api/commit/6e100853331521aeefdeb049c5d42f91075fcbbc))

## [12.0.9](https://github.com/stunor92/origo-eventor-api/compare/v12.0.8...v12.0.9) (2026-05-03)


### Bug Fixes

* fix error with entry-list if person has changed class after web-entry ([239e834](https://github.com/stunor92/origo-eventor-api/commit/239e834ffd72858081064e8eeac10a7d00c3c483))

## [12.0.8](https://github.com/stunor92/origo-eventor-api/compare/v12.0.7...v12.0.8) (2026-05-01)


### Bug Fixes

* use supabase_url from env variables ([58ac3f3](https://github.com/stunor92/origo-eventor-api/commit/58ac3f3e0f4335255204387d26aa3b13af4a23a5))
* use supabase_url from env variables ([d8079b7](https://github.com/stunor92/origo-eventor-api/commit/d8079b71891389f8f7127d0304801c526eab6280))

## [12.0.7](https://github.com/stunor92/origo-eventor-api/compare/v12.0.6...v12.0.7) (2026-04-29)


### Bug Fixes

* eventClassId is mandatory ([d817656](https://github.com/stunor92/origo-eventor-api/commit/d817656f609f26eb114d970aca645f0588380269))

## [12.0.6](https://github.com/stunor92/origo-eventor-api/compare/v12.0.5...v12.0.6) (2026-04-28)


### Performance Improvements

* improve performance on event and calendar services ([dc72f6f](https://github.com/stunor92/origo-eventor-api/commit/dc72f6ff2048d2ff0b74d534ab99440400a44e30))

## [12.0.5](https://github.com/stunor92/origo-eventor-api/compare/v12.0.4...v12.0.5) (2026-04-27)


### Bug Fixes

* make entries identifiable ([765a971](https://github.com/stunor92/origo-eventor-api/commit/765a97197c7cbdeb503c90e7088242c9f20a724f))

## [12.0.4](https://github.com/stunor92/origo-eventor-api/compare/v12.0.3...v12.0.4) (2026-04-27)


### Performance Improvements

* parallelize Eventor API calls in /event-list/me to reduce response time ([#307](https://github.com/stunor92/origo-eventor-api/issues/307)) ([1a94103](https://github.com/stunor92/origo-eventor-api/commit/1a941035fade830c17e3719c9973c8677ba76511))

## [12.0.3](https://github.com/stunor92/origo-eventor-api/compare/v12.0.2...v12.0.3) (2026-01-15)


### Bug Fixes

* test production deploy trigger ([#282](https://github.com/stunor92/origo-eventor-api/issues/282)) ([9181c34](https://github.com/stunor92/origo-eventor-api/commit/9181c34dee2c6d7823faef479314e871b2d34f06))

## [12.0.2](https://github.com/stunor92/origo-eventor-api/compare/v12.0.1...v12.0.2) (2026-01-15)


### Bug Fixes

* test triggering new deploy pipeline ([#279](https://github.com/stunor92/origo-eventor-api/issues/279)) ([a541c4d](https://github.com/stunor92/origo-eventor-api/commit/a541c4d5d3d180c49f2b69eef3fb2019f218fe84))

## [12.0.1](https://github.com/stunor92/origo-eventor-api/compare/v12.0.0...v12.0.1) (2026-01-10)


### Bug Fixes

* app handles invalid tokens with fallback to no auth ([#277](https://github.com/stunor92/origo-eventor-api/issues/277)) ([bbc387c](https://github.com/stunor92/origo-eventor-api/commit/bbc387cc9ec845fd77c7d75bcd174794545d12e1))
* migrate to modern and rotating jwt tokens with spring-boot-security ([#272](https://github.com/stunor92/origo-eventor-api/issues/272)) ([fe9abff](https://github.com/stunor92/origo-eventor-api/commit/fe9abffc7e9b1bd60567b62481c193a0084d4fc6))
* remove legacy interceptor in test ([#275](https://github.com/stunor92/origo-eventor-api/issues/275)) ([b1da818](https://github.com/stunor92/origo-eventor-api/commit/b1da81893d81569f373613ce4e257087b755dfd9))
* spexify jws-algorithms to ES256 ([#276](https://github.com/stunor92/origo-eventor-api/issues/276)) ([8bb7dba](https://github.com/stunor92/origo-eventor-api/commit/8bb7dba40031581b38eb6d73b6a9904f1d9a8d6b))

## [12.0.0](https://github.com/stunor92/origo-eventor-api/compare/v11.0.1...v12.0.0) (2026-01-08)


### ⚠ BREAKING CHANGES

* delete delete endpoint that should be handled in supabase. ([#269](https://github.com/stunor92/origo-eventor-api/issues/269))

### Bug Fixes

* delete delete endpoint that should be handled in supabase. ([#269](https://github.com/stunor92/origo-eventor-api/issues/269)) ([dc2c785](https://github.com/stunor92/origo-eventor-api/commit/dc2c785a7b4fecbfaf4689fad1d61d42aa9fdf98))

## [11.0.1](https://github.com/stunor92/origo-eventor-api/compare/v11.0.0...v11.0.1) (2026-01-06)


### Bug Fixes

* batch load memberships in sql query ([#265](https://github.com/stunor92/origo-eventor-api/issues/265)) ([8bb8844](https://github.com/stunor92/origo-eventor-api/commit/8bb8844a56d6ed3f82655f94c25d15745630732f))

## [11.0.0](https://github.com/stunor92/origo-eventor-api/compare/v10.0.5...v11.0.0) (2025-12-30)


### ⚠ BREAKING CHANGES

* migrate to jdk 25 ([#258](https://github.com/stunor92/origo-eventor-api/issues/258))

### Bug Fixes

* filter races outside requested date-range in calendar-list service ([#261](https://github.com/stunor92/origo-eventor-api/issues/261)) ([0f6e41c](https://github.com/stunor92/origo-eventor-api/commit/0f6e41ce13760733b3bfda088c657080bd0f8f66))


### Miscellaneous Chores

* migrate to jdk 25 ([#258](https://github.com/stunor92/origo-eventor-api/issues/258)) ([f34ec15](https://github.com/stunor92/origo-eventor-api/commit/f34ec15198dd07fbf54358388ea7d4f2b753bdbb))

## [10.0.5](https://github.com/stunor92/OriGo-EventorApi/compare/v10.0.4...v10.0.5) (2025-12-11)


### Bug Fixes

* add timestamp for saved person ([#254](https://github.com/stunor92/OriGo-EventorApi/issues/254)) ([dabd972](https://github.com/stunor92/OriGo-EventorApi/commit/dabd972926f5602ef974dfbca9d688d82ab4b150))

## [10.0.4](https://github.com/stunor92/OriGo-EventorApi/compare/v10.0.3...v10.0.4) (2025-12-11)


### Bug Fixes

* fix user_person-mapping ([#251](https://github.com/stunor92/OriGo-EventorApi/issues/251)) ([bf8fdac](https://github.com/stunor92/OriGo-EventorApi/commit/bf8fdac4b91038a77b743ea88c27177af50a402c))

## [10.0.3](https://github.com/stunor92/OriGo-EventorApi/compare/v10.0.2...v10.0.3) (2025-12-11)


### Bug Fixes

* map userId from String to UUID ([#248](https://github.com/stunor92/OriGo-EventorApi/issues/248)) ([aeaf8b3](https://github.com/stunor92/OriGo-EventorApi/commit/aeaf8b3751d81145584e64cedd404ccaba481cb2))

## [10.0.2](https://github.com/stunor92/OriGo-EventorApi/compare/v10.0.1...v10.0.2) (2025-12-11)


### Bug Fixes

* userId as UUID ([#245](https://github.com/stunor92/OriGo-EventorApi/issues/245)) ([d0cfcc2](https://github.com/stunor92/OriGo-EventorApi/commit/d0cfcc2b58643ae0728b51cd84826d7f5611c173))

## [10.0.1](https://github.com/stunor92/OriGo-EventorApi/compare/v10.0.0...v10.0.1) (2025-12-11)


### Bug Fixes

* fix sql-query to fetch persons from userId ([#242](https://github.com/stunor92/OriGo-EventorApi/issues/242)) ([c0f7cb2](https://github.com/stunor92/OriGo-EventorApi/commit/c0f7cb27630e1f599238333a521329a425636b46))

## [10.0.0](https://github.com/stunor92/OriGo-EventorApi/compare/v9.1.2...v10.0.0) (2025-12-11)


### ⚠ BREAKING CHANGES

* migrate from JPA/Hibernate to Spring JDBC ([#239](https://github.com/stunor92/OriGo-EventorApi/issues/239))

### Features

* migrate from JPA/Hibernate to Spring JDBC ([#239](https://github.com/stunor92/OriGo-EventorApi/issues/239)) ([17bb95f](https://github.com/stunor92/OriGo-EventorApi/commit/17bb95f1766cca3be8735c266764259e645721fb))

## [9.1.2](https://github.com/stunor92/OriGo-EventorApi/compare/v9.1.1...v9.1.2) (2025-11-12)


### Bug Fixes

* refactor entrylist ([#231](https://github.com/stunor92/OriGo-EventorApi/issues/231)) ([1c13ae3](https://github.com/stunor92/OriGo-EventorApi/commit/1c13ae39ad6b7c4ab549757e6fcac1fee294d3bd))

## [9.1.1](https://github.com/stunor92/OriGo-EventorApi/compare/v9.1.0...v9.1.1) (2025-11-07)


### Bug Fixes

* testing auto merge of snapshot-release ([#223](https://github.com/stunor92/OriGo-EventorApi/issues/223)) ([6b7115a](https://github.com/stunor92/OriGo-EventorApi/commit/6b7115a7b5bbd0c69739ea28d1e004f0412593bb))

## [9.1.0](https://github.com/stunor92/OriGo-EventorApi/compare/v9.0.4...v9.1.0) (2025-11-07)


### Features

* add auto-merge workflow for SNAPSHOT release PRs ([#219](https://github.com/stunor92/OriGo-EventorApi/issues/219)) ([67571d8](https://github.com/stunor92/OriGo-EventorApi/commit/67571d8556eea6aa56751514e23a7598fbbfac8d))

## [9.0.4](https://github.com/stunor92/OriGo-EventorApi/compare/v9.0.3...v9.0.4) (2025-11-07)


### Performance Improvements

* optimize entry list merging when results are available ([#216](https://github.com/stunor92/OriGo-EventorApi/issues/216)) ([a913e94](https://github.com/stunor92/OriGo-EventorApi/commit/a913e94f7f350e23e7594179ff646c1016aa88f3))

## [9.0.3](https://github.com/stunor92/OriGo-EventorApi/compare/v9.0.2...v9.0.3) (2025-11-06)


### Bug Fixes

* Optimize entry-lists from eventor ([#203](https://github.com/stunor92/OriGo-EventorApi/issues/203)) ([083da24](https://github.com/stunor92/OriGo-EventorApi/commit/083da24a6c25de51fc2d7e203418b9268a352cb7))

## [9.0.2](https://github.com/stunor92/OriGo-EventorApi/compare/v9.0.1...v9.0.2) (2025-11-05)


### Bug Fixes

* Add punching-units on start and result list ([#200](https://github.com/stunor92/OriGo-EventorApi/issues/200)) ([023bdfe](https://github.com/stunor92/OriGo-EventorApi/commit/023bdfece95f5579ec67e2bd099b7bd7f7bda186))

## [9.0.1](https://github.com/stunor92/OriGo-EventorApi/compare/v9.0.0...v9.0.1) (2025-10-29)


### Bug Fixes

* Return eventorId for person in json ([#197](https://github.com/stunor92/OriGo-EventorApi/issues/197)) ([7e2d77d](https://github.com/stunor92/OriGo-EventorApi/commit/7e2d77d4679b717dce446d867b78191088d6b4c8))

## [9.0.0](https://github.com/stunor92/OriGo-EventorApi/compare/v8.1.0...v9.0.0) (2025-10-29)


### ⚠ BREAKING CHANGES

* Use generated uuid as id on entities ([#192](https://github.com/stunor92/OriGo-EventorApi/issues/192))

### Features

* Use generated uuid as id on entities ([#192](https://github.com/stunor92/OriGo-EventorApi/issues/192)) ([d38215b](https://github.com/stunor92/OriGo-EventorApi/commit/d38215b1479a0fea43313e6660f822ff3e104d1f))

## [8.1.0](https://github.com/stunor92/OriGo-EventorApi/compare/v8.0.3...v8.1.0) (2025-10-26)


### Features

* Improve ci-pipeline ([#184](https://github.com/stunor92/OriGo-EventorApi/issues/184)) ([db39b0d](https://github.com/stunor92/OriGo-EventorApi/commit/db39b0d7d13161c5adff94150c5472cad7f0a4b2))


### Bug Fixes

* Add hypersistence again ([#186](https://github.com/stunor92/OriGo-EventorApi/issues/186)) ([22e5e71](https://github.com/stunor92/OriGo-EventorApi/commit/22e5e71c89bd81cf72af53a1097b0c9c8edab0b6))

## [8.0.3](https://github.com/stunor92/OriGo-EventorApi/compare/v8.0.2...v8.0.3) (2025-10-26)


### Features

* Codeql scan on all branch ([#181](https://github.com/stunor92/OriGo-EventorApi/issues/181)) ([3f432a7](https://github.com/stunor92/OriGo-EventorApi/commit/3f432a7fb03e07215eef9334c0d2f882a5df0864))


### Bug Fixes

* Only codeQL on prs ([#182](https://github.com/stunor92/OriGo-EventorApi/issues/182)) ([d57f3d2](https://github.com/stunor92/OriGo-EventorApi/commit/d57f3d20a105635b19b6eccaa8617a8e1415daec))
* Try to set Enumerated type in the array-types ([57b22a6](https://github.com/stunor92/OriGo-EventorApi/commit/57b22a68fd425838a51771ee071934a41b8cb7c3))

## [8.0.2](https://github.com/stunor92/OriGo-EventorApi/compare/v8.0.1...v8.0.2) (2025-10-26)


### Bug Fixes

* Distroless dockerimage ([f4a2bf8](https://github.com/stunor92/OriGo-EventorApi/commit/f4a2bf882632e034f7188e447a8d243c214a1033))
* Push image to github when releae is done ([5a1a9ea](https://github.com/stunor92/OriGo-EventorApi/commit/5a1a9ea4ea9e27b295fbb9379ae600df1e1d2dc6))

## [8.0.1](https://github.com/stunor92/OriGo-EventorApi/compare/v8.0.0...v8.0.1) (2025-10-26)


### Bug Fixes

* Remove hypersistence depndency ([a365fdc](https://github.com/stunor92/OriGo-EventorApi/commit/a365fdc4d08d47eaaacdd82cdecdc7d6a1e556e8))

## [8.0.0](https://github.com/stunor92/OriGo-EventorApi/compare/v7.0.2...v8.0.0) (2025-10-18)


### ⚠ BREAKING CHANGES

* Fees is not relevant to return in responses for this application

### Features

* Fees is not relevant to return in responses for this application ([d33da27](https://github.com/stunor92/OriGo-EventorApi/commit/d33da2718db93638926f2d5a85a0588ef20e2ea1))
* Fees is updated in database when a event is fetched ([3ffaf8a](https://github.com/stunor92/OriGo-EventorApi/commit/3ffaf8a36cd75de7e109a1a2f0abf7e44ade1ede))


### Bug Fixes

* Improve the combination of result-, start- and entry-list ([4c035ba](https://github.com/stunor92/OriGo-EventorApi/commit/4c035bab0ef5bc47f880977511baeecf4b76d936))

## [7.0.2](https://github.com/stunor92/OriGo-EventorApi/compare/v7.0.1...v7.0.2) (2025-10-03)


### Bug Fixes

* Fixed OrganisationEntries with test ([157ed5e](https://github.com/stunor92/OriGo-EventorApi/commit/157ed5e9d094fe0079dee9e88460f452b6c4147c))

## [7.0.1](https://github.com/stunor92/OriGo-EventorApi/compare/v7.0.0...v7.0.1) (2025-10-03)


### Bug Fixes

* Try to fix competitor count in calendarserivce ([4daaf99](https://github.com/stunor92/OriGo-EventorApi/commit/4daaf999ab3b1b723f5dbb394ea7dade230df818))

## [7.0.0](https://github.com/stunor92/OriGo-EventorApi/compare/v6.3.1...v7.0.0) (2025-10-03)


### ⚠ BREAKING CHANGES

* Use more inlined objects instead of references

### Features

* Use more inlined objects instead of references ([611f581](https://github.com/stunor92/OriGo-EventorApi/commit/611f5810a59a6f5cf74b20b2a703cc498744e9d3))

## [6.3.1](https://github.com/stunor92/OriGo-EventorApi/compare/v6.3.0...v6.3.1) (2025-09-07)


### Bug Fixes

* Skip major release tagging ([274c060](https://github.com/stunor92/OriGo-EventorApi/commit/274c060b544c2dd29d265b3bf3a5a695135518ca))

## [6.3.0](https://github.com/stunor92/OriGo-EventorApi/compare/v6.2.1...v6.3.0) (2025-09-06)


### Features

* Use joining table for organisers ([b6e5e91](https://github.com/stunor92/OriGo-EventorApi/commit/b6e5e9128b564db074eaf668361e6a0e4c2b686b))

## [6.2.1](https://github.com/stunor92/OriGo-EventorApi/compare/v6.2.0...v6.2.1) (2025-08-26)


### Bug Fixes

* Dump spring boot starter to 3.5.4 ([691fd72](https://github.com/stunor92/OriGo-EventorApi/commit/691fd72e1bfaf87381531c2b1890d373448ccd93))

## [6.2.0](https://github.com/stunor92/OriGo-EventorApi/compare/v6.1.0...v6.2.0) (2025-08-26)


### Features

* Automatic download fees when fetching event ([89e5bb1](https://github.com/stunor92/OriGo-EventorApi/commit/89e5bb19244e69c8c8f176f2a51491e835a15eaf))
* Cleanup application config and connect to local db locally ([7c14526](https://github.com/stunor92/OriGo-EventorApi/commit/7c14526e5955b63a347e97d5212fe4a5bdf8bf1c))

## [6.1.0](https://github.com/stunor92/OriGo-EventorApi/compare/v6.0.0...v6.1.0) (2025-08-12)


### Features

* Add automatic release via prof branch ([55ee347](https://github.com/stunor92/OriGo-EventorApi/commit/55ee34757593825f75f4a6ef3ef7ff942f4519d3))

## [6.0.0](https://github.com/stunor92/OriGo-EventorApi/compare/v5.1.0...v6.0.0) (2025-07-28)


### ⚠ BREAKING CHANGES

* Migated the event-endpoint to supabase
* Migated the event-endpoint to supabase
* Migrate auth person to use supabase ([#107](https://github.com/stunor92/OriGo-EventorApi/issues/107))

### Features

* Add api for delete person ([896f2c4](https://github.com/stunor92/OriGo-EventorApi/commit/896f2c4dbecc3e147498a2019214dc56c2967c4c))
* implemented disconnect eventor-person and delete user ([de7f7d6](https://github.com/stunor92/OriGo-EventorApi/commit/de7f7d6847d33b81c2974fb06879226c833ee1e5))
* Migated the event-endpoint to supabase ([ac04f3b](https://github.com/stunor92/OriGo-EventorApi/commit/ac04f3bb16fcc5a5aba4a8c3a737ca5ab19975d8))
* Migated the event-endpoint to supabase ([c29a9e3](https://github.com/stunor92/OriGo-EventorApi/commit/c29a9e3c651fd722fdab2cbbc445a1709776dc30))
* Migrate auth person to use supabase ([#107](https://github.com/stunor92/OriGo-EventorApi/issues/107)) ([2c998eb](https://github.com/stunor92/OriGo-EventorApi/commit/2c998eb10312fb5a7a76f40c36e02c9d8ec3c5a0))


### Bug Fixes

* Add missing depencency ([838ed44](https://github.com/stunor92/OriGo-EventorApi/commit/838ed44969d30fd265090b78368c07cb1571be40))
* Cast uuid to string in select query ([d513912](https://github.com/stunor92/OriGo-EventorApi/commit/d51391287cd8ab1454e3824ddf7867fad65b0fef))
* Change from apiKey to eventorApiKey columns ([ac09a50](https://github.com/stunor92/OriGo-EventorApi/commit/ac09a50f0b3a1622c7f3b8bb9c9b0eea7481742b))
* Change from apiKey to eventorApiKey columns ([26e97b2](https://github.com/stunor92/OriGo-EventorApi/commit/26e97b226732f18942781cdba28a32c52f0944d8))
* Fix some stuff with CalendarRace ([b823891](https://github.com/stunor92/OriGo-EventorApi/commit/b8238917c465325e1b517ab975342cb370024d57))
* Fixed problems with inserting events ([bdbfa0c](https://github.com/stunor92/OriGo-EventorApi/commit/bdbfa0cb7ee0efd10c97a01f847f5da545571836))
* Fixed the timestamp problem ([8c18748](https://github.com/stunor92/OriGo-EventorApi/commit/8c1874864a9c6872e23b1f8989ba30c3db5496d6))
* Instant time in CalendarRace ([18ea38c](https://github.com/stunor92/OriGo-EventorApi/commit/18ea38ce6079f8e30a50639197f6aee3d9d1f7c8))
* Rename eventClasses ([7a29178](https://github.com/stunor92/OriGo-EventorApi/commit/7a291783de2d663bf1d96fae98e3a6160f3a41cf))
* Rename eventClassId to classId ([17b3dbc](https://github.com/stunor92/OriGo-EventorApi/commit/17b3dbcdf710817ea3d1b53c6e5644b905f06255))
* Specify jpa hibernate dialect ([ed5049b](https://github.com/stunor92/OriGo-EventorApi/commit/ed5049be0a946da30d39d28b47b1d9c0cf12f661))
* Specify jwt algorithm used in supabase ([1211bd4](https://github.com/stunor92/OriGo-EventorApi/commit/1211bd4ad2ce01de34285ed1926e2dd211e8e9a3))
* Use instant for timestamp ([594cb32](https://github.com/stunor92/OriGo-EventorApi/commit/594cb327c8eafcf61fa30067daefbd73c55f4cde))
* Use uid from token ([38a77f8](https://github.com/stunor92/OriGo-EventorApi/commit/38a77f81db28a175f4859ee1e07af7a3760674a9))

## [5.1.0](https://github.com/stunor92/OriGo-EventorApi/compare/v5.0.1...v5.1.0) (2025-04-01)


### Features

* Remove deploy with GHA ([#104](https://github.com/stunor92/OriGo-EventorApi/issues/104)) ([92f5a7d](https://github.com/stunor92/OriGo-EventorApi/commit/92f5a7d71369b9bd426a6bd007e37632fed0238c))

## [5.0.1](https://github.com/stunor92/OriGo-EventorApi/compare/v5.0.0...v5.0.1) (2025-03-26)


### Bug Fixes

* Nullpointer on OrganisationConverter ([80124c0](https://github.com/stunor92/OriGo-EventorApi/commit/80124c02c003ffb4a6a221b6e0417208a2827180))
* Nullpointer on OrganisationConverter ([#100](https://github.com/stunor92/OriGo-EventorApi/issues/100)) ([51188be](https://github.com/stunor92/OriGo-EventorApi/commit/51188bedbd8e7dbbfe03239f8ff8392ed3406887))

## [5.0.0](https://github.com/stunor92/OriGo-EventorApi/compare/v4.1.0...v5.0.0) (2025-03-19)


### ⚠ BREAKING CHANGES

* Rollback refactoring ([#88](https://github.com/stunor92/OriGo-EventorApi/issues/88))
* Deprecate one punchingUnit and use list instead ([#86](https://github.com/stunor92/OriGo-EventorApi/issues/86))

### Features

* Deprecate one punchingUnit and use list instead ([#86](https://github.com/stunor92/OriGo-EventorApi/issues/86)) ([da15428](https://github.com/stunor92/OriGo-EventorApi/commit/da15428501285062ca9be1e93056c26bda45f566))


### Bug Fixes

* Rollback refactoring ([#88](https://github.com/stunor92/OriGo-EventorApi/issues/88)) ([bc01759](https://github.com/stunor92/OriGo-EventorApi/commit/bc01759c945bffcc4edfe94a1bde372ebaea8d27))
* wrong type of EntryBreak in CalendarRace ([9f00d20](https://github.com/stunor92/OriGo-EventorApi/commit/9f00d20fc59a937e4bd118df228187e7f9ade5ab))

## [4.1.0](https://github.com/stunor92/OriGo-EventorApi/compare/v4.0.4...v4.1.0) (2025-03-17)


### Features

* try again ([c90bc44](https://github.com/stunor92/OriGo-EventorApi/commit/c90bc44408c501763e8a85c9a35c43941485c1c0))

## [4.0.4](https://github.com/stunor92/OriGo-EventorApi/compare/v4.0.3...v4.0.4) (2025-03-17)


### Bug Fixes

* run deploy when release is created ([735b9f7](https://github.com/stunor92/OriGo-EventorApi/commit/735b9f72f4e31d611d7efe622b13ced5dea51a8d))

## [4.0.3](https://github.com/stunor92/OriGo-EventorApi/compare/v4.0.2...v4.0.3) (2025-03-17)


### Bug Fixes

* remove duplicated eventor date-format ([4c2610a](https://github.com/stunor92/OriGo-EventorApi/commit/4c2610af26e672862d63e6821832f533050d3322))
* remove whitespace ([f4f0eb0](https://github.com/stunor92/OriGo-EventorApi/commit/f4f0eb0301ae140238c143dfde2757cfced5576e))
* use jdk23 on codeql ([e8c6797](https://github.com/stunor92/OriGo-EventorApi/commit/e8c679701ca423465d4df2d704c8096efaf1d4af))

## [4.0.2](https://github.com/stunor92/OriGo-EventorApi/compare/v4.0.1...v4.0.2) (2025-03-17)


### Bug Fixes

* include patch tag in release job ([26df151](https://github.com/stunor92/OriGo-EventorApi/commit/26df15181742cda8b9bac6b66ebbd20b8237dbb9))

## [4.0.1](https://github.com/stunor92/OriGo-EventorApi/compare/v4.0.0...v4.0.1) (2025-03-17)


### Bug Fixes

* try again ([5af581f](https://github.com/stunor92/OriGo-EventorApi/commit/5af581f869b193a93d6f8502d2267fbbdb2376a2))

## [4.0.0](https://github.com/stunor92/OriGo-EventorApi/compare/v3.0.2...v4.0.0) (2025-03-17)


### ⚠ BREAKING CHANGES

* try to fix the prod deploy pipeline

### Bug Fixes

* try to fix the prod deploy pipeline ([f11ef7f](https://github.com/stunor92/OriGo-EventorApi/commit/f11ef7f70d4a6d8c67781019620d9fbcb5de3f4e))

## [3.0.2](https://github.com/stunor92/OriGo-EventorApi/compare/v3.0.1...v3.0.2) (2025-03-17)


### Bug Fixes

* deploy on all tags ([9a9efe5](https://github.com/stunor92/OriGo-EventorApi/commit/9a9efe5ca59b0272f29e1b4388a756b42ef21f0c))

## [3.0.1](https://github.com/stunor92/OriGo-EventorApi/compare/v3.0.0...v3.0.1) (2025-03-17)


### Bug Fixes

* update release.yml ([c012608](https://github.com/stunor92/OriGo-EventorApi/commit/c01260825a6904805921e2025e43f8e9ed033a54))

## [3.0.0](https://github.com/stunor92/OriGo-EventorApi/compare/v2.1.0...v3.0.0) (2025-03-17)


### ⚠ BREAKING CHANGES

* upgrade to java 23

### Features

* upgrade to java 23 ([cedccf7](https://github.com/stunor92/OriGo-EventorApi/commit/cedccf73e393ef6d3a8222e63a9345336f592ac1))
* upgrade to java 23 also in dockerfile ([6bfaf6d](https://github.com/stunor92/OriGo-EventorApi/commit/6bfaf6d5ebe7e47440c6df6ff052f529537776ad))
* upgrade to java 23 also in dockerfile ([13431e5](https://github.com/stunor92/OriGo-EventorApi/commit/13431e507746c1bc627dce152f50860ca24cc1e2))


### Bug Fixes

* remove debugging print in release.yml ([5d84f47](https://github.com/stunor92/OriGo-EventorApi/commit/5d84f4740ca0c41bc822e062f8f59444228d3f4b))

## [2.1.0](https://github.com/stunor92/OriGo-EventorApi/compare/v2.0.0...v2.1.0) (2025-03-17)


### Features

* add release-please json files ([02193d8](https://github.com/stunor92/OriGo-EventorApi/commit/02193d81b9f04214ad35d024c6c9eacafaee0b53))
* also incliude org-ids on entrylist ([373c1fe](https://github.com/stunor92/OriGo-EventorApi/commit/373c1fec80dd33836b5b08640cffa57b4418a072))
* checkout code after release-please ([57bc13b](https://github.com/stunor92/OriGo-EventorApi/commit/57bc13b5651481342bbcd09936a5676a8676e970))
* checkout code before release-please ([6a5bd42](https://github.com/stunor92/OriGo-EventorApi/commit/6a5bd42c117d262f5704778a24ea2df3f0e88b58))
* fix release-please-config.json with package-name ([a8b7e8e](https://github.com/stunor92/OriGo-EventorApi/commit/a8b7e8e92c0bf91ec6f4c09fa2d911c9dd31efad))
* modify release.yml ([f2b0545](https://github.com/stunor92/OriGo-EventorApi/commit/f2b05457c7ded757dbebc53545281d49f05b5f4c))
* more pipeline stuff ([b0e2e50](https://github.com/stunor92/OriGo-EventorApi/commit/b0e2e50d2d7ea19a64b6c1e50da67a994607b257))
* send only organisation with id in response ([ee29d85](https://github.com/stunor92/OriGo-EventorApi/commit/ee29d858fcce6d56edc3a9e79ec5970df929ba04))
* support multiple punchingUnits in event ([ad405f7](https://github.com/stunor92/OriGo-EventorApi/commit/ad405f79b305567d2fa86f1f5e2499996382f3ac))
* support multiple punchingUnits in event ([4660031](https://github.com/stunor92/OriGo-EventorApi/commit/466003184f93aa993311b8faca303e07dc7c4fb6))
* testing release ([939ae57](https://github.com/stunor92/OriGo-EventorApi/commit/939ae57a7a6bd181fb63044ad4358cf9105f86d9))
* update release-please-config.json ([650f7b7](https://github.com/stunor92/OriGo-EventorApi/commit/650f7b798b60fe6e17e8c054f1b7bc0d918aff28))


### Bug Fixes

* add config file ([313d9d3](https://github.com/stunor92/OriGo-EventorApi/commit/313d9d38b4e06baa6be7cf347b684941becded01))
* add manifest file ([df2a1b6](https://github.com/stunor92/OriGo-EventorApi/commit/df2a1b6a3b271e4ddcb54c1f6a0c8701ca6ab02a))
* add spring-context framework ([56f401b](https://github.com/stunor92/OriGo-EventorApi/commit/56f401ba1a778436dc74e2b24578b7af3b038547))
* back to simple setup ([9ce54b7](https://github.com/stunor92/OriGo-EventorApi/commit/9ce54b76016ce8ad44bef6e3b8be49a24ad1ba22))
* do not log PojoBeanMapper warnings ([f437b73](https://github.com/stunor92/OriGo-EventorApi/commit/f437b73014da12794e7d08004f1aeb2e77c2c79c))
* Fix nullpointer for team-entries ([9e61b40](https://github.com/stunor92/OriGo-EventorApi/commit/9e61b406cfc6872b813675d116019c4c77f1d05c))
* fix-reference to manifest and config ([9b812e7](https://github.com/stunor92/OriGo-EventorApi/commit/9b812e7e822bd1d33096741786b0f62cde286663))
* manifest-file with a dot ([613d274](https://github.com/stunor92/OriGo-EventorApi/commit/613d274a55b96a82bb799f0fa843127bad88693a))
* more release-please debuging ([fa2f96b](https://github.com/stunor92/OriGo-EventorApi/commit/fa2f96b4a2c2f36672e6d9a8504de32b7d06e21c))
* print output ([502ebf3](https://github.com/stunor92/OriGo-EventorApi/commit/502ebf38d32df40ae4252ddead8e7e7044d67ffd))
* release-type: maven ([50afe15](https://github.com/stunor92/OriGo-EventorApi/commit/50afe155137d120b8fffb3523dbede45246fd63c))
* remove organisation object for competitors ([2fedc9a](https://github.com/stunor92/OriGo-EventorApi/commit/2fedc9aab3c896b589ecba060610e0dc42af5454))
* set path to pom.xmm ([dfc91c5](https://github.com/stunor92/OriGo-EventorApi/commit/dfc91c501d86515756e2bfbc51abbf604d8e2b6a))
* set target-branch to main ([ef9e9d7](https://github.com/stunor92/OriGo-EventorApi/commit/ef9e9d774ade4ee867755ce9018cce5ef03c5a5e))
* support competitors without valid eventor-organisation in result-list ([f14630c](https://github.com/stunor92/OriGo-EventorApi/commit/f14630cefbd5f4b7e7fde912cdc427e924704c9d))
