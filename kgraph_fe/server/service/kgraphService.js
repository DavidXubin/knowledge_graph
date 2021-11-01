const config = require('../config');
const axios = require('axios').create({
    baseURL: config.graphService
});


const callGraphAPI = (user, graph, token, params) => {
    params["user"] = user;
    const p = graph.indexOf("_g");
    if (p > 0) {
        params["graph"] = graph.substring(0, p);
    } else {
        params["graph"] = graph;
    }
    params["token"] = token;

    return new Promise((resolve, reject) =>
        axios.post('/graph/api', JSON.stringify(params), {headers:{"Content-Type" : "application/json"}})
            .then(response => {
                if (response.data.code !== 200) {
                    //return reject(response.data.result);
                    return resolve(null);
                }
                console.log(response.data);
                return resolve(response.data.result);
            }).catch(err => {
            console.log(err);
            return reject(err);
        })
    )
};

const connectGraph =  (user, graph) => {

    const p = graph.indexOf("_g");
    if (p > 0) {
        graph = graph.substring(0, p);
    }

    return new Promise((resolve, reject) =>
        axios.post('/graph/connect?user=' + user + "&graph=" + graph)
        .then(response => {
            if (response.data.code !== 200) {
                return reject(response.data.result);
            }
            console.log(response.data);
            return resolve(response.data.result);
        }).catch(err => {
            console.log(err);
            return reject(err);
        })
    )
};

const searchGraphNodes = (user, graph, token, params) => {

    params["plugin_name"] = "searchNodesExt";

    return callGraphAPI(user, graph, token, params)
};

const searchGraphEdges = (user, graph, token, params) => {

    params["plugin_name"] = "searchEdges";

    return callGraphAPI(user, graph, token, params)
};

const getGraphMetaData = (user, graph, token, params) => {

    params["plugin_name"] = "getGraphMeta";

    return callGraphAPI(user, graph, token, params)
};

const getGraphRelationCount = (user, graph, token, params) => {

    params["plugin_name"] = "graphRelationCount";

    return callGraphAPI(user, graph, token, params)
};


const customizedGraphQuery = (user, graph, token, params) => {

    params["plugin_name"] = "clientSubmit";

    return callGraphAPI(user, graph, token, params)
};

module.exports = {
    connectGraph,
    searchGraphNodes,
    getGraphMetaData,
    getGraphRelationCount,
    searchGraphEdges,
    customizedGraphQuery
};