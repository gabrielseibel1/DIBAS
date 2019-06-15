fun <T> dibas(cluster: Cluster, block: () -> T): T {
    println(cluster)
    //TODO make it run dibas
    return run(block)
}

fun main() {
    val cluster = clusterFromFile("./config/cluster.csv")

    val message = dibas(cluster) {
        "Hello," + " Dibas!"
    }

    println(message)
}
