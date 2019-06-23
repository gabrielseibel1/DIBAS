package dibas

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