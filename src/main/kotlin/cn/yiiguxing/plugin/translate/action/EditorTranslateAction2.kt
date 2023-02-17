package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.TranslateListener
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.TranslateService
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.gson.Gson
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileChooser.ex.LocalFsFinder.VfsFile
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.fileTypes.StdFileTypes
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import org.apache.commons.codec.binary.Hex
import org.apache.commons.lang.StringEscapeUtils
import org.json.XML
import java.io.File
import java.nio.charset.Charset
import java.util.regex.Pattern
import javax.xml.stream.XMLOutputFactory
import cn.yiiguxing.plugin.translate.ResourcesGsonModel
import cn.yiiguxing.plugin.translate.documentation.Documentations
import cn.yiiguxing.plugin.translate.documentation.getTranslatedDocumentation
import cn.yiiguxing.plugin.translate.trans.DocumentationTranslator
import cn.yiiguxing.plugin.translate.trans.microsoft.MicrosoftTranslator
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.StringWriter




/**
 * 翻译动作，自动从最大范围内取词，优先选择
 */
class EditorTranslateAction2 : TranslateAction(true) {

    init {
        isEnabledInModalContext = true
//        templatePresentation.text = adaptedMessage("action.EditorTranslateAction.text")
        templatePresentation.text = "Gawd"
        templatePresentation.description = "Gawd"
    }


    override fun onActionPerformed(event: AnActionEvent, editor: Editor, selectionRange: TextRange) {
        FileDocumentManager.getInstance().getFile(editor.document)?.also {
            val content = VfsUtil.loadText(it)
            val project = editor.project ?: CommonDataKeys.PROJECT.getData(event.dataContext)
            println("File ${it.name}")
//            println("File $content")

            val mapper = XmlMapper().apply {
                enable(SerializationFeature.INDENT_OUTPUT)
                configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)
                configure(ToXmlGenerator.Feature.UNWRAP_ROOT_OBJECT_NODE, true)
                configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
//                configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
            if(it.name.endsWith(".xml", true).not() || it.parent == null) return
            val content2 = """
                <resources>
                <string name="app_name">Tummoc\'s %s</string>
                <string name="str_your_level">Your Level </string>
                <string name="app_name2">Yellow<b>Tummoc2</b>m h</string>
                </resources>
            """.trimIndent()
//            val a = "Please wait\u2026"
//            println("Gawd ${String(a.toByteArray())}")


            val parentPath = it.parent.path
            val f = File(parentPath, "xx.xml")
            if(f.exists().not())
                f.createNewFile()

//            val aa = mapper.readValue<Resources>(XML.toJSONObject(content).toString())
            val builder = StringBuilder()
            // reading from jackson issues with more than 472 items
            val objectMapper: ObjectMapper = ObjectMapper()


            objectMapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false
            )
            val obj = objectMapper.readValue(XML.toJSONObject(content).toString(), ResourcesGsonModel::class.java)
            val correctedObjs = obj.resources?.string.orEmpty().filter {
                it.b == null && it.u == null &&  (it.translatable == null || it.translatable)
            }

//            kotlin.io.println(mapper.writeValueAsString(aa))

//            val ao = mapper.writeValueAsString(ResourcesGsonModel(cn.yiiguxing.plugin.translate.Resources(correctedObjs)))
//            println(ao)
//            f.writeText(XML.toString())

//            val w: java.io.StringWriter = java.io.StringWriter()

//            mapper.writer().writeValue(w, cn.yiiguxing.plugin.translate.Resources(correctedObjs))
//            f.writeText(w.toString())

//            val obj = gson.fromJson(XML.toJSONObject(content).toString(), ResourcesGsonModel::class.java)
//            val hs = kotlin.collections.hashMapOf<String, String>()
//
//            obj.resources.string.forEach {
//                val content = it.content.toString()
//                if(it.translatable == null || it.translatable.not()) return@forEach
//                val content = it.content.toString()
//
//                if (content.startsWith("[") && content.endsWith("]"))
//                    val items =
//                hs[it.name] = it.content
//            }

            val seperator = "%$"
            correctedObjs.forEach {
                builder.append(it.content!!)
                if(correctedObjs.last() != it)
                    builder.append(seperator)
            }

            println("Wow original ${obj.resources?.string.orEmpty().size}")
            println("Wow corrected ${correctedObjs.size}")

            println("Current translator ${TranslateService.translator.name}")

            val inpT = Lang.ENGLISH
            val outT = Lang.HINDI

//            val items = builder.toString().split(seperator)
//            builder.clear()
//            items.forEach {
//                builder.appendLine(it)
//            }
//            f.writeText(builder.toString())
//            return
//            TranslateService.translator.getTranslatedDocumentation()


            TranslateService.translate(builder.toString(), inpT, outT, seperator, object : TranslateListener {
                override fun onSuccess(translation: Translation) {
                    val trans = translation.translation ?: ""

                    val items = trans.split(seperator)
                    builder.clear()
                    items.forEach {
                        builder.appendLine(it)
                    }
//                    println(translation)
//                    ogNicnList.zip(trans.split("\n")){v,vv ->
//                        v.inner = vv
//                    }
//                    f.writeBytes(mapper.writeValueAsBytes(Resources(ogNicList)))
//                    f.writeText(builder.toString())
                    f.writeText(builder.toString())
                }

                override fun onError(throwable: Throwable) {
                    throwable.printStackTrace()
                }
            })
        }
//        TranslateService.dispose()
    }

    private fun convertHexToUnicode(str: String): String {
        return "&#%d;".format(Integer.parseInt(str, 16))
    }

    override val selectionMode
        get() = Settings.autoSelectionMode

}
