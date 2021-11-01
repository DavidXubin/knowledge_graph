const Router = require('koa-router');
const pageController = require('../controller/page');
const AuthMiddleware = require('../middleware/auth');

const router = new Router();

router.get('/', AuthMiddleware.profile, pageController.render);

router.get('/datamodel', AuthMiddleware.profile, pageController.render);

router.get('/inquireGraph', AuthMiddleware.profile, pageController.render);

module.exports = router;
