const config = {
  env: process.env.NODE_ENV || 'development',
  serviceProxy: 'http://172.29.15.131:8021',
  port: 8013,
  uc: {
      passport: 'https://sso.stage.bkjk-inc.com/api/api/1.0/authentications/',
      sso: ['https://sso.stage.bkjk-inc.com/'],
      uc: 'https://uc.stage.bkjk-inc.com/service/api/1.0/users',
      secret: 'bkjk_pin',
      privateKey: 'ZMK1@4V6B7C#D3F!',
      authority: 'https://uc.stage.bkjk-inc.com/service/api/1.0/authorizations'
  },
  appIds: ['kgraph']
};

console.log('================ env ======================');
console.log(process.env.NODE_ENV || 'stage');

module.exports = config;
