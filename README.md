[![Build Status](https://travis-ci.org/dbsystel/microservice-integrationtest-gradle-plugin.svg?branch=develop)](https://travis-ci.org/dbsystel/microservice-integrationtest-gradle-plugin)

Microservice Integrationtest Gradle Plugin
==========================================

Use this plugin to test the integration of your docker micro services. 

## Components

### Gradle Plugin
The core artifact found under `buildSrc/gradle-plugin`. A gradle plugin encapsulating service integration test setup and tear down.

### Test Framework
Performs integration tests on the gradle-plugin and provides some helpful groovy classes like the `ServiceUriResolver`. An artifact is built which can be used in your integration tests.

### Test Runner
Sample docker images which work in sync with the gradle plugin as test runner containers.

## Usage
See test-framework for a sample integration of the plugin.

The plugin adds the following tasks:
* integrationTest
  * performs *Up, runs the tests, performs *Down
* integrationTestUp
  * starts up all configured containers (incl network, compose project), waits for health checks
* integrationTestDown
  * tears down started containers, removes network & compose project
* integrationTestPull
  * explicit pull of images configured: docker-compose pull
* integrationTestLog
  * shows live logs: docker-compose logs -f
* integrationTestRefresh
  * refreshes changed images while having a running setup (after integrationTestUp)
* integraitonTestBuild
  * force rebuilding images defined in docker compose
* integrationTestHelp
  * shows task usage

Run integrationTest task locally and in CI.
Run integrationTestUp to locally test and debug against a persistent setup from your IDE. When done execute integrationTestDown for a clean tear down.

### Debugging
To debug your service integration tests, you can start up an environment via integrationTestUp. Then use the `ServiceUriResolver` class or the whole test-framework library to easily resolve dynamic port bindings. You can then debug your tests from your IDE. Take a look at the test-framework module for a sample.

To debug your services while running integration tests, you have to start them up with debugging enable and simply bind the remote port in your docker-compose.yml. Then you can remote debug from your IDE.

## Configuration

### Docker
Your services are defined as a regular docker compose file in `src/integrationTest/docker/docker-compose.yml`.

#### Health checks 
Health checks can be configured via environment variables for each service individually providing the port and path (ports will not have to be published as the health check is performed in the compose network), like this
```
environment:
  SERVICE_CHECK_HTTP: ':80/'
```
Health check for a fire and forget (configuration) container with an exit code that can be interpreted: `SERVICE_CHECK_EXIT: 'true'`
If none of the environment variables is found, the health check will not be performed on that particular service.

#### Timeout
The timeout after which to fail the startup, can be configured via gradle command line property `-PstartupTimeoutInSeconds=30`
Or in a build.gradle via:
```
msintplugin {
  startupTimeoutInSeconds = 30
}
```
whereas the property overrides the build.gradle configuration.
Defaults to 180.

### Java / Groovy JUnit Tests
Your tests are defined in `src/integrationTest/test/java/` or `src/integrationTest/test/groovy/`. 
 
## Open points

* add proper up-to-date checks for each task
* move dynamic host config for services (ServiceUriResolver) to the plugin, possibly in lib?
* fail integrationTestPull task if the docker-compose command fails
