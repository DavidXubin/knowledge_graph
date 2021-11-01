const AuthService = require('../service/auth');
const config = require('../config');
const LRU = require('../utils/lru');
const { getSSOUrl } = require('../utils/tool');

function needJSON(req) {
  return req.headers.accept === 'application/json' || (req.headers['content-type'] || '').indexOf('json') >= 0;
}

/**
 * 检查登陆态中间件，登陆状态下会在 req.userInfo 上挂载用户信息
 * @param {*} req
 * @param {*} res
 * @param {*} next
 */
exports.profile = async (ctx, next) => {
  const secret = ctx.cookies.get(config.uc.secret);
  const ssoUrl = getSSOUrl(ctx);

  const requestJSON = needJSON(ctx.req);

  // session不存在，直接重定向到sso
  if (!secret) {
    if (requestJSON) {
      ctx.json({ code: 403, msg: '请登录!', data: getSSOUrl(ctx) });
    } else {
      ctx.redirect(getSSOUrl(ctx));
    }
    return;
  }

  // 缓存命中
  if (LRU.get(secret)) {
    console.log('================== checkLogin 缓存命中');
    ctx.userInfo = LRU.get(secret);
    
    return next();
  }

  console.log('================== checkLogin 缓存没有命中');

  // 没有缓存，去sso验证
  const response = await AuthService.profile(ctx);

  if (response.code === 200) {
    LRU.set(secret, {
      USER_INFO: response.result
    });
    ctx.userInfo = {
      USER_INFO: response.result
    };
    await next();
  } else if (requestJSON) {
    ctx.body = response;
  } else if (ssoUrl) {
    ctx.redirect(getSSOUrl(ctx));
  } else {
    await next();
  }
};
