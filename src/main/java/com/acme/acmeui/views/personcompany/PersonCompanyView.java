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
 *  PersonCompanyView.java
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.views.personcompany;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.acme.acmeui.data.dto.*;
import com.acme.acmeui.data.service.CompaniaService;
import com.acme.acmeui.data.service.DirigeService;
import com.acme.acmeui.data.service.PersonaService;
import com.acme.acmeui.data.service.TrabajaService;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

import org.vaadin.addons.visjs.network.event.SelectEvent;
import org.vaadin.addons.visjs.network.main.Edge;
import org.vaadin.addons.visjs.network.main.NetworkDiagram;
import org.vaadin.addons.visjs.network.main.Node;
import org.vaadin.addons.visjs.network.options.Options;
import org.vaadin.addons.visjs.network.options.edges.ArrowHead;
import org.vaadin.addons.visjs.network.options.edges.Arrows;
import org.vaadin.addons.visjs.network.options.edges.Edges;

import jakarta.annotation.security.RolesAllowed;

/**
 * Screen to manipulate and handle companies relationships with persons. It uses the vaadin.addon-vis-network to display the
 * relationships in a graph.
 *
 * For more info see the org-vaadin-addon-vis-network library in ailegorreta-kit-client group.
 *
 * @project acme-ui
 * @author rlh
 * @date November 2023
 */
@SuppressWarnings("serial")
@Route("serverviews/personacompania")
@PageTitle("Relaciones entre personas y compañías")
@RolesAllowed({"ROLE_RELACIONES","ROLE_ALL"})
public class PersonCompanyView extends VerticalLayout {

    final NetworkDiagram graph = new NetworkDiagram(Options.builder()
                                                            .withWidth("800px")
                                                            .withHeight("600px")
                                                            .withLocale("es")
                                                            .withEdges(Edges.builder()
                                                                    .withArrows(new Arrows(new ArrowHead(1, Arrows.Type.arrow)))
                                                                    .build())
                                                            .build());
    final List<Node> nodes = new LinkedList<>();
    final AtomicInteger idCounter = new AtomicInteger();
    final HashMap<String, Compania> hashMapNodesCompanies = new HashMap();
    final HashMap<String, Area> hashMapNodesAreas = new HashMap();
    final HashMap<String, Persona> hashMapNodesPersons = new HashMap();
    final HashMap<String, String>  addedNodes = new HashMap<>();      // Just nodes companies can be added
    Compania companyToBeAdded;
    Persona selectedPerson;
    Set<String> expandedNodes = new HashSet<>();                      // Just person nodes can be expanded
                                                                      // Company nodes area expanded to ares automatically
    ListDataProvider<Node> dataProviderNodes;
    final Set<Edge> edges = new HashSet<>();
    ListDataProvider<Edge> dataProviderEdges;
    Set<Edge> addedEdges = new HashSet<>();
    Set<Edge> deletedEdges = new HashSet<>();
    SelectEvent selection = null;
    Boolean graphGenerated = false;

    HashMap<String, Set<Compania>> selectedDirectCompanies = new HashMap<>();

    Registration registrationSelect;
    Registration registrationDoubleClick;
    Registration registrationAddEdge;
    Registration registrationDeleteEdge;

    final static Integer MAX_PERSONS = 30;
    final static Integer MAX_COMPANIES = 20;
    final static String DIRECTS = "Dirige";
    final static String NO_ACTIVE_COLOR = "#e5f4ff";
    final static String COMPANY_COLOR = "#faa775";
    final static String AREA_COLOR = "#996677";

    final CompaniaService companiaService;
    final PersonaService personaService;
    final TrabajaService trabajaService;
    final DirigeService dirigeService;

    MultiSelectListBox<Persona>     selectedPersonsLB;
    ListBox<Compania>               selectedCompanyLB;
    Button                          addAllPersons;
    Button                          addOneCompany;
    TextField                       originPersonTF;
    Button                          insert;

    public PersonCompanyView(CompaniaService companiaService, PersonaService personaService,
                             TrabajaService trabajaService, DirigeService dirigeService) {
        this.companiaService = companiaService;
        this.personaService = personaService;
        this.trabajaService = trabajaService;
        this.dirigeService = dirigeService;

        // create the view
        setMargin(true);
        setPadding(true);
        setSpacing(true);
        add(new HorizontalLayout(personCompanyView(), originView(), relationshipView()));
        add(manageGraphView());
        add(new HorizontalLayout(graph, personsCompaniesView()));
        setSizeFull();
    }

    private FormLayout personCompanyView() {
        FormLayout                  layout = new FormLayout();
        TextField                   criteriaPersonTF = new TextField("Apellido");
        TextField                   criteriaCompanyTF = new TextField("Razón social");

        addAllPersons = new Button("Generar", new Icon(VaadinIcon.CLUSTER), e -> createGraph(selectedPersonsLB.getSelectedItems(), null, null));
        addAllPersons.setEnabled(false);
        addOneCompany = new Button("Incluir Cía.",  new Icon(VaadinIcon.PLUS), e -> {
                                            if (companyToBeAdded != null) {
                                                // the graph is going to re-generate because we will expand the company node.
                                                var checkCompany = hashMapNodesCompanies.get(companyToBeAdded.get_id());

                                                if (checkCompany == null) {
                                                    String nodeId = companyToBeAdded.get_id();
                                                    String label;

                                                    if (companyToBeAdded.getNombre().length() > 10)
                                                        label = companyToBeAdded.getNombre().substring(0, 7) + "...";
                                                    else
                                                        label = companyToBeAdded.getNombre();
                                                    Node newNode = new Node(nodeId, label);        // companies node does not have any prefix

                                                    newNode.setColor(COMPANY_COLOR);
                                                    nodes.add(newNode);
                                                    hashMapNodesCompanies.put(companyToBeAdded.get_id(), companyToBeAdded);
                                                    expandCompanyNode(companyToBeAdded);        // create areas nodes
                                                }
                                                selectedCompanyLB.setValue(null);
                                            }
                                    });
        addOneCompany.setEnabled(false);
        criteriaPersonTF.setPlaceholder("Búsqueda"); criteriaPersonTF.setPrefixComponent(VaadinIcon.SEARCH.create());
        criteriaPersonTF.addValueChangeListener(e -> {
            if (e.getValue().length() > 2) {
                var items = personaService.allPersons(e.getValue(), null, 0, MAX_PERSONS).getContent();

                if (items.size() >= MAX_PERSONS) {
                    var notification = Notification.show("Se necesita especificar un criterio de selección mas restrictivo para leer todas las personas");
                    notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                }
                selectedPersonsLB.setItems(items);
            }
        });
        criteriaCompanyTF.addValueChangeListener(e -> {
            if (e.getValue().length() > 2) {
                var items = companiaService.allCompanies(e.getValue(), 0, MAX_COMPANIES).getContent();

                if (items.size() >= MAX_COMPANIES) {
                    var notification = Notification.show("Se necesita especificar un criterio de selección mas restrictivo para leer todas las compañías");
                    notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                }
                selectedCompanyLB.setItems(items);
            }
        });
        layout.add(criteriaPersonTF, addAllPersons, criteriaCompanyTF, addOneCompany);
        layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                                  new FormLayout.ResponsiveStep("350px", 2));

        return layout;
    }

    private VerticalLayout originView() {
        originPersonTF = new TextField("Persona analizada:");
        originPersonTF.setEnabled(false);

        var layout = new VerticalLayout(originPersonTF);

        layout.setMinWidth("200px");
        layout.setClassName("self-end");

        return layout;
    }

    private HorizontalLayout manageGraphView() {
        Button fit = new Button(new Icon(VaadinIcon.VIEWPORT), e -> {graph.diagramFit(); });
        Button deselect = new Button(new Icon(VaadinIcon.ARROWS_LONG_H), e -> { graph.diagramUnselectAll();
                                                                        selectedPerson = null;
                                                                        originPersonTF.setValue("");
                                                                     });
        Button erase = new Button(new Icon(VaadinIcon.ERASER), e -> clearGraph());

        fit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        deselect.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        erase.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        return new HorizontalLayout(fit, deselect, erase);
    }

    private FormLayout relationshipView() {
        FormLayout          layout = new FormLayout();
        TextField           puestoTF = new TextField();

        insert = new Button(new Icon(VaadinIcon.INSERT), e-> {
                                            graph.addEdgeMode();
                                            e.getSource().setEnabled(false);
                                            Notification.show("Seleccione el nodo y con arrastre el mouse al nodo destino");
                                        });
        final Button undo = new Button(new Icon(VaadinIcon.ARROW_CIRCLE_LEFT), e->{
                                            graph.disableEditMode();
                                            insert.setEnabled(true);
                                        });
        Button deleteEdge = new Button(new Icon(VaadinIcon.TRASH), e-> {
                                            if ((selection != null) &&
                                                    (selection.getParams().getArray("nodes").length() == 0) &&
                                                    (selection.getParams().getArray("edges").length() == 1))  {
                                                graph.deleteSelected();
                                            } else
                                                Notification.show("Seleccione solo una relación para ser eliminada");
                                        });
        Button save = new Button("Guardar", new Icon(VaadinIcon.HARDDRIVE), e-> {
            if (validEdges()) {
                // First delete edges. Must be first if the user added a new relationship and we don´t want to be deleted
                deletedEdges.stream().filter(edge -> !addedEdges.contains(edge))
                        .forEach(edge -> {
                            if (edge.getLabel().equals(DIRECTS))
                                try {
                                    String idArea = edge.getTo().substring(2);
                                    String idPersona = edge.getFrom().substring(2);

                                    dirigeService.deleteDirects(idPersona, idArea);
                                } catch (Exception ex) {
                                    Notification.show("ERROR: No se pudo borrar la relación de la persona al área que dirige");
                                }
                            else
                                try {
                                    String idPersona = edge.getFrom().substring(2);

                                    trabajaService.deleteWork(idPersona, searchCompanyId(edge.getTo()));
                                } catch (Exception ex) {
                                    Notification.show("ERROR: No se pudo borrara la relación de la persona a la empresa que trabaja");
                                }
                        });
                // Second add edges first
                addedEdges.stream().filter(edge -> !deletedEdges.contains(edge))
                        .forEach(edge -> {
                            if (edge.getLabel().equals(DIRECTS))
                                try {
                                    String idArea = edge.getTo().substring(2);
                                    String idPersona = edge.getFrom().substring(2);
                                    var companies = selectedDirectCompanies.get(edge.getId());

                                    if (companies != null) {
                                        companies.forEach(company ->
                                                dirigeService.addDirects(hashMapNodesPersons.get(idPersona),
                                                        idArea, company.get_id(), company.getNombre()));
                                    } else
                                        Notification.show("No se puede dar de alta la relación 'dirige' porque no existe una compañía");
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                    Notification.show("ERROR: No se pudo actualizar la relación de la persona al área que dirige");
                                }
                            else
                                try {
                                    String idPersona = edge.getFrom().substring(2);

                                    trabajaService.addWork(idPersona, searchCompanyId(edge.getTo()), edge.getLabel());
                                } catch (Exception ex) {
                                    Notification.show("ERROR: No se pudo actualiza la relación de la persona a la empresa que trabaja");
                                }
                        });
                Notification.show("Se almacenaron todos los cambios");
                clearGraph();
                selectedPersonsLB.clear();
            } else
                Notification.show("No se almacenaron los cambios debido a que existen relaciones no válidas. ");
        });

        puestoTF.addValueChangeListener(e -> {
            graph.setDefaultTitleEdge("Trabaja");
            graph.setDefaultLabelEdge(e.getValue());
        });

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        layout.add(puestoTF, new HorizontalLayout(insert, undo, deleteEdge, save));
        layout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1), // Use one column by default
                // Use two columns, if layout's width exceeds 500px
                new FormLayout.ResponsiveStep("300px", 2)); // Use two columns, if layout's width exceeds 500px

        return layout;
    }

    private boolean validEdges() {
        AtomicBoolean valid = new AtomicBoolean(true);

        addedEdges.stream().filter(edge -> !deletedEdges.contains(edge))
                .forEach( edge -> {
                    if (edge.getLabel().equals(DIRECTS))
                        try {
                            String idArea = edge.getTo().substring(2);
                            String idPersona = edge.getFrom().substring(2);

                            if (hashMapNodesAreas.get(idArea) == null)  valid.set(false);
                            if (hashMapNodesPersons.get(idPersona) == null)  valid.set(false);
                        } catch (Exception ex) {
                            Notification.show("ERROR: No se pudo actualizar la relación de la persona al área que dirige");
                            valid.set(false);
                        }
                    else try {
                        try {
                            String idCompania = edge.getTo();
                            String idPersona = edge.getFrom().substring(2);

                            if (hashMapNodesCompanies.get(idCompania) == null)  valid.set(false);
                            if (hashMapNodesPersons.get(idPersona) == null)  valid.set(false);
                        } catch (Exception ex) {
                            Notification.show("ERROR: No se pudo actualizar la relación de la persona al área que dirige");
                            valid.set(false);
                        }
                    } catch (Exception ex) {
                        Notification.show("ERROR: No se pudo actualizar la relación de la persona la compañía que trabaja");
                        valid.set(false);
                    }
                });

        return valid.get();
    }

    private String searchCompanyId(String nodeId) {
        try {
            var company = hashMapNodesCompanies.get(nodeId);

            if (company != null) return company.get_id(); // found it
        } catch (NumberFormatException e) {
            // do nothing is tha same as not found since it is a new node
        }
        // maybe it is and added Node.
        return addedNodes.get(nodeId);
    }

    private VerticalLayout personsCompaniesView() {
        selectedPersonsLB = new MultiSelectListBox<>();
        selectedPersonsLB.setRenderer(new ComponentRenderer<>(person -> new Text(person.getFullName())));
        selectedPersonsLB.setHeight("300px");
        selectedPersonsLB.addSelectionListener( e -> addAllPersons.setEnabled(!e.getValue().isEmpty()));
        selectedCompanyLB = new ListBox<>();
        selectedCompanyLB.setRenderer(new ComponentRenderer<>(company -> new Text(company.getNombre())));
        selectedCompanyLB.setHeight("200px");
        selectedCompanyLB.addValueChangeListener( e -> {
            addOneCompany.setEnabled(graphGenerated && e.getValue() != null);
            companyToBeAdded = e.getValue();
        });

        var layout = new VerticalLayout(new Text("Personas para seleccionar"), selectedPersonsLB,
                                        new Text("Compañía a añadir"), selectedCompanyLB);

        layout.setMinWidth("300px");

        return layout;
    }

    private void expandCompanyNode(Compania company) {
        var comp = companiaService.getCompany(company.get_id(), null)
                                                     .stream().findFirst();
                                     // ^ need to re-read the company in order to get the Areas relationship

        if (comp.isPresent() && comp.get().getAreas() != null) {
            comp.get().getAreas().forEach(area -> {
                var needEdge = hashMapNodesAreas.get(area.get_id());

                if (needEdge == null) {
                    String nodeId = area.get_id();
                    String label;

                    if (area.getNombre().length() > 10)
                        label = area.getNombre().substring(0, 7) + "...";
                    else
                        label = area.getNombre();

                    Node newNode = new Node("A-" + nodeId, label);      // add prefix 'A'

                    newNode.setColor(AREA_COLOR);
                    nodes.add(newNode);
                    hashMapNodesAreas.put(area.get_id(), area);
                    needEdge = area;
                }
                // Add the companies' relationship in area DTO in order to generate the Dirige relationship
                if (needEdge.getCompanias() == null) needEdge.setCompanias(new HashSet<>());
                needEdge.getCompanias().add(comp.get());

                var newEdge = new Edge(company.get_id(), "A-" + area.get_id());

                newEdge.setId(idCounter.incrementAndGet() + "");
                newEdge.setLabel("CONTIENE");
                edges.add(newEdge);
            });
            dataProviderEdges = new ListDataProvider<>(edges);
            graph.setEdgesDataProvider(dataProviderEdges);
            dataProviderNodes = new ListDataProvider<>(nodes);
            graph.setNodesDataProvider(dataProviderNodes);
        }
    }

    private void createGraph(Set<Persona> newPersons, Collection<Trabaja> trabaja,
                                                      Collection<Dirige> dirige ) {
        if (newPersons != null) {
            if (!graphGenerated) {
                hashMapNodesPersons.clear();
                hashMapNodesCompanies.clear();
                hashMapNodesAreas.clear();
                addedNodes.clear();
            }
            newPersons.stream()
                      .forEach( newPerson -> {
                          if (hashMapNodesPersons.get(newPerson.get_id()) == null) {
                            String nodeId = newPerson.get_id();
                            String fullName = newPerson.getApellidoPaterno() + newPerson.getNombre();
                            String label;

                            if (fullName.length() > 10)
                              label = fullName.substring(0, 7) + "...";
                            else
                              label = fullName;

                            var newNode = new Node("P-" + nodeId, label);   // add prefix 'P'

                            if (!newPerson.getActivo())
                                newNode.setColor(NO_ACTIVE_COLOR);
                            nodes.add(newNode);
                            hashMapNodesPersons.put(newPerson.get_id(), newPerson);
                        }
                    });
            dataProviderNodes = new ListDataProvider<>(nodes);
            graph.setNodesDataProvider(dataProviderNodes);
            // Register all graph listeners
            if (!graphGenerated)
                registerListeners();
            graphGenerated = true;
            selectedPersonsLB.deselectAll();
        }
        if (trabaja != null) {
            trabaja.forEach( work -> {
                var needEdge = hashMapNodesCompanies.get(work.getTo().get_id());

                if (needEdge == null) {
                    String nodeId = work.getTo().get_id();
                    String label;

                    if (work.getTo().getNombre().length() > 10)
                        label = work.getTo().getNombre().substring(0, 7) + "...";
                    else
                        label = work.getTo().getNombre();
                    Node newNode = new Node(nodeId, label);        // companies node does not have any prefix

                    newNode.setColor(COMPANY_COLOR);
                    nodes.add(newNode);
                    hashMapNodesCompanies.put(work.getTo().get_id(), work.getTo());
                    expandCompanyNode(work.getTo());        // create areas nodes
                }
                var newEdge = new Edge("P-" + selectedPerson.get_id(), work.getTo().get_id());

                newEdge.setId(idCounter.incrementAndGet() + "");
                newEdge.setColor("#00a86b");
                newEdge.setTitle("Trabaja");
                newEdge.setLabel(work.getPuesto());
                edges.add(newEdge);
            });
            dataProviderEdges = new ListDataProvider<>(edges);
            graph.setEdgesDataProvider(dataProviderEdges);
            dataProviderNodes = new ListDataProvider<>(nodes);
            graph.setNodesDataProvider(dataProviderNodes);
        }
        if (dirige != null) {
            dirige.forEach( direct -> {
                var needEdge = hashMapNodesAreas.get(direct.getTo().get_id());

                if (needEdge == null) {         // this case is rarely must be executed since we already expanded the
                                                // company node, but leave the code just in case
                    String nodeId = direct.getTo().get_id();
                    String label;

                    if (direct.getTo().getNombre().length() > 10)
                        label = direct.getTo().getNombre().substring(0, 7) + "...";
                    else
                        label = direct.getTo().getNombre();

                    Node newNode = new Node("A-" + nodeId, label);      // add prefix 'A'

                    newNode.setColor(AREA_COLOR);
                    nodes.add(newNode);
                    hashMapNodesAreas.put(direct.getTo().get_id(), direct.getTo());
                    needEdge = direct.getTo();
                }
                // Add the companies' relationship in area DTO in order to generate the Dirige relationship
                if (needEdge.getCompanias() == null) needEdge.setCompanias(new HashSet<>());
                var company = companiaService.getCompany(direct.getIdCompania(), null)
                                                               .stream().findFirst();
                if (company.isPresent())
                    needEdge.getCompanias().add(company.get());
                else
                    Notification.show("Error no se encontró la compañía en la relación de dirige. Base de datos inconsistente");

                var newEdge = new Edge("P-" + selectedPerson.get_id(), "A-" + direct.getTo().get_id());

                newEdge.setId(idCounter.incrementAndGet() + "");
                newEdge.setTitle(DIRECTS);
                newEdge.setLabel(DIRECTS);
                edges.add(newEdge);
            });
            dataProviderEdges = new ListDataProvider<>(edges);
            graph.setEdgesDataProvider(dataProviderEdges);
            dataProviderNodes = new ListDataProvider<>(nodes);
            graph.setNodesDataProvider(dataProviderNodes);
        }
        companyToBeAdded = null;
        selectedPerson = null;
    }

    private Dialog selectDirectCompanies(Edge edge, Area area, String idPersona, String idArea) {
        Dialog dialog = new Dialog();
        var grid = createDirectCompanies(area);

        dialog.setHeaderTitle("Compañías que dirige:" + area.getNombre());
        dialog.add(grid);
        dialog.getFooter().add(new Button("Seleccionar", e -> {
            if (grid.getSelectedItems().isEmpty())
                Notification.show("Seleccionar al menos una compañía");
            else {
                var companies = selectedDirectCompanies.get(edge.getId());

                if (companies == null) companies = new HashSet<>();
                companies.addAll(grid.getSelectedItems());
                selectedDirectCompanies.put(edge.getId(), companies);
                addedEdges.add(edge);
                dialog.close();
            }
        }));
        dialog.addThemeVariants(DialogVariant.LUMO_NO_PADDING);

        return dialog;
    }

    private static Grid<Compania> createDirectCompanies(Area area) {
        Grid<Compania> grid = new Grid<>(Compania.class, false);

        grid.setItems(area.getCompanias());
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addColumn(Compania::getNombre).setHeader("Razón social");
        grid.getStyle().set("width", "500px").set("max-width", "100%");

        return grid;
    }

    private void registerListeners() {
        registrationSelect = graph.addSelectListener(ls -> selection = ls);
        registrationDoubleClick = graph.addDoubleClickListener(ls -> {
            if (!addedEdges.isEmpty() || !deletedEdges.isEmpty())
                Notification.show("El grafo ha sido editado, no se puede expander el nodo. Salvar cambios o re-generar el grafo");
            else
                try {
                    selectedPerson = hashMapNodesPersons.get(ls.getParams()
                                                        .getArray("nodes")
                                                        .getString(0).substring(2));
                    if (selectedPerson != null) {
                        originPersonTF.setValue(selectedPerson.getNombre() + selectedPerson.getApellidoPaterno());
                        if (!expandedNodes.contains(selectedPerson.get_id())) {
                            expandedNodes.add(selectedPerson.get_id());
                            createGraph(null, selectedPerson.getTrabaja(), selectedPerson.getDirige());
                        }
                    } else
                        originPersonTF.setValue("error no se encontró a la persona");
                } catch (NumberFormatException e) {
                    // this is the case when an inserted node is selected.
                    selectedPerson = null;
                } catch (IndexOutOfBoundsException e) {
                    // This is a double click not in a node. Do nothing
                }
        });
        registrationAddEdge = graph.addAddEdgeListener(ls -> {
            var edge = ls.getNewEdge();
            if (edge.getLabel().equals(DIRECTS)) {
                try {
                    String idArea = edge.getTo().substring(2);
                    var area = hashMapNodesAreas.get(idArea);
                    String idPersona = edge.getFrom().substring(2);

                    if (area.getCompanias().size() == 1) {
                        var company = area.getCompanias().stream().findFirst().get();
                        var companies = selectedDirectCompanies.get(edge.getId());

                        if (companies == null) companies = new HashSet<>();
                        companies.add(company);
                        selectedDirectCompanies.put(edge.getId(), companies);
                        addedEdges.add(edge);
                    } else if (area.getCompanias().size() > 1)
                        selectDirectCompanies(edge, area, idPersona, idArea).open();
                    else
                        Notification.show("No se puede dar de alta la relación 'dirige' porque no existe una compañía");
                    insert.setEnabled(true);
                } catch(Exception ex){
                    ex.printStackTrace();
                    Notification.show("ERROR: No se pudo actualizar la relación de la persona al área que dirige");
                }
            } else {
                // trabaja relationship just register the edge
                addedEdges.add(edge);
                insert.setEnabled(true);
            }
        });
        registrationDeleteEdge = graph.addDeleteEdgeListener( ls -> {
            var deletedEdge = edges.stream().filter(ls.getDeletedEdge()::equals).findAny().orElse(null);

            if (deletedEdge == null) {     // find it in added edges maybe it is there
                deletedEdge = addedEdges.stream().filter(ls.getDeletedEdge()::equals).findAny().orElse(null);
                if (deletedEdge == null)
                    Notification.show("Algo sucedió no se encontró la relación a borrar. Se recomienda re-establecer el grafo");
                else
                    addedEdges.remove(deletedEdge);     // remove it form the previously added edge
            } else
                deletedEdges.add(deletedEdge);
        });
    }

    private void removeListeners() {
        registrationSelect.remove();
        registrationDoubleClick.remove();
        registrationAddEdge.remove();
        registrationDeleteEdge.remove();
    }

    private void clearGraph() {
        nodes.clear();
        expandedNodes.clear();
        edges.clear();
        addedEdges.clear();
        deletedEdges.clear();
        dataProviderNodes.refreshAll();
        // dataProviderEdges.refreshAll();
        removeListeners();
        graphGenerated = false;
        selectedPerson = null;
        selection = null;
        originPersonTF.setValue("");
        selectedDirectCompanies.clear();
    }
}
