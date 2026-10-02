const BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
const SECRET = 123456789;

function scramble(num) {
  return num ^ SECRET;
}

function toBase62(num) {
  if (num === 0) return BASE62[0];
  let result = "";
  while (num > 0) {
    result = BASE62[num % 62] + result;
    num = Math.floor(num / 62);
  }
  return result;
}

function generateShortCode(counter) {
  return toBase62(scramble(counter));
}

module.exports = { generateShortCode };