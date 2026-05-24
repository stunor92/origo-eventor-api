import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.1.21"
    id("com.gradleup.shadow") version "8.3.8"
}

group = "no.stunor.origo"
version = "13.1.0"

val ktorVersion = "3.1.3"
val koinVersion = "4.0.4"
val kotlinVersion = "2.1.21"

repositories {
    mavenCentral()
}

// ─── JAXB XJC source generation ──────────────────────────────────────────────
val jaxbXjc: Configuration by configurations.creating

val generateJaxb by tasks.registering(JavaExec::class) {
    val xsdFile = file("src/main/resources/IOF.xsd")
    val outputDir = layout.buildDirectory.dir("generated-sources/jaxb").get().asFile

    inputs.files(xsdFile)
    outputs.dir(outputDir)

    classpath = jaxbXjc
    mainClass.set("com.sun.tools.xjc.XJCFacade")
    args(
        "-d", outputDir.absolutePath,
        "-p", "org.iof.eventor",
        xsdFile.absolutePath
    )
    doFirst { outputDir.mkdirs() }
}

sourceSets.main {
    java.srcDir(layout.buildDirectory.dir("generated-sources/jaxb"))
}

tasks.compileKotlin  { dependsOn(generateJaxb) }
tasks.compileJava    { dependsOn(generateJaxb) }

// ─── Dependencies ─────────────────────────────────────────────────────────────
dependencies {
    // XJC code generator (compile-only classpath for JAXB source generation)
    jaxbXjc("com.sun.xml.bind:jaxb-xjc:2.3.9")
    jaxbXjc("com.sun.xml.bind:jaxb-impl:2.3.9")
    jaxbXjc("javax.xml.bind:jaxb-api:2.3.1")
    jaxbXjc("javax.activation:activation:1.1.1")

    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    // Ktor server
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")

    // Koin DI
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

    // Database – HikariCP + Spring JDBC (used standalone, no Spring Boot)
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.springframework:spring-jdbc:6.2.8")
    implementation("org.springframework:spring-tx:6.2.8")
    implementation("org.postgresql:postgresql:42.7.11")

    // Caching
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.0")

    // JAXB runtime (required at runtime for the XJC-generated classes)
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("com.sun.xml.bind:jaxb-impl:2.3.9")
    implementation("javax.activation:activation:1.1.1")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")

    // Tests
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.mockk:mockk:1.13.17")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation(kotlin("test"))
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.xmlunit:xmlunit-core:2.11.0")
}

// ─── Compile options ──────────────────────────────────────────────────────────
kotlin {
    jvmToolchain(17)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

// ─── Shadow fat JAR ───────────────────────────────────────────────────────────
tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "no.stunor.origo.eventorapi.ApplicationKt"
    }
    mergeServiceFiles()
}

tasks.build { dependsOn(tasks.shadowJar) }

// ─── Tests ────────────────────────────────────────────────────────────────────
tasks.test {
    useJUnitPlatform()
}
