package dibas

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.http.cio.websocket.*
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.*
import kotlinx.coroutines.channels.*
import java.time.Duration
import java.time.LocalDateTime

fun receiveTasksAndLoads(
    doOrDelegate: suspend (Task) -> Result,
    loads: SendChannel<NodeLoad>
) =
    embeddedServer(Netty, 8080) {
        install(CallLogging)
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(60)
            timeout = Duration.ofSeconds(15)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }
        val logger: Logger = ThreadAwareLogger()
        routing {
            get("/") {
                val time = LocalDateTime.now()
                call.respond("Hello from server $time")
            }

            webSocket("/tasks") {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Binary -> {
                            logger.log("Received at /task")
                            val task = frame.readBytes().to<Task>()
                            val result = doOrDelegate(task)
                            outgoing.send(Frame.Binary(true, result.toBytes()))
                        }
                    }
                }
            }
            webSocket("/loads") {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Binary -> {
                            val nl = frame.readBytes().to<NodeLoad>()
                            logger.log("Received $nl at /loads")
                            loads.send(nl)
                        }
                    }
                }
            }
        }
    }.start()