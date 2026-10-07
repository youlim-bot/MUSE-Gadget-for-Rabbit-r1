package dev.cameronpak.muser1

import dev.cameronpak.muser1.transport.MuseConnection
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

/** Exercises the real HTTP startup flow with entirely fictional tokens and intercepted responses. */
class ConnectionStartupTest {
    private val vm="""{"vm_list":[{"vm_auth_token":"fixture-vm","vm_id":"fixture","vm_ws_url":"https://hatch.metaaivm.com"}]}"""
    private val renewed="""{"access_token":"fixture-new-access","refresh_token":"fixture-new-refresh"}"""
    private class StopBeforeSocket:RuntimeException()
    private data class Trace(val paths:List<String>,val saved:List<DeviceCredentials>,val diagnostics:List<String>,val rejected:Boolean)
    private fun startup(vararg replies:Pair<Int,String>):Trace {
        val paths=mutableListOf<String>();val saved=mutableListOf<DeviceCredentials>();val diagnostics=mutableListOf<String>()
        val fixtureClient=OkHttpClient.Builder().addInterceptor { chain ->
            val request=chain.request();val index=paths.size;paths.add(request.url.encodedPath)
            check(index<replies.size){"Unexpected extra HTTP request"}
            if(request.url.encodedPath=="/device_token/refresh") {
                assertEquals("Bearer hatch_refresh:fixture-refresh",request.header("Authorization"))
            }
            if(index>0 && request.url.encodedPath=="/fetch_vms")assertEquals("Bearer fixture-new-access",request.header("Authorization"))
            val (code,body)=replies[index]
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("fixture")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        val connection=MuseConnection(DeviceCredentials("fixture-device","fixture-access","fixture-refresh"),
            "fixture-sdk",{saved.add(it)},{},{_,_,_->},onDiagnostic={
                diagnostics.add(it)
                if(it=="websocket_start")throw StopBeforeSocket()
            },refreshDeviceId="fixture-node")
        // Keep production endpoint validation intact; the interceptor prevents all network I/O.
        MuseConnection::class.java.getDeclaredField("client").apply{isAccessible=true}.set(connection,fixtureClient)
        runBlocking { try { connection.connect();fail("Fixture must stop before opening a socket") } catch(_:IllegalStateException) {} }
        return Trace(paths,saved,diagnostics,connection.pairingRejected)
    }
    @Test fun usablePairingDoesNotRefreshEvenWhenSdkTokenExists() {
        val t=startup(200 to vm)
        assertEquals(listOf("/fetch_vms"),t.paths);assertTrue(t.saved.isEmpty())
        assertTrue("websocket_start" in t.diagnostics);assertFalse(t.rejected)
    }
    @Test fun expiredAccessRefreshesOnceAndUsesTheSavedReplacement() {
        val t=startup(401 to "{}",200 to renewed,200 to vm)
        assertEquals(listOf("/fetch_vms","/device_token/refresh","/fetch_vms"),t.paths)
        assertEquals("fixture-new-access",t.saved.single().accessToken)
        assertEquals("fixture-new-refresh",t.saved.single().refreshToken)
        assertTrue("websocket_start" in t.diagnostics)
    }
    @Test fun rejectedRefreshKeepsStoredCredentialsAndStops() {
        val t=startup(401 to "{}",401 to "{}")
        assertEquals(listOf("/fetch_vms","/device_token/refresh"),t.paths)
        assertTrue(t.saved.isEmpty());assertTrue(t.rejected);assertFalse("websocket_start" in t.diagnostics)
    }
    @Test fun aSecondUnauthorizedAccountResponseDoesNotRotateAgain() {
        val t=startup(401 to "{}",200 to renewed,401 to "{}")
        assertEquals(1,t.paths.count{it=="/device_token/refresh"});assertFalse("websocket_start" in t.diagnostics)
    }
    @Test fun serverFailureOrAnEmptyInstanceListDoesNotRefreshWorkingCredentials() {
        for(reply in listOf(500 to "{}",200 to "{\"vm_list\":[]}")) {
            val t=startup(reply);assertEquals(listOf("/fetch_vms"),t.paths)
            assertTrue(t.saved.isEmpty());assertFalse(t.rejected)
        }
    }
}
