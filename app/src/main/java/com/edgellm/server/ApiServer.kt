package com.edgellm.server

import com.edgellm.engine.InferenceEngine
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.cio.CIO
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ChatRequest(
    val model: String = "default",
    val messages: List<Message>,
    val stream: Boolean = false
)

@Serializable
data class Message(val role: String, val content: String)

@Serializable
data class ChatResponse(
    val id: String = "chatcmpl-123",
    val `object`: String = "chat.completion",
    val created: Long = System.currentTimeMillis() / 1000,
    val model: String,
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val index: Int = 0,
    val message: Message,
    val finish_reason: String = "stop"
)

@Serializable
data class StreamResponse(
    val id: String = "chatcmpl-123",
    val `object`: String = "chat.completion.chunk",
    val created: Long = System.currentTimeMillis() / 1000,
    val model: String,
    val choices: List<StreamChoice>
)

@Serializable
data class StreamChoice(
    val index: Int = 0,
    val delta: Delta,
    val finish_reason: String? = null
)

@Serializable
data class Delta(val content: String? = null)

class ApiServer(private val engine: com.edgellm.engine.InferenceEngine) {

    private var server: io.ktor.server.engine.EmbeddedServer<*, *>? = null

    fun start(port: Int = 8080) {
        if (server != null) return
        
        // Final reference to ensure lambda visibility
        val activeEngine = this.engine

        val s = embeddedServer(CIO, port = port) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            routing {
                get("/v1/models") {
                    call.respond(
                        mapOf(
                            "object" to "list",
                            "data" to listOf(
                                mapOf(
                                    "id" to (activeEngine.modelName ?: "unknown"),
                                    "object" to "model",
                                    "owned_by" to "edgellm"
                                )
                            )
                        )
                    )
                }

                post("/v1/chat/completions") {
                    val req = call.receive<ChatRequest>()
                    val prompt = req.messages.joinToString("\n") { "${it.role}: ${it.content}" } + "\nassistant:"

                    if (req.stream) {
                        call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                            activeEngine.generateStream(prompt)
                                .collect { token ->
                                    val chunk = StreamResponse(
                                        model = activeEngine.modelName ?: "unknown",
                                        choices = listOf(StreamChoice(delta = Delta(content = token)))
                                    )
                                    val jsonStr = Json.encodeToString(StreamResponse.serializer(), chunk)
                                    write("data: $jsonStr\n\n")
                                    flush()
                                }
                            write("data: [DONE]\n\n")
                            flush()
                        }
                    } else {
                        val responseText = activeEngine.generate(prompt)
                        val resp = ChatResponse(
                            model = activeEngine.modelName ?: "unknown",
                            choices = listOf(Choice(message = Message("assistant", responseText)))
                        )
                        call.respond(resp)
                    }
                }
            }
        }
        server = s
        s.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
    }
}
