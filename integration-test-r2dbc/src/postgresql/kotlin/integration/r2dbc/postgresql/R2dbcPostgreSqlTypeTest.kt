package integration.r2dbc.postgresql

import integration.r2dbc.R2dbcEnv
import integration.r2dbc.inTransaction
import io.r2dbc.postgresql.codec.Interval
import io.r2dbc.postgresql.codec.Json
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.extension.ExtendWith
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.query.first
import org.komapper.r2dbc.R2dbcDatabase
import java.time.Period
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(R2dbcEnv::class)
class R2dbcPostgreSqlTypeTest(val db: R2dbcDatabase) {

    @Test
    fun interval(info: TestInfo) = inTransaction(db, info) {
        val m = Meta.intervalData
        val data = IntervalData(1, Interval.of(Period.of(2022, 2, 5)))
        db.runQuery { QueryDsl.insert(m).single(data) }
        val data2 = db.runQuery {
            QueryDsl.from(m).where { m.id eq 1 }.first()
        }
        assertEquals(data, data2)
    }

    @Test
    fun interval_null(info: TestInfo) = inTransaction(db, info) {
        val m = Meta.intervalData
        val data = IntervalData(1, null)
        db.runQuery { QueryDsl.insert(m).single(data) }
        val data2 = db.runQuery {
            QueryDsl.from(m).where { m.id eq 1 }.first()
        }
        assertEquals(data, data2)
    }

    @Test
    fun json(info: TestInfo) = inTransaction(db, info) {
        val m = Meta.jsonData
        val data = JsonData(
            1,
            Json.of(
                """
            {"a": 100, "b": "Hello"}
                """.trimIndent(),
            ),
        )
        db.runQuery { QueryDsl.insert(m).single(data) }
        val data2 = db.runQuery {
            QueryDsl.from(m).where { m.id eq 1 }.first()
        }
        assertEquals(data.value!!.asString(), data2.value!!.asString())

        val result = db.runQuery {
            QueryDsl.fromTemplate("select value->'b' as x from json_data")
                .select { it.get("x", Json::class)!! }
                .first()
        }
        assertEquals("\"Hello\"", result.asString())
    }

    @Test
    fun json_null(info: TestInfo) = inTransaction(db, info) {
        val m = Meta.jsonData
        val data = JsonData(1, null)
        db.runQuery { QueryDsl.insert(m).single(data) }
        val data2 = db.runQuery {
            QueryDsl.from(m).where { m.id eq 1 }.first()
        }
        assertEquals(data, data2)
    }
}
