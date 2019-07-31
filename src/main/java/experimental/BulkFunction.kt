package experimental

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

abstract class BulkFunction<I,R> {

    val nextReqId = AtomicLong()
    val bufferIn = ConcurrentHashMap<Long, I>()
    val bufferOut = ConcurrentHashMap<Long, R?>()

    suspend fun apply(input: I) : R? {
        val id = nextReqId.incrementAndGet()
        try {
            bufferIn.put(id, input)
            return coroutineScope {
                async {
                    if (!bufferIn.isEmpty()) {
                        val ids = ArrayList<Long>()
                        val inputs = ArrayList<I>()
                        bufferIn.forEach { k, v ->
                            ids.add(k)
                            inputs.add(v)
                        }
                        bufferIn.clear()
                        val results = execute(inputs)
                        val i1 = ids.iterator()
                        val i2 = results.iterator()
                        while (i1.hasNext()) {
                            val key = i1.next()
                            val value = i2.next()
                            if (value != null)
                                bufferOut.put(key, value)
                        }
                    }
                    bufferOut[id]
                }.await()
            }
        } finally {
            bufferOut.remove(id)
        }
    }

    abstract fun execute(input: List<I>) : List<R?>

}