const inquireGraphService = require('../service/inquireGraph');


const controller = {
    getDB: async ctx => {
        const response = await inquireGraphService.getDB(ctx);

        ctx.body = {
            code: response.data.code,
            result: response.data.result
        }
    },

    getRelation: async ctx => {
        const relations = await inquireGraphService.getRelation(ctx);

        ctx.body = {
            code: 200,
            result: relations
        }
    },

    inquireGraph: async ctx => {
        const data = await inquireGraphService.inquireGraph(ctx);

        if (data.length > 0 ) {
            ctx.body = {
                code: 200,
                result: {
                    totalCount: data.length,
                    recordBean: {
                        keyList: Object.keys(data[0]),
                        valueList: data
                    }
                }
            }
        } else {
            ctx.body = {
                code: 200,
                result: {
                    totalCount: 0,
                    recordBean: {
                        keyList: [],
                        valueList: []
                    }
                }
            }
        }
    },

    inquireGraphRelationCount: async ctx => {
        const data = await inquireGraphService.inquireGraphRelationCount(ctx);

        if (data.length > 0 ) {
            ctx.body = {
                code: 200,
                result: data
                }
        } else {
            ctx.body = {
                code: 200,
                result: []
            }
        }
    },

    loadHistoryQueries: async ctx => {
        const response = await inquireGraphService.loadHistoryQueries(ctx);

        //const history_queries = response.data.result.map(item => item["content"]);
        ctx.body = {
            code: response.data.code,
            result: response.data.result
        }
    },

    addQuery: async ctx => {
        const response = await inquireGraphService.addQuery(ctx);

        if (response.data.code && response.data.code === 200) {
            const response = await inquireGraphService.loadHistoryQueries(ctx);

            ctx.body = {
                code: response.data.code,
                result: response.data.result
            }
        } else {
            ctx.body = {
                code: response.data.status,
                result: []
            }
        }
    },

    customizedQuery: async ctx => {
        const data = await inquireGraphService.customizedQuery(ctx);

        if (data.length > 0 && data[0].is_table === true && data[1].length > 0) {
            ctx.body = {
                code: 200,
                result: {
                    isTable: true,
                    totalCount: data[1].length,
                    recordBean: {
                        keyList: Object.keys(data[1][0]),
                        valueList: data[1]
                    }
                }
            }
        } else if (data.length > 0 && data[0].is_table === false && data[1].length > 0) {
            ctx.body = {
                code: 200,
                result: {
                    isTable: false,
                    totalCount: data[1].length,
                    recordBean: {
                        keyList: [],
                        valueList: data[1]
                    }
                }
            }
        } else {
            ctx.body = {
                code: 200,
                result: {
                    totalCount: 0,
                    recordBean: {
                        keyList: [],
                        valueList: []
                    }
                }
            }
        }
    },
};

module.exports = controller;
