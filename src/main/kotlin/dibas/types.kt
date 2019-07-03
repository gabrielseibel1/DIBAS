package dibas

import java.io.*

data class Task(val toResult: suspend () -> Result) : Serializable
data class Result(val content: String): Serializable
data class NodeLoad(val node: Node, val load: Int): Serializable
data class Node(val id: String, val ip: String)

enum class LocalLoadUpdate {
    INC {
        override fun update(load: Int): Int = load + 1
    },
    DEC {
        override fun update(load: Int): Int = load - 1
    };

    abstract fun update(load: Int): Int
}