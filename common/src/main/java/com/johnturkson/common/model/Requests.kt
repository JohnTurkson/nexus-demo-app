package com.johnturkson.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class WebsocketRequest

@Serializable
@SerialName("/meta/handshake")
data class WebsocketHandshakeRequest(
    val id: String,
    val version: String,
    val supportedConnectionTypes: List<String>
) : WebsocketRequest()


@Serializable
@SerialName("/meta/connect")
data class WebsocketConnectionRequest(
    val id: String,
    val clientId: String,
    val connectionType: String
) : WebsocketRequest()

@Serializable
@SerialName("/meta/subscribe")
data class WebsocketSubscriptionRequest(
    val id: String,
    val clientId: String,
    val subscription: String,
    @SerialName("ext")
    val authentication: WebsocketAuthenticationParameters
): WebsocketRequest()

@Serializable
@SerialName("/meta/unsubscribe")
data class WebsocketUnsubscriptionRequest(
    val id: String,
    val clientId: String,
    val subscription: String
): WebsocketRequest()

@Serializable
data class WebsocketAuthenticationParameters(
    val userId: String,
    @SerialName("token")
    val userToken: String,
    @SerialName("nexusToken")
    val websocketToken: String
)

@Serializable
sealed class HttpRequest

@Serializable
@SerialName("CreateLinkinbioPostRequest")
data class CreateLinkinbioPostRequest(
    val url: String,
    val image: String
) : HttpRequest()

@Serializable
@SerialName("UpdateLinkinbioPostRequest")
data class UpdateLinkinbioPostRequest(
    val id: String,
    val url: String,
    val image: String
) : HttpRequest()
