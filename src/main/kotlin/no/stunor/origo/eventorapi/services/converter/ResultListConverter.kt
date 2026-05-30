package no.stunor.origo.eventorapi.services.converter

import no.stunor.origo.eventorapi.model.Eventor
import no.stunor.origo.eventorapi.model.event.entry.Entry
import no.stunor.origo.eventorapi.model.event.entry.EntryStatus
import no.stunor.origo.eventorapi.model.event.entry.PersonEntry
import no.stunor.origo.eventorapi.model.event.entry.Result
import no.stunor.origo.eventorapi.model.event.entry.ResultStatus
import no.stunor.origo.eventorapi.model.event.entry.SplitTime
import no.stunor.origo.eventorapi.model.event.entry.TeamEntry
import no.stunor.origo.eventorapi.model.event.entry.TeamMember
import no.stunor.origo.eventorapi.model.organisation.Organisation
import org.iof.eventor.Event
import org.iof.eventor.ResultList
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class ResultListConverter(
    private val organisationConverter: OrganisationConverter,
    private val personConverter: PersonConverter,
    private val entryListConverter: EntryListConverter
) {

    @Throws(NumberFormatException::class, ParseException::class)
    fun convertEventResultList(eventor: Eventor, resultList: ResultList, orgCache: Map<String, Organisation?> = emptyMap()): List<Entry> {
        val competitorList = mutableListOf<Entry>()

        for (classResult in resultList.classResult) {
            for (personOrTeamResult in classResult.personResultOrTeamResult) {
                when (personOrTeamResult) {
                    is org.iof.eventor.PersonResult -> handlePersonResult(eventor, resultList.event, classResult, personOrTeamResult, competitorList, orgCache)
                    is org.iof.eventor.TeamResult -> competitorList.add(convertTeamResult(eventor, resultList.event, classResult, personOrTeamResult, orgCache))
                }
            }
        }
        return competitorList
    }

    private fun handlePersonResult(
        eventor: Eventor,
        event: Event,
        classResult: org.iof.eventor.ClassResult,
        personResult: org.iof.eventor.PersonResult,
        competitorList: MutableList<Entry>,
        orgCache: Map<String, Organisation?>
    ) {
        val raceResults:  List<org.iof.eventor.RaceResult> = personResult.raceResult
        if (raceResults.isNotEmpty()) {
            for (raceResult in raceResults) {
                competitorList.add(convertMultiDayPersonResult(eventor, classResult, personResult, raceResult, orgCache))
            }
        } else {
            competitorList.add(convertOneDayPersonResult(eventor, event, classResult, personResult, orgCache))
        }
    }

    @Throws(NumberFormatException::class, ParseException::class)
    private fun convertOneDayPersonResult(
        eventor: Eventor,
        event: Event,
        classResult: org.iof.eventor.ClassResult,
        personResult: org.iof.eventor.PersonResult,
        orgCache: Map<String, Organisation?>
    ): Entry {
        return PersonEntry(
            raceEventorRef = event.eventRace[0].eventRaceId.content,
            classEventorRef = classResult.eventClass.eventClassId.content,
            personEventorRef = if (personResult.person.personId != null) personResult.person.personId.content else null,
            name = personConverter.convertPersonName(personResult.person.personName),
            organisation = if(personResult.organisation != null)
                organisationConverter.convertOrganisation(personResult.organisation, eventor.id, orgCache)
            else
                organisationConverter.convertOrganisation(personResult.organisationId, eventor.id, orgCache),
            birthYear = if (personResult.person.birthDate != null) personResult.person.birthDate.date.content.substring(
                0,
                4
            ).toInt() else null,
            nationality = if (personResult.person.nationality != null) personResult.person.nationality.country.alpha3.value else null,
            gender = personConverter.convertGender(personResult.person.sex),
            bib = if (personResult.result.bibNumber != null) personResult.result.bibNumber.content else null,
            punchingUnits = entryListConverter.convertPunchingUnits(personResult.result.cCardIdOrCCard?.filterIsInstance<org.iof.eventor.CCard>() ?: emptyList()),
            startTime = if (personResult.result.startTime != null) TimeStampConverter.parseDate(
                "${personResult.result.startTime.date.content} ${personResult.result.startTime.clock.content}",
                eventor.id
            ) else null,
            finishTime = if (personResult.result.finishTime != null) TimeStampConverter.parseDate(
                "${personResult.result.finishTime.date.content} ${personResult.result.finishTime.clock.content}",
                eventor.id
            ) else null,
            result = convertPersonResult(personResult.result),
            splitTimes = convertSplitTimes(personResult.result.splitTime),
            status = EntryStatus.Finished
        )
    }

    @Throws(NumberFormatException::class, ParseException::class)
    private fun convertMultiDayPersonResult(
        eventor: Eventor,
        classResult: org.iof.eventor.ClassResult,
        personResult: org.iof.eventor.PersonResult,
        raceResult: org.iof.eventor.RaceResult,
        orgCache: Map<String, Organisation?>
    ): Entry {
        return PersonEntry(
            raceEventorRef = raceResult.eventRaceId.content,
            classEventorRef = classResult.eventClass.eventClassId.content,
            personEventorRef = if (personResult.person.personId != null) personResult.person.personId.content else null,
            name = personConverter.convertPersonName(personResult.person.personName),
            organisation = if(personResult.organisation != null)
                organisationConverter.convertOrganisation(personResult.organisation, eventor.id, orgCache)
            else
                organisationConverter.convertOrganisation(personResult.organisationId, eventor.id, orgCache),
            birthYear = if (personResult.person.birthDate != null) personResult.person.birthDate.date.content.substring(
                0,
                4
            ).toInt() else null,
            nationality = if (personResult.person.nationality != null) personResult.person.nationality.country.alpha3.value else null,
            gender = personConverter.convertGender(personResult.person.sex),
            bib = if (raceResult.result?.bibNumber != null) raceResult.result.bibNumber.content else null,
            punchingUnits = entryListConverter.convertPunchingUnits(raceResult.result?.cCardIdOrCCard?.filterIsInstance<org.iof.eventor.CCard>() ?: emptyList()),
            startTime = if (raceResult.result?.startTime != null) TimeStampConverter.parseDate(
                "${raceResult.result.startTime.date.content} ${raceResult.result.startTime.clock.content}",
                eventor.id
            ) else null,
            finishTime = if (raceResult.result?.finishTime != null) TimeStampConverter.parseDate(
                "${raceResult.result.finishTime.date.content} ${raceResult.result.finishTime.clock.content}",
                eventor.id
            ) else null,
            result = convertPersonResult(raceResult.result),
            splitTimes = convertSplitTimes(raceResult.result?.splitTime),
            status = EntryStatus.Finished
        )
    }

    @Throws(NumberFormatException::class, ParseException::class)
    fun convertPersonResult(result: org.iof.eventor.Result?): Result? {
        if (result == null || result.competitorStatus.value == "Inactive")
            return null
        return Result(
            time = if (result.time != null) convertTimeSec(result.time.content) else null,
            timeBehind = if (result.timeDiff != null) convertTimeSec(result.timeDiff.content) else null,
            position = if (result.resultPosition != null && result.resultPosition.content != "0") result.resultPosition.content.toInt() else null,
            status = ResultStatus.valueOf(result.competitorStatus.value)
        )
    }

    @Throws(NumberFormatException::class, ParseException::class)
    private fun convertTeamResult(
        eventor: Eventor,
        event: Event,
        classResult: org.iof.eventor.ClassResult,
        teamResult: org.iof.eventor.TeamResult,
        orgCache: Map<String, Organisation?>
    ): Entry {
        return TeamEntry(
            raceEventorRef = event.eventRace[0].eventRaceId.content,
            classEventorRef = classResult.eventClass.eventClassId.content,
            name = teamResult.teamName.content,
            organisations =  organisationConverter.convertOrganisations(
                organisations = teamResult.organisationIdOrOrganisationOrCountryId,
                eventorId = eventor.id,
                orgCache = orgCache
            ),
            teamMembers = convertTeamMembers(eventor, teamResult.teamMemberResult),
            bib = if (teamResult.bibNumber != null) teamResult.bibNumber.content else null,
            startTime = if (teamResult.startTime != null) TimeStampConverter.parseDate(
                "${teamResult.startTime.date.content} ${teamResult.startTime.clock.content}",
                eventor.id
            ) else null,
            finishTime = if (teamResult.finishTime != null) TimeStampConverter.parseDate(
                "${teamResult.finishTime.date.content} ${teamResult.finishTime.clock.content}",
                eventor.id
            ) else null,
            result = convertTeamResult(teamResult),
            status = EntryStatus.Finished
        )
    }

    @Throws(NumberFormatException::class, ParseException::class)
    private fun convertSplitTimes(splitTimes: List<org.iof.eventor.SplitTime>?): MutableList<SplitTime> {
        if (splitTimes == null)
            return mutableListOf()
        val result: MutableList<SplitTime> = mutableListOf()
        for (splitTime in splitTimes) {
            result.add(convertSplitTime(splitTime))
        }
        return result
    }

    @Throws(NumberFormatException::class, ParseException::class)
    private fun convertSplitTime(splitTime: org.iof.eventor.SplitTime): SplitTime {
        return SplitTime(
            sequence = splitTime.sequence.toInt(),
            controlCode = splitTime.controlCode.content,
            time = if (splitTime.time != null) convertTimeSec(splitTime.time.content) else null
        )
    }

    @Throws(ParseException::class)
    fun convertTeamMembers(
        eventor: Eventor,
        teamMembers: List<org.iof.eventor.TeamMemberResult>
    ): MutableList<TeamMember> {
        val result  = mutableListOf<TeamMember>()
        for (teamMember in teamMembers) {
            result.add(convertTeamMember(eventor, teamMember))
        }
        return result
    }

    @Throws(ParseException::class)
    private fun convertTeamMember(
        eventor: Eventor,
        teamMember: org.iof.eventor.TeamMemberResult
    ): TeamMember {
        return TeamMember(
            personEventorRef = if (teamMember.person != null && teamMember.person.personId != null) teamMember.person.personId.content else null,
            name = if (teamMember.person != null) personConverter.convertPersonName(teamMember.person.personName) else null,
            birthYear = if (teamMember.person != null && teamMember.person.birthDate != null) teamMember.person.birthDate.date.content.substring(
                0,
                4
            ).toInt() else null,
            nationality = if (teamMember.person != null && teamMember.person.nationality != null) teamMember.person.nationality.country.alpha3.value else null,
            gender = if (teamMember.person != null) personConverter.convertGender(teamMember.person.sex) else null,
            leg = teamMember.leg.toInt(),
            startTime = if (teamMember.startTime != null) TimeStampConverter.parseDate(
                "${teamMember.startTime.date.content} ${teamMember.startTime.clock.content}",
                eventor.id
            ) else null,
            finishTime = if (teamMember.finishTime != null) TimeStampConverter.parseDate(
                "${teamMember.finishTime.date.content} ${teamMember.finishTime.clock.content}",
                eventor.id
            ) else null,
            legResult = convertLegResult(teamMember),
            overallResult = if (teamMember.overallResult != null) convertOverallResult(teamMember.overallResult) else null,
            splitTimes = convertSplitTimes(teamMember.splitTime)
        )
    }

    @Throws(NumberFormatException::class, ParseException::class)
    fun convertTeamResult(teamResult: org.iof.eventor.TeamResult): Result? {
        if (teamResult.teamStatus == null || teamResult.teamStatus.value == "Inactive")
            return null
        return Result(
            time = if (teamResult.time != null) convertTimeSec(teamResult.time.content) else null,
            timeBehind = if (teamResult.timeDiff != null) convertTimeSec(teamResult.timeDiff.content) else null,
            position = if (teamResult.resultPosition != null && teamResult.resultPosition.content != "0") teamResult.resultPosition.content.toInt() else null,
            status = ResultStatus.valueOf(teamResult.teamStatus.value)
        )
    }

    @Throws(NumberFormatException::class, ParseException::class)
    private fun convertOverallResult(overallResult: org.iof.eventor.OverallResult): Result {
        return Result(
            time = if (overallResult.time != null) convertTimeSec(overallResult.time.content) else null,
            timeBehind = if (overallResult.timeDiff != null) convertTimeSec(overallResult.timeDiff.content) else null,
            position = if (overallResult.resultPosition != null && overallResult.resultPosition.content != "0") overallResult.resultPosition.content.toInt() else null,
            status = ResultStatus.valueOf(overallResult.teamStatus.value)
        )
    }

    @Throws(ParseException::class)
    private fun convertLegResult(teamMember: org.iof.eventor.TeamMemberResult): Result {
        return Result(
            time = if (teamMember.time != null) convertTimeSec(teamMember.time.content) else null,
            timeBehind = if (teamMember.timeBehind != null) getTimeBehind(teamMember.timeBehind) else null,
            position = if (teamMember.position != null) getLegPosition(teamMember.position) else null,
            status = ResultStatus.valueOf(teamMember.competitorStatus.value)
        )
    }

    private fun getLegPosition(positionList: List<org.iof.eventor.TeamMemberResult.Position>): Int? {
        for (position in positionList) {
            if (position.type == "Leg" && position.value.toInt() > 0) {
                return position.value.toInt()
            }
        }
        return null
    }

    @Throws(ParseException::class)
    fun convertTimeSec(time: String?): Int {
        var date: Date
        var reference: Date
        try {
            val dateFormat: DateFormat = SimpleDateFormat("HH:mm:ss")
            reference = dateFormat.parse("00:00:00")
            date = dateFormat.parse(time)
        } catch (_: ParseException) {
            val dateFormat: DateFormat = SimpleDateFormat("mm:ss")
            reference = dateFormat.parse("00:00")
            date = dateFormat.parse(time)
        }
        val seconds = (date.time - reference.time) / 1000L
        return seconds.toInt()
    }


    private fun getTimeBehind(timeBehindList: List<org.iof.eventor.TeamMemberResult.TimeBehind>): Int? {
        for (timeBehind in timeBehindList) {
            if (timeBehind.type == "Leg") {
                return timeBehind.value.toInt()
            }
        }
        return null
    }
}