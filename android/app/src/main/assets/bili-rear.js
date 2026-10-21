/* SPDX-License-Identifier: GPL-3.0-or-later
 * Scoped, idempotent mobile-web adaptation. No Android JavascriptInterface.
 * Selectors are compatibility rules, not a guarantee against future site changes.
 */
(() => {
  if (window.__mrssBiliRear) { window.__mrssBiliRear(); return; }
  const css = `
    #comment, #comments, #commentapp, #comment-module, .comment-container,
    .comment-wrapper, .comment-list, .reply-box, .reply-list, .m-comment,
    .bili-comments, bili-comments, bili-comment-thread-renderer {display:none!important}
    .video-title, .video-info-title, .bili-video-card__info--tit,
    .video-card__title, .m-video-card__title {
      display:block!important; visibility:visible!important; opacity:1!important;
      height:auto!important; max-height:none!important; white-space:normal!important;
      -webkit-line-clamp:unset!important; overflow:visible!important;
      font-size:14px!important; line-height:1.4!important;
    }
    .mrss-card-title {display:block!important;font-size:14px!important;line-height:1.4!important;
      color:#222!important;white-space:normal!important;padding:4px 0!important}
    video {max-width:100%!important;object-fit:contain!important}
  `;
  function videoUrl(raw) {
    try {
      const u = new URL(raw, location.href);
      if (u.protocol === 'bilibili:' && u.hostname === 'video' && /^\d+$/.test(u.pathname.slice(1)))
        return 'https://m.bilibili.com/video/av' + u.pathname.slice(1);
      if (!['https:', 'http:'].includes(u.protocol) ||
          !(u.hostname === 'bilibili.com' || u.hostname.endsWith('.bilibili.com'))) return null;
      const id = u.pathname.match(/^\/video\/(BV[\da-zA-Z]{10}|av\d+)(?:\/|$)/);
      if (!id) return null;
      // Preserve part/timestamp, but keep playback in this WebView.
      return 'https://m.bilibili.com/video/' + id[1] + u.search;
    } catch (_) { return null; }
  }
  function adapt() {
    if (!document.getElementById('mrss-bili-style')) {
      const style = document.createElement('style'); style.id = 'mrss-bili-style';
      style.textContent = css; (document.head || document.documentElement).append(style);
    }
    if (/^\/video\//.test(location.pathname)) {
      const heading = document.querySelector('h1.video-title, .video-info-title, h1');
      const meta = document.querySelector('meta[property="og:title"]');
      const text = heading?.textContent?.trim() || meta?.content?.trim();
      if (text && document.title !== text) document.title = text;
    }
    document.querySelectorAll('a[href]').forEach(a => {
      const url = videoUrl(a.getAttribute('href'));
      if (!url) return;
      if (a.href !== url) a.href = url;
      if (a.target !== '_self') a.target = '_self';
      // Only fill missing visible text from real page metadata, never invent titles.
      const existing = a.querySelector('.mrss-card-title');
      const nativeText = Array.from(a.childNodes).filter(n => n !== existing).map(n => n.textContent).join('').trim();
      const text = a.getAttribute('title') || a.querySelector('img[alt]')?.getAttribute('alt');
      if (!nativeText && text) {
        const label = existing || document.createElement('span');
        label.className = 'mrss-card-title';
        if (label.textContent !== text) label.textContent = text;
        if (!existing) a.append(label);
      }
    });
  }
  document.addEventListener('click', e => {
    const a = e.target instanceof Element ? e.target.closest('a[href]') : null;
    const url = a && videoUrl(a.getAttribute('href'));
    if (!url) return;
    e.preventDefault(); e.stopImmediatePropagation();
    location.assign(url); // Real document history; avoids app-launch handlers and SPA residue.
  }, true);
  let queued = false;
  new MutationObserver(() => {
    if (queued) return;
    queued = true;
    requestAnimationFrame(() => { queued = false; adapt(); });
  }).observe(document.documentElement, {childList:true, subtree:true, attributes:true,
    attributeFilter:['href', 'title', 'alt']});
  window.addEventListener('pageshow', adapt);
  window.__mrssBiliRear = adapt;
  adapt();
})();
