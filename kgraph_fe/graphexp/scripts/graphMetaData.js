function Property(name, index_type="mixed", data_type="string") {
    this.name = name;
    this.index_type = index_type;
    this.data_type = data_type;
}

function GraphMetaData() {
    this.populate_graph_nodes = populate_graph_nodes;
    this.populate_predicates = populate_predicates;
    this.populate_graph_edges = populate_graph_edges;
    this.populate_graph_relations = populate_graph_relations;
    this.populate_graph_display_properties = populate_graph_display_properties
}

function populate_graph_nodes() {
    return new GraphDictionary();
}

function populate_predicates() {

    var graph_predicates = new GraphDictionary();

    graph_predicates.add("等于", "eq");
    graph_predicates.add("包含", "textContains");
    graph_predicates.add("前缀包含", "textContainsPrefix");
    graph_predicates.add("正则匹配包含", "textContainsRegex");
    graph_predicates.add("模糊匹配包含", "textContainsFuzzy");
    graph_predicates.add ("==", "eq");
    graph_predicates.add("!=", "neq");
    graph_predicates.add(">", "gt");
    graph_predicates.add(">=", "gte");
    graph_predicates.add("<", "lt");
    graph_predicates.add("<=", "lte");

    return graph_predicates;
}

function populate_graph_edges() {
    return new GraphDictionary();
}

function populate_graph_relations() {
    return new GraphDictionary();
}

function populate_graph_display_properties() {
    return new GraphDictionary();
}

