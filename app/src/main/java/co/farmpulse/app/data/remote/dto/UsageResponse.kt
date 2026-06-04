package co.farmpulse.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UsageResponse(
    @SerializedName("plan") val plan: String? = null,
    @SerializedName("period") val period: UsagePeriodDto? = null,
    @SerializedName("limits") val limits: UsageLimitsDto? = null,
    @SerializedName("remaining") val remaining: UsageRemainingDto? = null
)

data class UsagePeriodDto(
    @SerializedName("start") val start: String? = null,
    @SerializedName("end") val end: String? = null,
    @SerializedName("requestCount") val requestCount: Int? = null,
    @SerializedName("aiRequestCount") val aiRequestCount: Int? = null
)

data class UsageLimitsDto(
    @SerializedName("requests") val requests: Int? = null,
    @SerializedName("aiRequests") val aiRequests: Int? = null,
    @SerializedName("maxDays") val maxDays: Int? = null,
    @SerializedName("webhooks") val webhooks: Boolean? = null,
    @SerializedName("teamSeats") val teamSeats: Int? = null,
    @SerializedName("sms") val sms: Boolean? = null
)

data class UsageRemainingDto(
    @SerializedName("requests") val requests: Int? = null,
    @SerializedName("aiRequests") val aiRequests: Int? = null
)

