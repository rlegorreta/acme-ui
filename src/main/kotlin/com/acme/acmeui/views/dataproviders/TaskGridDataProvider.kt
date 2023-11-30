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
 *  TaskGridDataProvider.kt
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.views.dataproviders

import com.vaadin.flow.spring.annotation.SpringComponent
import com.vaadin.flow.spring.annotation.UIScope
import com.github.mvysny.vokdataloader.DataLoader
import com.github.mvysny.vokdataloader.Filter
import com.github.mvysny.vokdataloader.SortClause
import com.ailegorreta.client.bpm.data.dto.Task
import com.ailegorreta.client.bpm.service.BpmService
import com.ailegorreta.commons.service.FilterChangeListener
import com.ailegorreta.commons.service.ServiceWithFilter
import com.ailegorreta.client.dataproviders.service.InMemoryService

/**
 * DataProvider for Task Grid in class DocumentApproveView
 *
 * @project: acme-ui
 * @author: rlh
 * @date: November 2023
 */
@SpringComponent
@UIScope
class TaskGridDataProvider constructor(val bpmService: BpmService): DataLoader<Task>, ServiceWithFilter {

    private var dataSource: List<Task>? = null
    private val service = TaskService()

    fun initDataSource(candidateGroup: String? = null) {
        dataSource = bpmService.allTasks(candidateGroup)
        service.initDataSource(dataSource!!)
    }

    override fun fetch(filter: Filter<Task>?, sortBy: List<SortClause>, range: LongRange) =
        service.fetch(filter, sortBy, range)

    override fun getCount(filter: Filter<Task>?) = service.getCount(filter)

    override fun addFilterChangeListener(listener: FilterChangeListener) = service.addFilterChangeListener(listener)

    fun size() = dataSource!!.size
}

class TaskService: InMemoryService<Task>()
