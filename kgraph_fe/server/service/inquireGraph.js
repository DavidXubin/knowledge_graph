const { fetch } = require('../utils/fetch');
const config = require('../config');
const {tenant_g, loan_agent_g, risk_radar_g, real_estate_company_g, dcc_g, da_g, lineage_g} = require('./gremlin');


const getDB = ctx => {
    const  body  = ctx.request.body;
    let params = {
        user: body.user,
    };
    return fetch(ctx, { path: '/graph_names', method: 'GET', body: params }, config.graphService);
};


const getGraph = graph => {

    let g = tenant_g;

    let p = graph.lastIndexOf("_graph");
    if (p > 0){
        graph = graph.substring(0, p)
    }

    if (graph === "loan_agent") {
        g = loan_agent_g;
    } else if(graph === "risk_radar") {
        g = risk_radar_g;
    } else if(graph === "real_estate_company") {
        g = real_estate_company_g;
    } else if(graph === "dcc") {
        g = dcc_g;
    } else if(graph === "da") {
        g = da_g;
    } else if(graph === "lineage") {
        g = lineage_g;
    }

    return g
};

 const getRelation = async ctx => {

    const g = getGraph(ctx.request.body.graph);

    return await g.getGraphRelationNames();
};

 const inquireGraph = async ctx => {
    let params = {};

    params["label"] = ctx.request.body.relationName;
    params["start_idx"] = ctx.request.body.startIndex;
    params["limit"] = ctx.request.body.pageSize;
    params["tabKey"] = ctx.request.body.tabKey;

    const g = getGraph(ctx.request.body.graphName);

    if (params["tabKey"] === "entity") {
        return await g.searchNodeGraphService(params);
    } else if (params["tabKey"] === "edge") {
        return await g.searchEdgeGraphService(params);
    }
};

const inquireGraphRelationCount = async ctx => {
    let params = {};

    params["relationType"] = ctx.request.body.tabKey;
    params["label"] = ctx.request.body.relationName;

    const g = getGraph(ctx.request.body.graphName);

    return await g.getGraphRelationCount(params);
};

const loadHistoryQueries = ctx => {
    const  body  = ctx.request.body;
    let params = {
        user: body.user,
        graph: body.graph,
        days: body.days
    };

    return fetch(ctx, { path: '/graph/history_query', method: 'GET', body: params }, config.graphService);
};


const addQuery = ctx => {
    const  body  = ctx.request.body;
    let params = {
        user: body.user,
        graph: body.graph,
        content: body.content
    };

    return fetch(ctx, { path: '/graph/add_query', method: 'POST', body: params }, config.graphService);
};


const customizedQuery = async ctx => {
    const  body  = ctx.request.body;
    let params = {
        user: body.user,
        graph: body.graph,
        content: body.content
    };

    const g = getGraph(params.graph);

    return await g.customizedGraphQuery(params);
};


module.exports = {
    getDB,
    getRelation,
    inquireGraph,
    inquireGraphRelationCount,
    loadHistoryQueries,
    addQuery,
    customizedQuery
};
