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

    // === 서명 팝업 ===
    let signPopup;
    const openBtn         = $('#openSignPopup');
    const clearBtn        = $('#clearSignature');
    const signUrl         = $('#signUrl')?.value || '/residentregistration/sign';
    const signatureBase64 = $('#signatureBase64');

    // ▼ 서명 미리보기 보장: 없으면 즉석 생성 + 위치 삽입 (요청하신 기존 방식)
    function ensurePreview() {
      let preview = $('#signPreview');
      if (preview) return preview;

      const btnBlock = $('#openSignPopup')?.closest('.inline');
      const container = btnBlock?.parentElement;

      preview = document.createElement('img');
      preview.id = 'signPreview';
      preview.alt = '서명 미리보기';
      preview.style.width = '220px';
      preview.style.height = '120px';
      preview.style.border = '1px solid #e5e7eb';
      preview.style.background = '#fff';
      preview.style.objectFit = 'contain';
      preview.style.borderRadius = '6px';

      if (container) {
        container.insertBefore(preview, container.firstChild);
      } else if (btnBlock) {
        btnBlock.prepend(preview);
      } else {
        form.prepend(preview);
      }
      return preview;
    }

    // ▼ 서명 수신 → hidden 저장 + 프리뷰 즉시 갱신
    function setSignature(dataUrl) {
      if (signatureBase64) signatureBase64.value = dataUrl;
      const preview = ensurePreview();
      preview.src = dataUrl;
      preview.onload = () => {
        preview.setAttribute('tabindex', '-1');
        preview.focus?.();
      };
    }

    // 팝업 열기
    openBtn?.addEventListener('click', () => {
      const w=480, h=360, left=Math.round((screen.width-w)/2), top=Math.round((screen.height-h)/2);
      signPopup = window.open(
        signUrl,
        'signPopup',
        `width=${w},height=${h},left=${left},top=${top},resizable=no,scrollbars=no`
      );
      if (!signPopup) alert('팝업이 차단되었습니다. 팝업 허용 후 다시 시도하세요.');
    });

    // postMessage 수신 (type: SIGN_DONE | SIGN_SAVED 지원)
    window.addEventListener('message', (e) => {
      const msg = e?.data;
      if (!msg || (msg.type !== 'SIGN_DONE' && msg.type !== 'SIGN_SAVED')) return;
      const dataUrl = msg.dataUrl || msg.base64 || msg.signature || msg.data;
      if (!dataUrl) return;
      setSignature(String(dataUrl));
      try { signPopup?.close(); } catch(_) {}
    });

    // (대안) 팝업에서 opener.setSignature() 직접 호출 지원
    window.setSignature = setSignature;

    // 지우기
    clearBtn?.addEventListener('click', () => {
      const preview = $('#signPreview');
      if (preview) preview.removeAttribute('src');
      if (signatureBase64) signatureBase64.value = '';
    });

    // 제출 직전 정리
    form.addEventListener('submit', () => {
      // 체크박스 Y/N 일관화
      const checkboxNames = new Set($$('input[type="checkbox"][name]').map(cb => cb.name));
      $$('input[type="hidden"]').forEach(h => {
        const keep = new Set(['docType','consentYn','extraJson','signUrl','signatureBase64']);
        if (!keep.has(h.name) && checkboxNames.has(h.name) && h.disabled) h.remove();
      });
      checkboxNames.forEach(name => {
        const cb = $(`input[type="checkbox"][name="${name}"]`);
        if (!cb) return;
        if (!cb.checked) {
          const h = document.createElement('input');
          h.type='hidden'; h.name=name; h.value='N';
          form.appendChild(h);
        } else {
          cb.value = 'Y';
        }
      });

      // disabled 일시 해제
      const disabled = $$('[disabled]', form);
      disabled.forEach(el => { el.dataset.wasDisabled='1'; el.removeAttribute('disabled'); });
      setTimeout(() => disabled.forEach(el => {
        if (el.dataset.wasDisabled) { el.setAttribute('disabled','disabled'); el.removeAttribute('data-was-disabled'); }
      }), 0);

      // CSRF 보강
      const csrfParam = document.querySelector('meta[name="_csrf_parameter"]')?.content;
      const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
      if (csrfParam && csrfToken && !$(`input[name="${csrfParam}"]`)) {
        const h = document.createElement('input');
        h.type='hidden'; h.name=csrfParam; h.value=csrfToken; form.appendChild(h);
      }
    });
  });
})();

(function(){
  // 다음 우편번호 스크립트 로더 (중복 로드 방지)
  function loadDaumPostcodeScript(cb){
    if (window.daum && window.daum.Postcode) { cb(); return; }
    const cur = document.getElementById('daum-postcode-sdk');
    if (cur) { cur.addEventListener('load', cb); return; }
    var s = document.createElement('script');
    s.src = "//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js";
    s.id  = "daum-postcode-sdk";
    s.onload = cb;
    document.head.appendChild(s);
  }

  // 주소 컴포넌트 바인딩 (시/도 → address1, 시/군/구 → address2)
  function wirePicker(root){
    if (!root) return;
    var btn   = root.querySelector('[data-role="search"]');
    var addr1 = root.querySelector('[data-role="addr1"]'); // th:field="*{address1}"
    var addr2 = root.querySelector('[data-role="addr2"]'); // th:field="*{address2}"
    if (!btn || !addr1 || !addr2) return;

    btn.addEventListener('click', function(){
      loadDaumPostcodeScript(function(){
        new daum.Postcode({
          oncomplete: function(data){
            // 카카오 API: data.sido (시/도), data.sigungu (시/군/구)
            var sido   = data.sido    || '';   // 예: 서울특별시 / 경기도
            var sigg   = data.sigungu || '';   // 예: 강남구 / 수원시 영통구

            addr1.value = sido;
            addr2.value = sigg;

            // 바인딩 이벤트 발행
            ['change','input'].forEach(type => {
              addr1.dispatchEvent(new Event(type, { bubbles:true }));
              addr2.dispatchEvent(new Event(type, { bubbles:true }));
            });
          }
        }).open();
      });
    });
  }

  // 모든 주소 컴포넌트 초기화
  function initAll(){
    document.querySelectorAll('.addr-picker').forEach(wirePicker);
  }
  window.initAddressPickers = initAll;

  if (document.readyState === 'loading'){
    document.addEventListener('DOMContentLoaded', initAll);
  } else {
    initAll();
  }
})();
