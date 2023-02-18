package cn.yiiguxing.plugin.translate.action

import cn.yiiguxing.plugin.translate.ResourcesGsonModel
import cn.yiiguxing.plugin.translate.diagnostic.ErrorReportNotifications
import cn.yiiguxing.plugin.translate.trans.TranslateListener
import cn.yiiguxing.plugin.translate.trans.Translation
import cn.yiiguxing.plugin.translate.ui.SampleDialogWrapper
import cn.yiiguxing.plugin.translate.util.Notifications
import cn.yiiguxing.plugin.translate.util.Settings
import cn.yiiguxing.plugin.translate.util.TranslateService
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VfsUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import org.json.XML
import java.io.File
import java.util.concurrent.Executors


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
            if (it.name.endsWith(".xml", true).not() || it.parent == null) {
                ErrorReportNotifications.showNotification(project, "Format Exception", "No XML or parent folder found")
                return
            }
            SampleDialogWrapper(project, TranslateService.translator) { s, d ->
                if (s == null || d == null || s == d) {
                    ErrorReportNotifications.showNotification(
                        project,
                        "Language Parsing Exception",
                        "Expected different language found from=$s, to $d"
                    )
                    return@SampleDialogWrapper
                }

                val name = "$${it.name}_${TranslateService.translator.id}_${System.currentTimeMillis()}.xml"
                val parentPath = it.parent.path
                val f = File(parentPath, name)
                if (f.exists().not())
                    f.createNewFile()

                val builder = StringBuilder()
                val objectMapper = ObjectMapper()
                objectMapper.configure(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    false
                )
                val obj = objectMapper.readValue(XML.toJSONObject(content).toString(), ResourcesGsonModel::class.java)
                val correctedObjs = obj.resources?.string.orEmpty().filter {
                    it.b == null && it.u == null && (it.translatable == null || it.translatable)
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

                val separator = "%$"
                correctedObjs.forEach {
                    builder.append(it.content!!)
                    if (correctedObjs.last() != it)
                        builder.append(separator)
                }

                println("Wow original ${obj.resources?.string.orEmpty().size}")
                println("Wow corrected ${correctedObjs.size}")

//                val items = builder.toString().split(seperator)
//            builder.clear()
//            items.forEach {
//                builder.appendLine(it)
//            }
//            f.writeText(builder.toString())
//            return
//            TranslateService.translator.getTranslatedDocumentation()


                TranslateService.translate(builder.toString(), s, d, separator, object : TranslateListener {
                    override fun onSuccess(translation: Translation) {
                        try {

                            val trans = translation.translation ?: ""
                            val items = trans.split(separator)
                            val xmlList = mutableListOf<Abcd2>()
                            correctedObjs.map {
                                it.name
                            }.zip(items).forEach { (name, text) ->
                                xmlList.add(Abcd2(name, text))
                            }
//                            println(translation)
//                            ogNicnList.zip(trans.split("\n")) { v, vv ->
//                                v.inner = vv
//                            }
                            f.writeBytes(mapper.writeValueAsBytes(Resources(xmlList)))

                            Notifications.showFullContentNotification(
                                "Completed",
                                "${it.name} translated from ${s.name} to ${d.name}, saved in ${f.name}"
                            )
                        } catch (throwable: Exception) {
                            Notifications.showErrorNotification(
                                "Translating Exception",
                                "${throwable?.localizedMessage}",
                            )
                        }

//                    f.writeText(builder.toString())
                    }

                    override fun onError(throwable: Throwable) {
                        throwable.printStackTrace()
                        Notifications.showErrorNotification(
                            "Translating Exception",
                            "${throwable?.localizedMessage}",
                        )
                    }
                })

                builder.clear()
            }.apply {
                show()
            }
        }
//        TranslateService.dispose()
    }

    private fun convertHexToUnicode(str: String): String {
        return "&#%d;".format(Integer.parseInt(str, 16))
    }

    override val selectionMode
        get() = Settings.autoSelectionMode

}
