package org.komapper.core.dsl.operator

import org.komapper.core.dsl.expression.ArithmeticExpression
import org.komapper.core.dsl.expression.ColumnExpression
import org.komapper.core.dsl.expression.Operand

/**
 * Applies the `+` operator.
 */
infix operator fun <T : Any, S : Number> ColumnExpression<T, S>.plus(value: T): ColumnExpression<T, S> {
    val left = Operand.Column(this)
    val right = Operand.Argument(this, value)
    return ArithmeticExpression.Plus(this, left, right)
}

/**
 * Applies the `+` operator.
 */
infix operator fun <T : Any, S : Number> ColumnExpression<T, S>.plus(other: ColumnExpression<T, S>): ColumnExpression<T, S> {
    val left = Operand.Column(this)
    val right = Operand.Column(other)
    return ArithmeticExpression.Plus(this, left, right)
}

/**
 * Applies the `-` operator.
 */
infix operator fun <T : Any, S : Number> ColumnExpression<T, S>.minus(value: T): ColumnExpression<T, S> {
    val left = Operand.Column(this)
    val right = Operand.Argument(this, value)
    return ArithmeticExpression.Minus(this, left, right)
}

/**
 * Applies the `-` operator.
 */
infix operator fun <T : Any, S : Number> ColumnExpression<T, S>.minus(other: ColumnExpression<T, S>): ColumnExpression<T, S> {
    val left = Operand.Column(this)
    val right = Operand.Column(other)
    return ArithmeticExpression.Minus(this, left, right)
}

/**
 * Applies the `*` operator.
 */
infix operator fun <T : Any, S : Number> ColumnExpression<T, S>.times(value: T): ColumnExpression<T, S> {
    val left = Operand.Column(this)
    val right = Operand.Argument(this, value)
    return ArithmeticExpression.Times(this, left, right)
}

/**
 * Applies the `*` operator.
 */
infix operator fun <T : Any, S : Number> ColumnExpression<T, S>.times(other: ColumnExpression<T, S>): ColumnExpression<T, S> {
    val left = Operand.Column(this)
    val right = Operand.Column(other)
    return ArithmeticExpression.Times(this, left, right)
}

/**
 * Applies the `/` operator.
 */
infix operator fun <T : Any, S : Number> ColumnExpression<T, S>.div(value: T): ColumnExpression<T, S> {
    val left = Operand.Column(this)
    val right = Operand.Argument(this, value)
    return ArithmeticExpression.Div(this, left, right)
}

/**
 * Applies the `/` operator.
 */
infix operator fun <T : Any, S : Number> ColumnExpression<T, S>.div(other: ColumnExpression<T, S>): ColumnExpression<T, S> {
    val left = Operand.Column(this)
    val right = Operand.Column(other)
    return ArithmeticExpression.Div(this, left, right)
}

/**
 * Applies the `%` operator.
 */
infix operator fun <T : Any, S : Number> ColumnExpression<T, S>.rem(value: T): ColumnExpression<T, S> {
    val left = Operand.Column(this)
    val right = Operand.Argument(this, value)
    return ArithmeticExpression.Mod(this, left, right)
}

/**
 * Applies the `%` operator.
 */
infix operator fun <T : Any, S : Number> ColumnExpression<T, S>.rem(other: ColumnExpression<T, S>): ColumnExpression<T, S> {
    val left = Operand.Column(this)
    val right = Operand.Column(other)
    return ArithmeticExpression.Mod(this, left, right)
}
