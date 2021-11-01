const mapping = require('../../public/mapping.json');

const controller = {
  render: async ctx => {
    await ctx.render('index', {
      js: `<script src="${mapping['vendor.js']}"></script><script src="${mapping['kgraph.js']}"></script>`,
      css: `<link href="${mapping['kgraph.css']}" rel="stylesheet">`,
      data: `<script>window.INIT_DATA=${JSON.stringify(ctx.userInfo)}</script>`
    });
  }
};

module.exports = controller;
