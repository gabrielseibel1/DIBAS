package dibas

import java.io.File
import java.net.InetAddress

data class Cluster(val graph: Map<Node, List<Node>>) {

    private val hostNode: Node by lazy {
        val host = InetAddress.getLocalHost().hostAddress
        graph.keys.first { it.ip == host }
    }

    val neighbors: List<Node> by lazy { graph[hostNode].orEmpty() }
}

data class Node(val id: String, val ip: String)

fun clusterFromFile(config: String): Cluster {
    //mappings of id to ip (node) and of id to its neighbors' ids
    val idNeighbors = mutableMapOf<String, List<String>>()
    val idToNode = mutableMapOf<String, Node>()

    //read lines after header
    File(config).readLines().drop(1).forEach { line ->

        val cells = line.split(',').map { it.trim() }

        val (id, ip) = cells.slice(0..1)

        idToNode[id] = Node(id, ip)

        idNeighbors[id] = cells.drop(2)
    }

    //build graph
    val g = mutableMapOf<Node, List<Node>>()
    idNeighbors.forEach { (id, nbrs) ->
        g[idToNode[id]!!] = nbrs.map { idToNode[it]!! }.toList()
    }

    return Cluster(g.toMap())
}