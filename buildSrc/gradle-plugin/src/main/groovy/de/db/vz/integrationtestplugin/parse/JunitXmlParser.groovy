package de.db.vz.integrationtestplugin.parse

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors


class JunitXmlParser {

    XmlSlurper slurper = new XmlSlurper()

    boolean isSuccessful(File file) {
        if (file.isDirectory()) {
            List<Path> files = Files.list(file.toPath()).collect(Collectors.toList())
            return files.collect { it.toFile() }
                    .findAll { it.isFile() && isTestResultXml(it.name) }
                    .collect { parse it }
                    .find { hasFailuresOrErrors(it) } == null
        }

        def res = parse(file)
        return !hasFailuresOrErrors(res)
    }

    private static boolean isTestResultXml(String fileName) {
        fileName ==~ /^TEST-.*\.[xX][mM][lL]$/
    }

    private static boolean hasFailuresOrErrors(def testsuite) {
        testsuite.@errors != 0 || testsuite.@failures != 0
    }

    private def parse(File file) {
        slurper.parse(file)
    }
}
