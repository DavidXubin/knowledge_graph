const Router = require('koa-router');
const AuthController = require('../controller/auth');

const router = new Router({ prefix: '/server/auth' });

router.post('/profile', AuthController.profile);

router.get('/logout', AuthController.logout);

module.exports = router;
