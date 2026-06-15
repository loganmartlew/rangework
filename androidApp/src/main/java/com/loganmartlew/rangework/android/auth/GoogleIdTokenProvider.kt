package com.loganmartlew.rangework.android.auth

interface GoogleIdTokenProvider {
    suspend fun requestIdToken(): GoogleIdTokenRequestResult
}
