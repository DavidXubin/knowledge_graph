const Router = require('koa-router');
const gremlinController = require('../controller/gremlin');

const router = new Router();

router.post('/gremlin_get_property/:graph/:label', gremlinController.getProperties);

router.post('/gremlin_search/:graph', gremlinController.search);

router.post('/gremlin_query/:graph', gremlinController.query);

module.exports = router;