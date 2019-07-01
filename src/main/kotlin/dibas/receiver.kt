package dibas

import io.ktor.application.install
import io.ktor.http.cio.websocket.*
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.*
import kotlinx.coroutines.channels.*
import java.time.Duration

fun receiveTasksAndUpdates(
    doOrDelegate: suspend (Task) -> Result,
    loads: SendChannel<NodeLoad>
) =
    embeddedServer(Netty, 8080) {
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(60)
            timeout = Duration.ofSeconds(15)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }
        routing {
            webSocket("/tasks") {
                for (frame in incoming.mapNotNull { it as? Frame.Text }) {
                    val task = frame.readText().toTask()
                    val result = doOrDelegate(task)
                    outgoing.send(Frame.Text(result.toString()))
                }
            }
            webSocket("/loads") {
                for (frame in incoming.mapNotNull { it as? Frame.Text }) {
                    loads.send(frame.readText().toNodeLoad())
                }
            }
        }
    }.start()