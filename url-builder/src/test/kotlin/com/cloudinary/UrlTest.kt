package com.cloudinary

import com.cloudinary.asset.Asset
import com.cloudinary.config.CloudinaryConfig
import com.cloudinary.config.UrlConfig
import org.junit.Assert.*
import org.junit.Test
import java.util.regex.Pattern

private const val DEFAULT_ROOT_PATH = "https://test123.media.cloudinary.net/"
private const val DEFAULT_UPLOAD_PATH = DEFAULT_ROOT_PATH

class UrlTest {
    private val cloudinary = Cloudinary("cloudinary://a:b@test123?analytics=false")
    private val cloudinaryPrivateCdn = Cloudinary(
        cloudinary.config.copy(
            urlConfig = cloudinary.config.urlConfig.copy()
        )
    )

    private val cloudinaryPrivateCdnUseRootPath = Cloudinary(
        cloudinary.config.copy(
            urlConfig = cloudinary.config.urlConfig.copy()
        )
    )

    private val cloudinaryPrivateCdnSignUrl = Cloudinary(
        cloudinary.config.copy(
            urlConfig = cloudinary.config.urlConfig.copy(signUrl = true)
        )
    )

    private val cloudinarySignedUrl =
        Cloudinary(cloudinary.config.copy(urlConfig = cloudinary.config.urlConfig.copy(signUrl = true)))
    private val cloudinaryLongSignedUrl =
        Cloudinary(cloudinary.config.copy(urlConfig = cloudinary.config.urlConfig.copy(signUrl = true, signatureAlgorithm = "SHA-256")))

    @Test
    fun testConfigValues() {

        val urlConfig = UrlConfig(
            domain = "secure.api.com",
            secureCdnSubdomain = true,
            useRootPath = true,
            analytics = false
        )

        val cloudConfig = cloudinary.config.cloudConfig

        assertEquals("https://secure.api.com/sample", Asset(cloudConfig, urlConfig).generate("sample"))
    }

    @Test
    fun testUrlWithAnalytics() {
        val cloudinaryWithAnalytics =
            Cloudinary(cloudinary.config.copy(urlConfig = cloudinary.config.urlConfig.copy(analytics = true)))

        val result = cloudinaryWithAnalytics.image().generate("test")

        // note: This test validates the concatenation of analytics query param to the url.
        // This is not meant to test the string generation - This is tested separately in its own test.
        val expectedAnalytics = generateAnalyticsSignature()

        assertEquals("${DEFAULT_ROOT_PATH}test?_a=$expectedAnalytics", result)

    }

    @Test
    fun testUrlSuffixWithDotOrSlash() {
        val errors = arrayOfNulls<Boolean>(4)
        try {
            cloudinary.image {
                urlSuffix("dsfdfd.adsfad")
            }.generate("publicId")
        } catch (e: IllegalArgumentException) {
            errors[0] = true
        }
        try {
            cloudinary.image {
                urlSuffix("dsfdfd/adsfad")
            }.generate("publicId")
        } catch (e: IllegalArgumentException) {
            errors[1] = true
        }
        try {
            cloudinary.image {
                urlSuffix("dsfd.fd/adsfad")
            }.generate("publicId")
        } catch (e: IllegalArgumentException) {
            errors[2] = true
        }
        try {
            cloudinary.image {
                urlSuffix("dsfdfdaddsfad")
            }.generate("publicId")
        } catch (e: IllegalArgumentException) {
            errors[3] = true
        }
        assertTrue(errors[0]!!)
        assertTrue(errors[1]!!)
        assertTrue(errors[2]!!)
        assertNull(errors[3])
    }

    @Test
    fun testCloudName() { // should use cloud_name from config
        val result = cloudinary.image().generate("test")
        assertEquals(DEFAULT_UPLOAD_PATH + "test", result)
    }

    @Test
    fun testCloudNameOptions() { // should allow overriding cloud_name in options
        val cloudinaryDifferentCloud =
            Cloudinary(cloudinary.config.copy(cloudConfig = cloudinary.config.cloudConfig.copy(cloudName = "test321")))
        val result = cloudinaryDifferentCloud.image {
        }.generate("test")
        assertEquals("https://test321.media.cloudinary.net/test", result)
    }

    @Test
    fun testSecureAkamai() { // should default to akamai if secure is given with private_cdn and no
// secure_distribution
        val urlConfig = cloudinary.config.urlConfig.copy()
        val config = cloudinary.config.copy(urlConfig = urlConfig)
        val result = Cloudinary(config).image().generate("test")
        assertEquals("${DEFAULT_ROOT_PATH}test", result)
    }

    @Test
    fun testSecureDistribution() { // should use default secure distribution if secure=TRUE
        val cloudinarySecureFalse =
            Cloudinary(cloudinary.config.copy(urlConfig = cloudinary.config.urlConfig.copy()))

        val result = cloudinarySecureFalse.image().generate("test")
        assertEquals("${DEFAULT_ROOT_PATH}test", result)

        // should take secure distribution from config if secure=TRUE
        val newConfig =
            cloudinary.config.copy(urlConfig = cloudinary.config.urlConfig.copy(domain = "config.secure.distribution.com"))

        val result2 = Cloudinary(newConfig).image().generate("test")
        assertEquals("https://config.secure.distribution.com/test", result2)
    }

    @Test
    fun testSecureDistributionOverwrite() { // should allow overwriting secure distribution if secure=TRUE
        val cloudinarySecureDistribution =
            Cloudinary(cloudinary.config.copy(urlConfig = cloudinary.config.urlConfig.copy(domain = "something.else.com")))

        val result =
            cloudinarySecureDistribution.image().generate("test")
        assertEquals("https://something.else.com/test", result)
    }

    @Test
    fun testSecureNonAkamai() { // should not add cloud_name if private_cdn and secure non akamai
// secure_distribution
        val urlConfig = cloudinary.config.urlConfig.copy(
            domain = "something.cloudfront.net"
        )
        val config = cloudinary.config.copy(urlConfig = urlConfig)
        val result = Cloudinary(config).image().generate("test")
        assertEquals("https://something.cloudfront.net/test", result)
    }

    @Test
    fun testExtension() { // should use format from options
        val result = cloudinary.image {
            extension("jpg")
        }.generate("test")
        assertEquals(DEFAULT_UPLOAD_PATH + "test.jpg", result)
    }

    @Test
    fun testType() { // should use type from options
        val result = cloudinary.image {
        }.generate("test")
        assertEquals("${DEFAULT_ROOT_PATH}test", result)
    }

    @Test
    fun testResourceType() { // should use resource_type from options
        val result = cloudinary.raw().generate("test")
        assertEquals("${DEFAULT_ROOT_PATH}test", result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDisallowUrlSuffixWithSlash() {
        cloudinaryPrivateCdn.image {
            urlSuffix("hello/world")
        }.generate("test")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDisallowUrlSuffixWithDot() {
        cloudinaryPrivateCdn.image {
            urlSuffix("hello.world")
        }.generate("test")
    }

    @Test
    fun testPutFormatAfterUrlSuffix() {
        val actual =
            cloudinaryPrivateCdn.image {
                urlSuffix("hello")
                extension("jpg")
            }.generate("test")
        assertEquals("${DEFAULT_ROOT_PATH}test/hello.jpg", actual)
    }

    @Test
    fun testNotSignTheUrlSuffix() {
        val pattern = Pattern.compile("s--[0-9A-Za-z_-]{8}--")
        var url = cloudinarySignedUrl.image {
            extension("jpg")
        }.generate("test")!!
        var matcher = pattern.matcher(url)
        matcher.find()
        var expectedSignature = url.substring(matcher.start(), matcher.end())

        var actual =
            cloudinaryPrivateCdnSignUrl.image {
                extension("jpg")
                urlSuffix("hello")
            }.generate("test")

        assertEquals(
            "$DEFAULT_ROOT_PATH$expectedSignature/test/hello.jpg",
            actual
        )

        url = cloudinarySignedUrl.image {
            extension("jpg")
        }.generate("test")!!
        matcher = pattern.matcher(url)
        matcher.find()
        expectedSignature = url.substring(matcher.start(), matcher.end())
        actual = cloudinaryPrivateCdnSignUrl.image {
            extension("jpg")
            urlSuffix("hello")
        }.generate("test")
        assertEquals(
            "${DEFAULT_ROOT_PATH}$expectedSignature/test/hello.jpg",
            actual
        )
    }

    @Test
    fun testSupportUrlSuffixForRawUploads() {
        val actual =
            cloudinaryPrivateCdn.raw {
                urlSuffix("hello")
            }.generate("test")
        assertEquals("${DEFAULT_ROOT_PATH}test/hello", actual)
    }

    @Test
    fun testSupportUrlSuffixForVideoUploads() {
        val actual =
            cloudinaryPrivateCdn.video {
                urlSuffix("hello")
            }.generate("test")
        assertEquals("${DEFAULT_ROOT_PATH}test/hello", actual)
    }

    @Test
    fun testSupportUrlSuffixForAuthenticatedImages() {
        val actual =
            cloudinaryPrivateCdn.image {
                urlSuffix("hello")
            }
                .generate("test")
        assertEquals("${DEFAULT_ROOT_PATH}test/hello", actual)
    }

    @Test
    fun testSupportUrlSuffixForPrivateImages() {
        val actual =
            cloudinaryPrivateCdn.image {
                urlSuffix("hello")
            }
                .generate("test")
        assertEquals("${DEFAULT_ROOT_PATH}test/hello", actual)
    }

    @Test
    fun testHttpEscape() { // should escape http urls
        val result =
            cloudinary.image {
            }.generate("http://www.youtube.com/watch?v=d9NF2edxy-M")
        assertEquals(
            "${DEFAULT_ROOT_PATH}http://www.youtube.com/watch%3Fv%3Dd9NF2edxy-M",
            result
        )
    }

    @Test
    fun testFolders() { // should add version if public_id contains /
        var result = cloudinary.image().generate("folder/test")
        assertEquals(DEFAULT_UPLOAD_PATH + "v1/folder/test", result)
        result = cloudinary.image {
            version("123")
        }.generate("folder/test")
        assertEquals(
            DEFAULT_UPLOAD_PATH + "v123/folder/test",
            result
        )
    }

    @Test
    fun testFoldersWithExcludeVersion() { // should not add version if the user turned off forceVersion
        val cloudinaryForceVersionFalse =
            Cloudinary(cloudinary.config.copy(urlConfig = cloudinary.config.urlConfig.copy(forceVersion = false)))
        var result = cloudinaryForceVersionFalse.image {
        }.generate("folder/test")
        assertEquals(DEFAULT_UPLOAD_PATH + "folder/test", result)
        // should still show explicit version if passed by the user
        result = cloudinaryForceVersionFalse.image {
            version("1234")
        }.generate("folder/test")
        assertEquals(
            DEFAULT_UPLOAD_PATH + "v1234/folder/test",
            result
        )
        // should add version if no value specified for forceVersion:
        result = cloudinary.image().generate("folder/test")
        assertEquals(DEFAULT_UPLOAD_PATH + "v1/folder/test", result)
        // should not use v1 if explicit version is passed
        result = cloudinaryForceVersionFalse.image {
            version("1234")
        }.generate("folder/test")
        assertEquals(
            DEFAULT_UPLOAD_PATH + "v1234/folder/test",
            result
        )
    }

    @Test
    fun testFoldersWithVersion() { // should not add version if public_id contains version already
        val result = cloudinary.image().generate("v1234/test")
        assertEquals(DEFAULT_UPLOAD_PATH + "v1234/test", result)
    }

    @Test
    fun testEscapePublicId() { // should escape public_ids
        val tests = mapOf("a b" to "a%20b", "a+b" to "a%2Bb", "a%20b" to "a%20b", "a-b" to "a-b", "a??b" to "a%3F%3Fb")
        for ((key, value) in tests) {
            val result = cloudinary.image().generate(key)
            assertEquals(
                DEFAULT_UPLOAD_PATH + "" + value,
                result
            )
        }
    }

    @Test
    fun testSignedUrl() { // should correctly sign a url
        var expected: String =
            DEFAULT_UPLOAD_PATH + "s--Ai4Znfl3--/c_crop,h_20,w_10/v1234/image.jpg"

        var actual =
            cloudinarySignedUrl.image {
                version("1234")
                add("c_crop,h_20,w_10")
            }.generate("image.jpg")

        assertEquals(expected, actual)

        expected = DEFAULT_UPLOAD_PATH + "s----SjmNDA--/v1234/image.jpg"

        actual = cloudinarySignedUrl.image {
            version("1234")
        }.generate("image.jpg")

        assertEquals(expected, actual)

        expected = DEFAULT_UPLOAD_PATH + "s--Ai4Znfl3--/c_crop,h_20,w_10/image.jpg"

        actual = cloudinarySignedUrl.image {
            add("c_crop,h_20,w_10")
        }.generate("image.jpg")

        assertEquals(expected, actual)

        expected = "${DEFAULT_ROOT_PATH}s--2hbrSMPO--/sample.jpg"

        actual =
            cloudinaryLongSignedUrl.image {
            }.generate("sample.jpg")
        assertEquals(expected, actual)
    }

    @Test
    fun testCloudinaryUrlValidScheme() {
        val cloudinaryUrl = "cloudinary://123456789012345:ALKJdjklLJAjhkKJ45hBK92baj3@test"
        CloudinaryConfig.fromUri(cloudinaryUrl)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCloudinaryUrlInvalidScheme() {
        val cloudinaryUrl = "https://123456789012345:ALKJdjklLJAjhkKJ45hBK92baj3@test"
        CloudinaryConfig.fromUri(cloudinaryUrl)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCloudinaryUrlEmptyScheme() {
        val cloudinaryUrl = " "
        CloudinaryConfig.fromUri(cloudinaryUrl)
    }
}