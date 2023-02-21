package com.acmesoftware.cleanframework.actions

import com.acmesoftware.cleanframework.actions.dialogs.GenerateGatewayDialog
import com.acmesoftware.cleanframework.utils.Template
import com.google.common.base.CaseFormat
import com.intellij.ide.projectView.impl.IdeViewForProjectViewPane
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.file.PsiDirectoryFactory
import com.jetbrains.lang.dart.DartLanguage

class NewGatewayAction : AnAction(), GenerateGatewayDialog.Callback {
    private lateinit var dataContext: DataContext

    override fun actionPerformed(e: AnActionEvent) {
        val dialog = GenerateGatewayDialog(this)
        dialog.show()
    }

    override fun update(e: AnActionEvent) {
        e.dataContext.let {
            this.dataContext = it
            val presentation = e.presentation
            presentation.isEnabled = true
        }
    }

    override fun onGenerateGateway(name: String, requestName: String) {
        val application = ApplicationManager.getApplication()
        val project = CommonDataKeys.PROJECT.getData(dataContext)
        val view = LangDataKeys.IDE_VIEW.getData(dataContext)

        val adapterVirtualFile = if (view == null || view !is IdeViewForProjectViewPane) {
            val editor = LangDataKeys.EDITOR.getData(dataContext)!!
            FileChooser.chooseFile(
                FileChooserDescriptor(false, true, false, false, false, false),
                project,
                FileDocumentManager.getInstance().getFile(editor.document)!!.parent
            )!!
        } else {
            view.orChooseDirectory!!.virtualFile
        }

        application.runWriteAction {
            val runnable = Runnable {
                val fileFactory = PsiFileFactory.getInstance(project)
                val directory = PsiDirectoryFactory.getInstance(project).createDirectory(adapterVirtualFile)
                val fileName = "${CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name)}_gateway.dart"
                val fillValues = hashMapOf(
                    "name" to name,
                    "request_name" to requestName,
                )

                var file = directory.findFile(fileName)
                if (file == null) {
                    val content = Template("adapter/gateway.dart").fill(fillValues)
                    file = fileFactory.createFileFromText(fileName, DartLanguage.INSTANCE, content)
                    directory.add(file)

                    application.invokeLater {
                        Notifications.Bus.notify(
                            Notification(
                                "com.acmesoftware.notification",
                                "CREATED GATEWAY",
                                "Created '${name}Gateway' with '${requestName}Request'",
                                NotificationType.INFORMATION,
                            )
                        )
                    }
                } else {
                    application.invokeLater {
                        Notifications.Bus.notify(
                            Notification(
                                "com.acmesoftware.notification",
                                "GATEWAY CREATION FAILED",
                                "The '${name}Gateway' already exists!",
                                NotificationType.ERROR,
                            )
                        )
                    }
                }
            }

            CommandProcessor.getInstance().executeCommand(project, runnable, "Create New Gateway", null)
        }
    }
}

