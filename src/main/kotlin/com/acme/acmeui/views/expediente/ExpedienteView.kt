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
 *  ExpedienteView.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.views.expediente

import com.acme.acmeui.data.service.CompaniaService
import com.acme.acmeui.data.service.PersonaService
import com.acme.acmeui.service.cache.CacheService
import com.acme.acmeui.service.expediente.DocumentService
import com.acme.acmeui.service.expediente.ExpedienteService
import com.acme.acmeui.views.bpm.components.ViewDocument
import com.github.mvysny.karibudsl.v10.*
import com.github.mvysny.kaributools.hasChildren
import com.vaadin.flow.component.*
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.listbox.ListBox
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.radiobutton.RadioGroupVariant
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import org.apache.chemistry.opencmis.client.api.CmisObject
import org.apache.chemistry.opencmis.commons.PropertyIds
import jakarta.annotation.security.RolesAllowed

/**
 * This view shows per folder all documents and can view the in a subview.
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
@SuppressWarnings("serial")
@Route("expediente")
@PageTitle("Consulta de expedientes")
@RolesAllowed("ROLE_EXPEDIENTES","ROLE_ALL")
class ExpedienteView (private val expedienteService: ExpedienteService,
                      private val personaService: PersonaService,
                      private val companiaService: CompaniaService,
                      private val cacheService: CacheService,
                      private val documentService: DocumentService): KComposite() {
    companion object {
        const val MAX_PERSONS = 30
    }

    private lateinit var personaMoral: TextField
    private lateinit var personaFisica: HorizontalLayout
    private lateinit var nombre: TextField
    private lateinit var apellidoMaterno: TextField
    private lateinit var selectedContainer: VerticalLayout
    private lateinit var preview: VerticalLayout
    private lateinit var showExpediente: Button
    private lateinit var documentsList: ListBox<String>

    private var selected:String? = null
    private var document: DocumentService.Document? = null
    private val documents = HashMap<String, CmisObject>()

    val root = ui {
        verticalLayout {
            setSizeFull()
            addClassName("list-view")
            flexLayout {
                addClassNames("content", "gap-m")
                setSizeFull()

                /* Input for Persona or Company expediente */
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
                                checkEnableExpediente()
                            }
                        }
                    }
                    personaFisica = horizontalLayout {
                        isVisible = false
                        nombre = textField("Nombre")
                        textField("Apellido Paterno") {
                            addValueChangeListener {
                                if (it.value.length > 3) {
                                    val items = personaService.allPersons(it.value, activo = true,  0,
                                                                        MAX_PERSONS
                                    )?.content

                                    selectedContainer.removeAll()
                                    if (items != null && items.isNotEmpty()) {
                                        val filteredItems = items.filter { persona ->
                                            ((nombre.value.isNullOrEmpty() || persona.nombre.contains(nombre.value)) &&
                                                    (apellidoMaterno.value.isNullOrEmpty() || persona.apellidoMaterno.contains(apellidoMaterno.value))) }
                                        if (filteredItems.isNotEmpty()) {
                                            selected = "${filteredItems[0].nombre} ${filteredItems[0].apellidoPaterno} ${filteredItems[0].apellidoMaterno}"
                                            selectedContainer.add(Html("<h3>$selected</h3>"))
                                        }
                                        checkEnableExpediente()
                                    }
                                }
                            }
                        }
                        apellidoMaterno = textField("Apellido Materno")
                    }
                    selectedContainer = verticalLayout {}
                    documentsList = listBox {
                        isVisible = false
                        addValueChangeListener {
                            if (it.value != null) {
                                val fileId = documents[it.value]!!.id

                                document = DocumentService.Document(documentService, fileId.substring(0, fileId.indexOf(';')))
                                                                                    // ^ take out comillas the version
                                document!!.documentById()
                                preview.removeAll()
                                preview.add(Html("<h3>${document!!.title}</h3>"))
                                preview.add(Text("Creador:${document!!.author}"))
                                preview.add(ViewDocument().view(document!!))
                            }
                        }
                    }
                }
                preview = verticalLayout {
                    setWidthFull()
                    height = "610px"
                }
            }
            showExpediente = button("Abrir expediente", Icon(VaadinIcon.FOLDER_OPEN)) {
                addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                addClickListener { openExpediente() }
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

    private fun checkEnableExpediente() {
        showExpediente.isEnabled = selectedContainer.hasChildren && (selected != null)
    }

    private fun openExpediente() {
        var folder = documentService.getFolder(selected!!)

        if (folder == null) {
            Notification.show("La persona física o moral $selected no tiene ningún expediente")

            documentsList.isVisible = false
            return
        }
        documents.clear()
        for (cmisObject in folder.children) {
            val objectType = cmisObject.getProperty<Any>(PropertyIds.OBJECT_TYPE_ID).valueAsString

            if (objectType == "cmis:document")
                documents[cmisObject.name] = cmisObject
        }
        documentsList.removeAll()
        documentsList.setItems(documents.keys)
        documentsList.isVisible = true
    }

}
