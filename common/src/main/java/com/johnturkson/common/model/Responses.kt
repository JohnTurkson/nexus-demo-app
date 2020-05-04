package com.johnturkson.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WebsocketResponse(
    val id: String? = null,
    val clientId: String? = null,
    val channel: String? = null,
    val subscription: String? = null,
    val version: String? = null,
    val data: ResponseData? = null,
    val successful: Boolean? = null,
    val error: String? = null,
    val advice: WebsocketConnectionParameters? = null
)

@Serializable
data class WebsocketHandshakeResponse(
    val id: String,
    val clientId: String,
    val channel: String,
    val supportedConnectionTypes: List<String>,
    val version: String,
    val successful: Boolean,
    val error: String? = null,
    val advice: WebsocketConnectionParameters
)

@Serializable
data class WebsocketConnectionResponse(
    val id: String,
    val clientId: String,
    val channel: String,
    val successful: Boolean,
    val advice: WebsocketConnectionParameters
)

@Serializable
data class WebsocketSubscriptionResponse(
    val id: String,
    val clientId: String,
    val channel: String,
    val subscription: String,
    val successful: Boolean,
    val error: String? = null
)

@Serializable
data class WebsocketUnsubscriptionResponse(
    val id: String,
    val clientId: String,
    val channel: String,
    val subscription: String,
    val successful: Boolean,
    val error: String? = null
)

@Serializable
data class WebsocketUpdateResponse(
    val id: String,
    val channel: String,
    val data: ResponseData
)

@Serializable
data class WebsocketErrorResponse(
    val id: String? = null,
    val clientId: String? = null,
    val channel: String? = null,
    val subscription: String? = null,
    val successful: Boolean,
    val error: String
)

@Serializable
sealed class ResponseData

@Serializable
sealed class LinkinbioResponseData : ResponseData() {
    abstract val data: LinkinbioPostData
}

@Serializable
@SerialName("update_linkinbio_post")
data class UpdateLinkinbioPostResponseData(
    override val data: LinkinbioPostData
) : LinkinbioResponseData()

@Serializable
@SerialName("create_linkinbio_post")
data class CreateLinkinbioPostResponseData(
    override val data: LinkinbioPostData
) : LinkinbioResponseData()

@Serializable
@SerialName("delete_linkinbio_post")
data class DeleteLinkinbioPostResponseData(
    override val data: LinkinbioPostData
) : LinkinbioResponseData()

@Serializable
data class LinkinbioPostData(
    @SerialName("LinkinbioPost")
    val linkinbioPost: LinkinbioPost
)

@Serializable
data class WebsocketConnectionParameters(
    val reconnect: String,
    val interval: Int,
    val timeout: Int
)

@Serializable
data class LinkinbioPost(
    val id: String,
    val user: String,
    val url: String,
    val image: String
)
