package dibas

import java.io.*

fun Serializable.toBytes(): ByteArray {
    val bos = ByteArrayOutputStream()
    val out: ObjectOutput
    val bytes: ByteArray

    try {
        out = ObjectOutputStream(bos)
        out.writeObject(this)
        out.flush()
        bytes = bos.toByteArray()

    } finally {
        try {
            bos.close()
        } catch (ex: IOException) {
            // ignore close exception
        }
    }

    return bytes
}

fun <T> ByteArray.to(): T {
    val bis = ByteArrayInputStream(this)
    val oi: ObjectInput
    oi = ObjectInputStream(bis)
    return oi.readObject() as T
}



/*
var i = 1
val task = Task {
    Result("i^ = ${i*i}")
}
i = 2
i = 3

val bos = ByteArrayOutputStream()
val out: ObjectOutput
val bytes: ByteArray

try {
    out = ObjectOutputStream(bos)
    out.writeObject(task)
    out.flush()
    bytes = bos.toByteArray()

} finally {
    try {
        bos.close()
    } catch (ex: IOException) {
        // ignore close exception
    }
}

val bis = ByteArrayInputStream(bytes)
val oi: ObjectInput
oi = ObjectInputStream(bis)
val t = oi.readObject() as Int
val result = t.toResult()

println(result)
*/
