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
 *  CompanyCompanyView.java
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.views.companycompany;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.acme.acmeui.data.dto.Compania;
import com.acme.acmeui.data.dto.Proveedor;
import com.acme.acmeui.data.service.CompaniaService;
import com.acme.acmeui.data.service.ProveedorService;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

import org.springframework.stereotype.Component;
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
 * Screen to manipulate and handle companies relationships. It uses the vaadin.addon-vis-network to display the
 * relationships in a graph.
 *
 * For more info see the org-vaadin-addon-vis-network library en lmass-client group.
 *
 * The relationships between Companies are:
 * - Subsidiaries
 * - Suppliers (and added field is to detect what type os supplier it is.
 *
 * @author rlh
 * @project acme-ui
 * @date November 2023
 */
@SuppressWarnings("serial")
@Route("serverviews/companiacompania")
@PageTitle("Relaciones entre compañías")
@RolesAllowed({"ROLE_RELACIONES","ROLE_ALL"})
// @JsModule("./vis-graph.js")
public class CompanyCompanyView extends VerticalLayout {

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
    final HashMap<String, Compania> hashMapNodes = new HashMap();
    final HashMap<String, String>  addedNodes = new HashMap<>();
    Compania companyToBeAdded;
    Compania selectedCompany;
    Set<String> expandedNodes = new HashSet<>();
    ListDataProvider<Node> dataProviderNodes;
    final Set<Edge> edges = new HashSet<>();
    ListDataProvider<Edge> dataProviderEdges;
    Set<Edge> addedEdges = new HashSet<>();
    Set<Edge> deletedEdges = new HashSet<>();
    SelectEvent selection = null;
    Boolean graphGenerated = false;

    Registration registrationSelect;
    Registration registrationDoubleClick;
    Registration registrationAddNode;
    Registration registrationAddEdge;
    Registration registrationDeleteEdge;

    final static Integer MAX_COMPANIES = 30;
    final static String GRAY_COLOR = "#a2b3c4";
    final static String SUBSIDIARY = "Subsidiaria";
    final static String SUPPLIER = "Proveedor";
    final static String SUPPLIER_SHORT= "Prov:";

    final CompaniaService   companiaService;
    final ProveedorService proveedorService;

    MultiSelectListBox<Compania>  selectedCompaniesLB;
    Button                        addAllCompanies;
    Button                        addOneCompany;
    TextField                     originCompanyTF;

    public CompanyCompanyView(CompaniaService companiaService, ProveedorService proveedorService) {
        this.companiaService = companiaService;
        this.proveedorService = proveedorService;

        // create the view
        setMargin(true);
        setPadding(true);
        setSpacing(true);
        add(new HorizontalLayout(companyView(), originView(), relationshipView()));
        add(manageGraphView());
        add(new HorizontalLayout(graph, new VerticalLayout(new Text("Compañías para seleccionar"), companiesView())));
        setSizeFull();
    }

    private FormLayout companyView() {
        FormLayout                  layout = new FormLayout();
        TextField                   criteriaTF = new TextField("Razón social");

        addAllCompanies = new Button("Generar", new Icon(VaadinIcon.CLUSTER), e -> createGraph(selectedCompaniesLB.getSelectedItems(), null, null));
        addAllCompanies.setEnabled(false);
        addOneCompany = new Button("Incluir Cía.",  new Icon(VaadinIcon.PLUS), e -> {
                            if (selectedCompaniesLB.getSelectedItems().size() != 1)
                                Notification.show("Se debe de seleccionar solo a una compañía para añadir al grafo");
                            else
                                addGraphNodes(selectedCompaniesLB.getSelectedItems());
                         });
        addOneCompany.setEnabled(false);
        criteriaTF.setPlaceholder("Búsqueda"); criteriaTF.setPrefixComponent(VaadinIcon.SEARCH.create());
        criteriaTF.addValueChangeListener(e -> {
            if (e.getValue().length() > 2) {
                var items = companiaService.allCompanies(e.getValue(), 0, MAX_COMPANIES).getContent();

                if (items.size() >= MAX_COMPANIES) {
                    var notification = Notification.show("Se necesita especificar un criterio de selección mas restrictivo para leer todas las compañías");
                    notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                }
                selectedCompaniesLB.setItems(items);
            }
        });
        addAllCompanies.setMinWidth("140px");  addOneCompany.setMinWidth("140px");
        layout.add(criteriaTF, new HorizontalLayout(addAllCompanies, addOneCompany));
        layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                                  new FormLayout.ResponsiveStep("350px", 2));

        return layout;
    }

    private VerticalLayout originView() {
        originCompanyTF = new TextField("Compañía analizada:");
        originCompanyTF.setEnabled(false);

        var layout = new VerticalLayout(originCompanyTF);

        layout.setMinWidth("200px");
        layout.setClassName("self-end");

        return layout;
    }

    private HorizontalLayout manageGraphView() {
        Button fit = new Button(new Icon(VaadinIcon.VIEWPORT), e -> {graph.diagramFit(); });
        Button deselect = new Button(new Icon(VaadinIcon.ARROWS_LONG_H), e -> { graph.diagramUnselectAll();
                                                                                selectedCompany = null;
                                                                                originCompanyTF.setValue("");
                                                                                });
        Button erase = new Button(new Icon(VaadinIcon.ERASER), e -> clearGraph());

        fit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        deselect.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        erase.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        return new HorizontalLayout(fit, deselect, erase);
    }

    private FormLayout relationshipView() {
        FormLayout          layout = new FormLayout();
        ComboBox<String>    relationTypeCB = new ComboBox<>();
        TextField           suppliertTF = new TextField("Tipo proveedor:");
        Button              insert = new Button(new Icon(VaadinIcon.INSERT), e-> {
                                        graph.addEdgeMode();
                                        e.getSource().setEnabled(false);
                                        Notification.show("Seleccione el nodo y con arrastre el mouse al nodo destino");
                                    });
        final Button undo = new Button(new Icon(VaadinIcon.ARROW_CIRCLE_LEFT), e-> graph.disableEditMode());
        Button deleteEdge = new Button(new Icon(VaadinIcon.TRASH), e-> {
                                        if ((selection != null) &&
                                            (selection.getParams().getArray("nodes").length() == 0) &&
                                                (selection.getParams().getArray("edges").length() == 1))  {
                                            graph.deleteSelected();
                                        } else
                                            Notification.show("Seleccione solo una relación para ser eliminada");
                                    });
        Button save = new Button("Guardar", new Icon(VaadinIcon.HARDDRIVE), e-> {
                    // Add edges first
                    addedEdges.stream().filter(edge -> !deletedEdges.contains(edge))
                                       .forEach( edge -> {
                                               if (edge.getLabel().equals(SUBSIDIARY))
                                                   companiaService.addCompanySubsidiary(searchCompanyId(edge.getFrom()),
                                                                                        searchCompanyId(edge.getTo()));
                                               else {
                                                   var tipo = edge.getLabel().substring(5);

                                                   // the from is the supplier invert relationship
                                                   proveedorService.addProveedor(searchCompanyId(edge.getTo()),
                                                                                 searchCompanyId(edge.getFrom()), tipo);
                                               }
                                       });
                    // Second delete edges
                    deletedEdges.stream().filter(edge -> !addedEdges.contains(edge))
                                         .forEach( edge-> {
                                             if (edge.getLabel().equals(SUBSIDIARY))
                                                 companiaService.deleteCompanySubsidiaria(searchCompanyId(edge.getFrom()),
                                                                                          searchCompanyId(edge.getTo()));
                                             else
                                                 // the from is the supplier invert relationship
                                                 proveedorService.deleteProveedor(searchCompanyId(edge.getTo()),
                                                                                 searchCompanyId(edge.getFrom()));
                                         });
                    Notification.show("Se almacenaron todos los cambios");
                    clearGraph();
                    selectedCompaniesLB.clear();
        });

        insert.setEnabled(false);
        relationTypeCB.setLabel("Relación a insertar");  relationTypeCB.setItems(SUBSIDIARY, SUPPLIER);
        relationTypeCB.addValueChangeListener(e -> {
                                                    suppliertTF.setEnabled(e.getValue().equals(SUPPLIER));
                                                    insert.setEnabled(e.getValue().equals(SUBSIDIARY));
                                                    graph.setDefaultLabelEdge(e.getValue());
                                                    });
        suppliertTF.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        suppliertTF.addValueChangeListener( e-> {
                                                    insert.setEnabled(e.getValue().length() > 0);
                                                    graph.setDefaultLabelEdge(SUPPLIER_SHORT + e.getValue());
                                                });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(relationTypeCB, suppliertTF, new HorizontalLayout(insert, undo, deleteEdge, save));
        layout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1), // Use one column by default
                // Use two columns, if layout's width exceeds 500px
                new FormLayout.ResponsiveStep("300px", 2)); // Use two columns, if layout's width exceeds 500px

        return layout;
    }

    private String searchCompanyId(String nodeId) {
        try {
            var company = hashMapNodes.get(nodeId);

            if (company != null) return company.get_id(); // found it
        } catch (NumberFormatException e) {
            // do nothing is tha same as not found since it is a new node
        }
        // maybe it is and added Node.
        return addedNodes.get(nodeId);
    }

    private MultiSelectListBox companiesView() {
        selectedCompaniesLB = new MultiSelectListBox<>();
        selectedCompaniesLB.setRenderer(new ComponentRenderer<>(company -> new Text(company.getNombre())));
        selectedCompaniesLB.setHeight("600px");
        selectedCompaniesLB.addSelectionListener( e -> {
            addAllCompanies.setEnabled(!e.getValue().isEmpty());
            addOneCompany.setEnabled(graphGenerated && !e.getValue().isEmpty());
        });

        return selectedCompaniesLB;
    }

    private void addGraphNodes(Set<Compania> newCompanies) {
        if (newCompanies != null && graphGenerated) {
            newCompanies.stream()
                    .filter(company -> hashMapNodes.get(company.get_id()) == null)
                    .forEach(newCompany -> {
                        String label;

                        if (newCompany.getNombre().length() > 10)
                            label = newCompany.getNombre().substring(0, 7) + "...";
                        else
                            label = newCompany.getNombre();
                        Notification.show("Hacer 'click' en el grafo para añadir la Compañía");
                        companyToBeAdded = newCompany;
                        graph.setDefaultTitleNode(label);
                        graph.addNodeMode();
                    });
            selectedCompaniesLB.deselectAll();
            selectedCompany = null;
        }
    }

    private void createGraph(Set<Compania> newCompanies, Collection<Compania> subsidiarias,
                                                         Collection<Proveedor> proveedores ) {
        if (newCompanies != null) {
            if (!graphGenerated) {
                hashMapNodes.clear();
                addedNodes.clear();
            }
            newCompanies.stream()
                        .forEach( newCompany -> {
                            if (hashMapNodes.get(newCompany.get_id()) == null) {
                                String nodeId = newCompany.get_id();
                                String label;

                                if (newCompany.getNombre().length() > 10)
                                    label = newCompany.getNombre().substring(0, 7) + "...";
                                else
                                    label = newCompany.getNombre();
                                nodes.add(new Node(nodeId, label));
                                hashMapNodes.put(newCompany.get_id(), newCompany);
                            }
                        });
            dataProviderNodes = new ListDataProvider<>(nodes);
            graph.setNodesDataProvider(dataProviderNodes);
            // Register all graph listeners
            if (!graphGenerated)
                registerListeners();
            graphGenerated = true;
            selectedCompaniesLB.deselectAll();
        }
        if (subsidiarias != null) {
            subsidiarias.forEach( subsidiaria -> {
                var needEdge = hashMapNodes.get(subsidiaria.get_id());

                if (needEdge == null) {
                    String nodeId = subsidiaria.get_id();
                    String label;

                    if (subsidiaria.getNombre().length() > 10)
                        label = subsidiaria.getNombre().substring(0, 7) + "...";
                    else
                        label = subsidiaria.getNombre();

                    Node newNode = new Node(nodeId, label);

                    newNode.setColor(GRAY_COLOR);
                    nodes.add(newNode);
                    hashMapNodes.put(subsidiaria.get_id(), subsidiaria);
                }
                var newEdge = new Edge(selectedCompany.get_id(), subsidiaria.get_id());

                newEdge.setId(idCounter.incrementAndGet() + "");
                newEdge.setLabel(SUBSIDIARY);
                edges.add(newEdge);
            });
            dataProviderEdges = new ListDataProvider<>(edges);
            graph.setEdgesDataProvider(dataProviderEdges);
            dataProviderNodes = new ListDataProvider<>(nodes);
            graph.setNodesDataProvider(dataProviderNodes);
        }
        if (proveedores != null) {
            proveedores.forEach( proveedor -> {
                var needEdge = hashMapNodes.get(proveedor.getTo().get_id());

                if (needEdge == null) {
                    String nodeId = proveedor.getTo().get_id();
                    String label;

                    if (proveedor.getTo().getNombre().length() > 10)
                        label = proveedor.getTo().getNombre().substring(0, 7) + "...";
                    else
                        label = proveedor.getTo().getNombre();
                    Node newNode = new Node(nodeId, label);

                    newNode.setColor(GRAY_COLOR);
                    nodes.add(newNode);
                    hashMapNodes.put(proveedor.getTo().get_id(), proveedor.getTo());
                }
                var newEdge = new Edge(selectedCompany.get_id(), proveedor.getTo().get_id());

                newEdge.setId(idCounter.incrementAndGet() + "");
                newEdge.setColor("#00a86b");
                newEdge.setLabel(SUPPLIER_SHORT + proveedor.getTipo());
                edges.add(newEdge);
            });
            dataProviderEdges = new ListDataProvider<>(edges);
            graph.setEdgesDataProvider(dataProviderEdges);
            dataProviderNodes = new ListDataProvider<>(nodes);
            graph.setNodesDataProvider(dataProviderNodes);
        }
        companyToBeAdded = null;
        selectedCompany = null;
    }

    private void registerListeners() {
        registrationSelect = graph.addSelectListener(ls -> selection = ls);
        registrationDoubleClick = graph.addDoubleClickListener(ls -> {
            if (!addedEdges.isEmpty() || !deletedEdges.isEmpty())
                Notification.show("El grafo ha sido editado, no se puede expander el nodo. Salvar cambios o re-generar el grafo");
            else
                try {
                    selectedCompany = hashMapNodes.get(ls.getParams()
                                                  .getArray("nodes")
                                                  .getString(0));
                    if (selectedCompany != null) {
                        originCompanyTF.setValue(selectedCompany.getNombre());
                        if (!expandedNodes.contains(selectedCompany.get_id())) {
                            expandedNodes.add(selectedCompany.get_id());
                            createGraph(null, selectedCompany.getSubsidiarias(), selectedCompany.getProveedores());
                        }
                    } else
                        originCompanyTF.setValue("error no se encontró a la compañía");
                } catch (NumberFormatException e) {
                    // this is the case when an inserted node is selected.
                    selectedCompany = null;
                } catch (IndexOutOfBoundsException e) {
                    // This is a double click not in a node. Do nothing
                }
        });
        registrationAddNode = graph.addAddNodeListener(ls -> {
            if (companyToBeAdded != null) {
                hashMapNodes.put(companyToBeAdded.get_id(), companyToBeAdded);
                addedNodes.put(ls.getNewNode().getId(), companyToBeAdded.get_id());
                companyToBeAdded = null;
            }
            Notification.show("Se añadió el nodo " + ls.getNewNode().getLabel());
        });
        registrationAddEdge = graph.addAddEdgeListener(ls -> addedEdges.add(ls.getNewEdge()));
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
        registrationAddNode.remove();
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
        selectedCompany = null;
        selection = null;
        originCompanyTF.setValue("");
    }

}
