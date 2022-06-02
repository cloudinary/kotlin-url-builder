package com.cloudinary

import com.cloudinary.config.*
import org.junit.Test
import kotlin.test.assertEquals

class ConfigTest {
    @Test
    fun testConfigFromUrl() {
        val cloudinaryUrl =
            "cloudinary://123456123456123:3Sf3FAdasa2easdFGDS3afADFS2@cloudname?cname=custom.domain.com&chunk_size=5000"

        val config = CloudinaryConfig.fromUri(cloudinaryUrl)

        assertEquals("cloudname", config.cloudName)
        assertEquals("123456123456123", config.apiKey)
        assertEquals("3Sf3FAdasa2easdFGDS3afADFS2", config.apiSecret)
//        assertEquals(5000, config.chunkSize)

    }

    @Test
    fun testCloudConfig() {
        val cloudName = "my_cloud"
        val apiKey = "abcdefghijklmnop"
        val apiSecret = "1234567890"

        val config = CloudConfig(
            cloudName = cloudName,
            apiKey = apiKey,
            apiSecret = apiSecret
        )

        assertEquals(cloudName, config.cloudName)
        assertEquals(apiKey, config.apiKey)
        assertEquals(apiSecret, config.apiSecret)

        val copy = config.copy(cloudName = "different_cloud")

        assertEquals("different_cloud", copy.cloudName)
        assertEquals(apiKey, copy.apiKey)
        assertEquals(apiSecret, copy.apiSecret)
    }

    @Test
    fun testUrlConfigDefaults() {
        val config = UrlConfig()

        assertEquals(null, config.domain)
    }

    @Test
    fun testUrlConfig() {
        val domain = "secure.api.com"

        val config = UrlConfig(
            domain = domain
        )

        assertEquals(domain, config.domain)

        val copy = config.copy(
            domain = "copy.secure.distribution"
        )

        assertEquals("copy.secure.distribution", copy.domain)
    }
}