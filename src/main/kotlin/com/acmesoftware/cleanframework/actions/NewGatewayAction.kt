package com.acmesoftware.cleanframework.actions

import com.acmesoftware.cleanframework.actions.dialogs.GenerateGatewayDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class NewGatewayAction : AnAction(), GenerateGatewayDialog.Callback {
    override fun actionPerformed(e: AnActionEvent) {
        val dialog = GenerateGatewayDialog(this)
        dialog.show()
    }

    override fun onGenerateGateway(name: String, requestName: String) {
        TODO("Not yet implemented")
    }
}

