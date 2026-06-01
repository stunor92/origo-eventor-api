package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.person.UserPerson
import no.stunor.origo.eventorapi.model.person.UserPersonKey
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.*
import javax.sql.DataSource

internal object UserPersonTable : Table("user_person") {
    val userId = javaUUID("user_id")
    val personId = javaUUID("person_id").references(PersonTable.id)

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