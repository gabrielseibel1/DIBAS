package dibas

import io.ktor.application.install
import io.ktor.http.cio.websocket.*
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.*
import kotlinx.coroutines.channels.*
import java.time.Duration

fun receiveDelegationsAndUpdates(
    delegatedTodo: SendChannel<Task>,
    delegatedDone: ReceiveChannel<Result>,
    loadUpdates: SendChannel<RemoteLoadUpdate>
) =
    embeddedServer(Netty, 8080) {
        install(WebSockets) {
            pingPeriod = Duration.ofSeconds(60)
            timeout = Duration.ofSeconds(15)
            maxFrameSize = Long.MAX_VALUE
            masking = false
        }
        routing {
            webSocket("/delegation") {
                for (frame in incoming.mapNotNull { it as? Frame.Text }) {
                    delegatedTodo.send(Task(frame.readText()))
                    val result = delegatedDone.receive()
                    outgoing.send(Frame.Text(result.toString()))
                }
            }
            webSocket("/update") {
                for (frame in incoming.mapNotNull { it as? Frame.Text }) {
                    loadUpdates.send(frame.readText().toLoadUpdate())
                }
            }

        }
    }.start()