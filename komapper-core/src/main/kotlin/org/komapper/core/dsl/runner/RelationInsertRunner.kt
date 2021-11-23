package org.komapper.core.dsl.runner

import org.komapper.core.DatabaseConfig
import org.komapper.core.Statement
import org.komapper.core.dsl.builder.RelationInsertStatementBuilder
import org.komapper.core.dsl.context.RelationInsertContext
import org.komapper.core.dsl.expression.Operand
import org.komapper.core.dsl.metamodel.EntityMetamodel
import org.komapper.core.dsl.metamodel.IdGenerator
import org.komapper.core.dsl.metamodel.PropertyMetamodel
import org.komapper.core.dsl.options.InsertOptions

class RelationInsertRunner<ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>>(
    private val context: RelationInsertContext<ENTITY, ID, META>,
    @Suppress("unused") private val options: InsertOptions = InsertOptions.default
) : Runner {

    override fun dryRun(config: DatabaseConfig): Statement {
        val idAssignment = when (val idGenerator = context.target.idGenerator()) {
            is IdGenerator.Sequence<ENTITY, ID> -> {
                val argument = Operand.Argument(idGenerator.property, null)
                idGenerator.property to argument
            }
            else -> null
        }
        val versionAssignment = context.target.versionAssignment()
        val clock = config.clockProvider.now()
        val createdAtAssignment = context.target.createdAtAssignment(clock)
        val updatedAtAssignment = context.target.updatedAtAssignment(clock)
        return buildStatement(config, idAssignment, versionAssignment, createdAtAssignment, updatedAtAssignment)
    }

    fun buildStatement(
        config: DatabaseConfig,
        idAssignment: Pair<PropertyMetamodel<ENTITY, ID, *>, Operand>? = null,
        versionAssignment: Pair<PropertyMetamodel<ENTITY, *, *>, Operand>? = null,
        createdAtAssignment: Pair<PropertyMetamodel<ENTITY, *, *>, Operand>? = null,
        updatedAtAssignment: Pair<PropertyMetamodel<ENTITY, *, *>, Operand>? = null
    ): Statement {
        val builder = RelationInsertStatementBuilder(
            config.dialect,
            context,
            idAssignment,
            versionAssignment,
            createdAtAssignment,
            updatedAtAssignment
        )
        return builder.build()
    }
}
