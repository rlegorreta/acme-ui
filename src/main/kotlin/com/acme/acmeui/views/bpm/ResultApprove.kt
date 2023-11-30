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
 *  ResultApprove.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acmeui.views.bpm

import com.acme.acmeui.ApplicationContextProvider
import com.acme.acmeui.util.FormSchemaReaderUtil
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mvysny.karibudsl.v10.*
import com.ailegorreta.client.bpm.data.dto.Task
import com.ailegorreta.client.bpm.data.dto.VariableInput
import com.ailegorreta.client.bpm.form.ButtonContainer
import com.ailegorreta.client.bpm.form.FormData
import com.ailegorreta.client.bpm.views.CamundaFormView
import com.ailegorreta.client.bpm.views.FormView
import com.ailegorreta.client.bpm.views.TaskView
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.ComponentEvent
import com.vaadin.flow.component.ComponentEventListener
import com.vaadin.flow.component.HasValue
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.shared.Registration

class ResultApprove: FormView, KComposite() {
    private lateinit var form: VerticalLayout
    private var task: Task? = null
    private var formView: CamundaFormView? = null

    val root = ui {
        formLayout {
            className = "task-form"
            width = "25em"
            text("Esta sección de la forma es programática para consultar el documento")
            form = verticalLayout {
                setSizeFull()
            }
        }
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)

        val mapper = ApplicationContextProvider.getBean(ObjectMapper::class.java)
        val schema = FormSchemaReaderUtil.getBpmSchemaFromFileName("result-approve")

        formView = CamundaFormView(FormData(task!!, schema, mapper))
        // ^ generate an automatic form view as subview with the schema result-approve
        // make me listener if some button exists
        formView!!.addListenerForm(ButtonContainer.SubmitEvent::class.java) { fireEvent(TaskView.SubmitEvent(this, task)) }
        formView!!.addListenerForm(ButtonContainer.ResetEvent::class.java) { fireEvent(TaskView.ResetEvent(this, task)) }
        // now for the reset method
        formView!!.addListenerForm(TaskView.ObjectDidChangeEvent::class.java) { event ->
            objectDidChange(event.field, event.oldValue, event.newValue)
        }
        formView!!.setTask(task!!)
        form.add(formView!!)
    }

    override fun setTask(task: Task) {
        this.task = task
    }

    override fun submit() = formView!!.submit()
    // ^ in our custom subview we do not need to validate anything, just in the subview

    override fun reset() {
        formView!!.reset()
    }

    override fun objectDidChange(field: HasValue<*, *>, oldValue: Any?, newValue: Any?) {
        fireEvent(TaskView.ObjectDidChangeEvent(this, field, oldValue, newValue))
    }

    override fun variables(): Collection<VariableInput> = formView!!.variables()
    // ^ in our custom subview we do not declare any additional bpm variable. All the variables are declared in
    //   the subview

    /**
     * Ths listener is to forward the Camunda form to submit or reset the form but also to reset the binder.
     */
    override fun <T : ComponentEvent<*>> addListenerForm(eventType: Class<T>?, listener: ComponentEventListener<T>): Registration {
        return eventBus.addListener(eventType, listener)
    }
}
