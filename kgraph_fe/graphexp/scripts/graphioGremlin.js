/*
Copyright 2017 Benjamin RICAUD

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

// Interface between the visualization and the Gremlin server.
var traversal_source = getUrlParameter();
if (traversal_source == null) {
    traversal_source = "tenant";
}

var g_active_node = null;

var graph_color_inited = false;

var graphioGremlin = (function(){
	"use strict";

	var _node_properties = [];
	var _edge_properties = [];

	function get_node_properties(){
		return _node_properties;
	}
	function get_edge_properties(){
		return _edge_properties;
	}


	function get_graph_info(){

		let graphName = traversal_source;

		let label = g_graph_selection.node_label;
		if (g_active_node) {
			label = g_active_node.label;
		}

		send_to_server("gremlin_get_property/" + graphName + "/" + label,
			'graphInfo', null, null, "")
	}


	function search_query(start_idx, callback) {

		let input_string = $('#search_value').val();
		let input_field = $('#search_field').val();
		let label_field = $('#label_field').val();
		let limit_field = $('#limit_field').val();
		let search_type = g_graph_selection.predicate;

		let edge_input_string = $('#edge_search_value').val();
		let edge_input_field = $('#edge_search_field').val();

		let graphName = traversal_source;

		const gremlin_query = "gremlin_search/" + graphName;

		limit_field = limit_field.trim();
		if (limit_field === "") {
			limit_field = node_limit_per_request;
		}

		const query_data = {
			"label": label_field,
			"predicate": search_type,
			"property": input_field,
			"value": input_string.trim(),
            "start_idx": start_idx,
			"limit": limit_field,
            "edge_name": g_graph_selection.edge_label,
            "edge_degree_type": g_graph_selection.edge_degree_type,
            "edge_degree_predicate": g_graph_selection.edge_degree_predicate,
            "edge_degree_value": $('#degree_search_value').val().trim(),
			"edge_property": edge_input_field,
			"edge_predicate": g_graph_selection.edge_property_predicate,
			"edge_value": edge_input_string.trim()
		};

		send_to_server(gremlin_query,'search', query_data, null, "", callback);
	}


	function click_query(node) {
		// Query sent to the server when a node is clicked
        let edge_name = g_graph_selection.edge_label;
        let limit_field = $('#limit_field').val().trim();
		if (limit_field === "") {
			limit_field = node_limit_per_request;
		}

		let edge_input_string = $('#edge_search_value').val();
		let edge_input_field = $('#edge_search_field').val();

		// 'inject' is necessary in case of an isolated node ('both' would lead to an empty answer)
		console.log('Query for the node and its neighbors');

		// while busy, show we're doing something in the messageArea.
		$('#messageArea').html('<h3>（数据加载中...）</h3>');
		const message = "<p>Query ID: "+ node.id +"</p>";

		let graphName = traversal_source;

		const gremlin_query = "gremlin_query/" + graphName;

		const query_data = {
			"node_id": node.id,
			"edge_name": edge_name,
			"edge_property": edge_input_field,
			"edge_predicate": g_graph_selection.edge_property_predicate,
			"edge_value": edge_input_string.trim(),
			"limit": limit_field
		};

		send_to_server(gremlin_query,'click', query_data, node, message);
	}

	function send_to_server(gremlin_query, query_type, query_data, active_node, message, callback){

		run_ajax_request(gremlin_query, server_url, query_type, query_data, active_node, message, callback);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////
	// AJAX request for the REST API
	////////////////////////////////////////////////////////////////////////////////////////////////
	function run_ajax_request(gremlin_query, server_url, query_type, query_data, active_node, message, callback){
		// while busy, show we're doing something in the messageArea.
		$('#messageArea').html('<h3>（数据加载中...）</h3>');

		// Get the data from the server
		$.ajax({
			type: "POST",
			accept: "application/json",
			//contentType:"application/json; charset=utf-8",
			url: server_url + "/" + gremlin_query,
			data: query_data,
			timeout: REST_TIMEOUT,
			success: function(data, textStatus, jqXHR){
				if(callback) {
                    callback(data);
                }

				handle_server_answer(data, query_type, active_node, message);
			},
			error: function(result, status, error){
				console.log("Connection failed: " + status);

				$('#outputArea').html('Message: ' + status + ', ' + error);
				$('#messageArea').html('');
			}
		});
	}

	function handle_server_answer(data, query_type, active_node, message){

		data = graphson3to1(data);

		if (query_type === 'graphInfo'){
			_node_properties = make_properties_list(data);
			change_nav_bar(_node_properties);
			display_properties_bar(_node_properties,'nodes','节点属性:');
			display_color_choice(_node_properties,'nodes','节点颜色依据:');
		} else {
			if (query_type !== 'click' && query_type !== 'search') {
				return;
			}

			g_active_node = active_node;

			graph_viz.refresh_data(arrange_datav3(data), query_type ==='click' ? 0: 1,
				active_node ? active_node.id: null);
			if ((query_type === 'search' || query_type === 'click') && !graph_color_inited) {

				init_graph_colors();
				if (query_type === 'click'){
					graph_color_inited = true;
				}
			}

			if (query_type === 'search' || query_type === 'click') {
				show_labels();
			}
		}

		$('#outputArea').html(message);
		$('#messageArea').html('');
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	function make_properties_list(data){

		return data.split(',');
	}

	///////////////////////////////////////////////////
	function idIndex(list,elem) {
	  // find the element in list with id equal to elem
	  // return its index or null if there is no
	  for (var i=0; i < list.length; i++) {
		if (list[i].id === elem) return i;
	  }
	  return null;
	}  

	/////////////////////////////////////////////////////////////

	function arrange_datav3(data) {
		// Extract node and edges from the data returned for 'search' and 'click' request
		// Create the graph object
		var nodes=[], links=[];
		if(data != null) {
			for (var key in data){
				if(data[key] != null) {
					data[key].forEach(function (item) {
						if (!("inV" in item) && idIndex(nodes, item.id) == null){ // if vertex and not already in the list
							item.type = "vertex";
							nodes.push(extract_infov3(item));
						}
						if (("inV" in item) && idIndex(links, item.id) == null){
							item.type = "edge";
							links.push(extract_infov3(item));
						}
					});
				}
			}
		}
	  	return {nodes:nodes, links:links};
	}

    function extract_infov3(data) {
        var data_dic = { id: data.id, label: data.label, type: data.type, properties: {} };
        var prop_dic = {};

		// NOT VERSION 3.4
		prop_dic = data.properties;

		for (var key in prop_dic) {
			if (prop_dic.hasOwnProperty(key)) {
				if (data.type === 'vertex') {
					var property = prop_dic[key];
					property['summary'] = get_vertex_prop_in_list(prop_dic[key]).toString();
				} else {
					var property = prop_dic[key]['value'];
				}
				//property = property.toString();
				data_dic.properties[key] = property;
				// If  a node position is defined in the DB, the node will be positioned accordingly
				// a value in fx and/or fy tells D3js to fix the position at this value in the layout
				if (key === node_position_x) {
					data_dic.fx = prop_dic[node_position_x]['0']['value'];
				}
				if (key === node_position_y) {
					data_dic.fy = prop_dic[node_position_y]['0']['value'];
				}
			}
		}

        if (data.type === "edge"){
            data_dic.source = data.outV;
            data_dic.target = data.inV;
            if (data.id !== null && typeof data.id === 'object'){
                console.log('Warning the edge id is an object')
                if ("relationId" in data.id){
                    data_dic.id = data.id.relationId;
                }
            }
        }
        return data_dic;
	}

	function get_vertex_prop_in_list(vertexProperty){
		var prop_value_list = [];
		for (var key in vertexProperty){
			prop_value_list.push(vertexProperty[key]['value']);
		}
		return prop_value_list;
	}

	function graphson3to1(data){
		// Convert data from graphSON v2 format to graphSON v1
		if (!(Array.isArray(data) || ((typeof data === "object") && (data !== null)) )) return data;
		if ('@type' in data) {
			if (data['@type']==='g:List'){
				data = data['@value'];
				return graphson3to1(data);
			} else if (data['@type'] === 'g:Set'){
				data = data['@value'];
				return data;
			} else if(data['@type'] === 'g:Map'){
				var data_tmp = {};
				for (var i=0;i<data['@value'].length;i+=2){
					var data_key = data['@value'][i];
					if( (typeof data_key === "object") && (data_key !== null) ) data_key = graphson3to1(data_key);
					//console.log(data_key);
					if (Array.isArray(data_key)) {
						data_key = JSON.stringify(data_key).replace(/"/g,' ');
					}
					data_tmp[data_key] = graphson3to1(data['@value'][i+1]);
				}
				data = data_tmp;
				return data;
			} else {
				data = data['@value'];
				if ( (typeof data === "object") && (data !== null) ) data = graphson3to1(data);
				return data;
			}
		} else if (Array.isArray(data) || ((typeof data === "object") && (data !== null)) ){
			for (var key in data){
				data[key] = graphson3to1(data[key]);
			}
			return data;
		}
		return data;
	}

	return {
		get_node_properties : get_node_properties,
		get_edge_properties : get_edge_properties,
		get_graph_info : get_graph_info,
		search_query : search_query,
		click_query : click_query,
		send_to_server : send_to_server
	}
})();
