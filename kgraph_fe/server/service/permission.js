const { fetch } = require('../utils/fetch');

const checkPermission = ctx => {
    const  body  = ctx.request.body;
    let params = {
        user: body.user,
        sql: "select * from kgraph." + body.graph
    };

    debugger
    return fetch(ctx, { path: '/meta/auth2', method: 'POST', body: params });
};

module.exports = {
    checkPermission
}