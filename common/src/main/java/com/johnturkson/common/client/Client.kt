package com.johnturkson.common.client

import com.johnturkson.common.client.Client.State.*
import com.johnturkson.common.model.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

object Client {
    private val client = HttpClient(CIO) {
        install(WebSockets)
        install(JsonFeature) {
            serializer = KotlinxSerializer(Serializers.httpRequestSerializer)
        }
    }
    private val state: ConflatedBroadcastChannel<State> = ConflatedBroadcastChannel(NOT_CONNECTED)
    private var lastId = 0
    
    private lateinit var clientId: String
    private val token = "1"
    private val host = "10.0.2.2:8080"
    private val websocketToken = "websocketToken"
    private val requests: Channel<WebsocketRequest> = Channel(BUFFERED)
    private val responses: BroadcastChannel<Any> = BroadcastChannel(BUFFERED)
    val updates: BroadcastChannel<WebsocketUpdateResponse> = BroadcastChannel(BUFFERED)
    
    enum class State {
        NOT_CONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        DISCONNECTED
    }
    
    suspend fun getLinkinbioPost(id: String): LinkinbioPost {
        return client.get("http://$host/LinkinbioPost/$id") {
            header("Token", token)
            header("Content-Type", "application/json")
        }
    }
    
    suspend fun getLinkinbioPosts(user: String): List<LinkinbioPost> {
        return client.get("http://$host/LinkinbioPosts/$user") {
            header("Token", token)
            header("Content-Type", "application/json")
        }
    }
    
    suspend fun createLinkinbioPost(user: String, url: String, image: String): LinkinbioPost {
        return client.post("http://$host/CreateLinkinbioPost") {
            header("Token", token)
            header("Content-Type", "application/json")
            body = CreateLinkinbioPostRequest(url, image)
        }
    }
    
    suspend fun updateLinkinbioPost(id: String, url: String, image: String): LinkinbioPost {
        return client.post("http://$host/UpdateLinkinbioPost") {
            header("Token", token)
            header("Content-Type", "application/json")
            body = UpdateLinkinbioPostRequest(id, url, image)
        }
    }
    
    suspend fun deleteLinkinbioPost(id: String): LinkinbioPost {
        return client.delete("http://$host/DeleteLinkinbioPost/$id") {
            header("Token", token)
            header("Content-Type", "application/json")
        }
    }
    
    suspend fun subscribe(id: String) {
        println("waiting for connected")
        state.openSubscription().consumeAsFlow()
            .filter { it == CONNECTED }
            .first()
        println("waited for connected")
        
        val token = id
        val subscriptionRequest = WebsocketSubscriptionRequest(
            id = generateNewRequestId(),
            clientId = clientId,
            subscription = "/user/$id",
            authentication = WebsocketAuthenticationParameters(
                id,
                token,
                websocketToken
            )
        )
        
        val response = GlobalScope.launch {
            // wait until response
            responses.openSubscription().consumeAsFlow()
                .filterIsInstance<WebsocketSubscriptionResponse>()
                .filter { response -> response.id == subscriptionRequest.id }
                .first()
                .also { println("received subscription, id: ${it.clientId}") }
        }
        
        requests.send(subscriptionRequest)
        
        response.join()
    }
    
    suspend fun connect() {
        suspend fun listen(incoming: ReceiveChannel<Frame>) {
            GlobalScope.launch {
                // listen to future responses
                incoming.consumeAsFlow()
                    .filterIsInstance<Frame.Text>()
                    .map { response -> response.readText() }
                    .onEach { json -> process(json) }
                    .launchIn(GlobalScope)
            }
        }
        
        suspend fun handle(outgoing: SendChannel<Frame>) {
            GlobalScope.launch {
                // handle client requests
                requests.consumeAsFlow()
                    .map { request ->
                        Serializers.websocketRequestSerializer.stringify(
                            WebsocketRequest.serializer(),
                            request
                        )
                    }
                    .map { json -> Frame.Text(json) }
                    .onEach { request -> outgoing.send(request) }
                    .collect()
            }
        }
        
        GlobalScope.launch {
            if (state.value == CONNECTED) {
                return@launch
            }
            
            client.ws(
                method = HttpMethod.Get,
                host = "10.0.2.2",
                port = 8080,
                path = "/updates"
            ) {
                state.send(CONNECTING)
                
                listen(incoming)
                handle(outgoing)
                
                val handshakeRequest = WebsocketHandshakeRequest(
                    id = generateNewRequestId(),
                    version = "1.0",
                    supportedConnectionTypes = listOf("websocket")
                )
                
                val handshakeResponse = GlobalScope.launch {
                    // wait until response
                    responses.openSubscription().consumeAsFlow()
                        .filterIsInstance<WebsocketHandshakeResponse>()
                        .filter { response -> response.id == handshakeRequest.id }
                        .first()
                }
                
                requests.send(handshakeRequest)
                
                handshakeResponse.join()
                
                val connectionRequest = WebsocketConnectionRequest(
                    id = generateNewRequestId(),
                    clientId = clientId,
                    connectionType = "websocket"
                )
                
                val connectionResponse = GlobalScope.launch {
                    // wait until response
                    responses.openSubscription().consumeAsFlow()
                        .filterIsInstance<WebsocketConnectionResponse>()
                        .filter { response -> response.id == connectionRequest.id }
                        .first()
                }
                
                requests.send(connectionRequest)
                
                connectionResponse.join()
                
                state.send(CONNECTED)
                
                // wait until closed
                state.openSubscription().consumeAsFlow()
                    .filter { it == DISCONNECTING || it == DISCONNECTED }
                    .first()
            }
            state.send(DISCONNECTED)
        }
    }
    
    suspend fun disconnect() {
            val disconnected = GlobalScope.launch {
                state.openSubscription().consumeAsFlow()
                    .takeWhile { state -> state != DISCONNECTED }
                    .collect()
            }
        
            state.send(DISCONNECTING)
        
            disconnected.join()
    }
    
    private suspend fun generateNewRequestId(): String {
        return (++lastId).toString()
    }
    
    private suspend fun processHandshakeResponse(response: WebsocketHandshakeResponse) {
        clientId = response.clientId
        
    }
    
    private suspend fun processConnectionResponse(response: WebsocketConnectionResponse) {
    
    }
    
    private suspend fun processSubscriptionResponse(response: WebsocketSubscriptionResponse) {
    
    }
    
    private suspend fun processUnsubscriptionResponse(response: WebsocketUnsubscriptionResponse) {
    
    }
    
    private suspend fun processUpdateResponse(response: WebsocketUpdateResponse) {
        updates.send(response)
    }
    
    private suspend fun process(json: String) {
        println("response: $json")
        
        val response = Serializers.websocketResponseSerializer.parse(
            WebsocketResponse.serializer(),
            json
        )
        
        val deserializer = when {
            response.successful == false -> TODO()
            response.channel == null -> TODO()
            response.channel == "/meta/handshake" -> WebsocketHandshakeResponse.serializer()
            response.channel == "/meta/connect" -> WebsocketConnectionResponse.serializer()
            response.channel == "/meta/subscribe" -> WebsocketSubscriptionResponse.serializer()
            response.channel == "/meta/unsubscribe" -> WebsocketUnsubscriptionResponse.serializer()
            response.channel.startsWith("/user/") -> WebsocketUpdateResponse.serializer()
            else -> throw Exception("invalid response type")
        }
        
        val parsed = Serializers.websocketResponseSerializer.parse(deserializer, json)
        when (parsed) {
            is WebsocketHandshakeResponse -> processHandshakeResponse(parsed)
            is WebsocketConnectionResponse -> processConnectionResponse(parsed)
            is WebsocketSubscriptionResponse -> processSubscriptionResponse(parsed)
            is WebsocketUnsubscriptionResponse -> processUnsubscriptionResponse(parsed)
            is WebsocketUpdateResponse -> processUpdateResponse(parsed)
            else -> throw Exception("invalid response type")
        }
        
        responses.send(parsed)
    }
}
