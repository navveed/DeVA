package com.deva.voice.intents.impl

import android.content.Context
import android.content.Intent
import com.deva.voice.intents.AppIntent
import com.deva.voice.intents.ParameterSpec

class ShareTextIntent : AppIntent {
    override val name: String = "ShareText"

    override fun description(): String =
        "Open the system share sheet to send text. Use this when you want to send a text to someone, it will give access to all the apps here"

    override fun parametersSpec(): List<ParameterSpec> = listOf(
        ParameterSpec(
            name = "text",
            type = "string",
            required = true,
            description = "The text to share."
        ),
        ParameterSpec(
            name = "chooser_title",
            type = "string",
            required = false,
            description = "Optional chooser title shown on the share sheet."
        )
    )

    override fun buildIntent(context: Context, params: Map<String, Any?>): Intent? {
        val text = params["text"]?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return null
        val base = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val title = params["chooser_title"]?.toString()?.takeIf { it.isNotBlank() } ?: "Share via"
        return Intent.createChooser(base, title)
    }
}




