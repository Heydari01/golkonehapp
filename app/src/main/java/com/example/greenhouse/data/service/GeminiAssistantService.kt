package com.example.greenhouse.data.service

import android.util.Log
import com.example.greenhouse.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class GeminiParsedAction(
    val actionType: String, // "CREATE_TRANSACTION" or "CREATE_HARVEST"
    val transactionType: String? = null, // "INCOME" or "EXPENSE"
    val cropType: String? = null,
    val amount: Long = 0L,
    val category: String = "",
    val grade1Weight: Double = 0.0,
    val grade1Price: Long = 0L,
    val grade2Weight: Double = 0.0,
    val grade2Price: Long = 0L,
    val totalRevenue: Long = 0L,
    val note: String = ""
)

data class GeminiAiResponse(
    val replyText: String,
    val parsedAction: GeminiParsedAction? = null,
    val isOnlineAi: Boolean = true
)

object GeminiAssistantService {
    private const val TAG = "GeminiAssistantService"
    private const val MODEL_NAME = "gemini-3.5-flash-lite"
    private const val FALLBACK_API_KEY = "AIzaSyCSqbd_yfVvlJkK_lIn-bksJ_uWv0rpHck"

    private fun getResolvedApiKey(): String {
        val buildKey = BuildConfig.GEMINI_API_KEY.trim()
        return if (buildKey.isNotEmpty() && !buildKey.startsWith("your_")) buildKey else FALLBACK_API_KEY
    }

    private val SYSTEM_INSTRUCTION = """
تو دستیار هوشمند و تحلیل‌گر تخصصی حسابداری، کشت و مدیریت گلخانه‌های صنعتی در ایران هستی.
نام تو «دستیار هوشمند گلخانه» است.

وظایف اصلی تو:
۱. تحلیل متن کاربر برای ثبت خودکار هزینه‌ها، درآمدها و بارهای برداشت شده (به ویژه توت‌فرنگی، خیار، گوجه‌فرنگی، فلفل و...).
۲. اگر کاربر پیامی فرستاد که مفهوم ثبت رویداد مالی یا برداشت بار دارد (مثلاً «۵۰۰ کیلو توت‌فرنگی چیدیم کیلویی ۷۰ هزار تومن» یا «خرید کود پتاس به مبلغ ۱ میلیون و ۲۰۰ هزار تومان»)،
علاوه بر ارائه یک پاسخ فارسی گرم و کوتاه (۱ الی ۲ خط)، در انتهای پاسخت دقیقاً یک بلوک JSON به شکل زیر درج کن:

```json
{
  "action": "CREATE_HARVEST" یا "CREATE_TRANSACTION",
  "crop": "توت‌فرنگی" یا "خیار" یا ...,
  "txType": "INCOME" یا "EXPENSE",
  "amount": 1200000,
  "category": "خرید سم و کود" یا "فروش محصول توت‌فرنگی" یا ...,
  "grade1Weight": 450,
  "grade1Price": 70000,
  "grade2Weight": 50,
  "grade2Price": 45000,
  "totalRevenue": 33750000,
  "note": "توضیحات کوتاه"
}
```

۳. اگر کاربر درباره بهای تمام‌شده، سود هر کیلو، تحلیل هزینه‌ها یا راهنمایی‌های باغبانی، تغذیه و اقلیم گلخانه سوال کرد، بر اساس اطلاعات آماری جاری گلخانه که در پیام ورودی ضمیمه شده است، پاسخی دقیق، تخصصی، دلسوزانه و کاربردی به زبان فارسی بده.
۴. زبان پاسخ منحصراً فارسی و ارقام با لحن شفاف و حرفه‌ای بیان شوند.
""".trimIndent()

    suspend fun queryAssistant(
        userPrompt: String,
        greenhouseContext: String
    ): Result<GeminiAiResponse> = withContext(Dispatchers.IO) {
        val apiKey = getResolvedApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("کلید API جمینای تنظیم نشده است."))
        }

        var connection: HttpURLConnection? = null
        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"
            val url = URL(endpoint)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 25000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }

            // Build request payload
            val fullPrompt = buildString {
                append("اطلاعات جاری و زنده گلخانه:\n")
                append(greenhouseContext)
                append("\n\nپیام یا درخواست گلخانه‌دار:\n")
                append(userPrompt)
            }

            val requestBody = JSONObject().apply {
                // system_instruction
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", SYSTEM_INSTRUCTION) })
                    })
                })
                // contents
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", fullPrompt) })
                        })
                    })
                })
                // generationConfig
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("maxOutputTokens", 1200)
                })
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use {
                    it.readText()
                }
                val json = JSONObject(responseString)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val contentObj = candidate.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val rawText = parts.getJSONObject(0).optString("text", "")
                        val response = processModelOutput(rawText)
                        return@withContext Result.success(response)
                    }
                }
                Result.failure(Exception("پاسخ دریافتی از مدل خالی بود."))
            } else {
                val errorStream = connection.errorStream
                val errText = errorStream?.bufferedReader()?.use { it.readText() } ?: "کد خطا: $responseCode"
                Log.e(TAG, "Gemini API error ($responseCode): $errText")
                Result.failure(Exception("خطای سرویس جمینای ($responseCode): $errText"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to Gemini API", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun processModelOutput(rawText: String): GeminiAiResponse {
        var parsedAction: GeminiParsedAction? = null
        var cleanText = rawText

        // Extract JSON code block if present
        val jsonBlockRegex = Regex("```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```", RegexOption.IGNORE_CASE)
        val match = jsonBlockRegex.find(rawText)

        if (match != null) {
            val jsonString = match.groupValues[1]
            try {
                val actJson = JSONObject(jsonString)
                val action = actJson.optString("action", "")
                if (action.isNotEmpty()) {
                    parsedAction = GeminiParsedAction(
                        actionType = action,
                        transactionType = actJson.optString("txType", "EXPENSE"),
                        cropType = actJson.optString("crop", "توت‌فرنگی"),
                        amount = actJson.optLong("amount", 0L),
                        category = actJson.optString("category", "عمومی"),
                        grade1Weight = actJson.optDouble("grade1Weight", 0.0),
                        grade1Price = actJson.optLong("grade1Price", 0L),
                        grade2Weight = actJson.optDouble("grade2Weight", 0.0),
                        grade2Price = actJson.optLong("grade2Price", 0L),
                        totalRevenue = actJson.optLong("totalRevenue", 0L),
                        note = actJson.optString("note", "")
                    )
                }
                // Strip the raw code fence from the friendly text
                cleanText = rawText.replace(match.value, "").trim()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse action json from model output", e)
            }
        }

        if (cleanText.isBlank()) {
            cleanText = "دستور شما با موفقیت پردازش شد."
        }

        return GeminiAiResponse(
            replyText = cleanText,
            parsedAction = parsedAction,
            isOnlineAi = true
        )
    }
}
