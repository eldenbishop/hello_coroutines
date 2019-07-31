package experimental

import kotlinx.coroutines.*

class BulkLookup<I,R>(val dbName: String, val db: Map<I,R?>) : BulkFunction<I,R>() {
    val keys = db.keys
    suspend fun lookup(key:I) = apply(key)
    override fun execute(input: List<I>): List<R?> {
        println("${dbName}.lookup num-keys: " + input.size)
        return input.map { db[it] }
    }
}

data class Person(val name: String, val email: String)
data class Dealership(val name: String, val managerId: String)
data class Car(val dealerId: String, val ownerId: String)

fun main(args: Array<String>) {
    val people = BulkLookup("person", hashMapOf(
            "joe" to Person("Joe Smith", "joe@bmw.com"),
            "sue" to Person("Sue Perkins", "sue@toyota.com"),
            "ted" to Person("Ted White", "ted@ted.com")
    ));
    val dealerships = BulkLookup("dealership", hashMapOf(
            "bmw" to Dealership("Burlingame BMW", "joe"),
            "toyota" to Dealership("Bay Toyota", "sue")
    ));
    val cars = BulkLookup("car", hashMapOf(
            1000L to Car("bmw", "joe"),
            1001L to Car("bmw", "ted"),
            1002L to Car("toyota", "sue"),
            1003L to Car("toyota", "ted"),
            1004L to Car("bmw", "sue")
    ))
    runBlocking {
        /*
        Launch one "thread" per car. Although the async section is serial "blocking"
        code, the backing lookups (cars, people, dealerships) are invoked as bulk lookups.
        In this case, this should print car.lookup num-keys: 5, dealership.lookup num-keys: 5 etc.
        In total, this will perform only 4 lookups while appearing from the code below to
        be performing > 20 lookups.
         */
        for (vin in cars.keys) {
            async {
                val car = cars.lookup(vin)
                val dealer = dealerships.lookup(car!!.dealerId)
                if (car.ownerId != dealer!!.managerId) {
                    val owner = people.lookup(car.ownerId)
                    val contact = people.lookup(dealer.managerId)!!.email
                        println("From: ${owner!!.email}, to: ${contact}, at: ${dealer.name}, vin: ${vin}")
                }
            }
        }
    }
}