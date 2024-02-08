/* Copyright (c) 2023, LegoSoft Soluciones, S.C.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are not permitted.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *  ViewDocument.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.views.bpm.components

import com.acme.acmeui.service.expediente.DocumentService
import com.acme.acmeui.views.bpm.DocumentReception
import com.github.mvysny.karibudsl.v10.button
import com.vaadin.componentfactory.pdfviewer.PdfViewer
import com.vaadin.flow.component.Html
import com.vaadin.flow.component.dependency.CssImport
import com.vaadin.flow.component.dependency.JsModule
import com.vaadin.flow.component.dependency.NpmPackage
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.server.InputStreamFactory
import com.vaadin.flow.server.StreamResource
import com.wontlost.ckeditor.*
import java.io.ByteArrayInputStream
import java.io.InputStream


/**
 * Component to display different types of Documents in the different BPM views
 *
 * An extra functionality exists: is the document postfix is PREVIO and the document types is an HTML then using
 * the CKEditor we can save a new version or the final version.
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
@JsModule("./vaadin-ckeditor.js")
@JsModule("./translations/af.js")
@JsModule("./translations/ar.js")
@JsModule("./translations/ast.js")
@JsModule("./translations/az.js")
@JsModule("./translations/bg.js")
@JsModule("./translations/ca.js")
@JsModule("./translations/cs.js")
@JsModule("./translations/da.js")
@JsModule("./translations/de.js")
@JsModule("./translations/de-ch.js")
@JsModule("./translations/el.js")
@JsModule("./translations/en-au.js")
@JsModule("./translations/en-gb.js")
@JsModule("./translations/eo.js")
@JsModule("./translations/es.js")
@JsModule("./translations/et.js")
@JsModule("./translations/eu.js")
@JsModule("./translations/fa.js")
@JsModule("./translations/fi.js")
@JsModule("./translations/fr.js")
@JsModule("./translations/gl.js")
@JsModule("./translations/gu.js")
@JsModule("./translations/hi.js")
@JsModule("./translations/he.js")
@JsModule("./translations/hr.js")
@JsModule("./translations/hu.js")
@JsModule("./translations/id.js")
@JsModule("./translations/it.js")
@JsModule("./translations/ja.js")
@JsModule("./translations/km.js")
@JsModule("./translations/kn.js")
@JsModule("./translations/ko.js")
@JsModule("./translations/ku.js")
@JsModule("./translations/lt.js")
@JsModule("./translations/lv.js")
@JsModule("./translations/ms.js")
@JsModule("./translations/nb.js")
@JsModule("./translations/ne.js")
@JsModule("./translations/nl.js")
@JsModule("./translations/no.js")
@JsModule("./translations/oc.js")
@JsModule("./translations/pl.js")
@JsModule("./translations/pt.js")
@JsModule("./translations/pt-br.js")
@JsModule("./translations/ro.js")
@JsModule("./translations/ru.js")
@JsModule("./translations/si.js")
@JsModule("./translations/sk.js")
@JsModule("./translations/sl.js")
@JsModule("./translations/sq.js")
@JsModule("./translations/sr.js")
@JsModule("./translations/sr-latn.js")
@JsModule("./translations/sv.js")
@JsModule("./translations/th.js")
@JsModule("./translations/tk.js")
@JsModule("./translations/tr.js")
@JsModule("./translations/tt.js")
@JsModule("./translations/ug.js")
@JsModule("./translations/uz.js")
@JsModule("./translations/uk.js")
@JsModule("./translations/vi.js")
@JsModule("./translations/zh.js")
@JsModule("./translations/zh-cn.js")
@CssImport("./ckeditor.css")
@NpmPackage(value = "ckeditor5", version = "34.2.0")
class ViewDocument: VerticalLayout() {

    init {
        setWidthFull()
        height = "550px"
    }

    fun view(document: DocumentService.Document): VerticalLayout {
        removeAll()
        if (document.contentLength > DocumentReception.MAX_FILE_LENGTH) {
            Notification.show("Archivo con un tamaño muy grande ${document.contentLength} no se permite subirse")
            isVisible = false

            return this
        }
        when (document.mimeType) {
            DocumentReception.OCTET_STREAM -> {
                val txtView = TextArea()

                txtView.value = """
                El archivo que se esta seleccionado es un archivo no de texto y es un archivo tipo 'Binary'.
                
                No se muestra su contenido y no se recomienda subirlo al expediente ya que no lo podrá mostrar el sistema para su revisión.
                """.trimIndent()
                txtView.setSizeFull()
                add(txtView)
                isVisible = true
            }
            DocumentReception.JPG -> { showImage("jpg", document.fileData, document.mimeType) }
            DocumentReception.PNG -> { showImage("png", document.fileData, document.mimeType) }
            DocumentReception.GIF -> { showImage("gif", document.fileData, document.mimeType) }
            DocumentReception.BMP -> { showImage("bmp", document.fileData, document.mimeType) }
            DocumentReception.TIFF -> {
                val txtView = TextArea()

                txtView.value = """
                El archivo que se esta seleccionado es un archivo tipo 'tiff'.
                
                No se muestra su contenido y no se recomienda subirlo al expediente ya que no lo podrá mostrar el sistema para su revisión.
                """.trimIndent()
                txtView.setSizeFull()
                add(txtView)
                isVisible = true
            }
            DocumentReception.TEXT -> {
                val inputAsString = document.fileData.bufferedReader().use { it.readText() }  // defaults to UTF-8
                val txtArea = TextArea()

                txtArea.value = inputAsString
                txtArea.setSizeFull()
                add(txtArea)
                isVisible = true
            }
            DocumentReception.HTML -> {
                val inputAsString = document.fileData.bufferedReader().use { it.readText() }  // defaults to UTF-8

                if (document.title == null) {  // it is not yet a saved document just list the stream as an HTML (dont´use CKEditor)
                    val html = Html("<div>$inputAsString</div>")

                    add(html)
                } else {
                    val config = Config()

                    if (document.title.contains("PREVIO")) {
                        val saveContract = button("Aplicar cambios", Icon(VaadinIcon.FILE_ADD)) {
                            isVisible = false
                        }
                        val saveFinalContract = button("Contrato definitivo", Icon(VaadinIcon.FILE_TEXT_O)) {
                            isVisible = false
                        }
                        add(HorizontalLayout(saveContract, saveFinalContract))

                        val toolbar = arrayOf(
                            Constants.Toolbar.heading, Constants.Toolbar.fontFamily,
                            Constants.Toolbar.fontSize, Constants.Toolbar.fontColor,
                            Constants.Toolbar.bold, Constants.Toolbar.italic,
                            Constants.Toolbar.indent, Constants.Toolbar.outdent,
                            Constants.Toolbar.numberedList, Constants.Toolbar.bulletedList,
                            Constants.Toolbar.findAndReplace,
                            Constants.Toolbar.undo, Constants.Toolbar.redo,
                            Constants.Toolbar.underline, Constants.Toolbar.subscript,
                            Constants.Toolbar.selectAll, Constants.Toolbar.removeFormat,
                            Constants.Toolbar.pageBreak, Constants.Toolbar.specialCharacters,
                            Constants.Toolbar.alignment, Constants.Toolbar.highlight,
                            Constants.Toolbar.insertTable,
                            Constants.Toolbar.insertImage
                        )

                        config.setEditorToolBar(toolbar)
                        config.setPlaceHolder("Here is a place holder")
                        config.setUILanguage(Constants.Language.es)

                        val classicEditor = VaadinCKEditorBuilder().with { builder: VaadinCKEditorBuilder ->
                            builder.editorData = inputAsString
                            builder.editorType = Constants.EditorType.DECOUPLED
                            builder.theme = Constants.ThemeType.LIGHT
                            builder.config = config
                        }.createVaadinCKEditor()

                        classicEditor.width = "800px"
                        classicEditor.height = "900px"
                        classicEditor.addValueChangeListener {
                            saveContract.isVisible = true
                            saveFinalContract.isVisible = true
                        }
                        saveContract.addClickListener {
                            saveContract(document, newContent = classicEditor.value, definitive = false)
                            saveContract.isVisible = false
                        }
                        saveFinalContract.addClickListener {
                            saveContract(document, newContent = classicEditor.value, definitive = true)
                            saveContract.isVisible = false
                            saveFinalContract.isVisible = false
                        }
                        add(classicEditor)
                    } else {            // Just for show it. No editing functions are allowed
                        val toolbar = arrayOf(Constants.Toolbar.selectAll)

                        config.setEditorToolBar(toolbar)
                        config.setUILanguage(Constants.Language.es)

                        val classicEditor = VaadinCKEditorBuilder().with { builder: VaadinCKEditorBuilder ->
                            builder.editorData = inputAsString
                            builder.editorType = Constants.EditorType.CLASSIC
                            builder.theme = Constants.ThemeType.LIGHT
                            builder.config = config
                        }.createVaadinCKEditor()

                        classicEditor.width = "800px"
                        classicEditor.height = "900px"
                        add(classicEditor)
                    }
                }
                isVisible = true
            }
            DocumentReception.PDF -> {
                // For PDF viewer we use the Vaadin on located in: https://vaadin.com/directory/component/pdf-viewer/links
                val pdfViewer = PdfViewer()
                val resource = StreamResource("pdf", InputStreamFactory {
                        ByteArrayInputStream(document.fileData.readAllBytes())
                    })
                pdfViewer.setSrc(resource)
                pdfViewer.setSizeFull()
                add(pdfViewer)
                isVisible = true
            }
            else -> {
                val txtView = TextArea()

                txtView.value = """
                El archivo que se esta seleccionado no soporta el typo ${document.mimeType}.
                
                No se muestra su contenido y no se recomienda subirlo al expediente ya que no lo podrá mostrar el sistema para su revisión.
                """.trimIndent()
                txtView.setSizeFull()
                add(txtView)
                isVisible = true
            }
        }

        return this
    }

    private fun showImage(name: String, fileData : InputStream, mimeType: String) {
        val src = StreamResource(name, InputStreamFactory {
                                                ByteArrayInputStream(fileData.readAllBytes())
                                            })
        src.setContentType(mimeType)
        val imageView = Image(src, "No se pudo visualiza la imagen")

        add(imageView)
        isVisible = true
    }

    private fun saveContract(document: DocumentService.Document,
                             newContent: String?, definitive: Boolean) {
        if (newContent.isNullOrEmpty()) return

        val stream = newContent.byteInputStream()

        document.updateContent(stream, newContent.length.toLong(), definitive)
        Notification.show("Los cambios han sido actualizados")
    }
}
