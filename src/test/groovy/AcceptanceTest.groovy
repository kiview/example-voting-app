import geb.spock.GebSpec
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.openqa.selenium.remote.DesiredCapabilities
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.Network
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
        compose.start()

        String network = findNetworkIdOfService("vote")
        Network tcNet = new Network() {
            @Override
            String getId() {
                return network
            }

            @Override
            void close() throws Exception {

            }

            @Override
            Statement apply(Statement base, Description description) {
                return null
            }
        }
        chrome.withNetwork(tcNet)

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
        def containerName = compose.ambassadorContainer.linkedContainers.find { k, v ->
            k.contains(service)
        }.value.containerName

        def client = DockerClientFactory.instance().client()

        def containerInfo = client.inspectContainerCmd(containerName).exec()
        def networkName = containerInfo.networkSettings.networks.keySet().first()

        return client.inspectNetworkCmd().withNetworkId(networkName).exec().id
    }

}
