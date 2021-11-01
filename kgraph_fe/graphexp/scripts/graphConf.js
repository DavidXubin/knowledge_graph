
// configuration for the graph database access

const server_url = "http://local.dev.bkjk-inc.com:8013";

//const server_port = "127.0.0.1";

//const server_port = 8013;

// Time out for the REST protocol. Increase it if the graphDB is slow.
const REST_TIMEOUT = 2000000
// TODO: configuration for the secure server

// limit number of nodes and edges to query for graph info
// (avoid overwhelming the server for large graphs)
const limit_graphinfo_request = 50

// Graph configuration
const default_nb_of_layers = 5;
const node_limit_per_request = "50";

// Simulation
const force_strength = -600;
const link_strength = 0.2;
const force_x_strength = 0.1;
const force_y_strength = 0.1;

// Nodes
const default_node_size = 15;
const default_stroke_width = 2;
const default_node_color = "#80E810";
const active_node_margin = 6;
const active_node_margin_opacity = 0.3;
// Node position info in the DB
// Replace by the key storing the node position inside the DB if any
const node_position_x = 'graphexpx'
const node_position_y = 'graphexpy'

// Edges
const default_edge_stroke_width = 3;
const default_edge_color = "#CCC";
const edge_label_color = "#111";
// Choose between curved (true) and straight edges (false). 
// If set to false, multiple edges between 2 nodes will all be straight and overlap.
const use_curved_edges = true;