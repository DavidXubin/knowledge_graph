const Router = require('koa-router');
const permissionController = require('../controller/permission');

const router = new Router({ prefix: '/server/permission' });

router.post('/check', permissionController.checkPermission);

module.exports = router;
