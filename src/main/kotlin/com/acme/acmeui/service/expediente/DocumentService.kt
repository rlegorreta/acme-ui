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
 *  DocumentService.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.service.expediente

import com.acme.acmeui.service.event.EventService
import com.ailegorreta.commons.cmis.CMISService
import com.ailegorreta.commons.cmis.config.ServiceConfigAlfresco
import com.ailegorreta.commons.cmis.data.AbstractCmisStreamObject
import com.ailegorreta.commons.cmis.exception.AlfrescoNotFoundException
import com.ailegorreta.commons.cmis.util.HasLogger
import com.ailegorreta.commons.event.EventType
import org.apache.chemistry.opencmis.client.api.Folder
import org.apache.chemistry.opencmis.commons.PropertyIds
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.*

/**
 * This service saves de documents in Alfresco repository
 *
 * @project acme-ui
 * @author rlh
 * @date July 2022
 */
@Service
class DocumentService(override val serviceConfig: ServiceConfigAlfresco,
                      private val eventService: EventService): CMISService(serviceConfig), HasLogger {

    companion object {
        const val EN_REVISION_FOLDER = "En revision"
        const val CONTENT_TYPE = "cmis:document"
    }

    class Document: AbstractCmisStreamObject {

        constructor(documentService: DocumentService, objectId: String): super(documentService, objectId)

        constructor(documentService: DocumentService, fileName: String,
                    fileData: InputStream, contentLength: Long, mimeType: String, author: String)  :
                super(documentService, fileName, fileData, contentLength, mimeType, author)


    }

    /**
     * This method saves a new registered document inside the 'En revision' folder
     */
    fun recieveDocument(document: Document): Document? {
        // check if folder en revision exists if not create it
        var receiveFolder = getFolder(EN_REVISION_FOLDER)

        if (receiveFolder == null)
            receiveFolder = createFolder(rootFolder(), EN_REVISION_FOLDER)
        // check if document exits
        var documentAlfresco = getDocumentByFolder(receiveFolder, document.fileName)

        if (documentAlfresco != null) documentAlfresco.delete()     // delete any previous version of same document exists
        try {
            document.folder = receiveFolder
            document.addProperty(PropertyIds.CREATION_DATE, Calendar.getInstance())
            document.createDocument()

            return document
        } catch (e: Exception) {
            logger.error("Error en la creación del documento ${document.fileName} ${e.message}")
            eventService.sendEvent(eventType = EventType.ERROR_EVENT,
                                   headers = null, userName = document.author,
                                   eventName = "ERROR:ALMACENAR_DOCUMENTO",
                                   value = e.message!!)
        }

        return null
    }

    private fun rootFolder(): Folder {
        var rootFolder = getFolder(serviceConfig.getAlfrescoCompany())

        if (rootFolder == null) {
            // create it inside 'User Homes' folder
            val userHomesFolder = getFolder("User Homes")

            if (userHomesFolder != null)
                rootFolder = createFolder(userHomesFolder,serviceConfig.getAlfrescoCompany() )
            else
                throw AlfrescoNotFoundException("No se encontró el folder 'User Homes'")
        }

        if (rootFolder != null)
            return rootFolder
        else
            throw AlfrescoNotFoundException("Error al creat el folder ${serviceConfig.getAlfrescoCompany()}")
    }

}
