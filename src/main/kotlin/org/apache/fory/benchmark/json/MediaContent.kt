package org.apache.fory.benchmark.json

import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class MediaContent(
    var media: Media? = null,
    var images: List<Image>? = null,
)

@Serializable
@JsonClass(generateAdapter = true)
data class Media(
    var uri: String? = null,
    var title: String? = null,
    var width: Int = 0,
    var height: Int = 0,
    var format: String? = null,
    var duration: Long = 0,
    var size: Long = 0,
    var bitrate: Int = 0,
    var hasBitrate: Boolean = false,
    var persons: List<String>? = null,
    var player: Player? = null,
    var copyright: String? = null,
) {
    enum class Player {
        JAVA,
        FLASH,
    }
}

@Serializable
@JsonClass(generateAdapter = true)
data class Image(
    var uri: String? = null,
    var title: String? = null,
    var width: Int = 0,
    var height: Int = 0,
    var size: Size? = null,
    var media: Media? = null,
) {
    enum class Size {
        SMALL,
        LARGE,
    }
}
