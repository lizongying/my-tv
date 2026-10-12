'use strict';
(() => {
  let token = location.hash.slice(1);
  history.replaceState(null, '', location.pathname);
  const el = id => document.getElementById(id);
  const form = el('config');
  const status = (message, failed = false) => { el('status').textContent = message; el('status').classList.toggle('error', failed); };
  const lines = id => el(id).value.split(/\r?\n/).map(s => s.trim()).filter(Boolean);
  const data = () => ({enabled:el('enabled').checked,aiEnabled:el('aiEnabled').checked,logoLookupEnabled:el('logoLookupEnabled').checked,logoImageMb:Number(el('logoImageMb').value),endpoint:el('endpoint').value.trim(),model:el('model').value.trim(),key:el('key').value.trim(),clearKey:el('clearKey').checked,intervalHours:Number(el('intervalHours').value),sources:lines('sources'),blocked:lines('blocked')});
  const busy = value => { el('save').disabled = value; el('test').disabled = value; };
  const request = async (path, body) => {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 45000);
    try {
      const response = await fetch(path, {method:body === undefined ? 'GET' : 'POST',headers:{'Authorization':'Bearer ' + token,...(body === undefined ? {} : {'Content-Type':'application/json'})},body:body === undefined ? undefined : JSON.stringify(body),cache:'no-store',credentials:'omit',signal:controller.signal});
      const text = await response.text();
      if (!response.ok) throw new Error(text || '电视返回错误，请重新配对');
      return text;
    } finally { clearTimeout(timer); }
  };
  el('clearKey').addEventListener('change', () => { if (el('clearKey').checked) { el('aiEnabled').checked = false; el('key').value = ''; } });
  el('test').addEventListener('click', async () => {
    busy(true); status('正在测试 API，请稍候…');
    try { status(await request('/api/test', data())); }
    catch (error) { status(error.name === 'AbortError' ? '连接超时，请检查电视网络和 API 地址。' : error.message, true); }
    finally { busy(false); }
  });
  form.addEventListener('submit', async event => {
    event.preventDefault(); const body = data();
    if (body.aiEnabled && (!body.endpoint || !body.model)) { status('请填写 API 地址和模型名称。', true); return; }
    if (body.sources.length > 20 || body.blocked.length > 100) { status('来源最多 20 个，屏蔽列表最多 100 项。', true); return; }
    busy(true); status('正在保存到电视…');
    try { status(await request('/api/config', body)); el('key').value = ''; token = ''; form.hidden = true; }
    catch (error) { status(error.name === 'AbortError' ? '保存状态未知，请到电视上查看；需要时重新配对。' : error.message, true); busy(false); }
  });
  (async () => {
    if (!/^[a-f0-9]{64}$/.test(token)) { status('请扫描电视当前显示的二维码。此地址缺少有效配对令牌。', true); form.hidden = true; return; }
    busy(true);
    try {
      const current = JSON.parse(await request('/api/config'));
      ['enabled','aiEnabled','logoLookupEnabled'].forEach(id => { el(id).checked = !!current[id]; });
      ['endpoint','model','intervalHours','logoImageMb'].forEach(id => { el(id).value = current[id]; });
      el('sources').value = (current.sources || []).join('\n'); el('blocked').value = (current.blocked || []).join('\n');
      el('key').placeholder = current.hasKey ? '已保存；留空保留原密钥' : '输入密钥';
      el('keyState').textContent = current.hasKey ? '电视已保存 Key，不会回显。留空即可保留。' : '密钥加密存储在电视中，不会回显。';
      status('已连接电视。配对有效期 10 分钟，保存后立即失效。'); busy(false);
    } catch (error) { status(error.message, true); form.hidden = true; }
  })();
})();
