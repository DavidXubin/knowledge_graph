const permissionService = require('../service/permission');

const controller = {
    checkPermission: async ctx => {
        const response = await permissionService.checkPermission(ctx);
        console.log(response)

        let code = 200;
        if (response.data.error != 0) {
            code = 501
        }

        let message = response.data.message
        if (message.length == 0) {
            message = "success"
        }

        ctx.body = {
            code: code,
            result: message
        };
    },
}

module.exports = controller;