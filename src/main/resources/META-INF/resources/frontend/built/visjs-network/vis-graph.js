/**
 * This is to handle vis-network manipulation and call the Vaadin component NetworkDiagram
 * https://github.com/alisardarian/vis-network-vaadin
 *
 * Date: June, 2022
 */

/**
 * A new node has been added
 */
window.Vaadin.Flow.networkDiagramConnector.onManipulationNodeAdded = function(nodeData) {
    const element = document.getElementById("networkDiagram");

    nodeData.label = this.defaultTitleNode;
    nodeData.color="#faa775";
    element.$server.nodeAdded(nodeData.id, nodeData.label);
}

window.Vaadin.Flow.networkDiagramConnector.defaultTitleNode = "";

window.Vaadin.Flow.networkDiagramConnector.setDefaultTitleNode = function(title) {
    this.defaultTitleNode = title;
}

/**
 * A new edge has been added
 */
window.Vaadin.Flow.networkDiagramConnector.onManipulationEdgeAdded = function(edgeData) {
    const element = document.getElementById("networkDiagram");
    const uniqueId = (length= 16) => {
        return parseInt(Math.ceil(Math.random() * Date.now()).toPrecision(length).toString().replace(".", ""))
    }
    // Generate a unique id
    edgeData.id = uniqueId();
    if (edgeData.to.slice(0,1) === 'A') {       // this is for the special case of node Areas
        edgeData.label = this.defaultLabelAreaEdge;
        edgeData.title = "";
    } else {
        edgeData.title = this.defaultTitleEdge;
        edgeData.label = this.defaultLabelEdge;
    }
    edgeData.color="#d32821";
    element.$server.edgeAdded(edgeData.id, edgeData.from, edgeData.to, edgeData.title, edgeData.label);
}

window.Vaadin.Flow.networkDiagramConnector.defaultTitleEdge = "";

window.Vaadin.Flow.networkDiagramConnector.setDefaultTitleEdge = function(title) {
    this.defaultTitleEdge = title;
}

window.Vaadin.Flow.networkDiagramConnector.defaultLabelEdge = "";

window.Vaadin.Flow.networkDiagramConnector.setDefaultLabelEdge = function(label) {
    this.defaultLabelEdge = label;
}

window.Vaadin.Flow.networkDiagramConnector.defaultLabelAreaEdge = "Dirige";

/**
 * An existing edge has been deleted
 */
window.Vaadin.Flow.networkDiagramConnector.onManipulationEdgeDeleted = function(data) {
    const element = document.getElementById("networkDiagram");

    element.$server.edgeDeleted(data["edges"][0]);
}



