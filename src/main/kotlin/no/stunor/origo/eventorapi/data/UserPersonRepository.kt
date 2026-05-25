package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.person.UserPerson
import no.stunor.origo.eventorapi.model.person.UserPersonKey
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.util.*
import javax.sql.DataSource

private object UserPersonTable : Table("user_person") {
    val userId = uuid("user_id")
    val personId = uuid("person_id")

    override val primaryKey = PrimaryKey(userId, personId)
}

open class UserPersonRepository(dataSource: DataSource) {

    private val database = Database.connect(dataSource)

    private fun toUserPerson(row: ResultRow): UserPerson {
        return UserPerson(
            id = UserPersonKey(
                userId = row[UserPersonTable.userId],
                personId = row[UserPersonTable.personId]
            ),
            person = null // Avoid circular dependency
        )
    }

    open fun findAllByPersonId(personId: UUID): List<UserPerson> {
        return transaction(database) {
            UserPersonTable
                .selectAll()
                .where { UserPersonTable.personId eq personId }
                .map(::toUserPerson)
        }
    }

    open fun save(userPerson: UserPerson): UserPerson {
        transaction(database) {
            UserPersonTable.upsert {
                it[UserPersonTable.userId] = userPerson.id.userId!!
                it[UserPersonTable.personId] = userPerson.id.personId!!
            }
        }
        return userPerson
    }

}