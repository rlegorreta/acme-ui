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
 *  PersonPersonView.java
 *
 *  Developed 2023 by LegoSoftSoluciones, S.C. www.legosoft.com.mx
 */
package com.acme.acmeui.views.personperson;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.acme.acmeui.data.dto.Persona;
import com.acme.acmeui.data.dto.Relacion;
import com.acme.acmeui.data.service.PersonaService;
import com.acme.acmeui.data.service.RelacionService;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
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
 * Screen to manipulate and handle persons relationships. It uses the vaadin.addon-vis-network to display the
 * relationships in a graph.
 *
 * For more info see the org-vaadin-addon-vis-network library en ailegorreta-kit-client group.
 *
 * Ther relation between persons are:
 * - RELACION with type and name.
 *
 * @project acme-ui
 * @author rlh
 * @date February 2023
 */
@SuppressWarnings("serial")
@Route("serverviews/personapersona")
@PageTitle("Relaciones entre personas")
@RolesAllowed({"ROLE_RELACIONES","ROLE_ALL"})
public class PersonPersonView extends VerticalLayout {

    final NetworkDiagram graph = new NetworkDiagram(Options.builder()
                                                        .withWidth("700px")
                                                        .withHeight("600px")
                                                        .withLocale("es")
                                                        .withEdges(Edges.builder()
                                                                .withArrows(new Arrows(new ArrowHead(1, Arrows.Type.arrow)))
                                                                .build())
                                                        .build());
    final List<Node> nodes = new LinkedList<>();
    final AtomicInteger idCounter = new AtomicInteger();
    final HashMap<String, Persona> hashMapNodes = new HashMap();
    final HashMap<String, String>  addedNodes = new HashMap<>();
    Persona personToBeAdded;
    Persona selectedPerson;
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

    final static Integer MAX_PERSONS = 30;
    final static String GRAY_COLOR = "#a2b3c4";
    final static String NO_ACTIVE_COLOR = "#e5f4ff";
    final static String RELATED = "Parentesco";
    final static String RECOMMEND = "Recomendado";
    final static String FRIEND = "Amigo";

    final PersonaService personaService;
    final RelacionService relacionService;

    MultiSelectListBox<Persona>   selectedPersonsLB;
    Button                        addAllPersons;
    Button                        addOnePerson;
    TextField                     originPersonTF;

    public PersonPersonView(PersonaService personaService, RelacionService relacionService) {
        this.personaService = personaService;
        this.relacionService = relacionService;

        // Create the view
        setMargin(true);
        setPadding(true);
        setSpacing(true);

        add(new HorizontalLayout(personView(), originView(), relationshipView()));
        add(manageGraphView());
        add(new HorizontalLayout(graph, personsView()));
        setSizeFull();
    }

    private FormLayout personView() {
        FormLayout                  layout = new FormLayout();
        TextField                   criteriaTF = new TextField("Apellido");

        addAllPersons = new Button("Generar", new Icon(VaadinIcon.CLUSTER), e -> createGraph(selectedPersonsLB.getSelectedItems(),  null));
        addAllPersons.setEnabled(false);
        addOnePerson = new Button("Incluir persona",  new Icon(VaadinIcon.PLUS), e -> {
                            if (selectedPersonsLB.getSelectedItems().size() != 1)
                                Notification.show("Se debe de seleccionar solo a una persona para añadir al grafo");
                            else
                                addGraphNodes(selectedPersonsLB.getSelectedItems());
        });
        addOnePerson.setEnabled(false);
        criteriaTF.setPlaceholder("Búsqueda"); criteriaTF.setPrefixComponent(VaadinIcon.SEARCH.create());
        criteriaTF.addValueChangeListener(e -> {
            if (e.getValue().length() > 3) {
                var items = personaService.allPersons(e.getValue(), null, 0, MAX_PERSONS).getContent();

                if (items.size() >= MAX_PERSONS) {
                    var notification = Notification.show("Se necesita especificar un criterio de selección mas restrictivo para leer todas las personas");
                    notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                }
                selectedPersonsLB.setItems(items);
            }
        });
        addAllPersons.setMinWidth("140px");  addOnePerson.setMinWidth("140px");
        layout.add(criteriaTF, new HorizontalLayout(addAllPersons, addOnePerson));
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
        ComboBox<String>    relationTypeCB = new ComboBox<>();
        ComboBox<String>    relationNameCB = new ComboBox("Relación:");
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
                        var tipo = edge.getTitle();
                        var nombre = edge.getLabel();

                        // the from is the supplier invert relationship
                        relacionService.addRelacion(searchPersonId(edge.getTo()),
                                                    searchPersonId(edge.getFrom()), tipo, nombre);
                    });
            // Second delete edges
            deletedEdges.stream().filter(edge -> !addedEdges.contains(edge))
                    .forEach( edge-> {
                        // the from is the supplier invert relationship
                        relacionService.deleteRelacion(searchPersonId(edge.getTo()),
                                                       searchPersonId(edge.getFrom()));
                    });
            Notification.show("Se almacenaron todos los cambios");
            clearGraph();
            selectedPersonsLB.clear();
        });

        insert.setEnabled(false);
        relationTypeCB.setItems(RELATED, RECOMMEND, FRIEND);
        relationTypeCB.addValueChangeListener(e -> {
            if (e.getValue().equals(RELATED))
                relationNameCB.setItems("Padre", "Hijo", "Hermano", "Político", "Abuelo","Nieto","Tio");
            else if (e.getValue().equals(RELATED))
                relationNameCB.setItems("Peer", "Jefe", "Conocido", "Profesional");
            else
                relationNameCB.setItems("Conocido", "Amigo", "Muy amigo");
            graph.setDefaultTitleEdge(e.getValue());
        });
        relationNameCB.addValueChangeListener(e -> {
            insert.setEnabled(e.getValue() != null && !e.getValue().isEmpty());
            graph.setDefaultLabelEdge(e.getValue());
        });

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        layout.add(relationTypeCB, relationNameCB, new HorizontalLayout(insert, undo, deleteEdge, save));
        layout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1), // Use one column by default
                // Use two columns, if layout's width exceeds 500px
                new FormLayout.ResponsiveStep("300px", 2)); // Use two columns, if layout's width exceeds 500px

        return layout;
    }

    private String searchPersonId(String nodeId) {
        try {
            var person = hashMapNodes.get(nodeId);

            if (person != null) return person.get_id(); // found it
        } catch (NumberFormatException e) {
            // do nothing is tha same as not found since it is a new node
        }
        // maybe it is and added Node.
        return addedNodes.get(nodeId);
    }

    private VerticalLayout personsView() {
        selectedPersonsLB = new MultiSelectListBox<>();
        selectedPersonsLB.setRenderer(new ComponentRenderer<>(person -> new Text(person.getFullName())));
        selectedPersonsLB.setHeight("300px");
        selectedPersonsLB.addSelectionListener( e -> {
                                                addAllPersons.setEnabled(!e.getValue().isEmpty());
                                                addOnePerson.setEnabled(graphGenerated && !e.getValue().isEmpty());
                                            });

        TextArea notice = new TextArea();

        notice.setWidthFull();
        notice.setLabel("Sobre relaciones de parentesco");
        notice.setMaxHeight("200px");
        notice.setValue(
                        "'Funcionalidad adicional': cuando se inserta un relación de parentesco entre dos personas el sistema " +
                        "puede deducir nuevas relaciones con algoritmos de 'knowledge representation'.  Por ejemplo: si " +
                        "una persona A tiene la relación de 'hijo' la relación inversa es la relación de 'padre', o bien " +
                        "si dos personas tienen una relación con el mismo 'padre' el sistema puede deducir que son  " +
                        " hermanos y crear una relación nueva'.\n" +
                        "Reglas similares de Inteligencia Artificial pueden ser aplicadas con las relaciones de " +
                        "recomendaciones (e.g., 'similaridad') o amistad.\n" +
                        "Esto dependerá de los requerimientos de la BUP de ACME.");

        var layout = new VerticalLayout(new Text("Personas para seleccionar"), selectedPersonsLB, notice);

        layout.setMinWidth("300px");

        return layout;
    }

    private void addGraphNodes(Set<Persona> newPersons) {
        if (newPersons != null && graphGenerated) {
            newPersons.stream()
                      .filter(person -> hashMapNodes.get(person.get_id()) == null)
                      .forEach(newPerson -> {
                          String fullName = newPerson.getApellidoPaterno() + newPerson.getNombre();
                          String label;

                          if (fullName.length() > 10)
                            label = fullName.substring(0, 7) + "...";
                          else
                            label = fullName;
                          Notification.show("Hacer 'click' en el grafo para añadir a la persona");
                          personToBeAdded = newPerson;
                          graph.setDefaultTitleNode(label);
                          graph.addNodeMode();
            });
            selectedPersonsLB.deselectAll();
            selectedPerson = null;
        }
    }

    private void createGraph(Set<Persona> newPersons, Collection<Relacion> relaciones ) {
        if (newPersons != null) {
            if (!graphGenerated) {
                hashMapNodes.clear();
                addedNodes.clear();
            }
            newPersons.stream()
                      .forEach( newPerson -> {
                            if (hashMapNodes.get(newPerson.get_id()) == null) {
                                String nodeId = newPerson.get_id();
                                String fullName = newPerson.getApellidoPaterno() + newPerson.getNombre();
                                String label;

                                if (fullName.length() > 10)
                                    label = fullName.substring(0, 7) + "...";
                                else
                                    label = fullName;

                                var newNode = new Node(nodeId, label);

                                if (!newPerson.getActivo())
                                    newNode.setColor(NO_ACTIVE_COLOR);
                                nodes.add(newNode);
                                hashMapNodes.put(newPerson.get_id(), newPerson);
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
        if (relaciones != null) {
            relaciones.forEach( relationship -> {
                var needEdge = hashMapNodes.get(relationship.getTo().get_id());

                if (needEdge == null) {
                    String nodeId = relationship.getTo().get_id();
                    String fullName = relationship.getTo().getApellidoPaterno() + relationship.getTo().getNombre();
                    String label;

                    if (fullName.length() > 10)
                        label = fullName.substring(0, 7) + "...";
                    else
                        label = fullName;
                    Node newNode = new Node(nodeId, label);

                    newNode.setColor(GRAY_COLOR);
                    nodes.add(newNode);
                    hashMapNodes.put(relationship.getTo().get_id(), relationship.getTo());
                }
                var newEdge = new Edge(selectedPerson.get_id(), relationship.getTo().get_id());

                newEdge.setId(idCounter.incrementAndGet() + "");
                newEdge.setColor("#00a86b");
                newEdge.setTitle(relationship.getTipo());
                newEdge.setLabel(relationship.getNombre());
                edges.add(newEdge);
            });
            dataProviderEdges = new ListDataProvider<>(edges);
            graph.setEdgesDataProvider(dataProviderEdges);
            dataProviderNodes = new ListDataProvider<>(nodes);
            graph.setNodesDataProvider(dataProviderNodes);
        }
        personToBeAdded = null;
        selectedPerson = null;
    }

    private void registerListeners() {
        registrationSelect = graph.addSelectListener(ls -> selection = ls);
        registrationDoubleClick = graph.addDoubleClickListener(ls -> {
            if (!addedEdges.isEmpty() || !deletedEdges.isEmpty())
                Notification.show("El grafo ha sido editado, no se puede expander el nodo. Salvar cambios o re-generar el grafo");
            else
                try {
                    selectedPerson = hashMapNodes.get(ls.getParams()
                                                .getArray("nodes")
                                                .getString(0));
                    if (selectedPerson != null) {
                        originPersonTF.setValue(selectedPerson.getNombre() + selectedPerson.getApellidoPaterno());
                        if (!expandedNodes.contains(selectedPerson.get_id())) {
                            expandedNodes.add(selectedPerson.get_id());
                            createGraph(null, selectedPerson.getRelaciones());
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
        registrationAddNode = graph.addAddNodeListener(ls -> {
            if (personToBeAdded != null) {
                hashMapNodes.put(personToBeAdded.get_id(), personToBeAdded);
                addedNodes.put(ls.getNewNode().getId(), personToBeAdded.get_id());
                personToBeAdded = null;
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
        selectedPerson = null;
        selection = null;
        originPersonTF.setValue("");
    }

}
