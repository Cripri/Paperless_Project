(function () {
  const $  = (s, r=document) => r.querySelector(s);
  const $$ = (s, r=document) => Array.from(r.querySelectorAll(s));

  document.addEventListener('DOMContentLoaded', () => {
    const form = $('#passport-form');
    if (!form) return;

    /* 숫자만 */
    form.addEventListener('input', (e) => {
      const el = e.target;
      if (el.classList.contains('num-only')) {
        el.value = el.value.replace(/\D+/g, '');
      }
    });

    /* 여행증명서 왕복/편도 토글 */
    const typeRadios = $$('input[name="passportType"]', form);
    const travelModeRow = $('#travelModeRow');
    function refreshTravelMode(){
      const v = typeRadios.find(r=>r.checked)?.value;
      travelModeRow.style.display = (v === 'TRAVEL_CERT') ? 'flex' : 'none';
      if (v !== 'TRAVEL_CERT') {
        $$('input[name="travelMode"]').forEach(r => r.checked = false);
      }
    }
    typeRadios.forEach(r => r.addEventListener('change', refreshTravelMode));
    refreshTravelMode();

    /* 우편배송 희망 시 주소 활성화 */
    const deliveryRadios = $$('input[name="deliveryWanted"]', form);
    const deliveryBox = $('#deliveryBox');
    function refreshDelivery(){
      const want = deliveryRadios.find(r=>r.checked)?.value === 'Y';
      deliveryBox.classList.toggle('disabled', !want);
      $$('#deliveryBox input, #deliveryBox button').forEach(el => want ? el.removeAttribute('disabled')
                                                                      : el.setAttribute('disabled','disabled'));
    }
    deliveryRadios.forEach(r => r.addEventListener('change', refreshDelivery));
    refreshDelivery();

    /* 사진 미리보기 */
    const photoFile = $('#photoFile');
    const photoPreview = $('#photoPreview');
    photoFile?.addEventListener('change', () => {
      const f = photoFile.files?.[0];
      if (!f) { photoPreview.style.display='none'; photoPreview.src=''; return; }
      const reader = new FileReader();
      reader.onload = e => {
        photoPreview.src = String(e.target.result);
        photoPreview.style.display = 'block';
      };
      reader.readAsDataURL(f);
    });

    /* 다음 우편번호 */
    function openDaumPostcode(oncomplete){
      if (window.daum?.Postcode) {
        new daum.Postcode({ oncomplete }).open();
        return;
      }
      const start = Date.now();
      (function waitForDaum(){
        if (window.daum?.Postcode) return new daum.Postcode({ oncomplete }).open();
        if (Date.now() - start > 3000) return alert('주소 스크립트를 불러오지 못했습니다. 잠시 후 다시 시도하세요.');
        setTimeout(waitForDaum, 80);
      })();
    }

    function wireAddressPicker(root){
      if (!root) return;
      const searchBtn = root.querySelector('[data-role="search"]');
      const postcode  = root.querySelector('input[name="deliveryPostcode"]');
      const addr1     = root.querySelector('input[name="deliveryAddress1"]');
      const addr2     = root.querySelector('input[name="deliveryAddress2"]');
      if (!searchBtn || !postcode || !addr1 || !addr2) return;

      searchBtn.addEventListener('click', () => {
        openDaumPostcode((data) => {
          postcode.value = data.zonecode || '';
          addr1.value    = data.roadAddress || data.jibunAddress || '';
          addr2.focus();
          ['input','change'].forEach(t=>{
            postcode.dispatchEvent(new Event(t,{bubbles:true}));
            addr1.dispatchEvent(new Event(t,{bubbles:true}));
          });
        });
      });
    }
    wireAddressPicker($('.addr-picker', deliveryBox));

    /* 제출 시 disabled 임시 해제 + CSRF 보강 */
    form.addEventListener('submit', () => {
      const disabled = $$('[disabled]', form);
      disabled.forEach(el => { el.dataset.wasDisabled='1'; el.removeAttribute('disabled'); });
      setTimeout(() => disabled.forEach(el => {
        if (el.dataset.wasDisabled) { el.setAttribute('disabled','disabled'); el.removeAttribute('data-was-disabled'); }
      }), 0);

      const csrfParam = document.querySelector('meta[name="_csrf_parameter"]')?.content;
      const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
      if (csrfParam && csrfToken && !form.querySelector(`input[name="${csrfParam}"]`)) {
        const h = document.createElement('input');
        h.type='hidden'; h.name=csrfParam; h.value=csrfToken; form.appendChild(h);
      }
    });
  });
})();