import com.mongodb.rx.client.MongoClients
import dao.MongoDao
import io.netty.handler.codec.http.HttpMethod
import io.reactivex.netty.protocol.http.client.HttpClient
import io.reactivex.netty.protocol.http.server.HttpServer
import io.reactivex.netty.protocol.http.server.HttpServerRequest
import kotlinx.serialization.json.Json
import model.*
import rx.Observable

const val ENV_MONGO_URL = "MONGO_URL"
const val ENV_MONGO_DB_NAME = "MONGO_DB_NAME"
const val ENV_CURRENCY_API_URL = "CURRENCY_API_URL"
const val ENV_CURRENCY_API_PATH = "CURRENCY_API_PATH"

fun <T> HttpServerRequest<T>.param(name: String): String? = queryParameters[name]?.get(0)

private val json = Json { allowStructuredMapKeys = true }

fun main() {
    val env = object {
        val mongoUrl = System.getenv(ENV_MONGO_URL)
        val mongoDbName = System.getenv(ENV_MONGO_DB_NAME)
        val currencyApiHost = System.getenv(ENV_CURRENCY_API_URL)
        val currencyApiPath = System.getenv(ENV_CURRENCY_API_PATH)
    }

    val dao = MongoDao(MongoClients.create(env.mongoUrl).getDatabase(env.mongoDbName))

    val server = HttpServer.newServer(8080)

    server.start { req, resp ->
        val obs = try {
            when (req.httpMethod) {
                HttpMethod.PUT -> {
                    when (req.decodedPath) {
                        "/v0/user" -> {
                            val id = req.param("id")?.toLong()
                                ?: error("could not parse id of user")
                            val name = req.param("name")
                                ?: error("could not parse name of user")
                            val currency = req.param("currency")?.toCurrency()
                                ?: error("could not parse currency of user")
                            val user = User(id, name, currency)
                            dao.addUser(user).map { added ->
                                if (added) "user added" else "user not added"
                            }
                        }
                        "/v0/item" -> {
                            val id = req.param("id")?.toLong()
                                ?: error("could not parse id of item")
                            val name = req.param("name")
                                ?: error("could not parse name of item")
                            val price = req.param("price")?.toDouble()
                                ?: error("could not parse price of item")
                            val item = Item(id, name, price)
                            dao.addItem(item).map { added ->
                                if (added) "item added" else "item not added"
                            }
                        }
                        else -> error("unimplemented path")
                    }
                }
                HttpMethod.GET -> {
                    when (req.decodedPath) {
                        "/v0/user" -> {
                            val id = req.param("id")?.toLong()
                                ?: error("could not parse id of user")
                            dao.getUser(id).map { user ->
                                "User{id=${user.id},name=${user.name},currency=${user.currency}}"
                            }
                        }
                        "/v0/item" -> {
                            val userId = req.param("user_id")?.toLong()
                                ?: error("could not parse id of user")
                            val itemId = req.param("item_id")?.toLong()
                                ?: error("could not parse id of item")
                            dao.getUser(userId).flatMap { user ->
                                dao.getItem(itemId).flatMap { item ->
                                    HttpClient.newClient(env.currencyApiHost, 443)
                                        .createGet(env.currencyApiPath)
                                        .flatMap { resp ->
                                            resp.content.map { buf ->
                                                val currency =
                                                    json.decodeFromString(CurrencyResponse.serializer(), buf.toString())
                                                "Item{id=${item.id}name=${item.name},price=${item.price * currency.rates[user.currency]!!}}"
                                            }
                                        }
                                }
                            }
                        }
                        else -> error("unimplemented path")
                    }
                }
                else -> error("unimplemented method")
            }
        } catch (e: Exception) {
            Observable.just("exception occurred: ${e.message}")
        }
        resp.writeString(obs)
    }

    server.awaitShutdown()
}