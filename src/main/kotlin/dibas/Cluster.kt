package dibas

import java.io.File
import java.net.InetAddress
import kotlin.text.StringBuilder

data class Cluster(val graph: Map<Node, List<Node>>) {

    val hostNode: Node
    val neighbors: List<Node>
    val load: MutableMap<Node, Int>

    init {
        val host = InetAddress.getLocalHost().hostAddress
        hostNode = graph.keys.first { it.ip == host }
        neighbors = graph[hostNode].orEmpty()
        load = neighbors.map { it to 0 }.toMap().toMutableMap()
    }

    fun lessBusyNeighbor(): NodeLoad {
        val entry = load.toList().minBy { it.second }
        return NodeLoad(entry!!.first, entry.second)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        graph.forEach { (node, list) ->
            sb.append("\n$node\tknows\t")
            list.forEach { sb.append("$it, ") }
        }
        return sb.toString()
    }

    companion object {
        fun fromFile(config: String): Cluster {
            //mappings of id to ip (node) and of id to its neighbors' ids
            val idNeighbors = mutableMapOf<String, List<String>>()
            val idToNode = mutableMapOf<String, Node>()

            //read lines after header
            File(config).readLines().drop(1).forEach { line ->

                val cells = line.split(',').map { it.trim() }.filter { it.isNotBlank() }

                val (id, ip) = cells.slice(0..1)

                idToNode[id] = Node(id, ip)

                idNeighbors[id] = cells.drop(2)
            }

            //build graph
            val g = mutableMapOf<Node, List<Node>>()
            idNeighbors.forEach { (id, nbrs) ->
                val node = idToNode[id]
                if (node != null) {
                    g[node] =
                        nbrs
                            .map { nbr ->
                                idToNode[nbr]!!
                            }
                            .toList()
                }
            }

            return Cluster(g.toMap())
        }
    }
}