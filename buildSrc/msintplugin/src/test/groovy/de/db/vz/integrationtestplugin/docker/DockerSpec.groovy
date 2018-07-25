package de.db.vz.integrationtestplugin.docker

import spock.lang.Specification

class DockerSpec extends Specification {


    def "run, inspect and stop"() {
        given:
        def image = 'appropriate/curl'
        def docker = new Docker()

        when:
        def id = docker.run(image)

        then:
        id == docker.inspect('{{ .Id }}')

        and:
        docker.stop()

        and:
        docker.rm()
    }
}
