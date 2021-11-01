/**
 * 基础的工具方法
 * @type {request}
 */
const request = require("request"); //enable cookie
const crypto = require("crypto");

/**
 * aes加密
 * @param data 待加密内容
 * @param key 必须为16位私钥
 * @param iv
 * @returns {string}
 */
module.exports.aesEncryption = (data, key, iv) => {
  iv = iv || "";
  const clearEncoding = "utf8";
  const cipherEncoding = "base64";
  const cipherChunks = [];
  let cipher = crypto.createCipheriv("aes-128-ecb", key, iv);
  cipher.setAutoPadding(true);
  cipherChunks.push(cipher.update(data, clearEncoding, cipherEncoding));
  cipherChunks.push(cipher.final(cipherEncoding));
  return cipherChunks.join("");
};

/**
 * aes解密
 * @param data 待解密内容
 * @param key 必须为16位私钥
 * @param iv
 * @returns {*}
 */
module.exports.aesDecryption = (data, key, iv) => {
  if (!data) {
    return "";
  }
  iv = iv || "";
  const clearEncoding = "utf8";
  const cipherEncoding = "base64";
  const cipherChunks = [];
  const decipher = crypto.createDecipheriv("aes-128-ecb", key, iv);
  decipher.setAutoPadding(true);
  cipherChunks.push(decipher.update(data, cipherEncoding, clearEncoding));
  cipherChunks.push(decipher.final(clearEncoding));
  return cipherChunks.join("");
};