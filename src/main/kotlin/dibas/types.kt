package dibas

import java.util.*
import kotlin.Comparator

data class Task(val content: String)
data class Result(val task: Task, val content: String)
data class NodeLoad(val node: Node, val load: Int)
data class DelegationTask(val task: Task, val nodeId: String)
data class DelegationResult(val result: Result, val nodeId: String)

fun String.toLoadUpdate(): NodeLoad = TODO()

enum class LocalLoadUpdate {
    INC {
        override fun update(load: Int): Int = load + 1
    },
    DEC {
        override fun update(load: Int): Int = load - 1
    };

    abstract fun update(load: Int): Int
}

typealias SortedNodeLoads = PriorityQueue<NodeLoad>
fun sortedNodeLoadsOf(cluster: Cluster): SortedNodeLoads {
    //put all neighbors of localhost with 0 tasks
    val q = PriorityQueue<NodeLoad>(
        Comparator { o1, o2 -> o1.load - o2.load }
    )
    cluster.neighbors.forEach { node ->
        q.add(NodeLoad(node, 0))
    }
    return q
}