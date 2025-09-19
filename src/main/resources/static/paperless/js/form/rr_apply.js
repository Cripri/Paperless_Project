// ===== 전부포함 선택 시 나머지 옵션 비활성화 =====
const includeAllRadios = document.querySelectorAll('input[name="includeAll"]');
const partialBox = document.getElementById('partialBox');

function refreshPartial() {
  const selected = [...includeAllRadios].find(r => r.checked)?.value;
  if (selected === 'ALL') {
    partialBox.classList.add('disabled');
    [...partialBox.querySelectorAll('input,select,button,textarea')].forEach(el => {
      el.setAttribute('disabled','disabled');
    });
  } else {
    partialBox.classList.remove('disabled');
    [...partialBox.querySelectorAll('input,select,button,textarea')].forEach(el => {
      el.removeAttribute('disabled');
    });
  }
}
includeAllRadios.forEach(r => r.addEventListener('change', refreshPartial));
window.addEventListener('DOMContentLoaded', refreshPartial);

// ===== 서명 팝업 연동 =====
const signPreview = document.getElementById('signPreview');
const signatureBase64 = document.getElementById('signatureBase64');
const openBtn = document.getElementById('openSignPopup');
const clearBtn = document.getElementById('clearSignature');
let signPopup;

const signUrlHidden = document.getElementById('signUrl');
const signUrl = signUrlHidden ? signUrlHidden.value : '/residentregistration/sign';

openBtn?.addEventListener('click', () => {
  const w = 480, h = 360;
  const left = Math.round((window.screen.width - w) / 2);
  const top = Math.round((window.screen.height - h) / 2);
  signPopup = window.open(signUrl, 'signPopup',
    `width=${w},height=${h},left=${left},top=${top},resizable=no,scrollbars=no`);
  if (!signPopup) {
    alert('팝업이 차단되었습니다. 팝업 허용 후 다시 시도하세요.');
  }
});

window.addEventListener('message', (e) => {
  const msg = e.data;
  if (!msg || msg.type !== 'SIGN_DONE' || !msg.dataUrl) return;
  signPreview.src = msg.dataUrl;
  signatureBase64.value = msg.dataUrl;
  try { signPopup && signPopup.close(); } catch (_) {}
});

clearBtn?.addEventListener('click', () => {
  signPreview.removeAttribute('src');
  signatureBase64.value = '';
});

// ===== 새 DB 스키마에 맞춰 extraJson 구성 후 전송 =====
const form = document.getElementById('rr-form');
const extraJsonHidden = document.getElementById('extraJson');

form.addEventListener('submit', function(e) {
  const applicantName = document.getElementById('applicantName')?.value || '';
  const address1 = document.getElementById('address1')?.value || '';
  const address2 = document.getElementById('address2')?.value || '';
  const phone = document.getElementById('phone')?.value || '';
  const feeExempt = (document.querySelector('input[name="feeExempt"]:checked')?.value === 'true');
  const feeExemptReason = document.getElementById('feeExemptReason')?.value || '';

  // 2) 포함 범위
  const includeAll = (document.querySelector('input[name="includeAll"]:checked')?.value === 'ALL');

  const addrMode = document.querySelector('input[name="addressHistoryMode"]:checked')?.value || 'NONE';
  const addrYears = parseInt(document.querySelector('[name="addressHistoryYears"]')?.value || '0', 10);

  const includeHouseholdReason = document.querySelector('[name="includeHouseholdReason"]')?.checked || false;
  const includeHouseholdDate   = document.querySelector('[name="includeHouseholdDate"]')?.checked || false;
  const includeOccurReportDates= document.querySelector('[name="includeOccurReportDates"]')?.checked || false;

  const changeReasonScope = document.querySelector('input[name="changeReasonScope"]:checked')?.value || 'NONE';
  const includeOtherNames  = document.querySelector('[name="includeOtherNames"]')?.checked || false;

  const rrnBackInclusion = document.querySelector('input[name="rrnBackInclusion"]:checked')?.value || 'NONE'; // NONE/SELF/HOUSEHOLD
  const includeRelationshipToHead = document.querySelector('[name="includeRelationshipToHead"]')?.checked || false;
  const includeCohabitants = document.querySelector('[name="includeCohabitants"]')?.checked || false;

  // 3) JSON 스키마
  const options = {
    includeAll: includeAll,
    previousAddress: (includeAll ? { mode: "all" } : (
      addrMode === 'ALL'    ? { mode: "all" } :
      addrMode === 'RECENT' ? { mode: "recent", years: isFinite(addrYears) && addrYears>0 ? addrYears : null } :
      addrMode === 'CUSTOM' ? { mode: "custom" } : { mode: "exclude" }
    )),
    includeHouseholdReason: includeHouseholdReason,
    includeDates: { occurrence: includeHouseholdDate, report: includeOccurReportDates },
    changeReasonScope: changeReasonScope, // NONE/HOUSEHOLD/ALL_MEMBERS
    includeOtherNames: includeOtherNames,
    rrnBackMode: (rrnBackInclusion === 'NONE' ? 'none' :
                  rrnBackInclusion === 'SELF' ? 'self' : 'household'),
    includeRelationshipToHead: includeRelationshipToHead,
    includeCohabitants: includeCohabitants
  };

  const payload = {
    docType: document.getElementById('docType')?.value || 'resident_registration',
    applicant: { name: applicantName, address1: address, address2: address, phone: phone },
    fee: { exempt: feeExempt, reason: feeExempt ? feeExemptReason : null },
    signatureBase64: document.getElementById('signatureBase64')?.value || null,
    options: options
  };

  // 히든 필드에 JSON 문자열로 싣기 → 컨트롤러에서 Map으로 파싱
  extraJsonHidden.value = JSON.stringify(payload);
});

// rr_apply.js - 요지 (saveLocal 버튼을 서버 POST로 전송)
document.getElementById('saveLocal')?.addEventListener('click', () => {
  const form = document.createElement('form');
  form.method = 'POST';
  form.action = '/residentregistration/preview';
  const names = [
    'applicantName','rrnFront','rrnBack','address1','address2','phone',
    'feeExempt','feeExemptReason','includeAll','addressHistoryMode','addressHistoryYears',
    'includeHouseholdReason','includeHouseholdDate','includeOccurReportDates',
    'changeReasonScope','includeOtherNames','rrnBackInclusion',
    'includeRelationshipToHead','includeCohabitants','signatureBase64',
    'docType','consentYn','extraJson'
  ];
  const get = (n)=>{
    const els = document.getElementsByName(n);
    if(!els || els.length===0) return '';
    if(els.length>1 && els[0].type==='radio'){
      const c = Array.from(els).find(e=>e.checked);
      return c? c.value : '';
    }
    const el = els[0];
    if(el.type==='checkbox') return el.checked ? 'Y' : 'N'; // Y/N로 보낸다면
    return el.value ?? '';
  };
  const add = (n,v)=>{
    const i=document.createElement('input');
    i.type='hidden'; i.name=n; i.value=v??''; form.appendChild(i);
  };
  names.forEach(n=>add(n, get(n)));

  // CSRF 있으면 메타/히든에서 꺼내 붙이기
  const csrfParam = document.querySelector('meta[name="_csrf_parameter"]')?.content;
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
  if(csrfParam && csrfToken){ add(csrfParam, csrfToken); }

  document.body.appendChild(form);
  form.submit();
});

