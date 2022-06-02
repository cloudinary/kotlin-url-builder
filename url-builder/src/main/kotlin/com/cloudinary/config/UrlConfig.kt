package com.cloudinary.config

internal const val DEFAULT_FORCE_VERSION = true
internal const val DEFAULT_LONG_URL_SIGNATURE = false
internal const val DEFAULT_SIGN_URL = false
internal const val DEFAULT_SIGNATURE_ALGORITHM = "SHA-1"
internal const val DEFAULT_ANALYTICS = true

const val SIGN_URL = "sign_url"
const val LONG_URL_SIGNATURE = "long_url_signature"
const val FORCE_VERSION = "force_version"
const val DOMAIN = "domain"
const val SIGNATURE_ALGORITHM = "signature_algorithm"
const val ANALYTICS = "analytics"

interface IUrlConfig {
    val domain: String?
    val signUrl: Boolean
    val longUrlSignature: Boolean
    val forceVersion: Boolean
    val signatureAlgorithm: String?
    val analytics: Boolean
}

data class UrlConfig(
    override val domain: String? = null,
    override val signUrl: Boolean = DEFAULT_SIGN_URL,
    override val longUrlSignature: Boolean = DEFAULT_LONG_URL_SIGNATURE,
    override val forceVersion: Boolean = DEFAULT_FORCE_VERSION,
    override val signatureAlgorithm: String = DEFAULT_SIGNATURE_ALGORITHM,
    override val analytics: Boolean = DEFAULT_ANALYTICS
) : IUrlConfig {
    constructor(params: Map<String, Any>) : this(
        domain = params[DOMAIN]?.toString(),
        signUrl = params.getBoolean(SIGN_URL) ?: DEFAULT_SIGN_URL,
        longUrlSignature = params.getBoolean(LONG_URL_SIGNATURE) ?: DEFAULT_LONG_URL_SIGNATURE,
        forceVersion = params.getBoolean(FORCE_VERSION) ?: DEFAULT_FORCE_VERSION,
        signatureAlgorithm = params[SIGNATURE_ALGORITHM]?.toString() ?: DEFAULT_SIGNATURE_ALGORITHM,
        analytics = params.getBoolean(ANALYTICS) ?: DEFAULT_ANALYTICS
    )
}