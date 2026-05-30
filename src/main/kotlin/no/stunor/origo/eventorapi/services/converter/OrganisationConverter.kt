package no.stunor.origo.eventorapi.services.converter

import no.stunor.origo.eventorapi.data.OrganisationRepository
import no.stunor.origo.eventorapi.data.RegionRepository
import no.stunor.origo.eventorapi.model.organisation.Organisation
import no.stunor.origo.eventorapi.model.organisation.OrganisationType



class OrganisationConverter(
    private val organisationRepository: OrganisationRepository,
    private val regionRepository: RegionRepository
) {

    fun convertOrganisations(organisations: List<Any>, eventorId: String, orgCache: Map<String, Organisation?> = emptyMap()): MutableList<Organisation> {
        val result = mutableListOf<Organisation>()
        for (organisation in organisations) {
            convertOrganisation(organisation, eventorId, orgCache)?.let { result.add(it) }
        }
        return result
    }

    fun convertOrganisation(organisation: Any?, eventorId: String, orgCache: Map<String, Organisation?> = emptyMap()): Organisation? {
        if (organisation == null) return null
        return when (organisation) {
            is org.iof.eventor.Organisation -> mergeOrganisationWithCache(organisation, eventorId, orgCache)
            is org.iof.eventor.OrganisationId -> orgCache[organisation.content]
                ?: organisationRepository.findByEventorRefAndEventorId(organisation.content, eventorId)
            is String -> orgCache[organisation]
                ?: organisationRepository.findByEventorRefAndEventorId(organisation, eventorId)
            else -> null
        }
    }

    private fun mergeOrganisationWithCache(organisation: org.iof.eventor.Organisation, eventorId: String, orgCache: Map<String, Organisation?>): Organisation? {
        if (organisation.organisationId == null) return null
        val eventorRef = organisation.organisationId.content
        val country = if (organisation.country != null && organisation.country.alpha3 != null && organisation.country.alpha3.value.length == 3)
            organisation.country.alpha3.value else eventorId

        val existing = orgCache[eventorRef] ?: organisationRepository.findByEventorRefAndEventorId(eventorRef, eventorId)
        if (existing != null) {
            existing.name = organisation.name.content
            existing.type = convertOrganisationType(organisation)
            existing.country = country
            // region already populated from cache JOIN; skip extra DB lookup
            return existing
        }

        val region = if (organisation.parentOrganisation != null) {
            organisation.parentOrganisation.organisationId?.content?.let {
                regionRepository.findByEventorRefAndEventorId(it, eventorId)
                    ?: regionRepository.findByEventorRefAndEventorId(eventorRef, eventorId)
            }
        } else regionRepository.findByEventorRefAndEventorId(eventorRef, eventorId)

        return Organisation(
            eventorRef = eventorRef,
            eventorId = eventorId,
            name = organisation.name.content,
            type = convertOrganisationType(organisation),
            country = country,
            region = region
        )
    }

    private fun mergeOrganisation(organisation: org.iof.eventor.Organisation, eventorId: String): Organisation? =
        mergeOrganisationWithCache(organisation, eventorId, emptyMap())

    private fun convertOrganisationType(organisation: org.iof.eventor.Organisation): OrganisationType = when (organisation.organisationTypeId.content) {
        "1" -> OrganisationType.Federation
        "2" -> OrganisationType.Region
        "5" -> OrganisationType.IOF
        else -> OrganisationType.Club
    }
}
