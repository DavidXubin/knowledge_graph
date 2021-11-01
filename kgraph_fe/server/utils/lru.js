const LRU = require('lru-cache');

const options = { 
  maxAge: 1000 * 60 * 10 // 过期时间
};
const cache = LRU(options);

module.exports =  {
  get: (key) => {
    const cacheByKey = cache.get(key);
    return cacheByKey ? cacheByKey : null;
  },
  set: (key, data) => {
    cache.set(key, data);
  },

  del: (key) => {
    cache.del(key);
  }
}
