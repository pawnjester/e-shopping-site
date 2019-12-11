package co.loystar.loystarbusiness.models.databinders

import com.fasterxml.jackson.annotation.JsonProperty

data class ItemsItem (
        @JsonProperty("product")
        val product: Product? = null,

        @JsonProperty("amount")
        val amount: String? = null,

        @JsonProperty("quantity")
        val quantity: Int? = null
)