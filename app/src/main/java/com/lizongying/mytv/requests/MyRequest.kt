package com.lizongying.mytv.requests

import com.lizongying.mytv.api.ApiClient
import com.lizongying.mytv.api.Release
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MyRequest {
    private var releaseService = ApiClient().releaseService

    suspend fun getRelease(): Release? {
        return withContext(Dispatchers.IO) {
            fetchRelease()
        }
    }

    private suspend fun fetchRelease(): Release? {
        return suspendCoroutine { continuation ->
            releaseService.getRelease()
                .enqueue(object : Callback<Release> {
                    override fun onResponse(call: Call<Release>, response: Response<Release>) {
                        if (response.isSuccessful) {
                            continuation.resume(response.body())
                        } else {
                            continuation.resume(null)
                        }
                    }

                    override fun onFailure(call: Call<Release>, t: Throwable) {
                        continuation.resume(null)
                    }
                })
        }
    }

    companion object {
        private const val TAG = "MyRequest"
    }
}