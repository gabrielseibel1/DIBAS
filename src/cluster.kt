import java.io.File
import java.net.InetAddress

data class Cluster(val graph: Map<Node, List<Node>>) {
    fun hasHost() = graph.any { (node, _) -> node.ip == InetAddress.getLocalHost().hostAddress }
}

data class Node(val ip: String, val maxTasks: Int) {
    fun canDelegate(): Boolean = TODO("implement me")
}

fun clusterFromFile(config: String): Cluster {
    //mappings of id to ip (node) and of id to it's neighbors' ids
    val idNeighbors = mutableMapOf<String, List<String>>()
    val idToNode = mutableMapOf<String, Node>()

    //read lines after header
    File(config).readLines().drop(1).forEach { line ->

        val cells = line.split(',').map { it.trim() }

        val (id, ip, maxTasks) = cells.slice(0..2)

        idToNode[id] = Node(ip, maxTasks.toInt())

        idNeighbors[id] = cells.drop(3)
    }

    //build graph
    val g = mutableMapOf<Node, List<Node>>()
    idNeighbors.forEach { (id, nbrs) ->
        g[idToNode[id]!!] = nbrs.map { idToNode[it]!! }.toList()
    }

    return Cluster(g.toMap())
}
