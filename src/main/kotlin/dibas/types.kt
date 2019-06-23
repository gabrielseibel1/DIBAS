package dibas

data class Task(val toResult: suspend () -> Result)
data class Result(val content: String)
data class NodeLoad(val node: Node, val load: Int)

fun String.toTask(): Task = TODO()
fun String.toResult(): Result = TODO()
fun String.toNodeLoad(): NodeLoad = TODO()

enum class LocalLoadUpdate {
    INC {
        override fun update(load: Int): Int = load + 1
    },
    DEC {
        override fun update(load: Int): Int = load - 1
    };

    abstract fun update(load: Int): Int
}