var g_graph_metadata = null;

if (traversal_source === "tenant") {
    g_graph_metadata = new TenantGraph();
} else if (traversal_source === "risk_radar") {
    g_graph_metadata = new RiskRadarGraph();
} else if (traversal_source === "loan_agent") {
    g_graph_metadata = new LoanAgentGraph();
} else if (traversal_source === "real_estate_company") {
    g_graph_metadata = new RealEstateCompanyGraph();
} else if (traversal_source === "dcc") {
    g_graph_metadata = new DCCGraph();
} else if (traversal_source === "da") {
    g_graph_metadata = new DAGraph();
} else if (traversal_source === "lineage") {
    g_graph_metadata = new LineageGraph();
}

let g_graph_nodes = g_graph_metadata.populate_graph_nodes();
let g_graph_edges = g_graph_metadata.populate_graph_edges();
let g_graph_relations = g_graph_metadata.populate_graph_relations();
let g_graph_predicates = g_graph_metadata.populate_predicates();
let g_graph_display_properties = g_graph_metadata.populate_graph_display_properties();
let g_flatten_graph_display_properties = get_graph_all_display_properties();
let g_graph_search_result_start_index = 0;

var g_graph_selection = {
    node_label: "",
    node_property_name: "",
    predicate: "eq",
    edge_label: "",
    edge_property_name: "",
    edge_property_predicate: "",
    edge_degree_type: "both",
    edge_degree_predicate: ">=",
    _node_property: new Property(""),
    _edge_property: new Property(""),

    get node_property() {
        return this._node_property[0];
    },

    set node_property(node_property) {
        this._node_property = Object.assign({}, node_property);
    },

    get edge_property() {
        return this._edge_property[0];
    },

    set edge_property(edge_property) {
        this._edge_property = Object.assign({}, edge_property);
    }
};

if (traversal_source === "tenant") {
    g_graph_selection.node_label = "商户";
    g_graph_selection.node_property_name = "商户id";
    g_graph_selection.node_property = new Property("商户id", "composite");
} else if (traversal_source === "risk_radar") {
    g_graph_selection.node_label = "金融单";
    g_graph_selection.node_property_name = "订单编号";
    g_graph_selection.node_property = new Property("订单编号", "composite");
} else if (traversal_source === "loan_agent") {
    g_graph_selection.node_label = "借款用户";
    g_graph_selection.node_property_name = "身份证号";
    g_graph_selection.node_property = new Property("身份证号", "composite");
} else if (traversal_source === "real_estate_company") {
    g_graph_selection.node_label = "开发商";
    g_graph_selection.node_property_name = "公司名";
    g_graph_selection.node_property = new Property("公司名", "composite");
} else if (traversal_source === "dcc") {
    g_graph_selection.node_label = "系统";
    g_graph_selection.node_property_name = "标识号";
    g_graph_selection.node_property = new Property("标识号", "composite");
} else if (traversal_source === "da") {
    g_graph_selection.node_label = "应用";
    g_graph_selection.node_property_name = "标识号";
    g_graph_selection.node_property = new Property("标识号", "composite");
} else if (traversal_source === "lineage") {
    g_graph_selection.node_label = "table";
    g_graph_selection.node_property_name = "table_id";
    g_graph_selection.node_property = new Property("table_id", "composite");
}

function extract_property_name(property, index, array) {
    return property.name;
}

function filter_property_name(property, index, array) {
    return property.name === g_graph_selection.node_property_name;
}

function filter_edge_property_name(property, index, array) {
    return property.name === g_graph_selection.edge_property_name;
}


function display_graph_name() {

    if (traversal_source === "tenant") {
        d3.select("#graph_name").text("商户图谱");
    } else if (traversal_source === "risk_radar") {
        d3.select("#graph_name").text("信用分风险雷达");
    } else if (traversal_source === "loan_agent") {
        d3.select("#graph_name").text("贝用金图谱");
    } else if (traversal_source === "real_estate_company") {
        d3.select("#graph_name").text("开发商投资关系图谱");
    } else if (traversal_source === "dcc") {
        d3.select("#graph_name").text("DCC关系图谱");
    } else if (traversal_source === "da") {
        d3.select("#graph_name").text("DA关系图谱");
    } else if (traversal_source === "lineage") {
        d3.select("#graph_name").text("血缘关系图谱");
    }
}

function display_node_label_choice(){
    //prop_list = ['none','label'].concat(prop_list);
    var label_list = g_graph_nodes.getKeys();
    var nav_bar = d3.select("#label_field");

    nav_bar.attr("onchange","display_node_property_choice(this.value)")
        .selectAll("option")
        .data(label_list).enter()
        .append("option")
        .text(function (d) { return d; });

    display_node_property_choice(label_list[0]);
}

function display_node_property_choice(value) {

    console.log('Select nodel label:' + value);

    g_graph_selection.node_label = value;

    display_edge_label_choice();

    var property_list = g_graph_nodes.find(value).map(extract_property_name);

    var nav_bar = d3.select("#search_field");

    nav_bar.selectAll("option").remove();

    nav_bar.attr("onchange","change_node_property(this.value)")
        .selectAll("option")
        .data(property_list).enter()
        .append("option")
        .text(function (d) { return d; });

    change_node_property(property_list[0]);
}

function change_node_property(value) {
    g_graph_selection.node_property_name = value;
    g_graph_selection.node_property = g_graph_nodes.find(g_graph_selection.node_label).filter(filter_property_name);

    console.log('Select node property: ' + g_graph_selection.node_property.name);

    var nav_bar = d3.select("#search_type");

    nav_bar.selectAll("option").remove();

    var index_type = g_graph_selection.node_property.index_type;
    var data_type = g_graph_selection.node_property.data_type;

    var search_types = [];
    if (data_type === "number") {
        search_types = ["==", "!=", ">", ">=", "<", "<="]
    } else {
        if (index_type === "composite") {
            search_types = ["等于"]
        } else {
            search_types = ["包含", "前缀包含", "正则匹配包含", "模糊匹配包含"]
        }
    }

    nav_bar.attr("onchange","change_predicate(this.value)")
        .selectAll("option")
        .data(search_types).enter()
        .append("option")
        .text(function (d) { return d; });

    change_predicate(search_types[0]);
}

function change_predicate(value){
    g_graph_selection.predicate = g_graph_predicates.find(value);
    console.log('Select search type: ' + g_graph_selection.predicate);
}

function display_edge_label_choice() {
    var edge_list = ['所有关系'].concat(g_graph_relations.find(g_graph_selection.node_label));

    var nav_bar = d3.select("#edge_label");

    nav_bar.selectAll("option").remove();

    nav_bar.attr("onchange","display_edge_property_choice(this.value)")
        .selectAll("option")
        .data(edge_list).enter()
        .append("option")
        .text(function (d) { return d; });

    display_edge_property_choice(edge_list[0]);
}

function display_edge_property_choice(value){
    g_graph_selection.edge_label = value;
    console.log('Filter edge: '+ g_graph_selection.edge_label);

    let nav_bar = d3.select("#edge_search_field");
    let search_type_nav_bar = d3.select("#edge_search_type");

    if (value === "所有关系") {
        nav_bar.selectAll("option").remove();
        search_type_nav_bar.selectAll("option").remove();

        g_graph_selection.edge_label = "";
        g_graph_selection.edge_property_name = "";
        g_graph_selection.edge_property_predicate = "";
    } else if (g_graph_edges.contains(value)) {
        var property_list = g_graph_edges.find(value).map(extract_property_name);
        console.log("property_list =" + property_list);

        nav_bar.selectAll("option").remove();

        nav_bar.attr("onchange","change_edge_property(this.value)")
            .selectAll("option")
            .data(property_list).enter()
            .append("option")
            .text(function (d) { return d; });

        change_edge_property(property_list[0]);
    } else {
        nav_bar.selectAll("option").remove();
        search_type_nav_bar.selectAll("option").remove();

        g_graph_selection.edge_property_name = "";
        g_graph_selection.edge_property_predicate = "";
    }
}

function change_edge_property(value) {
    g_graph_selection.edge_property_name = value;
    g_graph_selection.edge_property = g_graph_edges.find(g_graph_selection.edge_label).filter(filter_edge_property_name);

    console.log('Select edge property: ' + g_graph_selection.edge_property.name);

    var nav_bar = d3.select("#edge_search_type");

    nav_bar.selectAll("option").remove();

    var index_type = g_graph_selection.edge_property.index_type;
    var data_type = g_graph_selection.edge_property.data_type;

    var search_types = [];
    if (data_type === "number") {
        search_types = ["==", "!=", ">", ">=", "<", "<="]
    } else {
        if (index_type === "composite") {
            search_types = ["等于"]
        } else {
            search_types = ["包含", "前缀包含", "正则匹配包含", "模糊匹配包含"]
        }
    }

    nav_bar.attr("onchange", "change_edge_property_predicate(this.value)")
        .selectAll("option")
        .data(search_types).enter()
        .append("option")
        .text(function (d) { return d; });

    change_edge_property_predicate(search_types[0]);
}

function change_edge_property_predicate(value){
    g_graph_selection.edge_property_predicate = g_graph_predicates.find(value);
    console.log('Select search edge type: ' + g_graph_selection.edge_property_predicate);
}

function display_edge_degree_choice() {
    var degree_list = ["所有边", "出度边", "入度边"];
    var nav_bar = d3.select("#edge_degree_filter");

    nav_bar.attr("onchange","change_edge_degree_type(this.value)")
        .selectAll("option")
        .data(degree_list).enter()
        .append("option")
        .text(function (d) { return d; });
}

function change_edge_degree_type(value) {
    if (value === "出度边") {
        g_graph_selection.edge_degree_type = "out"
    } else if (value === "入度边") {
        g_graph_selection.edge_degree_type = "in"
    } else {
        g_graph_selection.edge_degree_type = "both"
    }
}

function display_edge_degree_predicate_choice() {
    var predicate_list = [">=", ">", "<", "<=", "=="];
    var nav_bar = d3.select("#degree_search_type");

    nav_bar.attr("onchange", "set_edge_degree_predicate(this.value)")
        .selectAll("option")
        .data(predicate_list).enter()
        .append("option")
        .text(function (d) { return d; });

}

function set_edge_degree_predicate(value) {
    g_graph_selection.edge_degree_predicate = value
}

function get_graph_default_display_properties(entity_label) {

    if (g_graph_display_properties.contains(entity_label)) {
        return g_graph_display_properties.find(entity_label);
    } else {
        return [];
    }
}

function get_graph_all_display_properties() {

    const all_keys = g_graph_display_properties.getKeys();
    let all_properties = [];

    for (const idx in all_keys) {
        all_properties = all_properties.concat(g_graph_display_properties.find(all_keys[idx]))
    }

    return Array.from(new Set(all_properties));
}
