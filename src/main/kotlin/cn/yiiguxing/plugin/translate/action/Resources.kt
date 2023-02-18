package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.update.Version
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.annotation.*

@JacksonXmlRootElement(localName = "resources")
data class Resources(
//    @field:JacksonXmlElementWrapper(localName = "")
//    @field:JacksonXmlProperty(localName = "string")
    @field:JacksonXmlProperty
    @field:JacksonXmlElementWrapper(useWrapping = false)
    val string: List<Abcd2> = listOf()
)

@JsonIgnoreProperties
data class Abcd2(
    @field:JacksonXmlProperty(isAttribute = true)
    val name: String? = null,


//    @field:JacksonXmlElementWrapper(useWrapping = false)
//    @field:JacksonXmlProperty(isAttribute = true)
//    val translatable: String? = null,

    @field:JacksonXmlText
    @field:JacksonXmlElementWrapper(useWrapping = false, localName = "string")
    var inner: String? = null,

//    @field:JacksonXmlElementWrapper(localName = "b",)
//    val b: String? = null,

    ) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Abcd2) return false

        return other.name == name
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (inner?.hashCode() ?: 0)
//        result = 31 * result + (translatable?.hashCode() ?: 0)
        return result
    }
}

@JacksonXmlRootElement(localName = "resources")
data class Abcd(
//    @field:JacksonXmlElementWrapper(localName = "string", useWrapping = false)
//    @field:JacksonXmlProperty(isAttribute = true)
//    val name: String,

//    @field:JacksonXmlText
    val string: String,

//    @field:JacksonXmlElementWrapper(useWrapping = false)
//    @field:JacksonXmlProperty(isAttribute = true)
//    val translatable: String? = "true",
)
