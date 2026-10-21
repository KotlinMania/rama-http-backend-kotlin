package io.github.kotlinmania.ramahttpbackend

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import io.github.kotlinmania.ramahttpbackend.client.SvcTest
import io.github.kotlinmania.ramahttpbackend.client.proxy.layer.ProxyAuthHeaderTest
import io.github.kotlinmania.ramahttpbackend.client.proxy.layer.proxyconnector.HttpProxyErrorTest

class RamaHttpBackendInstrumentationRunner : Instrumentation() {
    private data class TestCase(
        val className: String,
        val methodName: String,
        val execute: () -> Unit,
    )

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        val integrationTests = IntegrationTest()
        val svcTests = SvcTest()
        val proxyAuthHeaderTests = ProxyAuthHeaderTest()
        val httpProxyErrorTests = HttpProxyErrorTest()

        val integrationClass = IntegrationTest::class.qualifiedName ?: "IntegrationTest"
        val svcClass = SvcTest::class.qualifiedName ?: "SvcTest"
        val proxyAuthClass = ProxyAuthHeaderTest::class.qualifiedName ?: "ProxyAuthHeaderTest"
        val proxyErrorClass = HttpProxyErrorTest::class.qualifiedName ?: "HttpProxyErrorTest"

        val cases =
            listOf(
                TestCase(integrationClass, "testHttp11Pipelining", integrationTests::testHttp11Pipelining),
                TestCase(integrationClass, "testHttp2Multiplex", integrationTests::testHttp2Multiplex),
                TestCase(integrationClass, "testProxyAddressParsingAndLayer", integrationTests::testProxyAddressParsingAndLayer),
                TestCase(svcClass, "shouldSanitizeHttp1ExceptConnect", svcTests::shouldSanitizeHttp1ExceptConnect),
                TestCase(svcClass, "shouldNotSanitizeHttp1Connect", svcTests::shouldNotSanitizeHttp1Connect),
                TestCase(
                    svcClass,
                    "shouldNotSanitizeInsecureHttp1RequestOverHttpProxy",
                    svcTests::shouldNotSanitizeInsecureHttp1RequestOverHttpProxy,
                ),
                TestCase(
                    svcClass,
                    "shouldSanitizeSecureHttp1RequestOverHttpProxy",
                    svcTests::shouldSanitizeSecureHttp1RequestOverHttpProxy,
                ),
                TestCase(
                    svcClass,
                    "shouldSanitizeInsecureHttp1RequestOverSocksProxy",
                    svcTests::shouldSanitizeInsecureHttp1RequestOverSocksProxy,
                ),
                TestCase(
                    proxyAuthClass,
                    "absentCredentialsPreserveExistingHeaderAndForwardRequest",
                    proxyAuthHeaderTests::absentCredentialsPreserveExistingHeaderAndForwardRequest,
                ),
                TestCase(
                    proxyAuthClass,
                    "basicCredentialsAreAddedOnlyForAnInsecureProxy",
                    proxyAuthHeaderTests::basicCredentialsAreAddedOnlyForAnInsecureProxy,
                ),
                TestCase(
                    proxyAuthClass,
                    "bearerCredentialsAreAddedForBothProxyProtocols",
                    proxyAuthHeaderTests::bearerCredentialsAreAddedForBothProxyProtocols,
                ),
                TestCase(
                    proxyErrorClass,
                    "authRequiredFormatsLikeUpstreamDisplay",
                    httpProxyErrorTests::authRequiredFormatsLikeUpstreamDisplay,
                ),
                TestCase(
                    proxyErrorClass,
                    "unavailableFormatsLikeUpstreamDisplay",
                    httpProxyErrorTests::unavailableFormatsLikeUpstreamDisplay,
                ),
                TestCase(
                    proxyErrorClass,
                    "transportFormatsAndExposesItsErrorSource",
                    httpProxyErrorTests::transportFormatsAndExposesItsErrorSource,
                ),
                TestCase(
                    proxyErrorClass,
                    "transportUsesNestedCauseWhenAvailable",
                    httpProxyErrorTests::transportUsesNestedCauseWhenAvailable,
                ),
                TestCase(
                    proxyErrorClass,
                    "fromTransportWrapsCauseAsTransportError",
                    httpProxyErrorTests::fromTransportWrapsCauseAsTransportError,
                ),
                TestCase(
                    proxyErrorClass,
                    "otherIncludesTheFirstHeaderLine",
                    httpProxyErrorTests::otherIncludesTheFirstHeaderLine,
                ),
                TestCase(
                    proxyErrorClass,
                    "fromThrowableWalksTheCauseChain",
                    httpProxyErrorTests::fromThrowableWalksTheCauseChain,
                ),
            )

        var failed = 0
        cases.forEachIndexed { index, testCase ->
            val status =
                Bundle().apply {
                    putString("id", "InstrumentationTestRunner")
                    putString("class", testCase.className)
                    putString("test", testCase.methodName)
                    putInt("numtests", cases.size)
                    putInt("current", index + 1)
                }
            sendStatus(1, status)
            try {
                testCase.execute()
                sendStatus(0, status)
            } catch (error: Throwable) {
                failed++
                status.putString("stack", error.stackTraceToString())
                sendStatus(-2, status)
            }
        }
        val result =
            Bundle().apply {
                putInt("numtests", cases.size)
                putString("stream", "RamaHttpBackend Android device tests: ${cases.size - failed} passed, $failed failed\n")
            }
        finish(if (failed == 0) Activity.RESULT_OK else Activity.RESULT_CANCELED, result)
    }
}
