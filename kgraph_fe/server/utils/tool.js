const config = require('../config');

module.exports.getSSOUrl = (ctx) => {
  const ssoUrl = config.uc.sso;
  const returnUrl = ctx.headers.referer || `http://${ctx.headers.host}`;
  return `${ssoUrl}login?returnUrl=${encodeURIComponent(returnUrl)}`;
};

module.exports.getLogoutUrl = (ctx) => {
  const ssoUrl = config.uc.sso;
  const returnUrl = ctx.headers.referer || `http://${ctx.headers.host}`;
  return `${ssoUrl}logout?returnUrl=${encodeURIComponent(returnUrl)}`;
};

module.exports.parseJSONStringToObject = (jsonString) => {
  let tempData;
  if ( typeof jsonString === 'string' ) {
    try {
      tempData = JSON.parse( jsonString )
    } catch ( err ) {
      tempData = {
        code: 201,
        msg: '返回数据格式异常',
        data: {}
      }
    }
  }
  return tempData
};

module.exports.checkNumber = (value) => {
  let isInt = !isNaN(value) && parseInt(value) === Number(value) && !isNaN(parseInt(value, 10));

  let isFloat = !isNaN(value) && parseFloat(value) === Number(value) && !isNaN(parseFloat(value));

  return isInt || isFloat
};
