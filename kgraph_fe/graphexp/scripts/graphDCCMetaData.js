function DCCGraph() {
}

DCCGraph.prototype = new GraphMetaData();

DCCGraph.prototype.populate_graph_nodes = function() {
    let graph_nodes = new GraphDictionary();

    graph_nodes.add("系统", [
        new Property("标识号", "composite"),
        new Property("名称"),
        new Property("tag")
    ]);

    graph_nodes.add("接口", [
        new Property("标识号", "composite"),
        new Property("名称"),
        new Property("tag")
    ]);

    return graph_nodes;
};

DCCGraph.prototype.populate_graph_edges = function() {
    let graph_edges = new GraphDictionary();

    graph_edges.add("调用", [
        new Property("调用次数", "mixed", "number")
    ]);

    return graph_edges;
};

DCCGraph.prototype.populate_graph_relations = function() {
    let graph_relations = new GraphDictionary();

    graph_relations.add("系统", ['调用']);
    graph_relations.add("接口", ['调用']);

    return graph_relations;
};

DCCGraph.prototype.populate_graph_display_properties = function() {
    let display_properties = new GraphDictionary();

    display_properties.add("系统", ["名称"]);
    display_properties.add("接口", ["名称"]);

    return display_properties;
};