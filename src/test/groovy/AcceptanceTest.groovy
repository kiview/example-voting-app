import geb.spock.GebSpec
import org.openqa.selenium.remote.DesiredCapabilities
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.DockerComposeContainer
import spock.lang.Shared
import spock.lang.Stepwise

@Stepwise
class AcceptanceTest extends GebSpec {

    @Shared
    DockerComposeContainer compose = new DockerComposeContainer(new File("docker-compose-at.yml"))
            .withExposedService("vote_1", 80)
            .withExposedService("result_1", 80)
            .withLocalCompose(true)

    @Shared
    BrowserWebDriverContainer chrome = new BrowserWebDriverContainer()
            .withDesiredCapabilities(DesiredCapabilities.chrome())

    def setupSpec() {
        compose.starting(null)

        String network = findNetworkIdOfService("vote")
        chrome.withNetworkMode(network)

        chrome.start()
        println chrome.vncAddress
        browser.driver = chrome.webDriver
    }

    def cleanupSpec() {
        chrome.stop()
        compose.finished(null)
    }

    def "can vote between groovy and kotlin"() {
        when: "I visit the vote page"
        browser.go ("http://vote")
        sleep(1000)

        then: "Both results show text%"
        $("button", 0).text() == "GROOVY"
        $("button", 1).text() == "KOTLIN"
    }

    def "can see result"() {
        when:
        browser.go ("http://result")
        sleep(1000)

        then:
        $("div.choice.cats div")[1].text() == "50.0%"
        $("div.choice.dogs div")[1].text() == "50.0%"
    }

    def "voting changes the result"() {
        when:
        browser.go ("http://vote")

        and: "voting for groovy"
        $("#a").click()
        sleep(2000)

        and: "going to results"
        browser.go ("http://result")
        sleep(1000)

        then:
        $("div.choice.cats div")[1].text() == "100.0%"
    }

    private String findNetworkIdOfService(String service) {
        compose.ambassadorContainers.find {
            it.key.contains(service)
        }.value.containerInfo.networkSettings.networks.values().first().networkID
    }

}
