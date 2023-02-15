package com.acmesoftware.cleanframework.actions

import com.google.common.base.CaseFormat
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
        val packageNamePascal = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, packageName)

        ApplicationManager.getApplication().runWriteAction {
            val runnable = Runnable {
                val fileFactory = PsiFileFactory.getInstance(project)
                val libDirectory = PsiDirectoryFactory.getInstance(project).createDirectory(libDir)

                var providersDirectory = libDirectory.findSubdirectory("providers")
                if (providersDirectory == null) {
                    providersDirectory = libDirectory.createSubdirectory("providers")
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

                var routingDirectory = libDirectory.findSubdirectory("routing")
                if (routingDirectory == null) {
                    routingDirectory = libDirectory.createSubdirectory("routing")
                }

                val routerFileName = "${packageName}_router.dart"
                val routerFile = libDirectory.findFile(routerFileName)
                if (routerFile == null) {
                    val content = """
                        import 'package:clean_framework_router/clean_framework_router.dart';
                        import 'package:$packageName/routing/src/routes.dart';
                        import 'package:flutter/material.dart';

                        class ${packageNamePascal}Router extends AppRouter<Routes> {
                          @override
                          RouterConfiguration configureRouter() {
                            return RouterConfiguration(
                              routes: [
                                AppRoute(
                                  route: Routes.home,
                                  builder: (_, __) => const Placeholder(),
                                ),
                              ],
                            );
                          }
                        }
                    """.trimIndent()
                    val file = fileFactory.createFileFromText(routerFileName, DartLanguage.INSTANCE, content)
                    routingDirectory.add(file)
                }

                val routesFileName = "routes.dart"
                val routesFile = libDirectory.findFile(routesFileName)
                if (routesFile == null) {
                    val content = """
                        import 'package:clean_framework_router/clean_framework_router.dart';

                        enum Routes with RoutesMixin {
                          home('/');

                          const Routes(this.path);

                          @override
                          final String path;
                        }
                    """.trimIndent()
                    val file = fileFactory.createFileFromText(routesFileName, DartLanguage.INSTANCE, content)
                    routingDirectory.add(file)
                }

                val routingFile = libDirectory.findFile("routing.dart")
                if (routingFile == null) {
                    val content = """
                        export 'package:$packageName/routing/$routerFileName';
                        export 'package:$packageName/routing/routes.dart';
                    """.trimIndent()

                    val file = fileFactory.createFileFromText("routing.dart", DartLanguage.INSTANCE, content)
                    libDirectory.add(file)
                }

                var appDirectory = libDirectory.findSubdirectory("app")
                if (appDirectory == null) {
                    appDirectory = libDirectory.createSubdirectory("app")
                }

                val appFileName = "${packageName}_app.dart"
                val appFile = libDirectory.findFile(appFileName)
                if (appFile == null) {
                    val content = """
                        import 'package:clean_framework/clean_framework.dart';
                        import 'package:clean_framework_router/clean_framework_router.dart';
                        import 'package:flutter/material.dart';
                        import 'package:$packageName/routing.dart';

                        class ${packageNamePascal}App extends StatelessWidget {
                          const ${packageNamePascal}App({super.key});

                          @override
                          Widget build(BuildContext context) {
                            return AppProviderScope(
                              child: AppRouterScope(
                                create: ${packageNamePascal}Router.new,
                                builder: (context) {
                                  return MaterialApp.router(
                                    title: '$packageName',
                                    theme: ThemeData(
                                      colorSchemeSeed: Colors.blue,
                                      useMaterial3: true,
                                    ),
                                    routerConfig: context.router.config,
                                  );
                                },
                              ),
                            );
                          }
                        }

                    """.trimIndent()

                    val file = fileFactory.createFileFromText(appFileName, DartLanguage.INSTANCE, content)
                    appDirectory.add(file)
                }

                val featuresDirectory = libDirectory.findSubdirectory("features")
                if (featuresDirectory == null) {
                    libDirectory.createSubdirectory("features")
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