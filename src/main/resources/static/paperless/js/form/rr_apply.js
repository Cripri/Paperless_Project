document.addEventListener('DOMContentLoaded', () => {
  const $  = (s, r=document) => r.querySelector(s);
  const $$ = (s, r=document) => Array.from(r.querySelectorAll(s));

  const form = $('#rr-form');
  if (!form) { console.warn('rr-form not found'); return; }

  // 1) 전부포함(ALL) → 상세옵션 비활성
  const includeAllRadios = $$('input[name="includeAll"]');
  const partialBox = $('#partialBox');

  function refreshPartial() {
    const sel = includeAllRadios.find(r => r.checked)?.value;
    const disable = (sel === 'ALL');
    if (!partialBox) return;
    partialBox.classList.toggle('disabled', !!disable);
    $$('#partialBox input, #partialBox select, #partialBox textarea, #partialBox button')
      .forEach(el => disable ? el.setAttribute('disabled','disabled')
                             : el.removeAttribute('disabled'));
  }
  includeAllRadios.forEach(r => r.addEventListener('change', refreshPartial));
  refreshPartial();

  // 2) 주소변동: RECENT일 때만 년수 활성화
  function refreshYears() {
    const years = $('#addressHistoryYears');
    if (!years) return;
    const recent = $('input[name="addressHistoryMode"][value="RECENT"]')?.checked;
    years.disabled = !recent;
  }
  $$('input[name="addressHistoryMode"]').forEach(r => r.addEventListener('change', refreshYears));
  refreshYears();

  // 3) 서명 팝업
  const signPreview     = $('#signPreview');
  const signatureBase64 = $('#signatureBase64');
  const openBtn         = $('#openSignPopup');
  const clearBtn        = $('#clearSignature');
  const signUrl         = $('#signUrl')?.value || '/residentregistration/sign';
  let signPopup;

  openBtn?.addEventListener('click', () => {
    const w=480, h=360, left=Math.round((screen.width-w)/2), top=Math.round((screen.height-h)/2);
    signPopup = window.open(signUrl, 'signPopup',
      `width=${w},height=${h},left=${left},top=${top},resizable=no,scrollbars=no`);
    if (!signPopup) alert('팝업이 차단되었습니다. 팝업 허용 후 다시 시도하세요.');
  });

  window.addEventListener('message', (e) => {
    const msg = e.data;
    if (!msg || msg.type !== 'SIGN_DONE' || !msg.dataUrl) return;
    if (signPreview) signPreview.src = msg.dataUrl;
    if (signatureBase64) signatureBase64.value = msg.dataUrl;
    try { signPopup?.close(); } catch(_) {}
  });

  clearBtn?.addEventListener('click', () => {
    signPreview?.removeAttribute('src');
    if (signatureBase64) signatureBase64.value = '';
  });

  // 4) 제출 직전: 체크박스 Y/N 단일화 + disabled 값 보존 + CSRF 첨부
  form.addEventListener('submit', () => {
    // 체크박스 hidden 중복 제거
    const checkboxNames = new Set($$('input[type="checkbox"][name]').map(cb => cb.name));
    $$('input[type="hidden"]').forEach(h => {
      const keep = new Set(['docType','consentYn','extraJson','signUrl','signatureBase64']);
      if (!keep.has(h.name) && checkboxNames.has(h.name)) h.remove();
    });
    // 체크된 건 Y만, 안 된 건 N hidden 생성
    checkboxNames.forEach(name => {
      const cb = $(`input[type="checkbox"][name="${name}"]`);
      if (!cb) return;
      if (cb.checked) cb.value = 'Y';
      else {
        const h = document.createElement('input');
        h.type='hidden'; h.name=name; h.value='N';
        form.appendChild(h);
      }
    });
    // disabled 임시 해제
    const disabled = $$('[disabled]', form);
    disabled.forEach(el => { el.dataset.wasDisabled='1'; el.removeAttribute('disabled'); });
    setTimeout(() => disabled.forEach(el => {
      if (el.dataset.wasDisabled) { el.setAttribute('disabled','disabled'); el.removeAttribute('data-was-disabled'); }
    }), 0);
    // CSRF
    const csrfParam = document.querySelector('meta[name="_csrf_parameter"]')?.content;
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    if (csrfParam && csrfToken && !$(`input[name="${csrfParam}"]`)) {
      const h = document.createElement('input');
      h.type='hidden'; h.name=csrfParam; h.value=csrfToken; form.appendChild(h);
    }
  });
});