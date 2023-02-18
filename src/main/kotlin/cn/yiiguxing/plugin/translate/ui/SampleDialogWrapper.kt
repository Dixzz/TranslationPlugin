package cn.yiiguxing.plugin.translate.ui

import cn.yiiguxing.plugin.translate.diagnostic.ErrorReportNotifications
import cn.yiiguxing.plugin.translate.trans.Lang
import cn.yiiguxing.plugin.translate.trans.LanguagePair
import cn.yiiguxing.plugin.translate.trans.Translator
import cn.yiiguxing.plugin.translate.util.TranslationStates
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.popup.util.PopupUtil
import com.intellij.openapi.wm.IdeGlassPane
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl
import com.intellij.ui.PopupBorder
import com.intellij.ui.WindowMoveListener
import com.intellij.ui.WindowResizeListener
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.event.*
import java.util.*
import javax.swing.*
import javax.swing.border.Border


class SampleDialogWrapper(
    project: Project?,
    private val translator: Translator,
    private val fileFromToLang: (Lang?, Lang?) -> Unit
) : DialogWrapper(project) {

    private var topPanel: JPanel = JPanel()

    private val sourceLangComboBox: LangComboBoxLink = LangComboBoxLink()
    private val targetLangComboBox: LangComboBoxLink = LangComboBoxLink()

    init {
        title = "Translation Config"
        setUndecorated(false)
        isModal = false
        cancelAction.isEnabled = true
        window.minimumSize = JBDimension(0, 0)
        rootPane.windowDecorationStyle = JRootPane.NONE
        rootPane.border = PopupBorder.Factory.create(true, true)
        addWindowListeners()
        val panel = createCenterPanel()
        peer.setContentPane(panel)
//        addMouseListeners()
//        registerESCListener()
        initLangComboBoxes()
        init()

    }

    private fun initLangComboBoxes() {
        fun addListener(comboBox: LangComboBoxLink) {
            comboBox.addItemListener { n, _, fromUser ->
                if (fromUser.not()) return@addItemListener
                comboBox.selected = n
            }
        }

        updateLanguages()
        addListener(sourceLangComboBox)
        addListener(targetLangComboBox)
    }

    private fun updateLanguages(languagePair: LanguagePair? = null) {
        sourceLangComboBox.apply {
            val srcSelected = (languagePair?.source ?: selected)
                ?.takeIf { translator.supportedSourceLanguages.contains(it) }
                ?: Lang.ENGLISH
            model = LanguageListModel.sorted(translator.supportedSourceLanguages, srcSelected)
        }
        targetLangComboBox.apply {
            val targetSelected = (languagePair?.target ?: selected)
                ?.takeIf { translator.supportedTargetLanguages.contains(it) }
                ?: Lang.ENGLISH
            model = LanguageListModel.sorted(translator.supportedTargetLanguages, targetSelected)
        }
    }

    override fun createCenterPanel(): JComponent {
        topPanel = JPanel(BorderLayout())


        val left = JPanel(HorizontalLayout(10)).apply {
            border = UI.emptyBorder(0, leftAndRight = 5)

            add(sourceLangComboBox, HorizontalLayout.LEFT)
        }
        val right = JPanel(HorizontalLayout(3)).apply {
            border = UI.emptyBorder(0, leftAndRight = 5)

            add(targetLangComboBox, HorizontalLayout.LEFT)
            add(Separator(), HorizontalLayout.RIGHT)
        }

        val halfTopPanel = "halfTopPanel"

        topPanel.apply {
            layout = UI.migLayout()
            border = UI.emptyBorder(topAndBottom = 6, leftAndRight = 0) + UI.lineBelow()
            add(left, UI.fillX().sizeGroup(halfTopPanel))
            add(right, UI.fillX().sizeGroup(halfTopPanel))
        }

        sourceLangComboBox.background = topPanel.background
        targetLangComboBox.background = topPanel.background

        return topPanel
    }

//    override fun createCancelAction(): ActionListener {
//        return ActionListener { doCancelAction() }
//    }

    override fun doOKAction() {
        fileFromToLang(sourceLangComboBox.selected, targetLangComboBox.selected)
        println("Wew ${sourceLangComboBox.selected}")
        super.doOKAction()
    }

    private fun registerESCListener() {
        val win = window

        fun isInside(event: MouseEvent): Boolean {
            val target = RelativePoint(event)
            if (UIUtil.isDescendingFrom(target.originalComponent, win)) {
                return true
            }
            return target.screenPoint.let { point ->
                SwingUtilities.convertPointFromScreen(point, win)
                win.contains(point)
            }
        }

        val awtEventListener = AWTEventListener { event ->
            val needCloseDialog = when (event) {
//                is MouseEvent -> event.id == MouseEvent.MOUSE_PRESSED &&
//                        !isInside(event)

                is KeyEvent -> event.keyCode == KeyEvent.VK_ESCAPE &&
                        !PopupUtil.handleEscKeyEvent() &&
                        !win.isFocused // close the displayed popup window first
                else -> false
            }
            if (needCloseDialog) {
                doCancelAction()
            }
        }

        val eventMask = AWTEvent.MOUSE_EVENT_MASK or AWTEvent.KEY_EVENT_MASK
        Toolkit.getDefaultToolkit().addAWTEventListener(awtEventListener, eventMask)
//        Disposer.register(this) {
//            Toolkit.getDefaultToolkit().removeAWTEventListener(awtEventListener)
//        }
    }

    private fun addMouseListeners() {
        topPanel.apply {
            WindowMoveListener(this).let {
                addMouseListener(it)
                addMouseMotionListener(it)
            }
        }
        val glassPane = rootPane.glassPane as IdeGlassPane

        val resizeListener = object : WindowResizeListener(rootPane, JBUI.insets(6), null) {
            var myCursor: Cursor? = null

            override fun setCursor(content: Component, cursor: Cursor) {
                if (myCursor !== cursor || myCursor !== Cursor.getDefaultCursor()) {
                    glassPane.setCursor(cursor, this)
                    myCursor = cursor
                    if (content is JComponent) {
                        IdeGlassPaneImpl.savePreProcessedCursor(content, content.getCursor())
                    }
                    super.setCursor(content, cursor)
                }
            }

            override fun mouseReleased(event: MouseEvent?) {
                super.mouseReleased(event)
            }
        }
        glassPane.addMouseMotionPreprocessor(resizeListener, this.disposable)
        glassPane.addMousePreprocessor(resizeListener, this.disposable)
    }

    private fun addWindowListeners() {
        val window = peer.window
        val rootPane = rootPane
        window.addWindowListener(object : WindowAdapter() {
            override fun windowOpened(e: WindowEvent) {
                window.addWindowFocusListener(object : WindowAdapter() {
                    override fun windowGainedFocus(e: WindowEvent) {
                        rootPane.border = PopupBorder.Factory.create(true, true)
                    }

                    override fun windowLostFocus(e: WindowEvent) {
                        rootPane.border = PopupBorder.Factory.create(false, true)
                    }
                })
                window.removeWindowListener(this)
            }
        })
    }

    private operator fun Border.plus(external: Border): Border = JBUI.Borders.merge(this, external, true)
    class Separator : JComponent() {
        private val myColor = JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()
        private val myHGap = 2
        private val myVGap = 4

        override fun paintComponent(g: Graphics) {
            g.color = myColor
            g.drawLine(myHGap, myVGap, myHGap, height - myVGap - 1)
        }

        override fun getPreferredSize(): Dimension {
            return Dimension(2 * myHGap + 1, myVGap * 2 + 1)
        }

        override fun getMinimumSize(): Dimension {
            return preferredSize
        }

        override fun getMaximumSize(): Dimension {
            return preferredSize
        }
    }
}