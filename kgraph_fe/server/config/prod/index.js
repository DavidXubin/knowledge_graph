const config = {
    env: process.env.NODE_ENV || 'development',
    serviceProxy: 'https://meta.bkjk-inc.com',
    port: 8000,
    uc: {
        passport: 'https://sso.bkjk-inc.com/api/api/1.0/authentications/',
        sso: ['https://sso.bkjk-inc.com/'],
        uc: 'https://uc.bkjk-inc.com/service/api/1.0/users',
        secret: 'bkjk_pin',
        privateKey: 'ZMK1@4V6B7C#D3F!',
        authority: 'https://uc.bkjk-inc.com/service/api/1.0/authorizations'
    },
    appIds: ['kgraph'],
    graphHost: "10.10.50.17:20006",
    graphUser: "datainfra",
    graphPassword: ""
};

console.log('================ env ======================');
console.log(process.env.NODE_ENV || 'prod');

module.exports = config;
