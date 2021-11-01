const authService = require('../service/auth');

const controller = {
  profile: async ctx => {
    const response = await authService.profile(ctx);

    ctx.body = response;
  },

  logout: async ctx => {
    const response = await authService.logout(ctx);

    ctx.redirect(response.result.url);
  }
};

module.exports = controller;
