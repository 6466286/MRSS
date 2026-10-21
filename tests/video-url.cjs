// Execute the actual URL normalizer from the shipped adapter without a browser.
const fs = require('fs');
const vm = require('vm');
const assert = require('node:assert/strict');
const source = fs.readFileSync('android/app/src/main/assets/bili-rear.js','utf8');
const start = source.indexOf('  function videoUrl(');
const end = source.indexOf('  function adapt()', start);
const context = { URL, location: { href: 'https://m.bilibili.com/search?keyword=test' } };
vm.createContext(context);
vm.runInContext(source.slice(start,end), context);
const normalize = context.videoUrl;
assert.equal(normalize('/video/BV1xx411c7mD?p=2'), 'https://m.bilibili.com/video/BV1xx411c7mD?p=2');
assert.equal(normalize('https://www.bilibili.com/video/av123/'), 'https://m.bilibili.com/video/av123');
assert.equal(normalize('bilibili://video/123'), 'https://m.bilibili.com/video/av123');
assert.equal(normalize('https://bilibili.com.evil.example/video/av123'), null);
assert.equal(normalize('javascript:alert(1)'), null);
assert.equal(normalize('/video/BV1xx411c7mDextra'), null);
assert.equal(normalize('/bangumi/play/ep123'), null);
console.log('PASS: 7 URL normalization and host-boundary assertions');
