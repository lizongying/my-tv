package com.lizongying.mytv.requests

import com.lizongying.mytv.api.ApiClient
import com.lizongying.mytv.api.ReleaseV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ReleaseRequest {
    private var releaseService = ApiClient().releaseService

    suspend fun getRelease(): ReleaseResponse? {
        return withContext(Dispatchers.IO) {
            fetchRelease()
        }
    }

    private suspend fun fetchRelease(): ReleaseResponse? {
        return suspendCoroutine { continuation ->
            releaseService.getRelease()
                .enqueue(object : Callback<ReleaseResponse> {
                    override fun onResponse(
                        call: Call<ReleaseResponse>,
                        response: Response<ReleaseResponse>
                    ) {
                        if (response.isSuccessful) {
                            continuation.resume(response.body())
                        } else {
                            continuation.resume(null)
                        }
                    }

                    override fun onFailure(call: Call<ReleaseResponse>, t: Throwable) {
                        continuation.resume(null)
                    }
                })
        }
    }

    companion object {
        private const val TAG = "ReleaseRequest"
    }
}