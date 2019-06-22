package dibas

import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.DefaultClientWebSocketSession
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readBytes
import io.ktor.http.cio.websocket.readText
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.channels.ReceiveChannel



@KtorExperimentalAPI
suspend fun DefaultClientWebSocketSession.delegate(toDelegate: ReceiveChannel<Task>) {
    for (task in toDelegate) {
        send(Frame.Text(task.toString()))
    }
}