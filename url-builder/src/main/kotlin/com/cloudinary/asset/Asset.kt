package com.cloudinary.asset


import com.cloudinary.NULL_AUTH_TOKEN
import com.cloudinary.config.CloudConfig
import com.cloudinary.config.UrlConfig
import com.cloudinary.generateAnalyticsSignature
import com.cloudinary.transformation.*
import com.cloudinary.util.*
import java.io.UnsupportedEncodingException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

private const val OLD_AKAMAI_SHARED_CDN = "cloudinary-a.akamaihd.net"
private const val AKAMAI_SHARED_CDN = "media.cloudinary.net"
private const val SHARED_CDN = AKAMAI_SHARED_CDN
internal const val DEFAULT_ASSET_TYPE = "image"
internal const val DEFAULT_DELIVERY_TYPE = "upload"

const val ASSET_TYPE_IMAGE = "image"
const val ASSET_TYPE_VIDEO = "video"

class Asset(
    // config
    cloudConfig: CloudConfig,
    urlConfig: UrlConfig,

    // fields
    version: String? = null,
    publicId: String? = null,
    extension: Any? = null,
    private val transformation: Transformation? = null
) : BaseAsset(
    cloudConfig,
    urlConfig,
    version,
    publicId,
    extension
) {

    override fun getTransformationString() = transformation?.toString()

    class Builder(
        cloudConfig: CloudConfig,
        urlConfig: UrlConfig
    ) :
        BaseAssetBuilder(cloudConfig, urlConfig), ITransformable<Builder> {

        private var transformation: Transformation? = null

        fun transformation(transformation: Transformation) =
            apply { this.transformation = transformation }

        fun transformation(transform: Transformation.Builder.() -> Unit) = apply {
            val builder = Transformation.Builder()
            builder.transform()
            this.transformation = builder.build()
        }

        override fun add(action: Action) = apply {
            this.transformation = (this.transformation ?: Transformation()).add(action)
        }

        fun build() = Asset(
            cloudConfig,
            urlConfig,
            version,
            publicId,
            extension,
            transformation
        )
    }
}

@TransformationDsl
abstract class BaseAsset constructor(
    // config
    private val cloudConfig: CloudConfig,
    private val urlConfig: UrlConfig,

    // fields
    private val version: String? = null,
    private val publicId: String? = null,
    private val extension: Any? = null
) {
    fun generate(source: String? = null): String? {
        require(cloudConfig.cloudName.isNotBlank()) { "Must supply cloud_name in configuration" }

        var mutableSource = source ?: publicId ?: return null

        val httpSource = mutableSource.cldIsHttpUrl()

        var signature = ""

        val finalizedSource =
            finalizeSource(mutableSource, extension)

        mutableSource = finalizedSource.source
        val sourceToSign = finalizedSource.sourceToSign

        var mutableVersion = version
        if (urlConfig.forceVersion && sourceToSign.contains("/") && !sourceToSign.cldHasVersionString() &&
            !httpSource && mutableVersion.isNullOrBlank()
        ) {
            mutableVersion = "1"
        }

        mutableVersion = if (mutableVersion == null) "" else "v$mutableVersion"

        val transformationString = getTransformationString()
        if (urlConfig.signUrl && (cloudConfig.authToken == null || cloudConfig.authToken == NULL_AUTH_TOKEN)) {
            val signatureAlgorithm =
                if (urlConfig.longUrlSignature) "SHA-256" else urlConfig.signatureAlgorithm


            val toSign = listOfNotNull(transformationString, sourceToSign)
                .joinToString("/")
                .cldRemoveStartingChars('/')
                .cldMergeSlashedInUrl()

            val hash = hash(toSign + cloudConfig.apiSecret, signatureAlgorithm)
            signature = Base64Coder.encodeURLSafeString(hash)
            signature =
                "s--" + signature.substring(0, if (urlConfig.longUrlSignature) 32 else 8) + "--"
        }


        val prefix = unsignedDownloadUrlPrefix(
            cloudConfig.cloudName,
            urlConfig.domain
        )

        val url =
            listOfNotNull(
                prefix,
                signature,
                transformationString,
                mutableVersion,
                mutableSource
            ).joinToString("/").cldMergeSlashedInUrl()

        if (urlConfig.signUrl && cloudConfig.authToken != null && cloudConfig.authToken != NULL_AUTH_TOKEN) {
            val token = cloudConfig.authToken.generate(URL(url).path)
            return "$url?$token"
        }
        try {
            var urlObject = URL(url)
            if (urlConfig.analytics && cloudConfig.authToken == null && urlObject.query == null) {
                val analytics = "_a=${generateAnalyticsSignature()}"
                return url.joinWithValues(analytics, separator = "?")
            }
        } catch (exception: MalformedURLException) {
            return url
        }

        return url
    }

    abstract fun getTransformationString(): String?

    @TransformationDsl
    abstract class BaseAssetBuilder
    internal constructor(
        protected val cloudConfig: CloudConfig,
        protected val urlConfig: UrlConfig
    ) {

        protected var version: String? = null
        protected var publicId: String? = null
        protected var extension: Any? = null

        fun version(version: String) = apply { this.version = version }
        fun publicId(publicId: String) = apply { this.publicId = publicId }
        fun extension(extension: Format) = apply { this.extension = extension }
        fun extension(extension: String) = apply { this.extension = extension }
    }
}

private fun finalizeSource(
    source: String,
    extension: Any?
): FinalizedSource {
    var mutableSource = source.cldMergeSlashedInUrl()
    var sourceToSign: String
    if (mutableSource.cldIsHttpUrl()) {
        mutableSource = mutableSource.cldSmartUrlEncode()
        sourceToSign = mutableSource
    } else {
        mutableSource = try {
            URLDecoder.decode(mutableSource.replace("+", "%2B"), "UTF-8").cldSmartUrlEncode()
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
        sourceToSign = mutableSource
        if (extension != null) {
            mutableSource = "$mutableSource.$extension"
            sourceToSign = "$sourceToSign.$extension"
        }
    }

    return FinalizedSource(mutableSource, sourceToSign)
}

private fun finalizeResourceType(
    resourceType: String?,
    urlSuffix: String?
): String? {
    var mutableResourceType: String? = resourceType ?: DEFAULT_ASSET_TYPE
    var mutableType: String? = DEFAULT_DELIVERY_TYPE

    if (!urlSuffix.isNullOrBlank()) {
        if (mutableResourceType == "image" && mutableType == "upload") {
            mutableResourceType = "images"
            mutableType = null
        } else if (mutableResourceType == "image" && mutableType == "private") {
            mutableResourceType = "private_images"
            mutableType = null
        } else if (mutableResourceType == "image" && mutableType == "authenticated") {
            mutableResourceType = "authenticated_images"
            mutableType = null
        } else if (mutableResourceType == "raw" && mutableType == "upload") {
            mutableResourceType = "files"
            mutableType = null
        } else if (mutableResourceType == "video" && mutableType == "upload") {
            mutableResourceType = "videos"
            mutableType = null
        } else {
            throw IllegalArgumentException("URL Suffix only supported for image/upload, image/private, raw/upload, image/authenticated  and video/upload")
        }
    }
    var result = mutableResourceType
    if (mutableType != null) {
        result += "/$mutableType"
    }

    return result
}

private fun unsignedDownloadUrlPrefix(
    cloudName: String?,
    domain: String?
): String {
    var mutableCloudName = cloudName
    var mutableSecureDistribution = domain
    mutableCloudName?.let {
        if (it.startsWith("/")) return "/res$mutableCloudName"
    }

    var sharedDomain: Boolean = true
    var prefix: String
    if (mutableSecureDistribution.isNullOrBlank() || mutableSecureDistribution == OLD_AKAMAI_SHARED_CDN) {
        mutableSecureDistribution = "$cloudName.$SHARED_CDN"
    }
    if (!sharedDomain) {
        sharedDomain = mutableSecureDistribution == SHARED_CDN
    }

    prefix = "https://$mutableSecureDistribution"
//    if (sharedDomain) {
//        // use original cloud name here:
//        prefix += "/$cloudName"
//    }

    return prefix
}

private class FinalizedSource(val source: String, val sourceToSign: String)

/**
 * Computes hash from input string using specified algorithm.
 *
 * @param input              string which to compute hash from
 * @param signatureAlgorithm algorithm to use for computing hash (supports only SHA-1 and SHA-256)
 * @return array of bytes of computed hash value
 */
private fun hash(input: String, signatureAlgorithm: String): ByteArray? {
    return try {
        MessageDigest.getInstance(signatureAlgorithm)
            .digest(input.toByteArray(Charset.forName("UTF-8")))
    } catch (e: NoSuchAlgorithmException) {
        throw RuntimeException("Unexpected exception", e)
    }
}