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
 *  DocumentReception.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.views.bpm

import com.acme.acmeui.data.dto.DocumentType
import com.acme.acmeui.data.service.CompaniaService
import com.acme.acmeui.data.service.PersonaService
import com.acme.acmeui.service.cache.CacheService
import com.acme.acmeui.service.expediente.DocumentService
import com.acme.acmeui.service.expediente.ExpedienteService
import com.acme.acmeui.views.bpm.components.ViewDocument
import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.hasChildren
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.DetachEvent
import com.vaadin.flow.component.Html
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.radiobutton.RadioGroupVariant
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import org.springframework.security.core.context.SecurityContextHolder
import java.io.InputStream
import jakarta.annotation.security.RolesAllowed


/**
 * Screen to manipulate the camunda BPM document reception.
 *
 * This BPM process example shows the following functionality:
 * - Start a bpm.
 * - Upload a document
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
@SuppressWarnings("serial")
@Route("bpm/recepcion")
@PageTitle("Recepción de documentos")
@RolesAllowed("ROLE_RECEPCIONDOCS","ROLE_ALL")
class DocumentReception(private val expedienteService: ExpedienteService,
                        private val personaService: PersonaService,
                        private val companiaService: CompaniaService,
                        private val cacheService: CacheService,
                        private val documentService: DocumentService): KComposite() {

    companion object {
        const val MAX_PERSONS = 30
        const val MAX_FILE_LENGTH = 2048000         // 2 MB
        const val TEXT = "text/plain"
        const val HTML = "text/html"
        const val PDF = "application/pdf"
        const val OCTET_STREAM = "application/octet-stream"
        const val JPG = "image/jpeg"
        const val PNG = "image/png"
        const val TIFF = "image/tiff"
        const val BMP = "image/bmp"
        const val GIF = "image/gif"
    }

    private lateinit var personaMoral: TextField
    private lateinit var personaFisica: HorizontalLayout
    private lateinit var nombre: TextField
    private lateinit var apellidoMaterno: TextField
    private lateinit var selectedContainer: VerticalLayout
    private lateinit var preview: VerticalLayout
    private lateinit var recepcionDocumento: Button
    private lateinit var tipoDocumento: ComboBox<DocumentType>

    private var buffer = MultiFileMemoryBuffer()
    private var selected:String? = null
    private var document: DocumentService.Document? = null

    val root = ui {
        verticalLayout {
            setSizeFull()
            addClassName("list-view")
            flexLayout {
                addClassNames("content", "gap-m")
                setSizeFull()

                /* Input for Persona */
                verticalLayout {
                    radioButtonGroup<String> {
                        setItems("Física", "Moral")
                        value = "Moral"
                        label = "Tipo de persona"
                        addThemeVariants(RadioGroupVariant.LUMO_VERTICAL)
                        addValueChangeListener {
                            if (it.value == "Moral") {
                                personaMoral.isVisible = true
                                personaFisica.isVisible = false
                            } else if (value == "Física") {
                                personaMoral.isVisible = false
                                personaFisica.isVisible = true
                            } else {
                                personaMoral.isVisible = false
                                personaFisica.isVisible = false
                            }
                        }
                    }
                    personaMoral = textField("Empresa:") {
                        addValueChangeListener {
                            if (it.value.length > 2) {
                                val items = companiaService.allCompanies(it.value, 0, 1)?.content

                                selectedContainer.removeAll()
                                if (items != null && items.isNotEmpty()) {
                                    selected = items[0].nombre
                                    selectedContainer.add(Html("<h3>$selected</h3>"))
                                }
                                checkEnableReceptionDocument()
                            }
                        }
                    }
                    personaFisica = horizontalLayout {
                        isVisible = false
                        nombre = textField("Nombre")
                        textField("Apellido Paterno") {
                            addValueChangeListener {
                                if (it.value.length > 3) {
                                    val items = personaService.allPersons(it.value, activo = true,  0, MAX_PERSONS)?.content

                                    selectedContainer.removeAll()
                                    if (items != null && items.isNotEmpty()) {
                                        val filteredItems = items.filter { persona ->
                                                        ((nombre.value.isNullOrEmpty() || persona.nombre.contains(nombre.value)) &&
                                                        (apellidoMaterno.value.isNullOrEmpty() || persona.apellidoMaterno.contains(apellidoMaterno.value))) }
                                        if (filteredItems.isNotEmpty()) {
                                            selected = "${filteredItems[0].nombre} ${filteredItems[0].apellidoPaterno} ${filteredItems[0].apellidoMaterno}"
                                            selectedContainer.add(Html("<h3>$selected</h3>"))
                                        }
                                        checkEnableReceptionDocument()
                                    }
                                }
                            }
                        }
                        apellidoMaterno = textField("Apellido Materno")
                    }
                    selectedContainer = verticalLayout {}
                    tipoDocumento = comboBox<DocumentType>("Tipo de documento") {
                        setItems(cacheService.allDocumentTypes())
                        setItemLabelGenerator { it.name }
                        addValueChangeListener { checkEnableReceptionDocument() }
                    }
                }
                verticalLayout {
                    /* Input for document type */
                    upload(buffer) {
                        setWidthFull()
                        isAutoUpload = false
                        i18n {
                            addFiles {
                                one = "Archivo..."
                                many = "Archivos..."
                            }
                            dropFiles {
                                many = "Arrastrar archivos"
                                one = "Arrastrar archivo"
                            }
                            uploading {
                                status {
                                    processing = "Leyendo..."
                                    held = "Visualizar..."
                                    stalled = "Parado..."
                                    connecting = "Conectando..."
                                }
                                error {
                                    forbidden = "Archivo sin permiso de lectura..."
                                    serverUnavailable = "Servidor no disponible..."
                                    unexpectedServerError = "Error en la lectura..."
                                }
                            }
                        }
                        addSucceededListener {
                            val fileName: String = it.fileName
                            val length = it.contentLength
                            val mimeType = it.mimeType
                            val inputStream = buffer.getInputStream(fileName)

                            processFile(inputStream, fileName, length, mimeType)
                        }
                    }
                    preview = verticalLayout {
                        setWidthFull()
                        height = "610px"
                    }
                }
            }
            recepcionDocumento = button("Recepción documento") {
                addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                addClickListener { startProcess() }
                isEnabled = false
            }
        }
    }

    override fun onAttach(attachEvent: AttachEvent?) {
        super.onAttach(attachEvent)
    }

    override fun onDetach(detachEvent: DetachEvent?) {
        document?.fileData?.close()            // free any inputStream opened
        super.onDetach(detachEvent)
    }

    private fun startProcess() {
        if (document == null) {
            Notification("Error no se tiene definido ningún documento").open()

            return
        }
        // Save document first in Alfresco repository
        document!!.title = tipoDocumento.value.name
        val result = documentService.recieveDocument(document!!)

        if (result == null) {
            Notification.show("Ocurrió algún error al guardar el documento en el repositorio. No se inició el proceso de recepción")

            return
        }
        // Now start the Document reception process
        val html = (selectedContainer.getComponentAt(0) as Html).innerHtml
        val variables = mapOf("username" to SecurityContextHolder.getContext().authentication!!.name,
                              "persona" to html,
                              "tipoDocumento" to tipoDocumento.value.name,
                              "fileId" to result.objectId)

        val process = expedienteService.startProcess("recepcion-documento", variables)

        preview.isVisible = false
        preview.removeAll()
        recepcionDocumento.isEnabled = false
        val notification = Notification("Se inició $process", 5000, Notification.Position.MIDDLE)

        notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY)
        notification.open()
    }

    private fun processFile(fileData : InputStream, fileName: String, contentLength: Long, mimeType: String) {
        document?.fileData?.close()
        document = DocumentService.Document(documentService, fileName, fileData, contentLength, mimeType,
                                            SecurityContextHolder.getContext().authentication!!.name)
        preview.removeAll()
        preview.add(ViewDocument().view(document!!))
        preview.isVisible = true
        checkEnableReceptionDocument()
    }

    private fun checkEnableReceptionDocument() {
        recepcionDocumento.isEnabled = preview.hasChildren && selectedContainer.hasChildren &&
                                        tipoDocumento.value != null
    }

}
