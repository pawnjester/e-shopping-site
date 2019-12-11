package co.loystar.loystarbusiness.models.databinders

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.DateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Invoice(

        @JsonProperty("id")
        val id: Int? = null,

        @JsonProperty("payment_message")
        val paymentMessage: String? = null,

        @JsonProperty("status")
        val status: String? = null,

        @JsonProperty("paid_at")
        val paidAt: String? = null,

        @JsonProperty("paid_amount")
        val paidAmount: String? = null,

        @JsonProperty("created_at")
        val createdAt: DateTime? = null,

        @JsonProperty("updated_at")
        val updatedAt: DateTime? = null,

        @JsonProperty("due_date")
        val dueDate: Any? = null,

        @JsonProperty("subtotal")
        val subtotal: String? = null,

        @JsonProperty("number")
        val number: String? = null,

        @JsonProperty("items")
        val items: List<ItemsItem?>? = null,

        @JsonProperty("business_name")
        val businessName: String? = null,

        @JsonProperty("customer")
        val customer: Customer? = null,

        @JsonProperty("invoice_payment_histories")
        val invoicePaymentHistories: List<InvoicePaymentHistoriesItem?>? = null
)