const gremlin = require('gremlin');
const config = require('../config');
const kgraphService = require('./kgraphService');

const __ = gremlin.process.statics;

const t = gremlin.process.t;
const c = gremlin.process.column;
const p = gremlin.process.P;
const textP = gremlin.process.TextP;


const predicate_map = new Map([
        ["eq", null],
        ["textContains", textP.containing],
        ["textContainsPrefix", textP.startingWith],
        ["textContainsRegex", textP.containing],
        ["textContainsFuzzy", textP.containing],
        ["neq", p.neq],
        ["gt", p.gt],
        ["gte", p.gte],
        ["lt", p.lt],
        ["lte", p.lte]
    ]
);

const edge_degree_predicate_map = new Map([
        ["==", p.eq],
        [">", p.gt],
        [">=", p.gte],
        ["<", p.lt],
        ["<=", p.lte]
    ]
);

const edge_degree_type_map = new Map([
        ["out", __.out],
        ["in", __.in_],
        ["both", __.both]
    ]
);


function mapToObj(strMap) {
    let obj = {};
    for (let [k,v] of strMap) {
        obj[k] = [{value: v[0]}];
    }
    return obj;
}

function mapEdgePropertyToObj(strMap) {
    let obj = {};
    for (let [k,v] of strMap) {
        obj[k] = {key: k, value: v};
    }
    return obj;
}


function formatProperty(property)  {
    for(let key in property) {
        property[key] = [{value: property[key]}];
    }
    return property;
}

class GremlinClient {

    constructor(graphName) {
        this._graphName = graphName;
        this._token = "";

        const authenticator = new gremlin.driver.auth.PlainTextSaslAuthenticator(config.graphUser, config.graphPassword);

        const connection = new gremlin.driver.DriverRemoteConnection(`ws://${config.graphHost}/gremlin`,
            { authenticator: authenticator, traversalSource: this._graphName });

        this._graph = new gremlin.structure.Graph().traversal().withRemote(connection);
    }

    async getProperties(label, limit=1000) {

        const data = await this._graph.V().hasLabel(label).limit(limit).valueMap().select(c.keys).groupCount().toList();
        return [...data[0].keys()][0].join(",");
    }

    async searchNodes(label, predicate, key, value, edge_info, start_idx=0, limit=50) {

        if (this._token.length === 0) {
            this._token = await this.connectGraphService();
        }

        if (this._token.length === 0) {
            let data = [];

            if (!value) {
                data = await this._graph.V().hasLabel(label);
            } else if (predicate === "eq") {
                data = await this._graph.V().hasLabel(label).has(key, value);
            } else {
                data = await this._graph.V().hasLabel(label).has(key, predicate_map.get(predicate)(value));
            }

            const edge_predicate = edge_degree_predicate_map.get(edge_info.edge_degree_predicate)(edge_info.edge_degree_value);

            if (!edge_info.edge_name || edge_info.edge_name.trim().length === 0) {
                data = await data.where(edge_degree_type_map.get(edge_info.edge_degree_type)().count().is(edge_predicate)).limit(limit).toList();
            } else {
                const edge_name = edge_info.edge_name;
                data = await data.where(edge_degree_type_map.get(edge_info.edge_degree_type)(edge_name).count().is(edge_predicate)).limit(limit).toList();
            }

            for (const idx in data) {
                const props = await this._graph.V(data[idx].id).valueMap().toList();
                data[idx].properties = mapToObj(props[0]);
            }
            return data;
        } else {
            let params = {};

            params["label"] = label;
            params["property"] = key;
            params["predicate"] = predicate;
            params["value"] = value;
            if (edge_info.edge_name && edge_info.edge_name.trim().length > 0) {
                params["edge_name"] = edge_info.edge_name;
            }

            if (edge_info.edge_degree_type && edge_info.edge_degree_type.trim().length > 0) {
                params["edge_degree_direct"] = edge_info.edge_degree_type;
            }

            if (edge_info.edge_degree_predicate && edge_info.edge_degree_predicate.trim().length > 0) {
                params["edge_degree_predicate"] = edge_info.edge_degree_predicate;
            }

            params["edge_degree_num"] = edge_info.edge_degree_value;

            if (edge_info.edge_property && edge_info.edge_property.trim().length > 0) {
                params["edge_property"] = edge_info.edge_property;
            }

            if (edge_info.edge_predicate && edge_info.edge_predicate.trim().length > 0) {
                params["edge_predicate"] = edge_info.edge_predicate;
            }

            if (edge_info.edge_value) {
                params["edge_value"] = edge_info.edge_value;
            }

            params["start_idx"] = start_idx;
            params["limit"] = limit;

            let data = await this.searchNodeGraphService(params);
            //console.log(data);

            let result_data = [];
            for (const idx in data) {
                let item = {};
                item.id = data[idx].id;
                item.label = data[idx].label;
                item.properties = data[idx];
                delete item.properties.id;
                delete item.properties.label;
                item.properties = formatProperty(item.properties);
                result_data.push(item);
            }

            return result_data
        }
    }

    async searchEdges(label, predicate, key, value, limit=50) {
        let data = [];

        if (!value) {
            data = await this._graph.V().hasLabel(label).limit(limit).aggregate('node').outE().as('edge')
                .inV().where(p.within('node')).select('edge').toList()
        } else if (predicate === "eq") {
            data = await this._graph.V().hasLabel(label).has(key, value).limit(limit).aggregate('node').outE()
                .as('edge').inV().where(p.within('node')).select('edge').toList()
        } else {
            data = await this._graph.V().hasLabel(label).has(key, predicate_map.get(predicate)(value)).limit(limit)
                .aggregate('node').outE().as('edge').inV().where(p.within('node')).select('edge').toList()
        }

        return data;
    }

    async getAdjacentNodes(node_id, edge_info, limit=50) {
        let data = [];

        if (!edge_info.edge_name || edge_info.edge_name.trim().length === 0) {
            data = await this._graph.V(node_id).both().limit(limit).toList();
        } else {
            data = await this._graph.V(node_id).bothE(edge_info.edge_name);

            if (!edge_info.edge_value) {
                data = await data.otherV().limit(limit).toList();
            } else if (edge_info.edge_predicate === "eq") {
                data = await data.has(edge_info.edge_property, edge_info.edge_value).otherV().limit(limit).toList();
            } else {
                data = await data.has(edge_info.edge_property, predicate_map.get(edge_info.edge_predicate)(edge_info.edge_value)).otherV().limit(limit).toList();
            }
        }

        const self = await this._graph.V(node_id).toList();
        data.push(self[0]);

        for (const idx in data) {
            const props = await this._graph.V(data[idx].id).valueMap().toList();
            data[idx].properties = mapToObj(props[0]);
        }

        return data;
    }

    async getAdjacentEdges(node_id, edge_info) {
        let data = [];

        if (!edge_info.edge_name || edge_info.edge_name.trim().length === 0) {
            data = await this._graph.V(node_id).bothE().toList();
        } else {
            data = await this._graph.V(node_id).bothE(edge_info.edge_name);

            if (!edge_info.edge_value) {
                data = await data.toList();
            } else if (edge_info.edge_predicate === "eq") {
                data = await data.has(edge_info.edge_property, edge_info.edge_value).toList();
            } else {
                data = await data.has(edge_info.edge_property, predicate_map.get(edge_info.edge_predicate)(edge_info.edge_value)).toList();
            }
        }

        for (const idx in data) {
            const props = await this._graph.E(data[idx].id.relationId).valueMap().toList();
            data[idx].properties = mapEdgePropertyToObj(props[0]);
        }
        return data;
    }

    async connectGraphService() {

        return await kgraphService.connectGraph(config.graphUser, this._graphName).then(data => {
            if (data != null) {
                return data.token;
            } else {
                return "";
            }
        });
    }

    async searchNodeGraphService(params) {

        return await kgraphService.searchGraphNodes(config.graphUser, this._graphName, this._token, params).then(data => {
            if (data != null) {
                return data;
            } else {
                return [];
            }
        });
    }

    async searchEdgeGraphService(params) {

        return await kgraphService.searchGraphEdges(config.graphUser, this._graphName, this._token, params).then(data => {
            if (data != null) {
                return data;
            } else {
                return [];
            }
        });
    }

    async getGraphMetaDataService() {

        if (this._token.length === 0) {
            this._token = await this.connectGraphService();
        }

        return await kgraphService.getGraphMetaData(config.graphUser, this._graphName, this._token, {}).then(data => {
            if (data != null) {
                return data;
            } else {
                return [];
            }
        });
    }

    async getGraphRelationNames() {

        let metadata = await this.getGraphMetaDataService();

        if (metadata.length > 0) {
            return [metadata[0], metadata[1]]
        } else {
            return []
        }
    }

    async getGraphPropertyNames() {

        let metadata = await this.getGraphMetaDataService();

        if (metadata.length > 0) {
            return metadata[2]
        } else {
            return []
        }
    }

    async getGraphRelationCount(params) {

        if (this._token.length === 0) {
            this._token = await this.connectGraphService();
        }

        return await kgraphService.getGraphRelationCount(config.graphUser, this._graphName, this._token, params).then(data => {
            if (data != null) {
                return data;
            } else {
                return [];
            }
        });
    }

    async customizedGraphQuery(params) {

        if (this._token.length === 0) {
            this._token = await this.connectGraphService();
        }

        return await kgraphService.customizedGraphQuery(config.graphUser, this._graphName, this._token, params).then(data => {
            if (data != null) {
                return data;
            } else {
                return [];
            }
        });
    }
}

tenant_g = new GremlinClient("tenant_g");

loan_agent_g = new GremlinClient("loan_agent_g");

risk_radar_g = new GremlinClient("risk_radar_g");

real_estate_company_g = new GremlinClient("real_estate_company_g");

dcc_g = new GremlinClient("dcc_g");

da_g = new GremlinClient("da_g");

lineage_g = new GremlinClient("lineage_g");

module.exports = {
    tenant_g,
    loan_agent_g,
    risk_radar_g,
    real_estate_company_g,
    dcc_g,
    da_g,
    lineage_g
};

