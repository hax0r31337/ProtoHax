package dev.sora.relay.cheat.value

interface Configurable {

    val values: MutableList<Value<*>>

    fun getValue(valueName: String) =
        values.find { it.name.equals(valueName, ignoreCase = true) }

    fun boolValue(name: String, value: Boolean)
        = BoolValue(name, value).also { values.add(it) }

    fun floatValue(name: String, value: Float, range: ClosedFloatingPointRange<Float>)
        = FloatValue(name, value, range).also { values.add(it) }

    fun intValue(name: String, value: Int, range: IntRange)
        = IntValue(name, value, range).also { values.add(it) }

    fun <T : NamedChoice> listValue(name: String, valuesArr: Array<T>, value: T)
        = ListValue(name, valuesArr, value).also { values.add(it) }

    fun stringValue(name: String, value: String)
        = StringValue(name, value).also { values.add(it) }
}