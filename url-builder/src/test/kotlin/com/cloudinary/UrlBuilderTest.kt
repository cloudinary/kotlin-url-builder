package com.cloudinary

import com.cloudinary.asset.Asset
import com.cloudinary.config.UrlConfig
import org.junit.Assert
import org.junit.Test

private const val DEFAULT_ROOT_PATH = "https://test123.media.cloudinary.net/"
private const val DEFAULT_UPLOAD_PATH = DEFAULT_ROOT_PATH

class UrlBuilderTest {
    private val cloudinary = Cloudinary("cloudinary://a:b@test123?analytics=false")
    private val cloudinaryPrivateCdn = Cloudinary(
        cloudinary.config.copy(
            urlConfig = cloudinary.config.urlConfig.copy()
        )
    )

    @Test
    fun testConfigValues() {

        val urlConfig = UrlConfig(
            domain = "secure.api.com",
            secureCdnSubdomain = true,
            useRootPath = true,
            analytics = false
        )

        val cloudConfig = cloudinary.config.cloudConfig

        Assert.assertEquals(
            "https://secure.api.com/sample",
            Asset(cloudConfig, urlConfig).generate("sample")
        )
    }

    @Test
    fun testCloudNameOptions() { // should allow overriding cloud_name in options
        val cloudinaryDifferentCloud =
            Cloudinary(cloudinary.config.copy(cloudConfig = cloudinary.config.cloudConfig.copy(cloudName = "test321")))
        val result = cloudinaryDifferentCloud.image {
        }.generate("test")
        Assert.assertEquals("https://test321.media.cloudinary.net/test", result)
    }

    @Test
    fun testSecureDistribution() { // should use default secure distribution if secure=TRUE
        val cloudinarySecureFalse =
            Cloudinary(cloudinary.config.copy(urlConfig = cloudinary.config.urlConfig.copy()))

        val result = cloudinarySecureFalse.image().generate("test")
        Assert.assertEquals("${DEFAULT_ROOT_PATH}test", result)

        // should take secure distribution from config if secure=TRUE
        val newConfig =
            cloudinary.config.copy(urlConfig = cloudinary.config.urlConfig.copy(domain = "config.secure.distribution.com"))

        val result2 = Cloudinary(newConfig).image().generate("test")
        Assert.assertEquals("https://config.secure.distribution.com/test", result2)
    }

    @Test
    fun testSecureDistributionOverwrite() { // should allow overwriting secure distribution if secure=TRUE
        val cloudinarySecureDistribution =
            Cloudinary(cloudinary.config.copy(urlConfig = cloudinary.config.urlConfig.copy(domain = "something.else.com")))

        val result =
            cloudinarySecureDistribution.image().generate("test")
        Assert.assertEquals("https://something.else.com/test", result)
    }

    @Test
    fun testHttpEscape() { // should escape http urls
        val result =
            cloudinary.image {
            }.generate("http://www.youtube.com/watch?v=d9NF2edxy-M")
        Assert.assertEquals(
            "${DEFAULT_ROOT_PATH}http://www.youtube.com/watch%3Fv%3Dd9NF2edxy-M",
            result
        )
    }
}