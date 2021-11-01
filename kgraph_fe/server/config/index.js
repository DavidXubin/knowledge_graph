let config = {
    env: process.env.NODE_ENV || 'development',
    serviceProxy: 'http://10.10.32.201:8012',
    port: 8013,
    uc: {
        passport: 'https://sso.dev.bkjk-inc.com/api/api/1.0/authentications/',
        sso: ['https://sso.dev.bkjk-inc.com/'],
        uc: 'https://uc.dev.bkjk-inc.com/service/api/1.0/users',
        secret: 'bkjk_pin',
        privateKey: 'ZMK1@4V6B7C#D3F!',
        authority: 'https://uc.dev.bkjk-inc.com/service/api/1.0/authorizations'
    },
    appIds: ['kgraph'],
    graphHost: "10.10.32.201:20006",
    graphUser: "datainfra",
    graphPassword: "BKJK@test",
    graphService: "http://10.10.32.201:20001"
};

console.log('================ env ======================');
console.log(process.env.NODE_ENV || 'development');

switch (process.env.NODE_ENV) {
    case 'dev':
        config = require('./dev');
        break;
    case 'test':
        config = require('./test');
        break;
    case 'stage':
        config = require('./stage');
        break;
    case 'prod':
        config = require('./prod');
        break;
    default:
        break;
}

module.exports = config;
