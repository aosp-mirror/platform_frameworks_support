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

import androidx.annotation.NonNull
import androidx.room.parser.SqlParser
import androidx.room.testing.TestInvocation
import androidx.room.testing.TestProcessor
import com.google.auto.common.MoreElements
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class QueryUtilsTest {

    companion object {
        const val DATABASE_PREFIX = """
            package foo.bar;
            import androidx.room.*;
            import androidx.annotation.NonNull;
            import java.util.*;
        """

        val ENTITIES = listOf(
            JavaFileObjects.forSourceString(
                "foo.bar.User", DATABASE_PREFIX + """
                    @Entity
                    public class User {
                        @PrimaryKey
                        public int id;
                        public String firstName;
                        public String lastName;
                        public int teamId;
                    }
                """
            ),
            JavaFileObjects.forSourceString(
                "foo.bar.Team", DATABASE_PREFIX + """
                    @Entity
                    public class Team {
                        @PrimaryKey
                        public int id;
                        public String name;
                    }
                """
            ),
            JavaFileObjects.forSourceString(
                "foo.bar.Employee", DATABASE_PREFIX + """
                    @Entity
                    public class Employee {
                        @PrimaryKey
                        public int id;
                        public String name;
                        public Integer managerId;
                    }
                """
            )
        )
    }

    @Test
    fun summary() {
        withEntities(
            "foo.bar.UserSummary", """
                public class UserSummary {
                    public int id;
                    public String firstName;
                }
            """,
            "SELECT * FROM User"
        ) { expanded, _ ->
            assertThat(expanded, `is`(equalTo("SELECT `id`, `firstName` FROM User")))
        }.compilesWithoutError()
    }

    @Test
    fun irrelevantAlias() {
        withEntities(
            "foo.bar.UserCopy", """
                public class UserCopy {
                    @Embedded
                    public User user;
                }
            """,
            "SELECT * FROM User u"
        ) { expanded, _ ->
            assertThat(
                expanded, `is`(
                    equalTo(
                        """
                SELECT `u`.`id`, `u`.`firstName`, `u`.`lastName`, `u`.`teamId` FROM User u
                        """.trim()
                    )
                )
            )
        }.compilesWithoutError()
    }

    @Test
    fun additional() {
        withEntities(
            "foo.bar.UserSummary", """
                public class UserSummary {
                    public int id;
                    public String name;
                }
            """,
            "SELECT *, firstName | ' ' | lastName AS name FROM User"
        ) { expanded, _ ->
            assertThat(
                expanded,
                `is`(equalTo("SELECT `id`, firstName | ' ' | lastName AS name FROM User"))
            )
        }.compilesWithoutError()
    }

    @Test
    fun ignore() {
        withEntities(
            "foo.bar.UserSummary", """
                public class UserSummary {
                    public int id;
                    public String firstName;
                    @Ignore
                    public String lastName;
                }
            """,
            "SELECT * FROM User"
        ) { expanded, _ ->
            assertThat(expanded, `is`(equalTo("SELECT `id`, `firstName` FROM User")))
        }.compilesWithoutError()
    }

    @Test
    fun join() {
        withEntities(
            "foo.bar.UserDetail", """
                public class UserDetail {
                    @Embedded
                    public User user;
                    @Embedded(prefix = "team_")
                    public Team team;
                }
            """,
            "SELECT * FROM User INNER JOIN Team AS team_ ON User.teamId = team_.id"
        ) { expanded, _ ->
            assertThat(
                expanded, `is`(
                    equalTo(
                        "SELECT `User`.`id`, `User`.`firstName`, `User`.`lastName`, " +
                                "`User`.`teamId`, `team_`.`id` AS `team_id`, " +
                                "`team_`.`name` AS `team_name` FROM User INNER JOIN " +
                                "Team AS team_ ON User.teamId = team_.id"
                    )
                )
            )
        }.compilesWithoutError()
    }

    @Test
    fun joinSelf() {
        withEntities(
            "foo.bar.EmployeeWithManager", """
                public class EmployeeWithManager {
                    @Embedded
                    public Employee employee;
                    @Embedded(prefix = "manager_")
                    public Employee manager;
                }
            """,
            "SELECT * FROM Employee LEFT OUTER JOIN Employee AS manager_ " +
                    "ON User.managerId = manager_.id"
        ) { expanded, _ ->
            assertThat(
                expanded,
                `is`(
                    equalTo(
                        "SELECT `Employee`.`id`, `Employee`.`name`, `Employee`.`managerId`, " +
                                "`manager_`.`id` AS `manager_id`, `manager_`.`name` AS `manager_name`, " +
                                "`manager_`.`managerId` AS `manager_managerId` FROM Employee " +
                                "LEFT OUTER JOIN Employee AS manager_ ON User.managerId = manager_.id"
                    )
                )
            )
        }.compilesWithoutError()
    }

    @Test
    fun specifyTable() {
        withEntities(
            "foo.bar.UserDetail", """
                public class UserDetail {
                    @Embedded
                    public User user;
                    @Embedded(prefix = "team_")
                    public Team team;
                }
            """,
            "SELECT User.*, team_.* FROM User INNER JOIN Team AS team_ ON User.teamId = team_.id"
        ) { expanded, _ ->
            assertThat(
                expanded, `is`(
                    equalTo(
                        "SELECT `User`.`id`, `User`.`firstName`, `User`.`lastName`, " +
                                "`User`.`teamId`, `team_`.`id` AS `team_id`, " +
                                "`team_`.`name` AS `team_name` FROM User INNER JOIN " +
                                "Team AS team_ ON User.teamId = team_.id"
                    )
                )
            )
        }.compilesWithoutError()
    }

    @Test
    fun specifyAlias() {
        withEntities(
            "foo.bar.UserPair", """
                public class UserPair {
                    @Embedded(prefix = "a_")
                    public User a;
                    @Embedded(prefix = "b_")
                    public User b;
                }
            """,
            "SELECT a_.*, b_.* FROM User AS a_, User AS b_"
        ) { expanded, _ ->
            assertThat(
                expanded, `is`(
                    equalTo(
                        "SELECT `a_`.`id` AS `a_id`, " +
                                "`a_`.`firstName` AS `a_firstName`, `a_`.`lastName` AS `a_lastName`, " +
                                "`a_`.`teamId` AS `a_teamId`, `b_`.`id` AS `b_id`, " +
                                "`b_`.`firstName` AS `b_firstName`, `b_`.`lastName` AS `b_lastName`, " +
                                "`b_`.`teamId` AS `b_teamId` FROM User AS a_, User AS b_"
                    )
                )
            )
        }.compilesWithoutError()
    }

    @Test
    fun parameter() {
        withEntities(
            "foo.bar.UserSummary", """
                public class UserSummary {
                    public int id;
                    public String firstName;
                }
            """,
            "SELECT id, firstName FROM User WHERE id = :id"
        ) { expanded, _ ->
            assertThat(expanded, `is`(equalTo("SELECT id, firstName FROM User WHERE id = ?")))
        }.compilesWithoutError()
    }

    @Test
    fun irrelevantQueries() {
        withEntities(
            "foo.bar.UserSummary", """
                public class UserSummary {
                    public int id;
                    public String firstName;
                }
            """,
            "SELECT id, firstName FROM User"
        ) { expanded, _ ->
            assertThat(expanded, `is`(equalTo("SELECT id, firstName FROM User")))
        }.compilesWithoutError()
    }

    @Test
    fun scattered() {
        withEntities(
            "foo.bar.UserSummary", """
               public class UserSummary {
                   public int id;
                   public String firstName;
               }
            """,
            "SELECT User.* FROM User"
        ) { expanded, _ ->
            assertThat(expanded, `is`(equalTo("SELECT `User`.`id`, `User`.`firstName` FROM User")))
        }.compilesWithoutError()
    }

    @Test
    fun scatteredAndAliased() {
        withEntities(
            "foo.bar.UserSummary", """
               public class UserSummary {
                   public int id;
                   public String firstName;
               }
            """,
            "SELECT u.* FROM User u"
        ) { expanded, _ ->
            assertThat(expanded, `is`(equalTo("SELECT `u`.`id`, `u`.`firstName` FROM User u")))
        }.compilesWithoutError()
    }

    @Test
    fun newlineInProjection() {
        withEntities(
            "foo.bar.UserSummary", """
                public class UserSummary {
                    public int id;
                    public String name;
                }
            """, """
                SELECT User
                .
                *,
                firstName
                |
                ' '
                |
                lastName
                AS
                `name` FROM User
            """
        ) { expanded, _ ->
            assertThat(
                expanded, `is`(
                    equalTo(
                        """
                SELECT `User`.`id`,
                firstName
                |
                ' '
                |
                lastName
                AS
                `name` FROM User
            """
                    )
                )
            )
        }.compilesWithoutError()
    }

    private fun withEntities(
        name: String,
        input: String,
        original: String,
        classLoader: ClassLoader = javaClass.classLoader,
        handler: (expanded: String, invocation: TestInvocation) -> Unit
    ): CompileTester {
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
            .that(ENTITIES + JavaFileObjects.forSourceString(name, DATABASE_PREFIX + input))
            .withClasspathFrom(classLoader)
            .processedWith(
                TestProcessor.builder()
                    .forAnnotations(
                        androidx.room.Entity::class,
                        androidx.room.PrimaryKey::class,
                        androidx.room.Embedded::class,
                        androidx.room.ColumnInfo::class,
                        NonNull::class
                    )
                    .nextRunHandler { invocation ->
                        val entities = invocation.roundEnv
                            .getElementsAnnotatedWith(androidx.room.Entity::class.java)
                            .map { element ->
                                TableEntityProcessor(
                                    invocation.context,
                                    MoreElements.asType(element)
                                ).process()
                            }
                        val pojoElement = invocation.roundEnv
                            .rootElements
                            .first { it.toString() == name }
                        val pojo = PojoProcessor.createFor(
                            invocation.context,
                            MoreElements.asType(pojoElement),
                            bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                            parent = null
                        ).process()
                        val query = SqlParser.parse(original)
                        val interpreter = QueryInterpreter(entities)
                        val expanded = interpreter.expandProjection(query, pojo)
                        handler(expanded, invocation)
                        true
                    }
                    .build())
    }
}
