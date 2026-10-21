import Testing
import RamaHttpBackend

@Suite("RamaHttpBackend Export Tests")
struct RamaHttpBackendExportTests {
    @Test("Version string matches expected release")
    func testVersionString() throws {
        #expect(RamaHttpBackend.VERSION == "0.2.0")
    }

    @Test("Client service factory initializes http1 client")
    func testHttpClientServiceCreation() throws {
        let clientService = client.HttpClientService.Companion.shared.http1()
        #expect(clientService.toString().contains("HttpClientService"))
    }

    @Test("Connector layer default factory creates layer")
    func testHttpConnectorLayerDefault() throws {
        let layer = client.HttpConnectorLayer.Companion.shared.default()
        #expect(layer != nil)
    }
}
