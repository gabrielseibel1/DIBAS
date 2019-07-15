package dibas

import java.time.LocalDateTime

interface Logger {
    fun log(message: String)
}

class BasicLogger: Logger {
    override fun log(message: String) {
        StringBuilder()
            .append("[${LocalDateTime.now()}] ")
            .append(message.toLowerCase())
            .also { println(it.toString()) }
    }
}

class ThreadAwareLogger: Logger {
    override fun log(message: String) {
        StringBuilder()
            //.append("[${LocalDateTime.now()}] ")
            .append("[TID ${Thread.currentThread().id}] ")
            .append(message.toLowerCase())
            .also { println(it.toString()) }
    }
}

class SilentLogger: Logger {
    override fun log(message: String) {
        //do nothing
    }
}