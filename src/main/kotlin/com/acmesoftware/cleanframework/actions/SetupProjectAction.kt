package com.acmesoftware.cleanframework.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.file.PsiDirectoryFactory
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.util.PubspecYamlUtil

class SetupProjectAction : AnAction() {
    private lateinit var dataContext: DataContext

    override fun actionPerformed(e: AnActionEvent) {
        val project = CommonDataKeys.PROJECT.getData(dataContext)
        val view = LangDataKeys.IDE_VIEW.getData(dataContext)
        val directory = view!!.orChooseDirectory!!


        val pubspecFile = PubspecYamlUtil.findPubspecYamlFile(project!!, directory.virtualFile)
        val libDir = pubspecFile!!.parent.findChild(PubspecYamlUtil.LIB_DIR_NAME)!!
        val packageName = PubspecYamlUtil.getDartProjectName(pubspecFile)!!

        ApplicationManager.getApplication().runWriteAction {
            val runnable = Runnable {
                val fileFactory = PsiFileFactory.getInstance(project)
                val libDirectory = PsiDirectoryFactory.getInstance(project).createDirectory(libDir)

                var providersDirectory = libDirectory.findSubdirectory("providers")
                if (providersDirectory == null) {
                    providersDirectory = libDirectory.createSubdirectory("providers")
                }

                val featuresDirectory = libDirectory.findSubdirectory("features")
                if (featuresDirectory == null) {
                    libDirectory.createSubdirectory("features")
                }

                val providerFiles = listOf(
                    "use_case_providers.dart",
                    "gateway_providers.dart",
                    "external_interface_providers.dart"
                )
                providerFiles.forEach {
                    if (providersDirectory.findFile(it) == null) {
                        providersDirectory.add(fileFactory.createFileFromText(it, DartLanguage.INSTANCE, ""))
                    }
                }

                val providersFile = libDirectory.findFile("providers.dart")
                if (providersFile == null) {
                    val content = """
                        export 'package:$packageName/providers/use_case_providers.dart';
                        export 'package:$packageName/providers/gateway_providers.dart';
                        export 'package:$packageName/providers/external_interface_providers.dart';
                    """.trimIndent()

                    val file = fileFactory.createFileFromText("providers.dart", DartLanguage.INSTANCE, content)
                    libDirectory.add(file)
                }
            }

            CommandProcessor.getInstance().executeCommand(project, runnable, "Setup Project Structure", null)
        }
    }

    override fun update(e: AnActionEvent) {
        e.dataContext.let {
            this.dataContext = it
            val presentation = e.presentation
            presentation.isEnabled = true
        }
    }
}