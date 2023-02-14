package com.acmesoftware.cleanframework.actions

import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.*

class GenerateFeatureDialog(private val callback: Callback): DialogWrapper(true) {
    private lateinit var featureNameTextField: JTextField
    private lateinit var errorLabel: JLabel

    init {
        init()
        title = "Generate Feature Directory"
        setOKButtonText("Generate")
    }

    override fun createCenterPanel(): JComponent? {
        val panel = JPanel(BorderLayout())

        val nameFieldLabel = JLabel("Feature Name: ")
        errorLabel = JLabel()
        panel.add(nameFieldLabel, BorderLayout.WEST)
        panel.add(errorLabel, BorderLayout.SOUTH)

        featureNameTextField = JTextField()
        featureNameTextField.preferredSize = Dimension(300, 30)
        featureNameTextField.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent?) {
                errorLabel.text = ""
            }
        })

        panel.add(featureNameTextField, BorderLayout.CENTER)
        return panel
    }

    override fun doOKAction() {
        val featureName = featureNameTextField.text
        val featureNameRegex = Regex("^[A-Z][a-z]+([A-Z][a-z]+)*$")

        if(featureNameRegex.matches(featureName)){
            super.doOKAction()

            callback.onGenerateFeature(featureNameTextField.text)
        } else {
            errorLabel.text = "Feature name should be in PascalCase"
            errorLabel.foreground = Color(16739176)
        }
    }

    interface Callback {
        fun onGenerateFeature(featureName: String)
    }
}