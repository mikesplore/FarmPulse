package co.farmpulse.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TreeQuotaResponse(
    @SerializedName("plan") val plan: String? = null,
    @SerializedName("used") val used: Int? = null,
    @SerializedName("limit") val limit: Int? = null,
    @SerializedName("remaining") val remaining: Int? = null,
    @SerializedName("unlimited") val unlimited: Boolean? = null,
    @SerializedName("resets_at") val resetsAt: String? = null
)

