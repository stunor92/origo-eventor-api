package no.stunor.origo.eventorapi.services.converter

import no.stunor.origo.eventorapi.model.Eventor
import no.stunor.origo.eventorapi.model.event.PunchingUnit
import no.stunor.origo.eventorapi.model.event.PunchingUnitType
import no.stunor.origo.eventorapi.model.event.entry.Entry
import no.stunor.origo.eventorapi.model.event.entry.EntryStatus
import no.stunor.origo.eventorapi.model.event.entry.PersonEntry
import no.stunor.origo.eventorapi.model.event.entry.TeamEntry
import no.stunor.origo.eventorapi.model.event.entry.TeamMember
import no.stunor.origo.eventorapi.model.organisation.Organisation
import org.iof.eventor.EntryList

class EntryListConverter(
    private val organisationConverter: OrganisationConverter,
    private val personConverter: PersonConverter
) {

    fun convertEventEntryList(eventor: Eventor, entryList: EntryList): List<Entry> {
        val result = mutableListOf<Entry>()
        for (entry in entryList.entry) {
            if (entry.competitor != null) result.addAll(convertPersonEventEntries(entry, eventor))
            else if (entry.teamCompetitor != null) result.addAll(convertTeamEventEntries(entry, eventor))
        }
        return result
    }

    private fun convertPersonEventEntries(entry: org.iof.eventor.Entry, eventor: Eventor): List<Entry> {
        return entry.eventRaceId.map { raceId ->
            PersonEntry(
                raceEventorRef = raceId.content,
                classEventorRef = entry.entryClass[0].eventClassId.content,
                personEventorRef = entry.competitor.person.personId?.content,
                name = personConverter.convertPersonName(entry.competitor.person.personName),
                organisation = if (entry.competitor.organisation != null)
                    organisationConverter.convertOrganisation(entry.competitor.organisation, eventor.id)
                else
                    organisationConverter.convertOrganisation(entry.competitor.organisationId, eventor.id),
                birthYear = entry.competitor.person.birthDate?.date?.content?.substring(0, 4)?.toInt(),
                nationality = entry.competitor.person.nationality?.country?.alpha3?.value,
                gender = personConverter.convertGender(entry.competitor.person.sex),
                punchingUnits = convertPunchingUnits(entry.competitor.cCard),
                bib = entry.bibNumber?.content,
                startTime = null, finishTime = null, result = null,
                splitTimes = mutableListOf(),
                status = EntryStatus.SignedUp
            )
        }
    }

    private fun convertTeamEventEntries(entry: org.iof.eventor.Entry, eventor: Eventor): List<Entry> {
        return entry.teamCompetitor[0].entryEntryFee.map { race ->
            TeamEntry(
                raceEventorRef = race.eventRaceId,
                classEventorRef = entry.entryClass[0].eventClassId.content,
                name = entry.teamName.content,
                organisations = convertTeamOrganisations(entry.teamCompetitor, eventor),
                bib = entry.bibNumber?.content,
                teamMembers = convertTeamMembers(entry.teamCompetitor),
                startTime = null, finishTime = null, result = null,
                status = EntryStatus.SignedUp
            )
        }
    }

    private fun convertTeamOrganisations(teamCompetitors: List<org.iof.eventor.TeamCompetitor>, eventor: Eventor): MutableList<Organisation> {
        val result = mutableListOf<Organisation>()
        for (tc in teamCompetitors) {
            if (tc.organisationId != null && !result.any { it.eventorRef == tc.organisationId.content }) {
                organisationConverter.convertOrganisation(tc.organisationId, eventor.id)?.let { result.add(it) }
            }
        }
        return result
    }

    private fun convertTeamMembers(teamMembers: List<org.iof.eventor.TeamCompetitor>): MutableList<TeamMember> =
        teamMembers.map { convertTeamMember(it) }.toMutableList()

    private fun convertTeamMember(teamMember: org.iof.eventor.TeamCompetitor): TeamMember = TeamMember(
        personEventorRef = teamMember.person?.personId?.content,
        name = teamMember.person?.let { personConverter.convertPersonName(it.personName) },
        birthYear = teamMember.person?.birthDate?.date?.content?.substring(0, 4)?.toInt(),
        nationality = teamMember.person?.nationality?.country?.alpha3?.value,
        gender = teamMember.person?.let { personConverter.convertGender(it.sex) },
        punchingUnits = convertPunchingUnits(teamMember.cCard),
        leg = teamMember.teamSequence.content.toInt(),
        startTime = null, finishTime = null, legResult = null, overallResult = null,
        splitTimes = mutableListOf()
    )

    fun convertPunchingUnits(cCards: List<org.iof.eventor.CCard>): MutableList<PunchingUnit> =
        cCards.map { convertPunchingUnit(it) }.toMutableList()

    private fun convertPunchingUnit(cCard: org.iof.eventor.CCard): PunchingUnit =
        PunchingUnit(cCard.cCardId.content, convertPunchingUnitType(cCard.punchingUnitType.value))

    fun convertPunchingUnitTypes(punchingUnitTypes: List<org.iof.eventor.PunchingUnitType>): ArrayList<PunchingUnitType> =
        ArrayList(punchingUnitTypes.map { convertPunchingUnitType(it.value) })

    private fun convertPunchingUnitType(value: String): PunchingUnitType = when (value) {
        "manual" -> PunchingUnitType.Manual
        "Emit" -> PunchingUnitType.Emit
        "SI" -> PunchingUnitType.SI
        "emiTag" -> PunchingUnitType.EmiTag
        else -> PunchingUnitType.Other
    }
}
