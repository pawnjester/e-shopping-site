package co.loystar.loystarbusiness.models.databinders

import com.fasterxml.jackson.annotation.JsonProperty

data class InvoicePaymentHistoriesItem (
        @JsonProperty("id")
        val id: Int? = null,
        @JsonProperty("invoice_id")
        val invoiceId: Int? = null,

        @JsonProperty("paid_amount")
        val paidAmount: String? = null,

        @JsonProperty("paid_at")
        val paidAt: String? = null,

        @JsonProperty("created_at")
        val createdAt: String? = null,

        @JsonProperty("updated_at")
        val updatedAt: String? = null
)