package com.payas.quelle

import com.payas.quelle.sync.SyncAction
import com.payas.quelle.sync.decideSync
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncTest {
    @Test
    fun firstSignInUploadsExistingData() = assertEquals(SyncAction.Upload, decideSync(500, null))

    @Test
    fun emptyAppWithEmptyCloudDoesNothing() = assertEquals(SyncAction.None, decideSync(0, null))

    @Test
    fun newerCloudDataIsDownloaded() = assertEquals(SyncAction.Download, decideSync(500, 900))

    @Test
    fun newerLocalDataIsUploaded() = assertEquals(SyncAction.Upload, decideSync(900, 500))

    @Test
    fun sameTimeMeansInSync() = assertEquals(SyncAction.None, decideSync(900, 900))
}
