const {tenant_g, loan_agent_g, risk_radar_g, real_estate_company_g, dcc_g, da_g, lineage_g} = require('../service/gremlin');
const {checkNumber} = require('../utils/tool')

const controller = {
    getProperties: async ctx => {
        let g = tenant_g;

        if (ctx.params.graph === "loan_agent") {
            g = loan_agent_g;
        } else if(ctx.params.graph === "risk_radar") {
            g = risk_radar_g;
        } else if(ctx.params.graph === "real_estate_company") {
            g = real_estate_company_g;
        } else if(ctx.params.graph === "dcc") {
            g = dcc_g;
        } else if(ctx.params.graph === "da") {
            g = da_g;
        } else if(ctx.params.graph === "lineage") {
            g = lineage_g;
        }

        ctx.body = await g.getProperties(ctx.params.label);
    },

    search: async ctx => {

        ctx.request.socket.setTimeout(10 * 60 * 1000);

        let g = tenant_g;

        if (ctx.params.graph === "loan_agent") {
            g = loan_agent_g;
        } else if(ctx.params.graph === "risk_radar") {
            g = risk_radar_g;
        } else if(ctx.params.graph === "real_estate_company") {
            g = real_estate_company_g;
        } else if(ctx.params.graph === "dcc") {
            g = dcc_g;
        } else if(ctx.params.graph === "da") {
            g = da_g;
        } else if(ctx.params.graph === "lineage") {
            g = lineage_g;
        }

        const label = ctx.request.body.label;

        let predicate = null;
        let property = null;
        let value = null;
        let edgeValue = null;
        let limit = null;
        let start_idx = null;

        if (ctx.request.body.predicate) {
            predicate = ctx.request.body.predicate;
        }

        if (ctx.request.body.property) {
            property = ctx.request.body.property;
        }

        if (ctx.request.body.value) {
            value = ctx.request.body.value;
            if (checkNumber(value)) {
                value = Number(value);
            }
        }

        let edge_info = {};

        if (ctx.request.body.edge_name) {
            edge_info.edge_name = ctx.request.body.edge_name;
        }

        if (ctx.request.body.edge_degree_type) {
            edge_info.edge_degree_type = ctx.request.body.edge_degree_type;
        }

        if (ctx.request.body.edge_degree_predicate) {
            edge_info.edge_degree_predicate = ctx.request.body.edge_degree_predicate;
        }

        if (ctx.request.body.edge_degree_value) {
            edge_info.edge_degree_value = parseInt(ctx.request.body.edge_degree_value);
        }

        if (ctx.request.body.edge_property) {
            edge_info.edge_property = ctx.request.body.edge_property;
        }

        if (ctx.request.body.edge_predicate) {
            edge_info.edge_predicate = ctx.request.body.edge_predicate;
        }

        if (ctx.request.body.edge_value) {
            edgeValue = ctx.request.body.edge_value;
            if (checkNumber(edgeValue)) {
                edgeValue = Number(edgeValue);
            }

            edge_info.edge_value = edgeValue;
        }

        if (ctx.request.body.limit) {
            limit = parseInt(ctx.request.body.limit);
        }

        if (ctx.request.body.limit) {
            limit = parseInt(ctx.request.body.limit);
        }

        let nodeList = await g.searchNodes(label, predicate, property, value, edge_info, ctx.request.body.start_idx, limit);
        let edgeList = [];
        /*
        if (nodeList.length > 0) {
            edgeList = await g.searchEdges(label, predicate, property, value, limit);
        }
         */

        ctx.body = [nodeList, edgeList]
    },

    query: async ctx => {
        let g = tenant_g;

        if (ctx.params.graph === "loan_agent") {
            g = loan_agent_g;
        } else if(ctx.params.graph === "risk_radar") {
            g = risk_radar_g;
        } else if(ctx.params.graph === "real_estate_company") {
            g = real_estate_company_g;
        } else if(ctx.params.graph === "dcc") {
            g = dcc_g;
        } else if(ctx.params.graph === "da") {
            g = da_g;
        } else if(ctx.params.graph === "lineage") {
            g = lineage_g;
        }

        let node_id = null;
        let limit = null;
        let edge = null;

        let edge_info = {};
        let edgeValue = null;

        if (ctx.request.body.node_id) {
            node_id = ctx.request.body.node_id;
        }

        if (ctx.request.body.edge_name) {
            edge_info.edge_name = ctx.request.body.edge_name;
        }

        if (ctx.request.body.edge_property) {
            edge_info.edge_property = ctx.request.body.edge_property;
        }

        if (ctx.request.body.edge_predicate) {
            edge_info.edge_predicate = ctx.request.body.edge_predicate;
        }

        if (ctx.request.body.edge_value) {
            edgeValue = ctx.request.body.edge_value;
            if (checkNumber(edgeValue)) {
                edgeValue = Number(edgeValue);
            }

            edge_info.edge_value = edgeValue;
        }

        if (ctx.request.body.limit) {
            limit = parseInt(ctx.request.body.limit);
        }

        let nodeList = await g.getAdjacentNodes(node_id, edge_info, limit);
        let edgeList = [];
        if (nodeList.length > 0) {
            edgeList = await g.getAdjacentEdges(node_id, edge_info);
        }

        ctx.body = [nodeList, edgeList]
    }

};

module.exports = controller;