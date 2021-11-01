const Router = require('koa-router');
const AuthMiddleware = require('../middleware/auth');

let router = new Router();

router.get('/test', AuthMiddleware.profile, async (ctx) => {
    ctx.response.body = 'Hello World';
})


module.exports = router;