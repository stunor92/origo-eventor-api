package no.stunor.origo.eventorapi.data

import no.stunor.origo.eventorapi.model.person.User
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import javax.sql.DataSource

private object UserAuthTable : Table("auth.users") {
    val id = text("id")
    override val primaryKey = PrimaryKey(id)
}

open class UserRepository(dataSource: DataSource) {

    private val database = Database.connect(dataSource)

    private fun toUser(row: ResultRow): User {
        return User(id = row[UserAuthTable.id])
    }

    open fun findById(id: String): User? {
        return transaction(database) {
            UserAuthTable
                .selectAll()
                .where { UserAuthTable.id eq id }
                .limit(1)
                .map(::toUser)
                .singleOrNull()
        }
    }

    open fun save(user: User): User {
        transaction(database) {
            UserAuthTable.upsert {
                it[UserAuthTable.id] = user.id
            }
        }
        return user
    }
}