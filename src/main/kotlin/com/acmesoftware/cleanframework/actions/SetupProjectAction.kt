package com.acmesoftware.cleanframework.actions

import com.acmesoftware.cleanframework.utils.Template
import com.google.common.base.CaseFormat
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.PsiDirectory
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
        val packageNamePascal = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, packageName)

        val packageInfo = hashMapOf(
            "package_name" to packageName,
            "package_name_pascal" to packageNamePascal,
        )

        ApplicationManager.getApplication().runWriteAction {
            val runnable = Runnable {
                val fileFactory = PsiFileFactory.getInstance(project)
                val libDirectory = PsiDirectoryFactory.getInstance(project).createDirectory(libDir)
                val providersDirectory = findOrCreateDirectory(libDirectory, "providers")

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

                createFileFromTemplate(
                    libDirectory,
                    fileFactory,
                    "providers.dart",
                    "project/providers.dart",
                    packageInfo
                )

                val routingDirectory = findOrCreateDirectory(libDirectory, "routing")
                createFileFromTemplate(
                    routingDirectory,
                    fileFactory,
                    "${packageName}_router.dart",
                    "project/routing/router.dart",
                    packageInfo
                )
                createFileFromTemplate(
                    routingDirectory,
                    fileFactory,
                    "routes.dart",
                    "project/routing/routes.dart",
                    packageInfo
                )
                createFileFromTemplate(libDirectory, fileFactory, "routing.dart", "project/routing.dart", packageInfo)

                val appDirectory = findOrCreateDirectory(libDirectory, "app")
                createFileFromTemplate(
                    appDirectory,
                    fileFactory,
                    "${packageName}_app.dart",
                    "project/app/app.dart",
                    packageInfo
                )

                findOrCreateDirectory(libDirectory, "features")
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

    private fun findOrCreateDirectory(parent: PsiDirectory, directoryName: String): PsiDirectory {
        return parent.findSubdirectory(directoryName) ?: return parent.createSubdirectory(directoryName)
    }

    private fun createFileFromTemplate(
        parent: PsiDirectory,
        fileFactory: PsiFileFactory,
        fileName: String,
        templatePath: String,
        fillValues: HashMap<String, String>
    ) {
        val providersFile = parent.findFile(fileName)
        if (providersFile == null) {
            val content = Template(templatePath).fill(fillValues)
            val file = fileFactory.createFileFromText(fileName, DartLanguage.INSTANCE, content)
            parent.add(file)
        }
    }
}