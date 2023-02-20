package com.acmesoftware.cleanframework.actions.dialogs

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.DarculaColors
import com.intellij.ui.JBColor
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*

class GenerateGatewayDialog(private val callback: Callback) : DialogWrapper(true) {
    private lateinit var nameTextField: JTextField
    private lateinit var requestNameTextField: JTextField
    private lateinit var errorLabel: JLabel

    init {
        init()
        title = "Create a Gateway"
        setOKButtonText("Create")
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        panel.minimumSize = Dimension(500, 100)
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        val namePanel = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 8))
        val requestNamePanel = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 8))
        val errorPanel = JPanel(BorderLayout())

        val nameFieldLabel = JLabel("Gateway Name:")
        val requestNameFieldLabel = JLabel("Request Name:")
        errorLabel = JLabel()
        namePanel.add(nameFieldLabel)

        nameTextField = JTextField()
        nameTextField.preferredSize = Dimension(300, 30)
        nameTextField.addKeyListener(
            object : KeyAdapter() {
                override fun keyReleased(e: KeyEvent?) {
                    requestNameTextField.text = nameTextField.text
                }
            }
        )
        nameTextField.addFocusListener(
            object : FocusAdapter() {
                override fun focusGained(e: FocusEvent?) {
                    if (errorLabel.text.isNotBlank()) {
                        errorLabel.text = ""
                    }
                }
            }
        )

        namePanel.add(nameTextField)
        requestNamePanel.add(requestNameFieldLabel)

        requestNameTextField = JTextField()
        requestNameTextField.preferredSize = Dimension(300, 30)
        requestNameTextField.addFocusListener(
            object : FocusAdapter() {
                override fun focusGained(e: FocusEvent?) {
                    if (errorLabel.text.isNotBlank()) {
                        errorLabel.text = ""
                    }
                }
            }
        )

        requestNamePanel.add(requestNameTextField, BorderLayout.CENTER)
        errorPanel.add(errorLabel, BorderLayout.SOUTH)

        panel.add(namePanel)
        panel.add(requestNamePanel)
        panel.add(errorPanel)
        return panel
    }

    override fun doOKAction() {
        val name = nameTextField.text
        val requestName = requestNameTextField.text
        val nameRegex = Regex("^[A-Z][a-z]+([A-Z][a-z]+)*$")

        if (nameRegex.matches(name) && nameRegex.matches(requestName)) {
            super.doOKAction()

            callback.onGenerateGateway(name, requestName)
        } else {
            errorLabel.text = "Name should be in PascalCase"
            errorLabel.foreground = JBColor(DarculaColors.RED, DarculaColors.RED)
        }
    }

    interface Callback {
        fun onGenerateGateway(name: String, requestName: String)
    }
}