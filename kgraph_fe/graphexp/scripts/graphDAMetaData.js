function DAGraph() {
}

DAGraph.prototype = new GraphMetaData();

DAGraph.prototype.populate_graph_nodes = function() {
    let graph_nodes = new GraphDictionary();

    graph_nodes.add("应用", [
        new Property("系统ID", "composite"),
        new Property("系统名称"),
        new Property("系统CODE"),
        new Property("tag")
    ]);

    graph_nodes.add("接口", [
        new Property("接口ID", "composite"),
        new Property("接口名称"),
        new Property("接口CODE"),
        new Property("tag")
    ]);

    graph_nodes.add("租户", [
        new Property("租户ID", "composite"),
        new Property("租户名称"),
        new Property("租户CODE"),
        new Property("tag")
    ]);

    return graph_nodes;
};

DAGraph.prototype.populate_graph_edges = function() {
    let graph_edges = new GraphDictionary();

    graph_edges.add("调用", [
        new Property("总调用次数", "mixed", "number"),
        new Property("平均调用延时", "mixed", "number")
    ]);

    return graph_edges;
};

DAGraph.prototype.populate_graph_relations = function() {
    let graph_relations = new GraphDictionary();

    graph_relations.add("应用", ['调用', '使用']);
    graph_relations.add("接口", ['调用']);
    graph_relations.add("租户", ['使用']);

    return graph_relations;
};

DAGraph.prototype.populate_graph_display_properties = function() {
    let display_properties = new GraphDictionary();

    display_properties.add("应用", ["系统名称"]);
    display_properties.add("接口", ["接口名称"]);
    display_properties.add("租户", ["租户名称"]);

    return display_properties;
};