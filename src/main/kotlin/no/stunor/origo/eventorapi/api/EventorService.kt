package no.stunor.origo.eventorapi.api

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
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class EventorService {
    private var restTemplate: RestTemplate = RestTemplate()
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    companion object {
        private const val TIMEOUT = 20000
    }

    init {
        val rf = restTemplate.requestFactory as SimpleClientHttpRequestFactory
        rf.setReadTimeout(TIMEOUT)
        rf.setConnectTimeout(TIMEOUT)

        val converters: MutableList<HttpMessageConverter<*>> = ArrayList()
        converters.add(Jaxb2RootElementHttpMessageConverter())
        restTemplate.messageConverters = converters
    }

    fun authenticatePerson(eventor: Eventor, username: String?, password: String?): org.iof.eventor.Person? {
        val headers = HttpHeaders()
        headers["Username"] = username
        headers["Password"] = password

        val request = HttpEntity<String>(headers)
        val response = restTemplate.exchange<org.iof.eventor.Person>(
            eventor.baseUrl + "api/authenticatePerson",
            HttpMethod.GET,
            request,
            1
        )
        return response.body
    }

    @Cacheable("event-lists")
    fun getEventList(
        eventor: Eventor,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        organisationIds: List<String?>?,
        classifications: List<EventClassificationEnum?>?
    ): EventList? {
        val headers = HttpHeaders()
        headers["ApiKey"] = eventor.eventorApiKey

        val classificationIds: MutableList<String> = ArrayList()

        if (classifications != null) {
            for (eventClassification in classifications) {
                when (eventClassification) {
                    EventClassificationEnum.Championship -> classificationIds.add("1")
                    EventClassificationEnum.National -> classificationIds.add("2")
                    EventClassificationEnum.Regional -> classificationIds.add("3")
                    EventClassificationEnum.Local -> classificationIds.add("4")
                    else -> classificationIds.add("5")
                }
            }
        }

        val request = HttpEntity<String>(headers)
        val response = restTemplate.exchange(
            """
                ${eventor.baseUrl}api/events
                ?fromDate=${if (fromDate == null) "" else dateFormat.format(fromDate)}
                &toDate=${if (toDate == null) "" else dateFormat.format(toDate)}
                ${if (organisationIds != null) "&organisationIds=" + organisationIds.joinToString() else ""}
                &classificationIds=${classificationIds.joinToString()}
                &includeEntryBreaks=true
            """.trimIndent(),
            HttpMethod.GET,
            request,
            EventList::class.java,
            1
        )
        return response.body
    }

    @Cacheable("competitor-counts")
    fun getCompetitorCounts(
        eventor: Eventor,
        events: List<String?>?,
        organisations: List<String?>?,
        persons: List<String?>?
    ): CompetitorCountList {
        val headers = HttpHeaders()
        headers["ApiKey"] = eventor.eventorApiKey

        val request = HttpEntity<String>(headers)
        val response = restTemplate.exchange<CompetitorCountList>(
            eventor.baseUrl + "api/competitorcount?eventIds=" + java.lang.String.join(",", events) +
                    ",&organisationIds=" + java.lang.String.join(",", organisations) +
                    "&personIds=" + java.lang.String.join(",", persons),
            HttpMethod.GET,
            request,
            1
        )
        return response.body ?: CompetitorCountList()
    }

    fun getGetPersonalStarts(
        eventor: Eventor,
        personId: String,
        eventId: String?,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): StartListList? {
        val headers = HttpHeaders()
        headers["ApiKey"] = eventor.eventorApiKey

        val request = HttpEntity<String>(headers)
        val response = restTemplate.exchange<StartListList>(
            eventor.baseUrl
                    + "api/starts/person?personId=" + personId
                    + "&fromDate=" + (if (fromDate == null) "" else dateFormat.format(fromDate))
                    + "&toDate=" + (if (toDate == null) "" else dateFormat.format(toDate))
                    + "&eventIds=" + (eventId ?: ""),
            HttpMethod.GET,
            request,
            1
        )
        return response.body
    }

    fun getGetPersonalResults(
        eventor: Eventor,
        personId: String,
        eventId: String?,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): ResultListList? {
        val headers = HttpHeaders()
        headers["ApiKey"] = eventor.eventorApiKey

        val request = HttpEntity<String>(headers)
        val response = restTemplate.exchange<ResultListList>(
            eventor.baseUrl
                    + "api/results/person?personId=" + personId
                    + "&fromDate=" + (if (fromDate == null) "" else dateFormat.format(fromDate))
                    + "&toDate=" + (if (toDate == null) "" else dateFormat.format(toDate))
                    + "&eventIds=" + (eventId ?: ""),
            HttpMethod.GET,
            request,
            1
        )
        return response.body
    }

    @Cacheable("organisation-entries")
    fun getGetOrganisationEntries(
        eventor: Eventor,
        organisations: List<String>,
        eventId: String?,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): EntryList {
        val headers = HttpHeaders()
        headers["ApiKey"] = eventor.eventorApiKey

        val request = HttpEntity<String>(headers)
        val response = restTemplate.exchange<EntryList>(
            eventor.baseUrl
                    + "api/entries?organisationIds=" + java.lang.String.join(",", organisations)
                    + "&fromEventDate=" + (if (fromDate == null) "" else dateFormat.format(fromDate))
                    + "&toEventDate=" + (if (toDate == null) "" else dateFormat.format(toDate))
                    + "&includeEventElement=true&eventIds=" + (eventId ?: ""),
            HttpMethod.GET,
            request,
            1
        )
        return response.body ?: EntryList()
    }

    fun getEvent(baseUrl: String, apiKey: String?, eventId: String): Event? {
        val headers = HttpHeaders()
        headers["ApiKey"] = apiKey

        val request = HttpEntity<String>(headers)
        val response = restTemplate.exchange<Event>(
            baseUrl + "api/event/" + eventId,
            HttpMethod.GET,
            request,
            1
        )
        return response.body
    }

    @Cacheable("event-classes")
    fun getEventClasses(eventor: Eventor, eventId: String): EventClassList? {
        val headers = HttpHeaders()
        headers["ApiKey"] = eventor.eventorApiKey

        val request = HttpEntity<String>(headers)
        val response = restTemplate.exchange<EventClassList>(
            eventor.baseUrl + "api/eventclasses?includeEntryFees=true&eventId=" + eventId,
            HttpMethod.GET,
            request,
            1
        )
        return response.body
    }

    fun getEventDocuments(baseUrl: String, apiKey: String?, eventId: String): DocumentList? {
        val headers = HttpHeaders()
        headers["ApiKey"] = apiKey

        val request = HttpEntity<String>(headers)
        val response = restTemplate.exchange<DocumentList>(
            baseUrl + "api/events/documents?eventIds=" + eventId,
            HttpMethod.GET,
            request,
            1
        )
        return response.body
    }

    fun getEventEntryList(baseUrl: String, apiKey: String?, eventId: String): EntryList? {
        val headers = HttpHeaders()
        headers["ApiKey"] = apiKey

        val request = HttpEntity<String>(headers)
        val response = restTemplate.exchange<EntryList>(
            baseUrl + "api/entries?includePersonElement=true&includeEntryFees=true&eventIds=" + eventId,
            HttpMethod.GET,
            request,
            1
        )
        return response.body
    }

    fun getEventStartList(baseUrl: String, apiKey: String?, eventId: String): StartList? {
        val headers = HttpHeaders()
        headers["ApiKey"] = apiKey

        val request = HttpEntity<String>(headers)
        val response = restTemplate.exchange<StartList>(
            baseUrl + "api/starts/event?eventId=" + eventId,
            HttpMethod.GET,
            request,
            1
        )
        return response.body
    }

    fun getEventResultList(baseUrl: String, apiKey: String?, eventId: String): ResultList? {
        val headers = HttpHeaders()
        headers["ApiKey"] = apiKey

        val request = HttpEntity<String>(headers)
        val response = restTemplate.exchange<ResultList>(
            baseUrl + "api/results/event?eventId=" + eventId + "&includeSplitTimes=true",
            HttpMethod.GET,
            request,
            1
        )
        return response.body
    }

    fun getEventEntryFees(eventor: Eventor, eventId: String): EntryFeeList? {
        val headers = HttpHeaders()
        headers["ApiKey"] = eventor.eventorApiKey

        val request = HttpEntity<String>(headers)
        val response = restTemplate.exchange<EntryFeeList>(
            eventor.baseUrl + "api/entryfees/events/" + eventId,
            HttpMethod.GET,
            request,
            1
        )
        return response.body
    }

}
