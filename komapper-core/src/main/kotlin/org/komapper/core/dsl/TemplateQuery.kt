package org.komapper.core.dsl

import org.komapper.core.dsl.query.ListQuery
import org.komapper.core.dsl.query.Query
import org.komapper.core.dsl.query.Row
import org.komapper.core.dsl.query.TemplateSelectQueryImpl
import org.komapper.core.dsl.query.TemplateUpdateQueryImpl

object TemplateQuery {

    fun <T> select(
        sql: String,
        params: Any = object {},
        provider: Row.() -> T
    ): ListQuery<T> {
        return TemplateSelectQueryImpl(sql, params, provider)
    }

    fun update(sql: String, params: Any = object {}): Query<Int> {
        return TemplateUpdateQueryImpl(sql, params)
    }

    fun insert(sql: String, params: Any = object {}): Query<Int> {
        return TemplateUpdateQueryImpl(sql, params)
    }

    fun delete(sql: String, params: Any = object {}): Query<Int> {
        return TemplateUpdateQueryImpl(sql, params)
    }
}
