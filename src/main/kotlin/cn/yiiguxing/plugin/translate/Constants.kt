/**
 * Constants
 */
@file:Suppress("SpellCheckingInspection")

package cn.yiiguxing.plugin.translate

import com.intellij.openapi.util.SystemInfo
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private val USER_HOME_PATH = System.getProperty("user.home")
private val DEFAULT_TRANSLATION_DIRECTORY = Paths.get(USER_HOME_PATH, ".translation")

val TRANSLATION_DIRECTORY: Path =
    if (SystemInfo.isLinux && !Files.exists(DEFAULT_TRANSLATION_DIRECTORY)) {
        System.getenv("XDG_DATA_HOME")
            ?.takeIf { it.isNotEmpty() }
            ?.let { Paths.get(it, ".translation") }
            ?: DEFAULT_TRANSLATION_DIRECTORY
    } else {
        DEFAULT_TRANSLATION_DIRECTORY
    }


const val STORAGE_NAME = "yiiguxing.translation.xml"

const val GITHUB_URL = "https://github.com/YiiGuxing/TranslationPlugin"
const val NEW_ISSUES_URL = "https://github.com/YiiGuxing/TranslationPlugin/issues/new/choose"
const val OPEN_COLLECTIVE_URL = "https://opencollective.com/translation-plugin"
const val SUPPORT_SHARE_URL = "https://plugins.jetbrains.com/plugin/8579-translation"

const val HTML_DESCRIPTION_SETTINGS = "#SETTINGS"
const val HTML_DESCRIPTION_TRANSLATOR_CONFIGURATION = "#TRANSLATOR_CONFIGURATION"
const val HTML_DESCRIPTION_SUPPORT = "#SUPPORT"

const val YOUDAO_TRANSLATE_URL = "https://openapi.youdao.com/api"
const val YOUDAO_AI_URL = "https://ai.youdao.com"

const val BAIDU_TRANSLATE_URL = "https://fanyi-api.baidu.com/api/trans/vip/translate"
const val BAIDU_FANYI_URL = "https://fanyi-api.baidu.com/manage/developer"
const val BAIDU_FANYI_PRODUCT_URL = "https://fanyi-api.baidu.com/choose"

@Suppress("HttpUrlsUsage")
const val ALI_TRANSLATE_URL = "http://mt.aliyuncs.com/api/translate/web/general"
const val ALI_CAPI_URL = "https://usercenter.console.aliyun.com/#/manage/ak"
const val ALI_TRANSLATE_PRODUCT_URL = "https://www.aliyun.com/product/ai/base_alimt"