package de.db.vz.integrationtestplugin

import de.db.vz.integrationtestplugin.tasks.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.tasks.GroovySourceSet
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar

class IntegrationTestPlugin implements Plugin<Project> {

    static File composeProjectFile
    static File composeFile

    private Project project
    static IntegrationTestExtension integrationTestExtension

    @Override
    void apply(Project project) {
        this.project = project

        composeProjectFile = composeProjectFile(project)
        composeFile = composeFile(project)

        createExtension(project)
        createConfigurations(project)
        createSourceSet(project)
        createIntegrationTestTasks(project)
    }

    private static File composeProjectFile(Project project) {
        // TODO how about moving the .compose-project into a newly defined sourceSet under integrationTest/runtime or similar
        new File("${project.projectDir}/src/integrationTest/resources", ".compose-project")
    }

    private static File composeFile(Project project) {
        new File("${project.projectDir}/src/integrationTest/docker", 'docker-compose.yml')
    }

    private static void createExtension(Project project) {
        integrationTestExtension = project.extensions.create('msintplugin', IntegrationTestExtension)

        def timeout = startupTimeoutViaProperty(project)
        if (timeout)
            integrationTestExtension.startUpTimeoutInSeconds = timeout
    }

    private static Integer startupTimeoutViaProperty(Project project) {
        project?.hasProperty('startupTimeout') ? project.startupTimeout as Integer : null
    }

    private static void createConfigurations(Project project) {
        def configurations = project.configurations
        configurations.create('integrationTestCompile')
        configurations.create('integrationTestRuntime')
        configurations['integrationTestRuntime'].extendsFrom configurations['integrationTestCompile']
    }

    private static void createSourceSet(Project project) {
        def sourceSet = project.sourceSets.create('integrationTest') as SourceSet

        sourceSet.compileClasspath = project.configurations['integrationTestCompile']
        sourceSet.runtimeClasspath = sourceSet.output + sourceSet.compileClasspath

        sourceSet.resources.srcDirs = ['src/integrationTest/resources']
        // TODO find a generic solution for all JVM languages
        sourceSet.java.srcDirs = ['src/integrationTest/java']
        def groovySourceSet = new DslObject(sourceSet).convention.findPlugin GroovySourceSet
        groovySourceSet?.groovy?.srcDirs = ['src/integrationTest/groovy']
    }

    private static def createJarTask(Project project) {
        def jarTask = project.tasks.create('integrationTestJar', Jar.class)
        jarTask.baseName = 'tests'
        jarTask.classifier = null
        jarTask.version = null
        jarTask.from project.sourceSets['integrationTest'].output
        jarTask.manifest.attributes.put('Main-Class', 'org.junit.runner.JUnitCore')
        jarTask
    }

    private static void createIntegrationTestTasks(Project project) {
        def integrationTestJarTask = createJarTask(project)

        def integrationTestUpTask = project.tasks.create('integrationTestUp', IntegrationTestUpTask.class)

        def processResources = project.tasks.processResources
        def integrationTestTask = project.tasks.create('integrationTest', IntegrationTestTask.class)
        integrationTestTask.dependsOn processResources
        integrationTestTask.dependsOn integrationTestJarTask, integrationTestUpTask
        integrationTestTask.jar = integrationTestJarTask as Jar

        def integrationTestDownTask = project.tasks.create('integrationTestDown', IntegrationTestDownTask.class)
        integrationTestTask.finalizedBy integrationTestDownTask

        def integrationTestRefreshTask = project.tasks.create('integrationTestRefresh', IntegrationTestRefreshTask.class)
        def integrationTestHelpTask = project.tasks.create('integrationTestHelp', IntegrationTestHelpTask.class)
        def integrationTestLogTask = project.tasks.create('integrationTestLog', IntegrationTestLogTask.class)
        def integrationTestPullTask = project.tasks.create('integrationTestPull', IntegrationTestPullTask.class)
        def integrationTestBuildTask = project.tasks.create('integrationTestBuild', IntegrationTestBuildTask.class)

        integrationTestRefreshTask.dependsOn integrationTestBuildTask

        // run order
        integrationTestLogTask.mustRunAfter integrationTestUpTask
    }
}
