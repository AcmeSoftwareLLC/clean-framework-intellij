package com.acmesoftware.cleanframework.utils

import com.google.common.io.CharStreams
import java.io.InputStreamReader

object TemplateUtils {
    fun read( templatePath: String): String {
        val resource = "/templates/$templatePath"

        try {
            val resourceStream = this::class.java.getResourceAsStream(resource)!!
            return CharStreams.toString(InputStreamReader(resourceStream, Charsets.UTF_8))
        } catch (e: Exception) {
            e.printStackTrace()
            throw TemplateNotFoundException(resource)
        }
    }
}

class TemplateNotFoundException(template: String) : Exception("Could not find template: $template")