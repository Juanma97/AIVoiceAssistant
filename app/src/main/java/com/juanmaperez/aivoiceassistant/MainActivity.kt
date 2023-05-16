package com.juanmaperez.aivoiceassistant

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Callback
import okhttp3.Call
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var textToSpeech: TextToSpeech
    private val client = OkHttpClient()

    companion object {
        private const val REQUEST_CODE_VOICE_INPUT = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textToSpeech = TextToSpeech(this, this)

        val btnVoiceInput = findViewById<Button>(R.id.btn_voice_input)
        btnVoiceInput.setOnClickListener {
            startVoiceInput()
        }

    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        startActivityForResult(intent, REQUEST_CODE_VOICE_INPUT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_VOICE_INPUT && resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = result?.get(0)

            getResponse(spokenText) { response ->
                runOnUiThread {
                    val jsonObject = JSONObject(response)
                    val textResponse = jsonObject.getJSONArray("choices").getJSONObject(0).getString("text")
                    val textViewResponse = findViewById<TextView>(R.id.textview_response)
                    textViewResponse.text = "Respuesta: $textResponse"
                    textToSpeech.speak(textResponse, TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        }
    }

    private fun getResponse(question: String?, callback: (String) -> Unit) {
        val apiKey = "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"
        val url = "https://api.openai.com/v1/engines/text-davinci-003/completions"

        val requestBody = """
            {
                "prompt": "$question",
                "max_tokens": 1500,
                "temperature": 0.8
            }
        """.trimIndent()

        val textViewQuestion = findViewById<TextView>(R.id.textview_question)
        textViewQuestion.text = "Pregunta: $question"

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("error", "API fail", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                callback.invoke(body ?: "")
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("MyActivity", "Language not supported")
            }
        } else {
            Log.e("MyActivity", "Initialization failed")
        }
    }
}