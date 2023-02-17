package cn.yiiguxing.plugin.translate

import com.google.gson.annotations.SerializedName

data class ResourcesGsonModel(
    val resources: Resources? = null
)

data class Resources(
//    val plurals: Plurals,
    val string: List<StringObject> = listOf(),
//    @SerializedName("string-array")
//    val stringArray: StringArray
)

//data class Plurals(
//    val item: List<Item>,
//    val name: String,
//    val translatable: String
//)

data class StringObject(
    val b: Any? = null,
    val content: Any? = null,
//    val font: Font,
    val name: String = "",
    val translatable: Boolean? = null,
    val u: Any? = null
)

//data class StringArray(
//    val item: List<String>,
//    val name: String,
//    val translatable: String
//)

//data class Item(
//    val content: String,
//    val quantity: String,
//    val translatable: String
//)
//
//data class Font(
//    val color: String,
//    val content: String
//)