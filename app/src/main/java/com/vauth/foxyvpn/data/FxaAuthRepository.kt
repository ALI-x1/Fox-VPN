package com.vauth.foxyvpn.data

import com.vauth.foxyvpn.i18n.tr
import android.util.Base64
import com.vauth.foxyvpn.data.model.RuntimeAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val TAG = "FxaAuthRepository"

enum class LoginStep { CREDENTIALS, TWO_FACTOR }

enum class SessionStatus {
    ACTIVE,
    NEEDS_LOGIN,
    UNREACHABLE,
}

class FxaApiError(message: String, val errno: Int?, val statusCode: Int?) : Exception(message)

class FxaRefreshFailure(
    message: String,
    val permanent: Boolean,
    cause: Throwable? = null,
) : Exception(message, cause)

private const val FXA_AUTH_SERVER = "https://api.accounts.firefox.com/v1"
private const val FIREFOX_CLIENT_ID = "5882386c6d801776"
private const val OAUTH_SCOPE = "profile https://identity.mozilla.com/apps/vpn"
private const val PROTOCOL_VERSION = "identity.mozilla.com/picl/v1/"
private const val PBKDF2_ROUNDS = 1000
private const val STRETCHED_PW_LEN = 32
private const val HKDF_LEN = 32
private const val VERIFICATION_METHOD_EMAIL_2FA = "email-2fa"
private const val FXA_ERRNO_INVALID_PARAMETER = 107
private const val FXA_MAX_CHALLENGE_ATTEMPTS = 5
private const val DEFAULT_ACCESS_TOKEN_TTL_SECONDS = 24 * 60 * 60

private val jsonMediaType = "application/json".toMediaType()

class FxaAuthRepository(
    private val tokenStore: TokenStore,
    private val challengeSolver: FastlyChallengeSolver = FastlyChallengeSolver(ControlPlaneHttp.cookieJar),
) {
    private var pendingSessionToken: String? = null

    companion object {
        fun normalizeEmail(email: String): String =
            email.replace(Regex("[\\p{Z}\\u200B-\\u200D\\uFEFF\\u00A0\\s]+"), "").trim().lowercase()

        fun sanitizeCode(code: String): String =
            code.replace(Regex("[\\p{Z}\\u200B-\\u200D\\uFEFF\\u00A0\\s]+"), "").trim()
    }

    suspend fun startLogin(email: String, password: String): Result<Boolean> = runCatching {
        val cleanEmail = normalizeEmail(email)
        val data = loginAttempt(cleanEmail, password)
        val sessionToken = data.getString("sessionToken")
        pendingSessionToken = sessionToken
        val verified = data.optBoolean("verified", false)
        if (!verified) {
            true
        } else {
            completeLogin(sessionToken)
            false
        }
    }

    suspend fun submitTwoFactorCode(code: String): Result<Boolean> = runCatching {
        val cleanCode = sanitizeCode(code)
        val sessionToken = pendingSessionToken ?: error(tr("No pending FxA session. Start sign-in again."))
        fxaDo("POST", "/session/verify_code", sessionToken = sessionToken, jsonBody = JSONObject().put("code", cleanCode))
        completeLogin(sessionToken)
        false
    }

    /**
     * ورود با «کلید دسترسی» (refresh token) که از Redeem code گرفته شده؛ هیچ لاگین جدیدی به Mozilla فرستاده نمی‌شه.
     * مقدار اولیه‌ی access token فقط جاگذاره و بلافاصله با refresh تازه می‌شه.
     */
    suspend fun loginWithRefreshToken(refreshToken: String): Result<Unit> = runCatching {
        tokenStore.saveAuth(RuntimeAuth(accessToken = "pending", refreshToken = refreshToken, expiresAtEpochSeconds = 0L))
        try {
            ensureFreshAccessToken(force = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runCatching { tokenStore.clear() }
            throw e
        }
        Unit
    }

    /** کلید دسترسی فعلی (فقط برای حالت ادمین: ساخت کد Redeem). */
    fun currentRefreshToken(): String? = tokenStore.loadAuth()?.refreshToken

    suspend fun restoreSession(): SessionStatus {
        val stored = runCatching { tokenStore.loadAuth() }.getOrNull()
            ?: return SessionStatus.NEEDS_LOGIN
        if (stored.refreshToken == null && !tokenStore.hasValidAccessToken()) {
            runCatching { tokenStore.clear() }
            return SessionStatus.NEEDS_LOGIN
        }
        if (tokenStore.hasValidAccessToken()) return SessionStatus.ACTIVE

        return try {
            ensureFreshAccessToken()
            SessionStatus.ACTIVE
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: FxaRefreshFailure) {
            if (failure.permanent) {
                AppLogger.w(TAG, "FxA rejected the stored session; signing out", failure)
                runCatching { tokenStore.clear() }
                SessionStatus.NEEDS_LOGIN
            } else {
                AppLogger.w(TAG, "could not renew the session right now; staying signed in", failure)
                SessionStatus.UNREACHABLE
            }
        } catch (unexpected: Exception) {
            AppLogger.w(TAG, "unexpected error while restoring the session", unexpected)
            SessionStatus.UNREACHABLE
        }
    }

    suspend fun ensureFreshAccessToken(force: Boolean = false): RuntimeAuth {
        val current = tokenStore.loadAuth()
            ?: throw FxaRefreshFailure(tr("Not signed in"), permanent = true)
        if (!force && tokenStore.hasValidAccessToken()) return current

        val refreshToken = current.refreshToken
            ?: throw FxaRefreshFailure(
                tr("The stored session has no refresh token; sign in again."),
                permanent = true,
            )

        val body = JSONObject().apply {
            put("client_id", FIREFOX_CLIENT_ID)
            put("grant_type", "refresh_token")
            put("refresh_token", refreshToken)
            put("scope", OAUTH_SCOPE)
        }

        val tokenData = try {
            refreshViaOauthHost(body) ?: fxaDo("POST", "/oauth/token", jsonBody = body)
        } catch (rejected: FxaApiError) {
            throw FxaRefreshFailure(
                rejected.message ?: tr("FxA rejected the refresh token"),
                permanent = rejected.isPermanentRefreshRejection(),
                cause = rejected,
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (transport: Exception) {
            throw FxaRefreshFailure(
                transport.message ?: tr("Could not reach the Firefox Accounts server"),
                permanent = false,
                cause = transport,
            )
        }

        val accessToken = tokenData.optString("access_token").takeIf { it.isNotBlank() }
            ?: throw FxaRefreshFailure(
                tr("FxA returned no access token for the refresh grant"),
                permanent = false,
            )

        val renewed = RuntimeAuth(
            accessToken = accessToken,
            refreshToken = tokenData.optString("refresh_token", "").ifBlank { null } ?: refreshToken,
            expiresAtEpochSeconds = expiryFrom(tokenData),
        )
        runCatching { tokenStore.saveAuth(renewed) }
            .onFailure { AppLogger.w(TAG, "could not persist the renewed token", it) }
        return renewed
    }

    suspend fun currentAccessToken(): String? = try {
        ensureFreshAccessToken().accessToken
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: FxaRefreshFailure) {
        if (failure.permanent) null else tokenStore.loadAuth()?.accessToken
    }

    suspend fun refreshAccessToken(): RuntimeAuth? = try {
        ensureFreshAccessToken(force = true)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: FxaRefreshFailure) {
        AppLogger.w(TAG, "access token renewal failed", failure)
        null
    }

    private fun FxaApiError.isPermanentRefreshRejection(): Boolean {
        return statusCode == 400 || statusCode == 401 || statusCode == 403
    }

    private fun expiryFrom(tokenData: JSONObject): Long {
        val expiresIn = tokenData.optInt("expires_in", 0)
            .takeIf { it > 0 } ?: DEFAULT_ACCESS_TOKEN_TTL_SECONDS
        return System.currentTimeMillis() / 1000 + expiresIn
    }

    private suspend fun completeLogin(sessionToken: String) {
        val tokenData = oauthToken(sessionToken)
        tokenStore.saveAuth(
            RuntimeAuth(
                accessToken = tokenData.getString("access_token"),
                refreshToken = tokenData.optString("refresh_token", "").ifBlank { null },
                expiresAtEpochSeconds = expiryFrom(tokenData),
            ),
        )
        pendingSessionToken = null
    }

    private suspend fun loginAttempt(email: String, password: String, withVerificationMethod: Boolean = true): JSONObject {
        val body = JSONObject().apply {
            put("email", email)
            put("authPW", deriveAuthPw(email, password))
            if (withVerificationMethod) put("verificationMethod", VERIFICATION_METHOD_EMAIL_2FA)
        }
        return try {
            fxaDo("POST", "/account/login", jsonBody = body)
        } catch (e: FxaApiError) {
            if (e.errno == FXA_ERRNO_INVALID_PARAMETER && withVerificationMethod) {
                loginAttempt(email, password, withVerificationMethod = false)
            } else {
                throw e
            }
        }
    }

    /** پروفایل درخواست OAuth فایرفاکس (همون که کلاینت آرمان می‌فرسته). */
    private fun Request.Builder.firefoxOauthProfile(ua: String): Request.Builder = this
        .header("User-Agent", ua)
        .header("Accept-Encoding", "identity")
        .header("Accept", "application/json;q=0.9,*/*;q=0.2")
        .header("Accept-Language", "en-US, en")
        .header("Cache-Control", "no-cache")
        .header("Pragma", "no-cache")
        .header("Sec-Fetch-Dest", "empty")
        .header("Sec-Fetch-Mode", "no-cors")
        .header("Sec-Fetch-Site", "none")
        .header("Priority", "u=4")

    private fun b64url(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    /**
     * مسیر جایگزین وقتی /oauth/token را Fastly رد می‌کند (۴۰۶): نشست ← /oauth/authorization (Hawk) ← code،
     * بعد code + PKCE روی oauth.accounts.firefox.com/v1/token (بدون Hawk). null = ۴۰۶ شد؛ برو سراغ راه قبلی.
     */
    private suspend fun oauthTokenViaCode(sessionToken: String, ua: String): JSONObject? = withContext(Dispatchers.IO) {
        val (id, key) = deriveHawkCredentials(sessionToken)
        val verifier = b64url(ByteArray(32).also { SecureRandom().nextBytes(it) })
        val challenge = b64url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.UTF_8)))
        val state = b64url(ByteArray(12).also { SecureRandom().nextBytes(it) })
        val jsonType = "application/json; charset=utf-8".toMediaType()

        val authUrl = "$FXA_AUTH_SERVER/oauth/authorization".toHttpUrl()
        val authBytes = JSONObject().apply {
            put("client_id", FIREFOX_CLIENT_ID)
            put("response_type", "code")
            put("scope", OAUTH_SCOPE)
            put("state", state)
            put("access_type", "offline")
            put("code_challenge", challenge)
            put("code_challenge_method", "S256")
        }.toString().toByteArray(Charsets.UTF_8)
        val authReq = Request.Builder().url(authUrl).firefoxOauthProfile(ua)
            .header("Authorization", hawkHeader("POST", authUrl, id, key, authBytes))
            .post(authBytes.toRequestBody(jsonType)).build()
        val (c1, t1) = ControlPlaneHttp.client.newCall(authReq).execute().use { it.code to it.body?.string().orEmpty() }
        if (c1 == 406) return@withContext null
        if (c1 >= 400) {
            val eb = try { JSONObject(t1) } catch (e: Exception) { JSONObject() }
            throw FxaApiError(eb.optString("message", t1.ifEmpty { "HTTP $c1" }), if (eb.has("errno")) eb.optInt("errno") else null, c1)
        }
        val auth = JSONObject(t1)
        val code = auth.optString("code")
        if (code.isBlank() || auth.optString("state") != state) throw IllegalStateException("invalid OAuth authorization response")

        val tokenBytes = JSONObject().apply {
            put("client_id", FIREFOX_CLIENT_ID)
            put("grant_type", "authorization_code")
            put("code", code)
            put("code_verifier", verifier)
        }.toString().toByteArray(Charsets.UTF_8)
        val tokenReq = Request.Builder().url("https://oauth.accounts.firefox.com/v1/token").firefoxOauthProfile(ua)
            .post(tokenBytes.toRequestBody(jsonType)).build()
        val (c2, t2) = ControlPlaneHttp.client.newCall(tokenReq).execute().use { it.code to it.body?.string().orEmpty() }
        if (c2 == 406) return@withContext null
        if (c2 >= 400) {
            val eb = try { JSONObject(t2) } catch (e: Exception) { JSONObject() }
            throw FxaApiError(eb.optString("message", t2.ifEmpty { "HTTP $c2" }), if (eb.has("errno")) eb.optInt("errno") else null, c2)
        }
        JSONObject(t2).takeIf { it.optString("access_token").isNotBlank() }
    }

    /**
     * تازه‌سازی با پروفایل درخواست فایرفاکس روی oauth.accounts.firefox.com (مثل کلاینت آرمان).
     * null = ۴۰۶/خطای شبکه؛ مسیر قبلی ادامه می‌دهد. خطای واقعیِ موزیلا (۴xx) پرتاب می‌شود.
     */
    private suspend fun refreshViaOauthHost(body: JSONObject): JSONObject? = withContext(Dispatchers.IO) {
        val bytes = body.toString().toByteArray(Charsets.UTF_8)
        val jsonType = "application/json; charset=utf-8".toMediaType()
        for (ua in listOf("MozillaVPN/2.24.0 (sys:android 14)", FIREFOX_DESKTOP_UA)) {
            try {
                val req = Request.Builder().url("https://oauth.accounts.firefox.com/v1/token").firefoxOauthProfile(ua)
                    .post(bytes.toRequestBody(jsonType)).build()
                val (code, text) = ControlPlaneHttp.client.newCall(req).execute().use { it.code to it.body?.string().orEmpty() }
                if (code == 406 || code >= 500) continue
                if (code >= 400) {
                    val eb = try { JSONObject(text) } catch (e: Exception) { JSONObject() }
                    throw FxaApiError(eb.optString("message", text.ifEmpty { "HTTP $code" }), if (eb.has("errno")) eb.optInt("errno") else null, code)
                }
                val j = JSONObject(text)
                if (j.optString("access_token").isNotBlank()) return@withContext j
            } catch (e: FxaApiError) {
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                continue
            }
        }
        null
    }

    private suspend fun oauthToken(sessionToken: String): JSONObject {
        // مثل FoxyVPN رسمی و آرمان: اول با UAِ Mozilla VPN، بعد UA دسکتاپ فایرفاکس؛ تا تأییدشدن نشست (errno 104/138) هر ۳ ثانیه تکرار
        for (ua in listOf("MozillaVPN/2.24.0 (sys:android 14)", FIREFOX_DESKTOP_UA)) {
            var viaCode: JSONObject? = null
            for (attempt in 0 until 12) {
                try {
                    viaCode = oauthTokenViaCode(sessionToken, ua)
                    break
                } catch (e: FxaApiError) {
                    if ((e.errno == 104 || e.errno == 138) && attempt < 11) {
                        kotlinx.coroutines.delay(3_000L)
                        continue
                    }
                    throw e
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    break
                }
            }
            if (viaCode != null) return viaCode
        }
        val body = JSONObject().apply {
            put("client_id", FIREFOX_CLIENT_ID)
            put("grant_type", "fxa-credentials")
            put("scope", OAUTH_SCOPE)
            put("access_type", "offline")
        }
        return fxaDo("POST", "/oauth/token", sessionToken = sessionToken, jsonBody = body)
    }

    private suspend fun fxaDo(
        method: String,
        path: String,
        sessionToken: String? = null,
        jsonBody: JSONObject,
    ): JSONObject = withContext(Dispatchers.IO) {
        val url = "$FXA_AUTH_SERVER$path".toHttpUrl()
        val bodyBytes = jsonBody.toString().toByteArray(Charsets.UTF_8)
        var tokenId: String? = null
        var hmacKey: ByteArray? = null
        if (sessionToken != null) {
            val (id, key) = deriveHawkCredentials(sessionToken)
            tokenId = id
            hmacKey = key
        }

        fun buildRequest(): Request {
            val builder = Request.Builder().url(url).applyMozillaVpnHeaders()
            builder.header("Content-Type", "application/json")
            if (sessionToken != null) {
                builder.header("Authorization", hawkHeader(method, url, tokenId!!, hmacKey!!, bodyBytes))
            }
            builder.method(method, bodyBytes.toRequestBody(jsonMediaType))
            return builder.build()
        }

        val viaWebViewOk = sessionToken == null && path == "/oauth/token"
        suspend fun viaWebView(): JSONObject {
            val (code, text) = WebViewFxa.postJson(path, jsonBody)
            if (code >= 400) {
                val eb = try { JSONObject(text) } catch (e: Exception) { JSONObject() }
                throw FxaApiError(eb.optString("message", text.ifEmpty { "HTTP $code" }), if (eb.has("errno")) eb.optInt("errno") else null, code)
            }
            return if (text.isEmpty()) JSONObject() else JSONObject(text)
        }

        // اگر به‌تازگی برای همین درخواست ۴۰۶ دیده‌ایم، درخواست OkHttp محکوم به شکست را رد می‌کنیم و مستقیم WebView
        if (viaWebViewOk && WebViewFxa.challengeRecent() && ControlPlaneHttp.appContext != null) {
            try {
                return@withContext viaWebView()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (api: FxaApiError) {
                throw api
            } catch (_: Exception) {
                // WebView شکست خورد؛ مسیر معمولی (OkHttp + حل‌کننده) را امتحان می‌کنیم
            }
        }
        var response: Response = ControlPlaneHttp.client.newCall(buildRequest()).execute()
        if (response.code == 406 && viaWebViewOk) WebViewFxa.sawChallengeAt = System.currentTimeMillis()
        // تازه‌سازی توکن: اول WebView واقعی (سریع‌تر و مطمئن‌تر)؛ فقط اگر خودِ WebView خراب شد سراغ حل‌کننده می‌رویم
        if (response.code == 406 && viaWebViewOk) {
            try {
                val viaWv = viaWebView()
                response.close()
                return@withContext viaWv
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (api: FxaApiError) {
                response.close()
                throw api
            } catch (_: Exception) {
                // WebView در دسترس نبود یا شکست خورد؛ ادامه با حل‌کننده‌ی داخلی
            }
        }
        var attempts = 1
        while (response.code == 406 && attempts < FXA_MAX_CHALLENGE_ATTEMPTS) {
            val challengedBody = response.use { it.body?.string().orEmpty() }
            try {
                challengeSolver.solveAndInstall(challengedBody, "${url.scheme}://${url.host}")
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (solveFailure: Exception) {
                // حل‌کننده‌ی داخلی نشد؛ برای تازه‌سازی توکن، همان درخواست را از داخل WebView واقعی امتحان کن
                if (viaWebViewOk) return@withContext viaWebView()
                throw solveFailure
            }
            response = ControlPlaneHttp.client.newCall(buildRequest()).execute()
            attempts++
        }
        if (response.code == 406 && viaWebViewOk) {
            response.close()
            return@withContext viaWebView()
        }

        response.use { resp ->
            val text = resp.body?.string().orEmpty()
            if (resp.code >= 400) {
                val errorBody = try {
                    JSONObject(text)
                } catch (e: Exception) {
                    JSONObject()
                }
                val errno = if (errorBody.has("errno")) errorBody.optInt("errno") else null
                val message = errorBody.optString("message", text.ifEmpty { "HTTP ${resp.code}" })
                throw FxaApiError(message, errno, resp.code)
            }
            if (text.isEmpty()) return@withContext JSONObject()
            return@withContext JSONObject(text)
        }
    }

    private fun deriveQuickStretch(email: String, password: String): ByteArray {
        val salt = "${PROTOCOL_VERSION}quickStretch:$email".toByteArray(Charsets.UTF_8)
        return pbkdf2HmacSha256(password.toByteArray(Charsets.UTF_8), salt, PBKDF2_ROUNDS, STRETCHED_PW_LEN)
    }

    private fun deriveAuthPw(email: String, password: String): String {
        val quickStretched = deriveQuickStretch(email, password)
        return hkdf(quickStretched, "${PROTOCOL_VERSION}authPW", HKDF_LEN).toHex()
    }

    private fun deriveHawkCredentials(sessionTokenHex: String): Pair<String, ByteArray> {
        val sessionToken = sessionTokenHex.hexToBytes()
        val expanded = hkdf(sessionToken, "${PROTOCOL_VERSION}sessionToken", 64)
        return expanded.copyOfRange(0, 32).toHex() to expanded.copyOfRange(32, 64)
    }

    private fun hawkHeader(method: String, url: HttpUrl, tokenId: String, hmacKey: ByteArray, body: ByteArray): String {
        val ts = (System.currentTimeMillis() / 1000).toString()
        val nonceBytes = ByteArray(6).also { SecureRandom().nextBytes(it) }
        val nonce = Base64.encodeToString(nonceBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val path = url.encodedPath + if (url.encodedQuery != null) "?${url.encodedQuery}" else ""

        var payloadHash = ""
        if (body.isNotEmpty()) {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update("hawk.1.payload\napplication/json\n".toByteArray(Charsets.UTF_8))
            digest.update(body)
            digest.update("\n".toByteArray(Charsets.UTF_8))
            payloadHash = Base64.encodeToString(digest.digest(), Base64.NO_WRAP)
        }

        val normalized = "hawk.1.header\n$ts\n$nonce\n${method.uppercase()}\n$path\n${url.host}\n${url.port}\n$payloadHash\n\n"

        val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(hmacKey, "HmacSHA256")) }
        val macB64 = Base64.encodeToString(mac.doFinal(normalized.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)

        var header = "Hawk id=\"$tokenId\", ts=\"$ts\", nonce=\"$nonce\", mac=\"$macB64\""
        if (payloadHash.isNotEmpty()) header += ", hash=\"$payloadHash\""
        return header
    }
}

private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(key, "HmacSHA256"))
    return mac.doFinal(data)
}

private fun hkdf(ikm: ByteArray, info: String, length: Int, salt: ByteArray = ByteArray(32)): ByteArray {
    val prk = hmacSha256(salt, ikm)
    val infoBytes = info.toByteArray(Charsets.UTF_8)
    val result = ByteArray(length)
    var previousBlock = ByteArray(0)
    var generated = 0
    var counter = 1
    while (generated < length) {
        val block = hmacSha256(prk, previousBlock + infoBytes + byteArrayOf(counter.toByte()))
        val toCopy = minOf(block.size, length - generated)
        System.arraycopy(block, 0, result, generated, toCopy)
        generated += toCopy
        previousBlock = block
        counter++
    }
    return result
}

private fun pbkdf2HmacSha256(password: ByteArray, salt: ByteArray, iterations: Int, keyLengthBytes: Int): ByteArray {
    val hLen = 32
    val numBlocks = (keyLengthBytes + hLen - 1) / hLen
    val result = ByteArray(numBlocks * hLen)
    val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(password, "HmacSHA256")) }
    for (blockIndex in 1..numBlocks) {
        val blockIndexBytes = ByteBuffer.allocate(4).putInt(blockIndex).array()
        var u = mac.doFinal(salt + blockIndexBytes)
        val t = u.copyOf()
        for (iter in 2..iterations) {
            u = mac.doFinal(u)
            for (i in t.indices) t[i] = (t[i].toInt() xor u[i].toInt()).toByte()
        }
        System.arraycopy(t, 0, result, (blockIndex - 1) * hLen, hLen)
    }
    return result.copyOf(keyLengthBytes)
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
private fun String.hexToBytes(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
