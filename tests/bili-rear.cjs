const { chromium } = require('playwright');
const fs = require('fs');
const assert = require('node:assert/strict');
(async () => {
  const browser = await chromium.launch({headless:true,args:['--no-sandbox']});
  const page = await browser.newPage({viewport:{width:360,height:240}});
  const script = fs.readFileSync('android/app/src/main/assets/bili-rear.js','utf8');
  const first = 'BV1xx411c7mD', second = 'BV1xx411c7mE';
  await page.route('https://m.bilibili.com/**', route => {
    const video = route.request().url().includes('/video/');
    route.fulfill({contentType:'text/html',body: video
      ? '<title>视频标题</title><video controls></video><div id="comment">隐藏的评论</div>'
      : `<title>浏览</title><a id="first" href="/video/${first}"><img alt="第一个视频的完整标题"></a><a id="second" href="/video/${second}?p=2" target="_blank"><span class="video-title" style="display:none">第二个标题</span></a>`});
  });
  await page.goto('https://m.bilibili.com/');
  await page.evaluate(script);
  await page.evaluate(script);
  assert.equal(await page.locator('.mrss-card-title').count(),1);
  assert.equal(await page.locator('#second .video-title').isVisible(),true);
  assert.equal(await page.locator('#second').getAttribute('target'),'_self');
  await page.locator('#first').click();
  await page.waitForURL('**/video/' + first);
  await page.evaluate(script);
  assert.equal(await page.locator('#comment').isVisible(),false);
  await page.evaluate(() => {
    const reply = document.createElement('div');reply.className='comment-list';reply.textContent='异步评论';document.body.append(reply);
  });
  assert.equal(await page.locator('.comment-list').isVisible(),false);
  await page.goBack();
  await page.evaluate(script);
  await page.locator('#second').click();
  await page.waitForURL('**/video/' + second + '?p=2');
  assert.equal(page.context().pages().length,1);
  await page.goto('https://m.bilibili.com/');
  await page.evaluate(() => document.body.innerHTML='<a id="evil" href="https://bilibili.com.evil.example/video/BV1xx411c7mD"><img alt="x"></a>');
  await page.evaluate(script);
  assert.equal(await page.locator('#evil').getAttribute('href'),'https://bilibili.com.evil.example/video/BV1xx411c7mD');
  console.log('PASS: titles, comments, dynamic comments, idempotency, video-back-video, multipart URL, single window, host boundary');
  await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
