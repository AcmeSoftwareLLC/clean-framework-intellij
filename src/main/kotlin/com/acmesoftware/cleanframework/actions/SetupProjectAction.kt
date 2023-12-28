package com.acmesoftware.cleanframework.actions

import com.acmesoftware.cleanframework.utils.Flutter
import com.acmesoftware.cleanframework.utils.Template
import com.google.common.base.CaseFormat
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.file.PsiDirectoryFactory
import com.jetbrains.lang.dart.DartLanguage
import com.jetbrains.lang.dart.util.PubspecYamlUtil
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.utils.addToStdlib.UnsafeCastFunction
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.yaml.snakeyaml.Yaml


class SetupProjectAction : AnAction() {
    private lateinit var dataContext: DataContext

    @OptIn(UnsafeCastFunction::class)
    override fun actionPerformed(e: AnActionEvent) {
        val application = ApplicationManager.getApplication()
        val project = CommonDataKeys.PROJECT.getData(dataContext)
        val view = LangDataKeys.IDE_VIEW.getData(dataContext)

        val focusedVirtualFile = if (view == null) {
            val editor = LangDataKeys.EDITOR.getData(dataContext)!!
            FileDocumentManager.getInstance().getFile(editor.document)!!.parent
        } else {
            view.orChooseDirectory!!.virtualFile
        }

        val pubspecFile = PubspecYamlUtil.findPubspecYamlFile(project!!, focusedVirtualFile)
        val libDir = pubspecFile!!.parent.findChild(PubspecYamlUtil.LIB_DIR_NAME)!!
        val packageName = PubspecYamlUtil.getDartProjectName(pubspecFile)!!
        val packageNamePascal = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, packageName)


        val packageInfo = hashMapOf(
            "package_name" to packageName,
            "package_name_pascal" to packageNamePascal,
        )

        application.runWriteAction {
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
                val appFile = createFileFromTemplate(
                    appDirectory,
                    fileFactory,
                    "${packageName}_app.dart",
                    "project/app/app.dart",
                    packageInfo
                )

                findOrCreateDirectory(libDirectory, "features")

                pubspecFile.toPsiFile(project)?.also {
                    val pubspecYamlBuilder = StringBuilder(it.text)

                    val yamlMap = Yaml().load<HashMap<String, Any>>(pubspecFile.inputStream)
                    val dependencies = yamlMap[PubspecYamlUtil.DEPENDENCIES].cast<HashMap<String, Any>>().keys
                    val devDependencies = yamlMap[PubspecYamlUtil.DEV_DEPENDENCIES].cast<HashMap<String, Any>>().keys

                    listOf("clean_framework_router", "clean_framework").forEach { dep ->
                        if (!dependencies.contains(dep)) insertDependency(pubspecYamlBuilder, dep)
                    }
                    val clearFrameworkTestDep = "clean_framework_test"
                    if (!devDependencies.contains(clearFrameworkTestDep)) {
                        insertDependency(pubspecYamlBuilder, clearFrameworkTestDep, true)
                    }

                    val dir = it.parent
                    val file = fileFactory.createFileFromText(
                        pubspecFile.name,
                        PlainTextLanguage.INSTANCE,
                        pubspecYamlBuilder.toString()
                    )

                    it.delete()
                    dir?.add(file)

                    Flutter.getPackages(project, pubspecFile)
                }

                FileEditorManager.getInstance(project).openTextEditor(
                    OpenFileDescriptor(project, appFile.virtualFile), true
                )

                application.invokeLater {
                    Notifications.Bus.notify(
                        Notification(
                            "com.acmesoftware.notification",
                            "PROJECT STRUCTURE SETUP",
                            "Clean Framework project structure has been setup successfully!",
                            NotificationType.INFORMATION,
                        )
                    )
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

    private fun findOrCreateDirectory(parent: PsiDirectory, directoryName: String): PsiDirectory {
        return parent.findSubdirectory(directoryName) ?: return parent.createSubdirectory(directoryName)
    }

    private fun createFileFromTemplate(
        parent: PsiDirectory,
        fileFactory: PsiFileFactory,
        fileName: String,
        templatePath: String,
        fillValues: HashMap<String, String>
    ): PsiFile {
        val providersFile = parent.findFile(fileName)
        if (providersFile == null) {
            val content = Template(templatePath).fill(fillValues)
            val file = fileFactory.createFileFromText(fileName, DartLanguage.INSTANCE, content)
            parent.add(file)
            return file
        }

        return providersFile
    }

    private fun insertDependency(builder: StringBuilder, dependency: String, isDev: Boolean = false) {
        val insertionIndex = builder.indexOf("\n", builder.indexOf("${if (isDev) "dev_" else ""}dependencies:")) + 3
        builder.insert(insertionIndex, "$dependency:\n  ")
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}