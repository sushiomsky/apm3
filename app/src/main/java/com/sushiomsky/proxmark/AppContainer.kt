package com.sushiomsky.proxmark

import android.content.Context
import com.sushiomsky.proxmark.persistence.SettingsStore
import com.sushiomsky.proxmark.persistence.TagStore
import com.sushiomsky.proxmark.protocol.ProxmarkClient
import com.sushiomsky.proxmark.reader.AcrUsbReader
import com.sushiomsky.proxmark.reader.BuiltInNfcReader
import com.sushiomsky.proxmark.reader.Proxmark3Reader
import com.sushiomsky.proxmark.repository.ProxmarkRepository
import com.sushiomsky.proxmark.usb.ProxmarkUsbManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val usbManager: ProxmarkUsbManager = ProxmarkUsbManager(appContext)
    private val proxmarkClient: ProxmarkClient = ProxmarkClient(usbManager, appScope)
    val tagStore: TagStore = TagStore(appContext)
    val settingsStore: SettingsStore = SettingsStore(appContext)

    val proxmark3Reader: Proxmark3Reader = Proxmark3Reader(proxmarkClient)
    val builtInNfcReader: BuiltInNfcReader = BuiltInNfcReader()
    val acrUsbReader: AcrUsbReader = AcrUsbReader(appContext)

    val repository: ProxmarkRepository = ProxmarkRepository(
        proxmarkClient, appScope, tagStore, settingsStore,
        proxmark3Reader, builtInNfcReader, acrUsbReader
    )

    fun close() {
        repository.close()
        appScope.cancel()
    }
}
