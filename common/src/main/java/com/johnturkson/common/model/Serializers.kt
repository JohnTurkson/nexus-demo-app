package com.johnturkson.common.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

object Serializers {
    val httpRequestSerializer = Json(
        JsonConfiguration(
            encodeDefaults = true,
            ignoreUnknownKeys = true
        )
    )
    
    val httpResponseSerializer = Json(
        JsonConfiguration(
            encodeDefaults = true,
            ignoreUnknownKeys = true
        )
    )
    
    val websocketRequestSerializer = Json(
        JsonConfiguration(
            encodeDefaults = true,
            ignoreUnknownKeys = true,
            classDiscriminator = "channel"
        )
    )
    
    val websocketResponseSerializer = Json(
        JsonConfiguration(
            encodeDefaults = true,
            ignoreUnknownKeys = true,
            classDiscriminator = "name"
        )
    )
}
