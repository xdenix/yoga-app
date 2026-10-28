package com.example.yogaapp.update

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GithubAppUpdaterTest {

    @Test
    fun parseVersionCode_readsCodeFromReleaseAssetName() {
        assertEquals(2, GithubAppUpdater.parseVersionCode("yoga-app-2-1.0.1-debug.apk"))
        assertEquals(10, GithubAppUpdater.parseVersionCode("yoga-app-10-2.0.0.apk"))
    }

    @Test
    fun parseVersionCode_rejectsUnversionedNames() {
        assertNull(GithubAppUpdater.parseVersionCode("app-debug.apk"))
        assertNull(GithubAppUpdater.parseVersionCode("yoga-app.apk"))
    }
}
