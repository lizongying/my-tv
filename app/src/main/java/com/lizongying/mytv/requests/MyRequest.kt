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

class MyRequest {
    private var releaseService = ApiClient().releaseService

    suspend fun getRelease(): ReleaseV2? {
        return withContext(Dispatchers.IO) {
            fetchRelease()
        }
    }

    private suspend fun fetchRelease(): ReleaseV2? {
        return suspendCoroutine { continuation ->
            releaseService.getRelease()
                .enqueue(object : Callback<ReleaseV2> {
                    override fun onResponse(call: Call<ReleaseV2>, response: Response<ReleaseV2>) {
                        if (response.isSuccessful) {
                            continuation.resume(response.body())
                        } else {
                            continuation.resume(null)
                        }
                    }

                    override fun onFailure(call: Call<ReleaseV2>, t: Throwable) {
                        continuation.resume(null)
                    }
                })
        }
    }

    companion object {
        private const val TAG = "MyRequest"
    }
}