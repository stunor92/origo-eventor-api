# Changelog

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
