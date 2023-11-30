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
 *  TasksLegalView.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.views.bpm

import com.acme.acmeui.views.dataproviders.TaskGridDataProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mvysny.karibudsl.v10.*
import com.ailegorreta.client.bpm.data.dto.Task
import com.ailegorreta.client.bpm.data.dto.TaskState
import com.ailegorreta.client.bpm.service.BpmService
import com.ailegorreta.client.bpm.views.CustomViews
import com.ailegorreta.client.bpm.views.TaskView
import com.ailegorreta.commons.utils.FormattingUtils
import com.vaadin.flow.component.AbstractField
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.renderer.ComponentRenderer
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import eu.vaadinonkotlin.vaadin.*
import java.time.LocalDateTime
import jakarta.annotation.security.RolesAllowed

/**
 * Screen to manipulate the camunda BPM tasks for Legal area
 *
 * This BPM process example shows the following functionality:
 * - Lists all tasks.
 * - Edits a task form and add some data.
 * - Assign the task.
 * - Completes the task.
 * - TODO: use alfresco repository to see a document.
 * - TODO: Task list paging (maybe)
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
@SuppressWarnings("serial")
@Route("bpm/taskslegal")
@PageTitle("Tareas del Area de Legal")
@RolesAllowed("ROLE_REVISIONLEGALDOCS","ROLE_ALL")
class TasksLegalView(override val service: BpmService,
                     override val dataProvider: TaskGridDataProvider,
                     private val customViews: CustomViews,
                     mapper: ObjectMapper): TasksAbstractView() {

    override val taskView = TaskView(service, customViews, mapper)

    override val root = ui {
        verticalLayout {
            setSizeFull()
            addClassName("list-view")
            flexLayout {
                addClassNames("content", "gap-m")
                setSizeFull()
                grid = grid(dataProvider = dataProvider.asDataProvider { it.id }) {
                    className = "task-grid"
                    setSizeFull()
                    appendHeaderRow()
                    filterBar = appendHeaderRow().asFilterBar(this)
                    asSingleSelect().addValueChangeListener { event: AbstractField.ComponentValueChangeEvent<Grid<Task>, Task> ->
                        editTask(event.value)
                    }
                    columnFor(Task::processName) {
                        isAutoWidth = true
                        val button = Button("Procesos", Icon(VaadinIcon.REFRESH))

                        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY)
                        button.addClickListener { refresh() }
                        setHeader(button)
                        filterBar.forField(TextField(), this).istartsWith()
                    }
                    columnFor(Task::name) {
                        isAutoWidth = true
                        setHeader("Tarea")
                        filterBar.forField(TextField(), this).istartsWith()
                    }
                    columnFor(
                        Task::creationTimeDateTime,
                        converter = { FormattingUtils.LOCALDATETIME_FORMATTER.format(it) },
                        sortable = true) {
                        setHeader("Fecha creación")
                        isAutoWidth = true
                        filterBar.forField(DateRangePopup(), this).inRange(LocalDateTime::class.java)
                    }
                    addColumn(ComponentRenderer { task ->
                        val res = if (task.completionTime.isNullOrEmpty()) ""
                        else FormattingUtils.LOCALDATETIME_FORMATTER.format(LocalDateTime.parse(task.completionTime, DATE_FORMATTER))
                        Text(res)
                    }).apply {
                        isAutoWidth = true
                        setHeader("Fecha terminación")
                    }
                    columnFor(Task::assignee) {
                        isAutoWidth = true
                        setHeader("Asignado")
                        filterBar.forField(TextField(), this).istartsWith()
                    }
                    col = addColumn(ComponentRenderer { task -> val span = Span()
                        val theme = String.format("badge %s", when (task.taskState) {
                            TaskState.COMPLETED -> "success"
                            TaskState.CREATED -> "warning"
                            else -> "error" } )

                        span.element.setAttribute("theme", theme)
                        span.text = task.taskState.toString()
                        span
                    }).apply {
                        setHeader("Estatus")
                        isAutoWidth = true
                        this.key = "taskState"      // need to do this because we use a render
                        val filter = ComboBox<TaskState>()

                        filter.setItems(TaskState.CREATED, TaskState.CANCELED, TaskState.COMPLETED)
                        filterBar.forField(filter, this).eq()
                    }
                }
                setFlexGrow(2.0, grid)
                add(taskView)
                taskView.addListener(TaskView.ClaimEvent::class.java) { event: TaskView.ClaimEvent -> claimTask(event) }
                taskView.addListener(TaskView.UnclaimedEvent::class.java) { event: TaskView.UnclaimedEvent -> unclaimedTask(event) }
                taskView.addListener(TaskView.CompleteEvent::class.java) { event: TaskView.CompleteEvent -> completeTask(event) }
                taskView.addListener(TaskView.CloseEvent::class.java) { closeEditor() }
                taskView.addListener(TaskView.SubmitEvent::class.java) { event: TaskView.SubmitEvent -> submit(event) }
                taskView.addListener(TaskView.ResetEvent::class.java) { event: TaskView.ResetEvent -> reset(event) }
                setFlexGrow(1.0, taskView)
                setFlexShrink(0.0, taskView)
            }
        }
    }

    /**
     * List just task for the legal departament
     */
    override fun updateList() {
        dataProvider.initDataSource("legal")        // read just Tasks from Legal Departament
        grid.dataProvider.refreshAll()
    }

}

