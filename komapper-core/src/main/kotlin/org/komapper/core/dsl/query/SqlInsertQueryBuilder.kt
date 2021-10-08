package org.komapper.core.dsl.query

import org.komapper.core.ThreadSafe
import org.komapper.core.dsl.declaration.ValuesDeclaration
import org.komapper.core.dsl.expression.SubqueryExpression

@ThreadSafe
interface SqlInsertQueryBuilder<T : Any> {
    fun values(declaration: ValuesDeclaration<T>): SqlInsertQuery<T>
    fun select(block: () -> SubqueryExpression<T>): SqlInsertQuery<T>
}

internal class SqlInsertQueryBuilderImpl<T : Any>(private val query: SqlInsertQuery<T>) : SqlInsertQueryBuilder<T> {
    override fun values(declaration: ValuesDeclaration<T>): SqlInsertQuery<T> {
        return query.values(declaration)
    }

    override fun select(block: () -> SubqueryExpression<T>): SqlInsertQuery<T> {
        return query.select(block)
    }
}
