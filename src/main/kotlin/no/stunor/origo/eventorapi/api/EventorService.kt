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
