package dev.limebeck.utils.json

import kotlinx.serialization.json.*

fun JsonElement?.parseElement(): Any? =
    when (this) {
        null -> null
        is JsonNull -> null
        is JsonObject -> this.convertJson()
        is JsonArray -> this.jsonArray.map { it.parseElement() }
        is JsonPrimitive -> {
            listOfNotNull(
                contentOrNull,
                booleanOrNull,
                doubleOrNull,
                floatOrNull,
                intOrNull,
                longOrNull,
            ).firstOrNull()
        }
    }

fun JsonObject.convertJson(): Map<String, Any?> =
    entries.associate {
        it.key to it.value.parseElement()
    }
