package org.komapper.jdbc.dsl.runner

import kotlinx.coroutines.flow.Flow
import org.komapper.core.DatabaseConfig
import org.komapper.core.DryRunStatement
import org.komapper.core.dsl.context.SelectContext
import org.komapper.core.dsl.runner.SelectRunner
import org.komapper.jdbc.JdbcDataOperator
import org.komapper.jdbc.JdbcDatabaseConfig
import org.komapper.jdbc.JdbcExecutor
import java.sql.ResultSet

internal class JdbcSelectRunner<T, R>(
    private val context: SelectContext<*, *, *>,
    private val transform: (JdbcDataOperator, ResultSet) -> T,
    private val collect: suspend (Flow<T>) -> R,
) :
    JdbcRunner<R> {

    private val runner: SelectRunner = SelectRunner(context)

    override fun check(config: DatabaseConfig) {
        runner.check(config)
    }

    override fun run(config: JdbcDatabaseConfig): R {
        val statement = runner.buildStatement(config)
        val executor = JdbcExecutor(config, context.options)
        return executor.executeQuery(statement, transform, collect)
    }

    override fun dryRun(config: DatabaseConfig): DryRunStatement {
        return runner.dryRun(config)
    }
}
