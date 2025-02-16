package org.komapper.core.dsl.builder

import org.komapper.core.BuilderDialect
import org.komapper.core.Statement
import org.komapper.core.StatementBuffer
import org.komapper.core.Value
import org.komapper.core.dsl.context.SelectContext
import org.komapper.core.dsl.context.SetOperationContext
import org.komapper.core.dsl.expression.AggregateFunction
import org.komapper.core.dsl.expression.AliasExpression
import org.komapper.core.dsl.expression.ArithmeticExpression
import org.komapper.core.dsl.expression.CaseExpression
import org.komapper.core.dsl.expression.ColumnExpression
import org.komapper.core.dsl.expression.Criterion
import org.komapper.core.dsl.expression.EscapeExpression
import org.komapper.core.dsl.expression.LiteralExpression
import org.komapper.core.dsl.expression.MathematicalFunction
import org.komapper.core.dsl.expression.Operand
import org.komapper.core.dsl.expression.PropertyExpression
import org.komapper.core.dsl.expression.ScalarExpression
import org.komapper.core.dsl.expression.ScalarQueryExpression
import org.komapper.core.dsl.expression.StringFunction
import org.komapper.core.dsl.expression.SubqueryExpression
import org.komapper.core.dsl.expression.TableExpression

class BuilderSupport(
    private val dialect: BuilderDialect,
    private val aliasManager: AliasManager,
    private val buf: StatementBuffer,
    private val escapeSequence: String? = null,
) {
    private val operation = CriterionOperation()

    fun visitTableExpression(expression: TableExpression<*>, nameType: TableNameType) {
        val name = expression.getCanonicalTableName(dialect::enquote)
        when (nameType) {
            TableNameType.NAME_ONLY -> {
                buf.append(name)
            }
            TableNameType.ALIAS_ONLY -> {
                val alias = aliasManager.getAlias(expression) ?: error("Alias is not found. table=$name ,sql=$buf")
                buf.append(alias)
            }
            TableNameType.NAME_AND_ALIAS -> {
                buf.append(name)
                val alias = aliasManager.getAlias(expression) ?: error("Alias is not found. table=$name ,sql=$buf")
                if (alias.isNotBlank()) {
                    if (dialect.supportsAsKeywordForTableAlias()) {
                        buf.append(" as")
                    }
                    buf.append(" $alias")
                }
            }
        }
    }

    fun visitColumnExpression(expression: ColumnExpression<*, *>) {
        when (expression) {
            is AliasExpression<*, *> -> {
                visitAliasExpression(expression)
            }
            is ArithmeticExpression<*, *> -> {
                visitArithmeticExpression(expression)
            }
            is CaseExpression<*, *> -> {
                visitCaseExpression(expression)
            }
            is LiteralExpression<*> -> {
                visitLiteralExpression(expression)
            }
            is MathematicalFunction<*, *> -> {
                visitMathematicalFunction(expression)
            }
            is ScalarExpression<*, *> -> {
                visitScalarExpression(expression)
            }
            is StringFunction -> {
                visitStringFunction(expression)
            }
            is PropertyExpression<*, *> -> {
                val name = expression.getCanonicalColumnName(dialect::enquote)
                val owner = expression.owner
                val alias = aliasManager.getAlias(owner)
                    ?: error("Alias is not found. table=${owner.getCanonicalTableName(dialect::enquote)}, column=$name ,sql=$buf")
                if (alias.isBlank()) {
                    buf.append(name)
                } else {
                    buf.append("$alias.$name")
                }
            }
        }
    }

    private fun visitAliasExpression(expression: AliasExpression<*, *>) {
        visitColumnExpression(expression.expression)
        buf.append(" as ${dialect.enquote(expression.alias)}")
    }

    private fun visitArithmeticExpression(expression: ArithmeticExpression<*, *>) {
        buf.append("(")
        when (expression) {
            is ArithmeticExpression.Plus<*, *> -> {
                visitOperand(expression.left)
                buf.append(" + ")
                visitOperand(expression.right)
            }
            is ArithmeticExpression.Minus<*, *> -> {
                visitOperand(expression.left)
                buf.append(" - ")
                visitOperand(expression.right)
            }
            is ArithmeticExpression.Times<*, *> -> {
                visitOperand(expression.left)
                buf.append(" * ")
                visitOperand(expression.right)
            }
            is ArithmeticExpression.Div<*, *> -> {
                visitOperand(expression.left)
                buf.append(" / ")
                visitOperand(expression.right)
            }
            is ArithmeticExpression.Mod<*, *> -> {
                if (dialect.supportsModuloOperator()) {
                    visitOperand(expression.left)
                    buf.append(" % ")
                    visitOperand(expression.right)
                } else {
                    buf.append("mod(")
                    visitOperand(expression.left)
                    buf.append(", ")
                    visitOperand(expression.right)
                    buf.append(")")
                }
            }
        }
        buf.append(")")
    }

    private fun visitCaseExpression(expression: CaseExpression<*, *>) {
        buf.append("case")
        for (`when` in expression.whenList) {
            if (`when`.criteria.isNotEmpty()) {
                buf.append(" when ")
                for ((index, criterion) in `when`.criteria.withIndex()) {
                    visitCriterion(index, criterion)
                    buf.append(" and ")
                }
                buf.cutBack(5)
                buf.append(" then ")
                visitOperand(`when`.thenOperand)
            }
        }
        if (expression.otherwise != null) {
            buf.append(" else ")
            visitColumnExpression(expression.otherwise)
        }
        buf.append(" end")
    }

    private fun visitLiteralExpression(expression: LiteralExpression<*>) {
        val string = dialect.formatValue(expression.value, expression.interiorClass, false)
        buf.append(string)
    }

    private fun visitMathematicalFunction(function: MathematicalFunction<*, *>) {
        @Suppress("USELESS_IS_CHECK")
        when (function) {
            is MathematicalFunction<*, *> -> {
                val random = dialect.getRandomFunction()
                buf.append("$random()")
            }
        }
    }

    private fun visitScalarExpression(expression: ScalarExpression<*, *>) {
        when (expression) {
            is AggregateFunction<*, *> -> {
                visitAggregateFunction(expression)
            }
            is ScalarQueryExpression<*, *, *> -> {
                visitScalarQueryExpression(expression)
            }
        }
    }

    private fun visitAggregateFunction(function: AggregateFunction<*, *>) {
        when (function) {
            is AggregateFunction.Avg -> {
                buf.append("avg(")
                visitColumnExpression(function.expression)
                buf.append(")")
            }
            is AggregateFunction.CountAsterisk -> {
                buf.append("count(*)")
            }
            is AggregateFunction.Count -> {
                buf.append("count(")
                visitColumnExpression(function.expression)
                buf.append(")")
            }
            is AggregateFunction.Max<*, *> -> {
                buf.append("max(")
                visitColumnExpression(function.expression)
                buf.append(")")
            }
            is AggregateFunction.Min<*, *> -> {
                buf.append("min(")
                visitColumnExpression(function.expression)
                buf.append(")")
            }
            is AggregateFunction.Sum<*, *> -> {
                buf.append("sum(")
                visitColumnExpression(function.expression)
                buf.append(")")
            }
        }
    }

    private fun visitScalarQueryExpression(expression: ScalarQueryExpression<*, *, *>) {
        buf.append("(")
        val statement = buildSubqueryStatement(expression)
        buf.append(statement)
        buf.append(")")
    }

    fun buildSubqueryStatement(
        expression: SubqueryExpression<*>,
        projectionPredicate: (ColumnExpression<*, *>) -> Boolean = { true },
    ): Statement {
        return when (val context = expression.context) {
            is SelectContext<*, *, *> -> {
                val childAliasManager = DefaultAliasManager(context, aliasManager)
                val builder = SelectStatementBuilder(dialect, context, childAliasManager, projectionPredicate)
                builder.build()
            }
            is SetOperationContext -> {
                val builder = SetOperationStatementBuilder(dialect, context, aliasManager, projectionPredicate)
                builder.build()
            }
        }
    }

    private fun visitStringFunction(function: StringFunction<*>) {
        buf.append("(")
        when (function) {
            is StringFunction.Concat -> {
                buf.append("concat(")
                visitOperand(function.left)
                buf.append(", ")
                visitOperand(function.right)
                buf.append(")")
            }
            is StringFunction.Lower -> {
                buf.append("lower(")
                visitOperand(function.operand)
                buf.append(")")
            }
            is StringFunction.Ltrim -> {
                buf.append("ltrim(")
                visitOperand(function.operand)
                buf.append(")")
            }
            is StringFunction.Rtrim -> {
                buf.append("rtrim(")
                visitOperand(function.operand)
                buf.append(")")
            }
            is StringFunction.Substring -> {
                val substring = dialect.getSubstringFunction()
                buf.append("$substring(")
                visitOperand(function.target)
                buf.append(", ")
                visitOperand(function.startIndex)
                val length = function.length
                if (length == null) {
                    val defaultLength = dialect.getDefaultLengthForSubstringFunction()
                    if (defaultLength != null) {
                        buf.append(", $defaultLength")
                    }
                } else {
                    buf.append(", ")
                    visitOperand(length)
                }
                buf.append(")")
            }
            is StringFunction.Trim -> {
                buf.append("trim(")
                visitOperand(function.operand)
                buf.append(")")
            }
            is StringFunction.Upper -> {
                buf.append("upper(")
                visitOperand(function.operand)
                buf.append(")")
            }
        }
        buf.append(")")
    }

    fun visitOperand(operand: Operand) {
        when (operand) {
            is Operand.Column -> {
                visitColumnExpression(operand.expression)
            }
            is Operand.Argument<*, *> -> {
                buf.bind(operand.value)
            }
        }
    }

    fun visitCriterion(index: Int, c: Criterion) {
        when (c) {
            is Criterion.Eq -> operation.binary(c.left, c.right, "=")
            is Criterion.NotEq -> operation.binary(c.left, c.right, "<>")
            is Criterion.Less -> operation.binary(c.left, c.right, "<")
            is Criterion.LessEq -> operation.binary(c.left, c.right, "<=")
            is Criterion.Greater -> operation.binary(c.left, c.right, ">")
            is Criterion.GreaterEq -> operation.binary(c.left, c.right, ">=")
            is Criterion.IsNull -> operation.isNull(c.left)
            is Criterion.IsNotNull -> operation.isNull(c.left, true)
            is Criterion.Like -> operation.like(c.left, c.right)
            is Criterion.NotLike -> operation.like(c.left, c.right, true)
            is Criterion.Between -> operation.between(c.left, c.right)
            is Criterion.NotBetween -> operation.between(c.left, c.right, true)
            is Criterion.InList -> operation.inList(c.left, c.right)
            is Criterion.NotInList -> operation.inList(c.left, c.right, true)
            is Criterion.InList2 -> operation.inList2(c.left, c.right)
            is Criterion.NotInList2 -> operation.inList2(c.left, c.right, true)
            is Criterion.InSubQuery -> operation.inSubQuery(c.left, c.right)
            is Criterion.NotInSubQuery -> operation.inSubQuery(c.left, c.right, true)
            is Criterion.InSubQuery2 -> operation.inSubQuery2(c.left, c.right)
            is Criterion.NotInSubQuery2 -> operation.inSubQuery2(c.left, c.right, true)
            is Criterion.Exists -> operation.exists(c.expression)
            is Criterion.NotExists -> operation.exists(c.expression, true)
            is Criterion.And -> operation.logicalBinary("and", c.criteria, index)
            is Criterion.Or -> operation.logicalBinary("or", c.criteria, index)
            is Criterion.Not -> operation.not(c.criteria)
        }
    }

    private inner class CriterionOperation {

        fun binary(left: Operand, right: Operand, operator: String) {
            visitOperand(left)
            buf.append(" $operator ")
            visitOperand(right)
        }

        fun isNull(left: Operand, not: Boolean = false) {
            visitOperand(left)
            val predicate = if (not) {
                " is not null"
            } else {
                " is null"
            }
            buf.append(predicate)
        }

        fun like(left: Operand, right: EscapeExpression, not: Boolean = false) {
            visitOperand(left)
            if (not) {
                buf.append(" not")
            }
            buf.append(" like ")
            val finalEscapeSequence = escapeSequence ?: dialect.escapeSequence
            val newValue = escape(right) { dialect.escape(it, finalEscapeSequence) }
            buf.bind(Value(newValue, String::class, left.masking))
            buf.append(" escape ")
            buf.bind(Value(finalEscapeSequence, String::class))
        }

        private fun escape(expression: EscapeExpression, escape: (String) -> String): String {
            val buf = StringBuilder(expression.length + 10)
            fun visit(e: EscapeExpression) {
                when (e) {
                    is EscapeExpression.Text -> buf.append(e.value)
                    is EscapeExpression.Escape -> buf.append(escape(e.value.toString()))
                    is EscapeExpression.Composite -> {
                        visit(e.left)
                        visit(e.right)
                    }
                }
            }
            visit(expression)
            return buf.toString()
        }

        fun between(left: Operand, right: Pair<Operand, Operand>, not: Boolean = false) {
            visitOperand(left)
            if (not) {
                buf.append(" not")
            }
            buf.append(" between ")
            val (start, end) = right
            visitOperand(start)
            buf.append(" and ")
            visitOperand(end)
        }

        fun inList(left: Operand, right: List<Operand>, not: Boolean = false) {
            visitOperand(left)
            if (not) {
                buf.append(" not")
            }
            buf.append(" in (")
            if (right.isEmpty()) {
                buf.append("null")
            } else {
                for (parameter in right) {
                    visitOperand(parameter)
                    buf.append(", ")
                }
                buf.cutBack(2)
            }
            buf.append(")")
        }

        fun inList2(
            left: Pair<Operand, Operand>,
            right: List<Pair<Operand, Operand>>,
            not: Boolean = false,
        ) {
            if (dialect.supportsMultipleColumnsInInPredicate()) {
                buf.append("(")
                visitOperand(left.first)
                buf.append(", ")
                visitOperand(left.second)
                buf.append(")")
                if (not) {
                    buf.append(" not")
                }
                buf.append(" in (")
                if (right.isEmpty()) {
                    buf.append("(null, null)")
                } else {
                    for ((first, second) in right) {
                        buf.append("(")
                        visitOperand(first)
                        buf.append(", ")
                        visitOperand(second)
                        buf.append(")")
                        buf.append(", ")
                    }
                    buf.cutBack(2)
                }
                buf.append(")")
            } else {
                if (not) {
                    buf.append("not ")
                }
                buf.append("(")
                if (right.isEmpty()) {
                    visitOperand(left.first)
                    buf.append(" = null")
                    buf.append(" and ")
                    visitOperand(left.second)
                    buf.append(" = null")
                } else {
                    for ((first, second) in right) {
                        buf.append("(")
                        visitOperand(left.first)
                        buf.append(" = ")
                        visitOperand(first)
                        buf.append(" and ")
                        visitOperand(left.second)
                        buf.append(" = ")
                        visitOperand(second)
                        buf.append(")")
                        buf.append(" or ")
                    }
                    buf.cutBack(4)
                }
                buf.append(")")
            }
        }

        fun inSubQuery(left: Operand, right: SubqueryExpression<*>, not: Boolean = false) {
            visitOperand(left)
            if (not) {
                buf.append(" not")
            }
            buf.append(" in (")
            val statement = buildSubqueryStatement(right)
            buf.append(statement)
            buf.append(")")
        }

        fun inSubQuery2(left: Pair<Operand, Operand>, right: SubqueryExpression<*>, not: Boolean = false) {
            if (!dialect.supportsMultipleColumnsInInPredicate()) {
                throw UnsupportedOperationException("Dialect(driver=${dialect.driver}) does not support multiple columns in IN predicate.")
            }
            buf.append("(")
            visitOperand(left.first)
            buf.append(", ")
            visitOperand(left.second)
            buf.append(")")
            if (not) {
                buf.append(" not")
            }
            buf.append(" in (")
            val statement = buildSubqueryStatement(right)
            buf.append(statement)
            buf.append(")")
        }

        fun exists(expression: SubqueryExpression<*>, not: Boolean = false) {
            if (not) {
                buf.append("not ")
            }
            buf.append("exists (")
            val statement = buildSubqueryStatement(expression)
            buf.append(statement)
            buf.append(")")
        }

        fun logicalBinary(operator: String, criteria: List<Criterion>, index: Int) {
            if (criteria.isNotEmpty()) {
                if (index > 0) {
                    buf.cutBack(5)
                }
                if (index != 0) {
                    buf.append(" $operator ")
                }
                buf.append("(")
                for ((i, c) in criteria.withIndex()) {
                    visitCriterion(i, c)
                    buf.append(" and ")
                }
                buf.cutBack(5)
                buf.append(")")
            }
        }

        fun not(criteria: List<Criterion>) {
            if (criteria.isNotEmpty()) {
                buf.append("not ")
                buf.append("(")
                for ((index, c) in criteria.withIndex()) {
                    visitCriterion(index, c)
                    buf.append(" and ")
                }
                buf.cutBack(5)
                buf.append(")")
            }
        }
    }
}
