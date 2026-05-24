package no.stunor.origo.eventorapi.api

import com.github.benmanes.caffeine.cache.Cache
import no.stunor.origo.eventorapi.exception.EventorAuthException
import no.stunor.origo.eventorapi.exception.EventorConnectionException
import no.stunor.origo.eventorapi.model.Eventor
import no.stunor.origo.eventorapi.model.event.EventClassificationEnum
import org.iof.eventor.CompetitorCountList
import org.iof.eventor.DocumentList
import org.iof.eventor.EntryFeeList
import org.iof.eventor.EntryList
import org.iof.eventor.Event
import org.iof.eventor.EventClassList
import org.iof.eventor.EventList
import org.iof.eventor.ResultList
import org.iof.eventor.ResultListList
import org.iof.eventor.StartList
import org.iof.eventor.StartListList
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.xml.bind.JAXBContext

class EventorService(
    private val eventListCache:       Cache<String, EventList>,
    private val competitorCountCache: Cache<String, CompetitorCountList>,
    private val eventClassCache:      Cache<String, EventClassList>,
    private val orgEntriesCache:      Cache<String, EntryList>
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(TIMEOUT.toLong()))
        .build()

    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    companion object {
        private const val TIMEOUT = 20_000
    }

    // ─── JAXB unmarshallers (thread-local for thread safety) ─────────────────
    private inline fun <reified T> unmarshal(xml: String): T {
        val ctx = JAXBContext.newInstance(T::class.java)
        return ctx.createUnmarshaller().unmarshal(xml.reader()) as T
    }

    private fun get(url: String, apiKey: String? = null, username: String? = null, password: String? = null): String {
        val builder = HttpRequest.newBuilder(URI(url))
            .timeout(Duration.ofMillis(TIMEOUT.toLong()))
            .GET()
        apiKey?.let   { builder.header("ApiKey", it) }
        username?.let { builder.header("Username", it) }
        password?.let { builder.header("Password", it) }

        val response = httpClient.send(builder.build(), BodyHandlers.ofString())
        when (response.statusCode()) {
            200 -> return response.body()
            401 -> throw EventorAuthException()
            else -> {
                log.warn("Eventor API error: HTTP ${response.statusCode()} for $url")
                throw EventorConnectionException()
            }
        }
    }

    // ─── API methods ─────────────────────────────────────────────────────────

    fun authenticatePerson(eventor: Eventor, username: String?, password: String?): org.iof.eventor.Person {
        val xml = get(eventor.baseUrl + "api/authenticatePerson", username = username, password = password)
        return unmarshal(xml)
    }

    fun getEventList(
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
        return eventListCache.get(cacheKey) {
            val xml = get(url, apiKey = eventor.eventorApiKey)
            unmarshal(xml)
        }
    }

    fun getCompetitorCounts(
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
        return competitorCountCache.get(cacheKey) {
            val xml = get(url, apiKey = eventor.eventorApiKey)
            unmarshal(xml)
        } ?: CompetitorCountList()
    }

    fun getGetPersonalStarts(
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

    fun getGetPersonalResults(
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

    fun getGetOrganisationEntries(
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
        return orgEntriesCache.get(cacheKey) {
            val xml = get(url, apiKey = eventor.eventorApiKey)
            unmarshal(xml)
        } ?: EntryList()
    }

    fun getEvent(baseUrl: String, apiKey: String?, eventId: String): Event? {
        val xml = get("${baseUrl}api/event/$eventId", apiKey = apiKey)
        return unmarshal(xml)
    }

    fun getEventClasses(eventor: Eventor, eventId: String): EventClassList? {
        val url = "${eventor.baseUrl}api/eventclasses?includeEntryFees=true&eventId=$eventId"
        val cacheKey = "${eventor.id}:$url"
        return eventClassCache.get(cacheKey) {
            val xml = get(url, apiKey = eventor.eventorApiKey)
            unmarshal(xml)
        }
    }

    fun getEventDocuments(baseUrl: String, apiKey: String?, eventId: String): DocumentList? {
        val xml = get("${baseUrl}api/events/documents?eventIds=$eventId", apiKey = apiKey)
        return unmarshal(xml)
    }

    fun getEventEntryList(baseUrl: String, apiKey: String?, eventId: String): EntryList? {
        val xml = get(
            "${baseUrl}api/entries?includePersonElement=true&includeEntryFees=true&eventIds=$eventId",
            apiKey = apiKey
        )
        return unmarshal(xml)
    }

    fun getEventStartList(baseUrl: String, apiKey: String?, eventId: String): StartList? {
        val xml = get("${baseUrl}api/starts/event?eventId=$eventId", apiKey = apiKey)
        return unmarshal(xml)
    }

    fun getEventResultList(baseUrl: String, apiKey: String?, eventId: String): ResultList? {
        val xml = get("${baseUrl}api/results/event?eventId=$eventId&includeSplitTimes=true", apiKey = apiKey)
        return unmarshal(xml)
    }

    fun getEventEntryFees(eventor: Eventor, eventId: String): EntryFeeList? {
        val xml = get("${eventor.baseUrl}api/entryfees/events/$eventId", apiKey = eventor.eventorApiKey)
        return unmarshal(xml)
    }
}
