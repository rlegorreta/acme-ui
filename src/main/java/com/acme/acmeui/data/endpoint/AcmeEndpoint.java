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
 *  AcmeEndpoint.java
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.data.endpoint;

import java.time.LocalDateTime;
import java.util.*;

import com.acme.acmeui.data.dto.*;
import com.acme.acmeui.data.service.*;
import com.acme.acmeui.views.dataproviders.CompaniesDataProvider;
import com.acme.acmeui.views.dataproviders.OrdersDataProvider;
import com.acme.acmeui.views.dataproviders.PersonsDataProvider;
import com.ailegorreta.client.security.utils.HasLogger;
import dev.hilla.Endpoint;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.annotation.security.PermitAll;

/**
 * EndPoint to handle all ACME data reading and actualization.
 *
 * Also handle cache data
 *
 * @author rlh
 * @project: ACME-UI
 * @date: November 2023
 *
 */
@Endpoint
@PermitAll
public class AcmeEndpoint implements HasLogger {
    private SectorService sectorService;
    private EstadoService estadoService;
    private CompaniaService companiaService;
    private CompaniesDataProvider companiesDataProvider;
    private OrdersDataProvider ordersDataProvider;
    private PersonaService personaService;
    private PersonsDataProvider personsDataProvider;
    private CodigoService codigoService;
    private MunicipioService municipioService;
    private SysData sysData = null;

    /**
     * This class is to handle page handler for "any" hilla table. We use a small Page in order not to send
     * all data to the client.
     * note : We CANNOT declare a generic PageResponse because hilla generates a model with 'unknwon type' ant
     *        typescript generates the following error:
     *        Type 'unknown' does not satisfy the constraint 'Record<never, never>' in the PageResonseModel
     * So the work around is to generate a PageResponse class for each type of DTO
     *
     */
    static class PageResponseCompanias<T extends Compania> {
        public List<T> content;
        public long size;

        PageResponseCompanias(List<T> content, long size) {
            this.content = content;
            this.size = size;
        }
    }
    static class PageResponsePersonas<T extends Persona> {
        public List<T> content;
        public long size;

        PageResponsePersonas(List<T> content, long size) {
            this.content = content;
            this.size = size;
        }
    }
    static class PageResponseOrders<T extends Order> {
        public List<T> content;
        public long size;

        PageResponseOrders(List<T> content, long size) {
            this.content = content;
            this.size = size;
        }
    }

    public AcmeEndpoint(SectorService sectorService,
                        CompaniaService companiaService, CompaniesDataProvider companiesDataProvider,
                        CodigoService codigoService, MunicipioService municipioService,
                        PersonaService personaService, PersonsDataProvider personsDataProvider,
                        EstadoService estadoService,
                        OrdersDataProvider ordersDataProvider) {
        this.sectorService = sectorService;
        this.companiaService = companiaService;
        this.companiesDataProvider = companiesDataProvider;
        this.ordersDataProvider = ordersDataProvider;
        this.codigoService = codigoService;
        this.municipioService = municipioService;
        this.estadoService = estadoService;
        this.personaService = personaService;
        this.personsDataProvider = personsDataProvider;
    }

    public static class SysData {
        public List<Sector> sectors;
        public List<Estado> states;
    }

    public SysData getSysData() {
        if (sysData == null) {
            sysData = new SysData();
            sysData.sectors = sectorService.allSectors();
            sysData.states = estadoService.allStates();
        }

        return sysData;
    }

    public SysData refreshSysData() {
        sysData = null;

        return getSysData();
    }

    /**
     * Sector methods
     */
    public String uniqueSectorValidator(Sector sector) {
        return sectorService.uniqueValidator(sector);
    }

    public Sector saveSector(Sector sector) {
        var isNew = sector.getUsuarioModificacion() == null;

        sector.setUsuarioModificacion(SecurityContextHolder.getContext().getAuthentication().getName());
        sector.setFechaModificacion(LocalDateTime.now());
        if (isNew)
            return sectorService.addSector(sector);
        else
            return sectorService.updateSector(sector);
    }

    public String deleteSector(String id) {
        return sectorService.deleteSector(id);
    }

    /**
     * Companies methods
     */
    public PageResponseCompanias<Compania> companies(int page, int size, ArrayList<LinkedHashMap> params) {
        String nombre = null;

        if (params != null)
            for (LinkedHashMap<String, String> param: params) {
                String path = param.get("path");

                if (path.equals("nombre"))
                    nombre = param.get("value");
                else
                    getLogger().error("Error in filter path:" + path);
            };

        var companiesPage =  companiesDataProvider.allCompanies(nombre, page, size);

        return new PageResponseCompanias<>(companiesPage.getContent(), companiesDataProvider.getTotalElements());
    }

    public String uniqueCompaniaValidator(Compania compania) {
        return companiaService.uniqueValidator(compania);
    }

    public Compania saveCompany(Compania company) {
        var isNew = company.getUsuarioModificacion() == null;

        company.setUsuarioModificacion(SecurityContextHolder.getContext().getAuthentication().getName());
        company.setFechaModificacion(LocalDateTime.now());
        if (isNew)
            return companiaService.addCompany(company);
        else
            return companiaService.updateCompany(company);
    }

    /**
     * Persons methods
     */
    public PageResponsePersonas<Persona> persons(int page, int size, ArrayList<LinkedHashMap> params, boolean justActivePersons) {
        String apellidoPaterno = null;
        Boolean activo = justActivePersons ? true : null;

        if (params != null)
            for (LinkedHashMap<String, String> param: params) {
                String path = param.get("path");

                if (path.equals("apellidoPaterno"))
                    apellidoPaterno = param.get("value");
                else
                    getLogger().error("Error in filter path:" + path);
            };

        var personsPage =  personsDataProvider.allPersons(apellidoPaterno, activo, page, size);

        return new PageResponsePersonas<>(personsPage.getContent(), personsDataProvider.getTotalElements());
    }

    public String uniquePersonValidator(Persona persona) {
        return personaService.uniqueValidator(persona);
    }

    public Persona savePerson(Persona person) {
        var isNew = person.getUsuarioModificacion() == null;

        person.setUsuarioModificacion(SecurityContextHolder.getContext().getAuthentication().getName());
        person.setFechaModificacion(LocalDateTime.now());
        if (isNew)
            return personaService.addPerson(person);
        else
            return personaService.updatePerson(person);
    }

    /**
     * Codigo methods
     *
     * note: a zipcode is never deleted
     */
    public Optional<Codigo> getZipcode(Integer zipCode) {
        return codigoService.getZipcode(null, zipCode).stream().findFirst();
    }

    public Codigo addZipcode(Codigo zipcode) {
        if (zipcode.getEstado() == null)
            getLogger().error("Error se debe tener un estado definido");

        return codigoService.addZipcode(zipcode);
    }

    /**
     * Municipios methods
     *
     * note: a colony is never deleted
     */
    public Optional<Municipio> getMunicipio(String colony) {
        return municipioService.getColony(null, colony).stream().findFirst();
    }

    public Municipio addColony(Municipio colony) {
        if (colony.getCodigos() == null || colony.getCodigos().isEmpty())
            getLogger().error("Error se debe tener un al menos un código definido");

        return municipioService.addColony(colony);
    }

    public Municipio addColonyZipcode(String idColony, String idZipcode) {
        return municipioService.addColonyZipcode(idColony, idZipcode);
    }

    /**
     * Order methods
     */
    @NotNull
    public PageResponseOrders<Order> orders(int page, int size) {
        var ordersPage =  ordersDataProvider.allOrders(page, size);

        return new PageResponseOrders<>(ordersPage.getContent(), ordersDataProvider.getTotalElements());
    }

    @NotNull
    public Long ordersCount() {
        return ordersDataProvider.count();
    }

    public Order saveOrder(@NotNull Order order) {
        return null;
    }

    @NotNull
    @Override
    public Logger getLogger() { return HasLogger.DefaultImpls.getLogger(this); }
}
