package dev.sora.relay.cheat.value

interface ValueHolder {

    val values: MutableList<Value<*>>

    fun getValue(valueName: String) =
        values.find { it.name.equals(valueName, ignoreCase = true) }

    fun boolValue(name: String, value: Boolean)
        = BoolValue(name, value).also { values.add(it) }

    fun floatValue(name: String, value: Float, minimum: Float = 0F, maximum: Float = Float.MAX_VALUE)
        = FloatValue(name, value, minimum, maximum).also { values.add(it) }

    fun intValue(name: String, value: Int, minimum: Int = 0, maximum: Int = Integer.MAX_VALUE)
        = IntValue(name, value, minimum, maximum).also { values.add(it) }

    fun listValue(name: String, valuesArr: Array<String>, value: String)
        = ListValue(name, valuesArr, value).also { values.add(it) }

    fun stringValue(name: String, value: String)
        = StringValue(name, value).also { values.add(it) }
}