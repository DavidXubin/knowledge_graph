const Router = require('koa-router');
const path = require('path')
const AuthMiddleware = require('../middleware/auth');
const assert = require('assert')
const send = require('koa-send')
const normalize = require('upath').normalizeSafe
const resolve = require('path').resolve
const fs = require('fs').promises
const findRoot = require('find-root')
const LRU = require('../utils/lru');
const config = require('../config');
const { fetch, returnURl } = require('../utils/fetch');

let router = new Router();


const checkPermission = ctx => {
    const secret = ctx.cookies.get(config.uc.secret);
    if (!secret || !LRU.get(secret)) {
        return {
            code: 1,
            data: {
                error: 2,
                message: '请登录',
                url: returnURl(ctx.request)
            }
        };
    }

    let login_user = LRU.get(secret).USER_INFO.account.toLowerCase();

    const refer = ctx.request.originalUrl
    const p = refer.lastIndexOf("/")
    if (p < 0 || !ctx.request.query.user || ctx.request.query.user.toLowerCase() !== login_user) {
        return {
            code: 1,
            data: {
                error: 2,
                message: '请登录',
                url: returnURl(ctx.request)
            }
        };
    }

    const q = refer.lastIndexOf("?")
    let graph

    if (q > 0) {
        graph = refer.substring(p + 1, q)
    } else {
        graph = refer.substring(p + 1)
    }
    console.log("graph is " + graph)

    let params = {
        user: login_user,
        sql: "select * from kgraph." + graph
    };
    return fetch(ctx, { path: '/meta/auth2', method: 'POST', body: params });
};


function koaServer(opts) {
    assert(typeof opts.rootDir === 'string', 'rootDir must be specified (as a string)')

    let options = opts || {}
    options.root = resolve(options.rootDir || process.cwd())

    // due to backward compatibility uses "index.html" as default but also supports disabling that with ""
    options.index = (typeof options.index === 'string' || options.index instanceof String) ? options.index : "index.html"

    options.last = (typeof opts.last === 'boolean') ? opts.last : true

    const log = options.log || false
    const rootPath = normalize(options.rootPath ? options.rootPath + "/" : "/")
    const forbidden = opts.forbidden || [];

    return async (ctx, next) => {
        assert(ctx, 'koa context required')


        // skip if this is not a GET/HEAD request
        if (ctx.method !== 'HEAD' && ctx.method !== 'GET') {
            return next()
        }

        let path = ctx.path
        console.log("ctx.path = " + ctx.path)

        // Redirect non-slashed request to slashed, eg. /doc to /doc/
        if (path + '/' === rootPath) {
            return ctx.redirect(rootPath)
        }

        // Check if request path (eg: /doc/file.html) is allowed (eg. in /doc)
        if (path.indexOf(rootPath) !== 0) {
            return next()
        }

        /* Serve folders as specified
         eg. for options:
          rootDir = 'web/static'
          rootPath = '/static'

        'web/static/file.txt' will be served as 'http://server/static/file.txt'
        */

        console.log(ctx.params)
        if (ctx.params.name) {
            options.rootPath = rootPath + ctx.params.name
        } else {
            options.rootPath = rootPath
        }

        path = normalize(path.replace(options.rootPath, "/"))
        console.log(path)

        /**
         * LOG
         */
        log && console.log(new Date().toISOString(), path)

        /**
         * If folder is in forbidden list, refuse request
         */
        if (forbidden.length > 0) {
            for (let folder of forbidden) {
                folder = new RegExp(`\/${folder}\/`, 'i');
                if (folder.test(path)) {
                    ctx.status = 403;
                    ctx.body = "FORBIDDEN FOLDER"

                    return (options.last) ? undefined : next()
                }
            }
        }

        let sent
        console.log(options)

        if (path == "/") {
            const response = await checkPermission(ctx);
            if (response.data.error != 0) {
                return ctx.redirect("/")
            }
        }

        /* In case of error from koa-send try to serve the default static file
         eg. 404 error page or image that illustrates error
        */
        try {
            sent = await send(ctx, path, options)
        } catch (error) {
            if (!options.notFoundFile) {
                if (options.last)
                    ctx.throw(404)
            } else {
                sent = await send(ctx, options.notFoundFile, options)
            }
        }

        if (sent && options.last) {
            return
        } else {
            return next()
        }
    }
}

router.get('/graph/:name',  AuthMiddleware.profile,
    koaServer({rootDir: path.join(findRoot(__dirname), '/graphexp'), rootPath: "/graph"}  ))

router.get('/graph/css/(.*)', AuthMiddleware.profile,
    koaServer({rootDir: path.join(findRoot(__dirname), '/graphexp/css'), rootPath: "/graph/css"}))

router.get('/graph/scripts/(.*)', AuthMiddleware.profile,
    koaServer({rootDir: path.join(findRoot(__dirname), '/graphexp/scripts'), rootPath: "/graph/scripts"}))

module.exports = router;