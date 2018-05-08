package de.db.vz.integrationtestplugin.parse

import spock.lang.Shared
import spock.lang.Specification

class JunitXmlParserSpec extends Specification {

    @Shared
    def fileFail
    @Shared
    def fileError
    @Shared
    def fileSuccess
    @Shared
    def folderErrorAndSuccess
    @Shared
    def folderFailureAndSuccess
    @Shared
    def folderSuccess

    def setupSpec() {
        fileFail = File.createTempFile('TEST-fail-', '.xml').absoluteFile
        fileFail << '<testsuite errors="0" failures="1"></testsuite>'
        fileError = File.createTempFile('TEST-error-', '.xml')
        fileError << '<testsuite errors="1" failures="0"></testsuite>'
        fileSuccess = File.createTempFile('TEST-success-', '.xml')
        fileSuccess << '<testsuite errors="0" failures="0"></testsuite>'

        folderErrorAndSuccess = File.createTempDir()
        copy(fileError, folderErrorAndSuccess)
        copy(fileSuccess, folderErrorAndSuccess)

        folderFailureAndSuccess = File.createTempDir()
        copy(fileFail, folderFailureAndSuccess)
        copy(fileSuccess, folderFailureAndSuccess)

        folderSuccess = File.createTempDir()
        copy(fileSuccess, folderSuccess)
        new File(folderSuccess, 'notxml') << 'this is not xml and it should not be parsed'
    }

    def copy(File file, File into) {
        def target = new File(into, file.name)
        target.createNewFile()
        target.text = file.text
    }

    def 'detect failure and error and success in junit report file'() {
        given:
        def parser = new JunitXmlParser()

        expect:
        parser.isSuccessful(xml) == success

        where:
        xml         | success
        fileFail    | false
        fileError   | false
        fileSuccess | true
    }

    def 'detect failure and error and success in junit report directory'() {
        given:
        def parser = new JunitXmlParser()

        expect:
        parser.isSuccessful(folder) == success

        where:
        folder                  | success
        folderFailureAndSuccess | false
        folderErrorAndSuccess   | false
        folderSuccess           | true
    }
}
