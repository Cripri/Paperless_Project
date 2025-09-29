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

    // ▼ 서명 미리보기 보장: 없으면 즉석 생성 + 위치 삽입
    function ensurePreview() {
      let preview = $('#signPreview');
      if (preview) return preview;

      // 프리뷰를 넣을 자리를 찾는다 (버튼 블록 바로 앞)
      const btnBlock = $('#openSignPopup')?.closest('.inline');
      // 버튼 블록 상위 .inline(두 줄짜리 컨테이너) 내 맨 앞에 이미지가 오도록
      const container = btnBlock?.parentElement; // label과 나란히 있는 inline 컨테이너
      // 기존 마크업: <div class="inline" style="align-items:flex-start; gap:16px;">
      //   <img id="signPreview" ...>
      //   <div class="inline" ...>버튼들...</div>
      // </div>

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
        // 최후 수단: 폼 맨 앞에 붙임
        form.prepend(preview);
      }
      return preview;
    }

    // ▼ 서명 수신 → hidden 저장 + 프리뷰 즉시 갱신
    function setSignature(dataUrl) {
      if (signatureBase64) signatureBase64.value = dataUrl;
      const preview = ensurePreview();
      preview.src = dataUrl;

      // 접근성: 새 이미지가 로드되면 포커스 힌트
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

    // postMessage 수신 (권장 방식)
    window.addEventListener('message', (e) => {
      const msg = e.data;
      if (!msg || msg.type !== 'SIGN_DONE' || !msg.dataUrl) return;
      setSignature(msg.dataUrl);
      try { signPopup?.close(); } catch(_) {}
    });

    // (대안) 팝업이 window.opener.setSignature() 직접 호출하는 경우도 지원
    // 전역에 노출
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
