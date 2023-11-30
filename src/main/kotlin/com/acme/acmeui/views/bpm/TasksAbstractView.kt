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
 *  TasksAbstractView.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.views.bpm

import com.acme.acmeui.views.dataproviders.TaskGridDataProvider
import com.github.mvysny.karibudsl.v10.*
import com.ailegorreta.client.bpm.data.dto.Task
import com.ailegorreta.client.bpm.service.BpmService
import com.ailegorreta.client.bpm.views.TaskView
import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import eu.vaadinonkotlin.vaadin.*
import java.time.format.DateTimeFormatter

/**
 * TaskAbstract View to share code between TasksXXXXViews is we want to keep an standard between areas,
 * but each area could develope a complete different Tasks view which is the common case
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
abstract class TasksAbstractView(): KComposite() {

    protected lateinit var grid: Grid<Task>
    protected lateinit var filterBar: VokFilterBar<Task>
    protected lateinit var col: Grid.Column<Task>
    abstract val taskView: TaskView
    abstract val service: BpmService
    abstract val root: VerticalLayout
    abstract val dataProvider: TaskGridDataProvider


    companion object {
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    }

    override fun onAttach(attachEvent: AttachEvent?) {
        super.onAttach(attachEvent)
        updateList()
        closeEditor()
    }

    protected fun claimTask(event: TaskView.ClaimEvent) {
        service.claimTask(event.task!!.id, "demo")  // TODO include security in the BPM
        Notification("La tarea fue asignada.",5000, Notification.Position.BOTTOM_START).open()
        updateList()
    }

    protected fun unclaimedTask(event: TaskView.UnclaimedEvent) {
        service.unclaimedTask(event.task!!.id)
        Notification("La tarea fue liberada para que otro usuario se la pueda asignar.",5000, Notification.Position.BOTTOM_START).open()
        updateList()
    }

    protected fun completeTask(event: TaskView.CompleteEvent) {
        service.completeTask(event.taskId, event.variables)
        Notification("La tarea fue completada se continúa on el proceso.",5000, Notification.Position.BOTTOM_START).open()
        closeEditor()
        updateList()
    }

    protected fun closeEditor() {
        taskView.isVisible = false
        root.removeClassName("editing")
        // ^ make it responsive the TaskView
    }

    abstract fun updateList()

    protected fun submit(event: TaskView.SubmitEvent) {
        Notification("TODO: submit event received.",5000, Notification.Position.BOTTOM_START).open()
    }

    protected fun reset(event: TaskView.ResetEvent) {
        Notification("TODO: reset event received.",5000, Notification.Position.BOTTOM_START).open()
    }

    protected fun editTask(task: Task?) {
        if (task == null)
            closeEditor()
        else {
            taskView.setTask(task)
            taskView.isVisible = true
            root.addClassName("editing")
            // ^ make it responsive the TaskForm
        }
    }

    protected fun refresh() {
        updateList()
        closeEditor()
    }
}
