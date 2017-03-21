import geb.Browser
import org.junit.runner.Description
import org.openqa.selenium.remote.DesiredCapabilities
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.DockerComposeContainer
import spock.lang.Shared
import spock.lang.Specification

import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL

class AcceptanceTest extends Specification {

    @Shared
    DockerComposeContainer compose = new DockerComposeContainer(new File("docker-compose-at.yml"))
            .withExposedService("vote_1", 80)
            .withExposedService("result_1", 80)
            .withLocalCompose(true)

    @Shared
    BrowserWebDriverContainer chrome = new BrowserWebDriverContainer()
            .withDesiredCapabilities(DesiredCapabilities.chrome())
            .withRecordingMode(RECORD_ALL, new File("./build/"))

    @Shared
    Browser browser

    def setupSpec() {
        compose.starting(null)

        String network = findNetworkIdOfService("vote")
        chrome.withNetworkMode(network)

        chrome.start()
        println chrome.vncAddress
        browser = new Browser(driver: chrome.webDriver)
    }

    def cleanup() {
        chrome.succeeded(new Description(AcceptanceTest, "foo", []))
    }

    def cleanupSpec() {
        chrome.stop()
        compose.finished(null)
    }

    def "foo"() {

        given:
        browser.go ("http://vote")
        sleep(10000)

        expect:
        true
    }

    private String findNetworkIdOfService(String service) {
        compose.ambassadorContainers.find {
            it.key.contains(service)
        }.value.containerInfo.networkSettings.networks.values().first().networkID
    }




}
