package co.farmpulse.app.data.remote.api

import co.farmpulse.app.data.remote.dto.TreeAnalysisResponse
import co.farmpulse.app.data.remote.dto.TreeHistoryResponse
import co.farmpulse.app.data.remote.dto.TreeQuotaResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface TreeApiService {
    @Multipart
    @POST("v1/trees/analyze")
    suspend fun analyzeTrees(
        @Part image: MultipartBody.Part,
        @Part("farmerId") farmerId: RequestBody,
        @Part("county") county: RequestBody,
        @Part("landAcres") landAcres: RequestBody,
        @Part("notes") notes: RequestBody
    ): TreeAnalysisResponse

    @GET("v1/trees/history")
    suspend fun getHistory(
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null
    ): TreeHistoryResponse

    @GET("v1/trees/quota")
    suspend fun getQuota(): TreeQuotaResponse
}

