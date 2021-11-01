const Router = require('koa-router');
const inquireController = require('../controller/queryGraph');

const router = new Router({ prefix: '/server/inquireGraph' });

router.post('/getDB', inquireController.getDB);

router.post('/getRelation', inquireController.getRelation);

router.post('/inquireGraph', inquireController.inquireGraph);

router.post('/inquireGraphRelationCount', inquireController.inquireGraphRelationCount);

router.post('/loadHistoryQueries', inquireController.loadHistoryQueries);

router.post('/addQuery', inquireController.addQuery);

router.post('/customizedQuery', inquireController.customizedQuery);

module.exports = router;