package model

import kotlinx.serialization.Serializable

@Serializable
data class CurrencyResponse(val rates: Map<Currency, Double>)

enum class Currency {
    USD,
    EUR,
    RUB,
}

fun String.toCurrency(): Currency {
    return when (this) {
        Currency.USD.name -> Currency.USD
        Currency.EUR.name -> Currency.EUR
        Currency.RUB.name -> Currency.RUB
        else -> error("nu such currency")
    }
}