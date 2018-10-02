/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room.processor

import androidx.room.parser.SqlParser
import androidx.room.parser.Table
import androidx.room.vo.EmbeddedField
import androidx.room.vo.Field
import androidx.room.vo.Pojo
import mockElementAndType
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItems
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

@RunWith(JUnit4::class)
class QueryUtilsTest {

    @Test
    fun irrelevantQueries() {
        val embeddedFields = listOf(
                embedded("User", "", listOf("id", "name", "teamId")),
                embedded("Team", "team_", listOf("id", "name"))
        )
        listOf(
                "SELECT id FROM User",
                "DELETE FROM User",
                "UPDATE User SET name = :name"
        ).forEach { query ->
            assertThat(QueryUtils.inferProjection(SqlParser.parse(query), embeddedFields),
                    `is`(equalTo(query)))
        }
    }

    @Test
    fun noEmbeddedFields() {
        val query = SqlParser.parse("SELECT * FROM User")
        val sql = QueryUtils.inferProjection(query, emptyList())
        assertThat(sql, `is`(equalTo("SELECT * FROM User")))
    }

    @Test
    fun oneEmbedded() {
        val query = SqlParser.parse("SELECT * FROM User AS user_")
        assertThat(query.tables.size, `is`(1))
        assertThat(query.tables.first(), `is`(equalTo(Table("User", "user_"))))
        val sql = QueryUtils.inferProjection(query, listOf(
                embedded("User", "user_", listOf("id", "name"))))
        assertThat(sql, `is`(equalTo("SELECT `user_`.`id` AS `user_id`, " +
                "`user_`.`name` AS `user_name` FROM User AS user_")))
    }

    @Test
    fun twoEmbedded() {
        val query = SqlParser.parse(
                "SELECT * FROM User AS user_ INNER JOIN Team AS team_ ON user_.teamId = team_.id")
        assertThat(query.tables.size, `is`(2))
        assertThat(query.tables, hasItems(Table("User", "user_"), Table("Team", "team_")))
        val sql = QueryUtils.inferProjection(query, listOf(
                embedded("User", "user_", listOf("id", "name", "teamId")),
                embedded("Team", "team_", listOf("id", "name"))))
        assertThat(sql, `is`(equalTo("SELECT `user_`.`id` AS `user_id`, " +
                "`user_`.`name` AS `user_name`, `user_`.`teamId` AS `user_teamId`, " +
                "`team_`.`id` AS `team_id`, `team_`.`name` AS `team_name` FROM User AS user_ " +
                "INNER JOIN Team AS team_ ON user_.teamId = team_.id")))
    }

    @Test
    fun twoEmbedded_sameTable() {
        val query = SqlParser.parse(
                "SELECT * FROM Team AS a, Team AS b")
        assertThat(query.tables.size, `is`(2))
        assertThat(query.tables, hasItems(Table("Team", "a"), Table("Team", "b")))
        val sql = QueryUtils.inferProjection(query, listOf(
                embedded("Team", "a", listOf("id", "name")),
                embedded("Team", "b", listOf("id", "name"))))
        assertThat(sql, `is`(equalTo("SELECT `a`.`id` AS `aid`, `a`.`name` AS `aname`, " +
                "`b`.`id` AS `bid`, `b`.`name` AS `bname` FROM Team AS a, Team AS b")))
    }

    @Test
    fun prefixMismatch() {
        val query = SqlParser.parse(
                "SELECT * FROM User AS u INNER JOIN Team AS t ON u.teamId = t.id")
        assertThat(query.tables.size, `is`(2))
        assertThat(query.tables, hasItems(Table("User", "u"), Table("Team", "t")))
        val sql = QueryUtils.inferProjection(query, listOf(
                embedded("User", "user_", listOf("id", "name", "teamId")),
                embedded("Team", "team_", listOf("id", "name"))))
        assertThat(sql, `is`(equalTo(
                "SELECT * FROM User AS u INNER JOIN Team AS t ON u.teamId = t.id")))
    }

    @Test
    fun onePrefixOmitted() {
        val query = SqlParser.parse(
                "SELECT * FROM User INNER JOIN Team AS team_ ON User.teamId = team_.id")
        assertThat(query.tables.size, `is`(2))
        assertThat(query.tables, hasItems(Table("User", "User"), Table("Team", "team_")))
        val sql = QueryUtils.inferProjection(query, listOf(
                embedded("User", "", listOf("id", "name", "teamId")),
                embedded("Team", "team_", listOf("id", "name"))))
        assertThat(sql, `is`(equalTo("SELECT `User`.`id`, `User`.`name`, `User`.`teamId`, " +
                "`team_`.`id` AS `team_id`, `team_`.`name` AS `team_name` FROM User " +
                "INNER JOIN Team AS team_ ON User.teamId = team_.id")))
    }

    private fun embedded(
        tableName: String,
        prefix: String,
        fields: List<String>
    ): EmbeddedField {
        // The field name of @Embedded does not matter here.
        return EmbeddedField(field("a"), prefix, null).apply {
            pojo = Pojo(
                    element = mock(TypeElement::class.java).apply {
                        doReturn(StringName(tableName)).`when`(this).simpleName
                    },
                    type = mock(DeclaredType::class.java),
                    fields = fields.map { field(prefix + it) },
                    embeddedFields = emptyList(),
                    constructor = null,
                    relations = emptyList())
        }
    }

    private fun field(name: String): Field {
        val (element, type) = mockElementAndType()
        return Field(
                element = element,
                name = name,
                type = type,
                affinity = null)
    }

    private class StringName(val string: String) : Name {
        override fun get(index: Int) = string[index]
        override fun contentEquals(other: CharSequence?) =
                other != null && string.contentEquals(other)

        override val length = string.length
        override fun subSequence(startIndex: Int, endIndex: Int) =
                string.subSequence(startIndex, endIndex)

        override fun toString() = string
    }
}
