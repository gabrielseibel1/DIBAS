package dibas

import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.channels.ReceiveChannel


fun openWebSocketsWithNeighbors(cluster: Cluster, host: String)


@KtorExperimentalAPI
suspend fun DefaultClient.delegate(toDelegate: ReceiveChannel<Task>) {
    for (task in toDelegate) {
        send(Frame.Text(task.toString()))
    }
}