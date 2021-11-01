const http = require('http')
const Koa = require('koa')
const path = require('path')
const views = require('koa-views')
const convert = require('koa-convert')
const json = require('koa-json')
const Bodyparser = require('koa-bodyparser')
const logger = require('koa-logger')
const koaStatic = require('koa-static-plus')
var cors = require('koa2-cors');
const config = require('./config')
const authRouter = require('./router/auth')
const pageRouter = require('./router/page')
const graphRouter = require('./router/graph')
const testRouter = require('./router/test')
const permissionRouter = require('./router/permission')
const gremlinRouter = require('./router/gremlin')
const queryGraphRouter = require('./router/queryGraph')

const app = new Koa();
app.use(cors());
const bodyparser = Bodyparser();

// middlewares
app.use(convert(bodyparser));
app.use(convert(json()));
app.use(convert(logger()));
// views

app.use(
    views(path.join(__dirname, './views'), {
      map: { hbs: 'handlebars' },
      extension: 'hbs'
    })
);

// response router
app.use(authRouter.routes());


// static
app.use(
    convert(
        koaStatic(path.join(__dirname, '../public'), {
          pathPrefix: ''
        })
    )
);

app.use(pageRouter.routes());
app.use(graphRouter.routes());
app.use(testRouter.routes());
app.use(permissionRouter.routes());
app.use(gremlinRouter.routes());
app.use(queryGraphRouter.routes());


// logger
app.use(async (ctx, next) => {
  const start = new Date();
  await next();
  const ms = new Date() - start;
  console.log(`${ctx.method} ${ctx.url} - ${ms}ms`)
});

// 404
app.use(async ctx => {
  ctx.status = 404;
  ctx.render('404')
});


const port = parseInt(config.port || '3000', 10);
const server = http.createServer(app.callback());

server.listen(port);
server.on('error', error => {
  if (error.syscall !== 'listen') {
    throw error
  }
  // handle specific listen errors with friendly messages
  switch (error.code) {
    case 'EACCES':
      console.error(`${port} requires elevated privileges`);
      process.exit(1);
      break;
    case 'EADDRINUSE':
      console.error(`${port} is already in use`);
      process.exit(1);
      break;
    default:
      throw error
  }
});

server.on('listening', () => {
  console.log('Listening on port: %d', port)
});

module.exports = app;
