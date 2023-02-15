package com.acmesoftware.cleanframework.utils

import com.google.common.io.CharStreams
import org.apache.commons.lang.text.StrSubstitutor
import java.io.InputStreamReader

class Template(templatePath: String) {
    private val template: String

    init {
        val resource = "/templates/$templatePath"

        try {
            val resourceStream = this::class.java.getResourceAsStream(resource)!!
            template = CharStreams.toString(InputStreamReader(resourceStream, Charsets.UTF_8))
        } catch (e: Exception) {
            e.printStackTrace()
            throw TemplateNotFoundException(resource)
        }
    }

    fun fill(values: HashMap<String, String>): String {
        val substitutor = StrSubstitutor(values, "{{", "}}", '\\')
        return substitutor.replace(template)
    }
}

class TemplateNotFoundException(template: String) : Exception("Could not find template: $template")