package dao

import com.mongodb.client.model.Filters
import com.mongodb.rx.client.MongoDatabase
import model.Currency
import model.Item
import model.User
import org.bson.Document
import rx.Observable
import rx.Scheduler
import rx.schedulers.Schedulers

const val USERS_COLLECTION = "users"
const val ITEMS_COLLECTION = "items"

class MongoDao(
    private val db: MongoDatabase,
) : Dao {
    private val scheduler: Scheduler = Schedulers.io()

    override fun addUser(user: User): Observable<Boolean> {
        return getUser(user.id)
            .singleOrDefault(null)
            .flatMap { foundUser ->
                if (foundUser != null) {
                    Observable.just(false)
                } else {
                    val doc = Document(
                        mutableMapOf(
                            "id" to user.id,
                            "name" to user.name,
                            "currency" to user.currency.toString(),
                        ) as Map<String, Any>?
                    )
                    db.getCollection(USERS_COLLECTION)
                        .insertOne(doc)
                        .asObservable()
                        .isEmpty
                        .map { empty -> !empty }
                }
            }
    }

    override fun getUser(id: Long): Observable<User> {
        return db.getCollection(USERS_COLLECTION)
            .find(Filters.eq("id", id))
            .toObservable()
            .map { doc ->
                User(
                    id = id,
                    name = doc.getString("name"),
                    currency = Currency.valueOf(doc.getString("currency")),
                )
            }.subscribeOn(scheduler)
    }

    override fun addItem(item: Item): Observable<Boolean> {
        return getItem(item.id)
            .singleOrDefault(null)
            .flatMap { foundItem ->
                if (foundItem != null) {
                    Observable.just(false)
                } else {
                    val doc = Document(
                        mutableMapOf(
                            "id" to item.id,
                            "name" to item.name,
                            "price" to item.price,
                        ) as Map<String, Any>?
                    )
                    db.getCollection(ITEMS_COLLECTION)
                        .insertOne(doc)
                        .asObservable()
                        .isEmpty
                        .map { empty -> !empty }
                }
            }
    }

    override fun getItem(id: Long): Observable<Item> {
        return db.getCollection(ITEMS_COLLECTION)
            .find(Filters.eq("id", id))
            .toObservable()
            .map { doc ->
                Item(
                    id = id,
                    name = doc.getString("name"),
                    price = doc.getDouble("price"),
                )
            }.subscribeOn(scheduler)
    }
}