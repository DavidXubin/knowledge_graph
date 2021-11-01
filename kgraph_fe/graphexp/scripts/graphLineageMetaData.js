function LineageGraph() {
}

LineageGraph.prototype = new GraphMetaData();

LineageGraph.prototype.populate_graph_nodes = function() {
    let graph_nodes = new GraphDictionary();

    graph_nodes.add("table", [
        new Property("table_id", "composite"),
        new Property("table"),
        new Property("update_time"),
        new Property("user_id"),
        new Property("tag")
    ]);

    graph_nodes.add("report", [
        new Property("table_id", "composite"),
        new Property("report_full_name"),
        new Property("report_name"),
        new Property("tag")
    ]);

    return graph_nodes;
};

LineageGraph.prototype.populate_graph_relations = function() {
    let graph_relations = new GraphDictionary();

    graph_relations.add("table", ['lineage', 'reported_by']);
    graph_relations.add("report", ['reported_by']);

    return graph_relations;
};

LineageGraph.prototype.populate_graph_edges = function() {
    let graph_edges = new GraphDictionary();

    graph_edges.add("lineage", [
        new Property("sql_type")
        //,new Property("job_id")
    ]);

    return graph_edges;
};

LineageGraph.prototype.populate_graph_display_properties = function() {
    let display_properties = new GraphDictionary();

    display_properties.add("table", ["table"]);
    display_properties.add("report", ["report_name"]);
    display_properties.add("lineage", ["job_id"]);

    return display_properties;
};
