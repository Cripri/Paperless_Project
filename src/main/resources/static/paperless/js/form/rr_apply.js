(function () {
  const $  = (s, r=document) => r.querySelector(s);
  const $$ = (s, r=document) => Array.from(r.querySelectorAll(s));

  document.addEventListener('DOMContentLoaded', () => {
    const form = $('#rr-form');
    if (!form) return;

    // 전부 포함(ALL) → 부분옵션 비활성화
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
      // disabled여도 서버로 값 보내려면 제출 직전 잠깐 풀어줌(아래 submit 훅에서 처리)
    }
    includeAllRadios.forEach(r => r.addEventListener('change', refreshPartial));

    // 주소변동: RECENT일 때만 N년 활성화
    function toggleYears() {
      const years  = $('#addressHistoryYears');
      const recent = $('input[name="addressHistoryMode"][value="RECENT"]');
      if (years && recent) years.disabled = !recent.checked;
    }
    $$('input[name="addressHistoryMode"]').forEach(r => r.addEventListener('change', toggleYears));

    // 초기 렌더 상태 반영
    refreshPartial();
    toggleYears();

    // 서명 팝업
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

    // 제출 직전 정리:
    //  - 체크박스: 미체크는 N, 체크는 Y만 남기기
    //  - disabled 일시 해제하여 값 누락 방지
    //  - CSRF 메타 → hidden 보강(템플릿에 이미 있더라도 중복 방지)
    form.addEventListener('submit', () => {
      // 1) 체크박스 Y/N 일관화
      const checkboxNames = new Set($$('input[type="checkbox"][name]').map(cb => cb.name));
      // 이미 템플릿의 hidden(N) 이 들어있을 수 있으므로, 중복 hidden 정리
      $$('input[type="hidden"]').forEach(h => {
        const keep = new Set(['docType','consentYn','extraJson','signUrl','signatureBase64']);
        if (!keep.has(h.name) && checkboxNames.has(h.name) && h.disabled) h.remove();
      });
      // 미체크 → N hidden 추가
      checkboxNames.forEach(name => {
        const cb = $(`input[type="checkbox"][name="${name}"]`);
        if (!cb) return;
        if (!cb.checked) {
          const h = document.createElement('input');
          h.type='hidden'; h.name=name; h.value='N';
          form.appendChild(h);
        } else {
          // 체크되면 값은 Y로 보장
          cb.value = 'Y';
        }
      });

      // 2) disabled 일시 해제(ALL일 때 partialBox 내부가 disabled인 경우 포함)
      const disabled = $$('[disabled]', form);
      disabled.forEach(el => { el.dataset.wasDisabled='1'; el.removeAttribute('disabled'); });
      setTimeout(() => disabled.forEach(el => {
        if (el.dataset.wasDisabled) { el.setAttribute('disabled','disabled'); el.removeAttribute('data-was-disabled'); }
      }), 0);

      // 3) CSRF 보강
      const csrfParam = document.querySelector('meta[name="_csrf_parameter"]')?.content;
      const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
      if (csrfParam && csrfToken && !$(`input[name="${csrfParam}"]`)) {
        const h = document.createElement('input');
        h.type='hidden'; h.name=csrfParam; h.value=csrfToken; form.appendChild(h);
      }
    });
  });
})();
