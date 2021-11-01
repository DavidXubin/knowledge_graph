//const { request } = require('bk-fe-fetch');
const fetch_api = require('isomorphic-fetch');
const config = require('../config');

/**
 *判断返回地址链接 reg URL
 * @param {*} req
 * @return {url}
 */
const returnURl = req => {
  const ssoUrls = config.uc.sso;
  const regexbkjkcom = /^.*bkjk\.com.*$/;
  const regexbkjkinccom = /^.*bkjk-inc\.com.*$/;
  const host = req.header.host;
  const isBkjkCom = regexbkjkcom.test(host);
  const isBkjkIncCom = regexbkjkinccom.test(host);
  // test prev dev ssoUrl:sso.bkjk-inc.com
  let ssoUrl = ssoUrls[0];
  // prod
  if (config && config.env === 'prod') {
    for (let i = 0; i < ssoUrls.length; i++) {
      if ((regexbkjkcom.test(ssoUrls[i]) && isBkjkCom) || (regexbkjkinccom.test(ssoUrls[i]) && isBkjkIncCom)) {
        ssoUrl = ssoUrls[i];
        break;
      }
    }
  }
  const httpProvider = config && config.env === 'prod' && isBkjkIncCom ? 'http' : 'https';
  const returnUrl = `${httpProvider}:${ssoUrl}?returnUrl=${encodeURIComponent(req.origin)}` || '';
  return returnUrl;
};

const fetch = async (ctx, { path, method, body }, baseURL=config.serviceProxy) => {
  //const baseURL = config.serviceProxy;
  let res = null;

  console.log('请求地址', `${baseURL}${path}`);
  console.log('请求数据', body);

  if (method.toUpperCase() === 'GET' || method.toUpperCase() === 'POST') {
    res = await request_internal(ctx)(`${baseURL}${path}`, {
      method,
      params: body
    });
  } else {
    res = await request_internal(ctx)(`${baseURL}${path}`, {
      method,
      body
    });
  }

  /*
  if (res && res.data && res.data.errorCode == 401) {
    return {
      code: 1,
      data: {
        errorCode: '403',
        status: 403,
        errorMessage: '请登录',
        url: returnURl(ctx.request)
      }
    };
  }
  */

  console.log(res)

  return res;
};

const transmitCookie = (ctx) => {
  return (response) => {
    if (response.headers.get('set-cookie')) {
      ctx.set({
        'set-cookie': response.headers.get('set-cookie')
      });
    }
    return response;
  }
}

const parseText = (response) => {
  return response.text();
}

const query = (params = {}) => {
  return Object.keys(params)
      .map(k => `${encodeURIComponent(k)}=${encodeURIComponent(params[k])}`)
      .join('&');
}

const request_internal = (ctx) => {
  return (url, options = {}) => {

    const ip = ctx.req.headers['x-real-ip'];

    if (options.method.toUpperCase() === 'GET' || options.method.toUpperCase() === 'POST') {
      if (options.params) {
        url = `${url}?${query(options.params)}`;
      }
    }
    // extra-headers should like 'extra-xxxx-xxxx'
    const extraHeaders = {}
    Object.keys(ctx.request.headers).forEach(item => {
      if (item.indexOf('extra-') > -1 ) {
        extraHeaders[item.slice(6)] = ctx.request.headers[item]
      }
    })
    const headers = Object.assign({
      'Content-Type': 'application/json',
      Cookie: ctx.request.header.cookie,
      'verify-ip': ip
    }, extraHeaders)

    console.log(options)
    console.log("Now the url is: " + url)

    return fetch_api(url, {
      credentials: 'include',
      method: options.method,
      headers,
      ...options
    })
      .then((response) => transmitCookie(ctx)(response))
      .then(parseText)
      .then((data) => {
        // 先转成text再转成json的原因是，可以在kibana中看到真正的response。
        // 如果直接转json，在kibana中只能看到error信息，比如json格式不正确。
        console.log('================== response text ================');
        console.log(data);
        const result = {
          code: 1,
          data: JSON.parse(data)
        }
        return result;
      })
      .catch(err => err)
  }
}

module.exports = {
  fetch,
  returnURl
};
