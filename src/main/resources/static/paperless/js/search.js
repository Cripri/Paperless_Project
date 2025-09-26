// 민원 검색/정렬/페이지네이션 스크립트 (Vanilla JS)
// 인트로/헤더 테마와 무관, DOMContentLoaded 후 동작

document.addEventListener('DOMContentLoaded', () => {
  // ===== 샘플 데이터 (실제 데이터와 연결 시 이 배열을 서버 데이터로 교체) =====
  // createdAt은 최신순 정렬용 (YYYY-MM-DD)
  const SERVICES = [
    { id: 'resident-copy',        name: '주민등록등본 발급',       href: '/minwon/certificates/resident-copy',     tag:'등본',     createdAt:'2025-09-01' },
    { id: 'resident-abstract',    name: '주민등록초본 발급',       href: '/minwon/certificates/resident-abstract', tag:'초본',     createdAt:'2025-08-28' },
    { id: 'move-transfer',        name: '전입신고',               href: '/minwon/move/transfer',                  tag:'전입',     createdAt:'2025-08-20' },
    { id: 'family-cert',          name: '가족관계증명서 발급',     href: '/minwon/certificates/family',            tag:'가족',     createdAt:'2025-08-18' },
    { id: 'passport-reissue',     name: '여권 재발급 신청',        href: '/minwon/passport/reissue',               tag:'여권',     createdAt:'2025-08-15' },
    { id: 'id-reissue',           name: '주민등록증 재발급',       href: '/minwon/id/reissue',                     tag:'신분증',   createdAt:'2025-08-10' },
    { id: 'seal-cert',            name: '인감증명서 발급',         href: '/minwon/certificates/seal',              tag:'인감',     createdAt:'2025-08-02' },
    { id: 'birth-report',         name: '출생신고',               href: '/minwon/life/birth',                     tag:'출생',     createdAt:'2025-07-29' },
    { id: 'marriage-report',      name: '혼인신고',               href: '/minwon/life/marriage',                  tag:'혼인',     createdAt:'2025-07-20' },
    { id: 'death-report',         name: '사망신고',               href: '/minwon/life/death',                     tag:'사망',     createdAt:'2025-07-16' },
    { id: 'driver-change',        name: '운전면허 주소변경',        href: '/minwon/driver/address',                 tag:'운전면허', createdAt:'2025-07-10' },
    { id: 'vehicle-reg',          name: '자동차 등록증 재발급',     href: '/minwon/vehicle/registration',           tag:'자동차',   createdAt:'2025-07-05' },
    { id: 'tax-certificate',      name: '지방세 납세증명서 발급',   href: '/minwon/tax/local',                      tag:'세금',     createdAt:'2025-06-30' },
    { id: 'health-cert',          name: '건강보험 자격득실 확인서',  href: '/minwon/nhis/eligibility',               tag:'건강보험', createdAt:'2025-06-18' },
    { id: 'employment-cert',      name: '고용보험 자격이력 내역서',  href: '/minwon/moel/employment',                tag:'고용',     createdAt:'2025-06-10' },
    { id: 'biz-reg',              name: '사업자등록증명',           href: '/minwon/nts/biz',                        tag:'사업자',   createdAt:'2025-06-02' },
    { id: 'address-change',       name: '주소변경 통합신고',         href: '/minwon/address/change',                 tag:'주소',     createdAt:'2025-05-28' },
    { id: 'school-cert',          name: '재학(재적)증명서',          href: '/minwon/edu/enroll',                     tag:'교육',     createdAt:'2025-05-20' },
    { id: 'scholarship',          name: '장학금 신청',               href: '/minwon/edu/scholarship',                tag:'교육',     createdAt:'2025-05-12' },
    { id: 'pet-reg',              name: '반려동물 등록',             href: '/minwon/pet/registration',               tag:'동물',     createdAt:'2025-05-01' },
    { id: 'welfare-benefit',      name: '기초생활 수급 신청',        href: '/minwon/welfare/basic',                  tag:'복지',     createdAt:'2025-04-25' },
    { id: 'parking-permit',       name: '거주자 주차 허가',           href: '/minwon/parking/permit',                 tag:'주차',     createdAt:'2025-04-12' },
    { id: 'noise-report',         name: '층간소음 상담/민원',         href: '/minwon/environment/noise',              tag:'환경',     createdAt:'2025-04-01' },
    { id: 'lost-report',          name: '분실물 신고',               href: '/minwon/police/lost',                    tag:'신고',     createdAt:'2025-03-22' },
    { id: 'complaint',            name: '일반 민원 접수',             href: '/minwon/general/complaint',              tag:'민원',     createdAt:'2025-03-10' },
  ];

  // ===== 상태 =====
  let keyword   = '';
  let pageSize  = 10;
  let sortBy    = 'latest'; // 'latest' | 'name'
  let page      = 1;
  let filtered  = [...SERVICES];

  // ===== DOM =====
  const searchForm  = document.getElementById('search-form');
  const searchInput = document.getElementById('search-input');
  const pageSizeSel = document.getElementById('page-size');
  const sortSel     = document.getElementById('sort-by');
  const listEl      = document.getElementById('list');
  const prevBtn     = document.getElementById('prev');
  const nextBtn     = document.getElementById('next');
  const pageInfo    = document.getElementById('page-info');
  const resultCount = document.getElementById('result-count');

  // ===== 유틸 =====
  const normalize = s => (s || '').toString().toLowerCase().trim();
  const compareLatest = (a,b) => (b.createdAt?.localeCompare(a.createdAt||'') || 0);
  const compareName   = (a,b) => a.name.localeCompare(b.name, 'ko');

  function applyFilter(){
    const k = normalize(keyword);
    filtered = SERVICES.filter(s => {
      if (!k) return true;
      return normalize(s.name).includes(k) || normalize(s.tag).includes(k);
    });

    // 정렬
    filtered.sort(sortBy === 'name' ? compareName : compareLatest);

    // 첫 페이지로
    page = 1;
    render();
  }

  function paginate(arr, page, pageSize){
    const start = (page-1) * pageSize;
    return arr.slice(start, start + pageSize);
  }

  function render(){
    // 카운트/페이지 계산
    const total = filtered.length;
    const totalPages = Math.max(1, Math.ceil(total / pageSize));
    if (page > totalPages) page = totalPages;

    resultCount.textContent = `총 ${total}건`;
    pageInfo.textContent = `${page} / ${totalPages}`;

    prevBtn.disabled = (page <= 1);
    nextBtn.disabled = (page >= totalPages);

    // 목록 렌더
    const view = paginate(filtered, page, pageSize);
    listEl.innerHTML = view.map(item => `
      <div class="item" role="listitem">
        <div class="item-col--grow">
          <div class="item-title">${item.name}</div>
          <div class="item-sub">분류: <span class="badge">${item.tag}</span> · 등록일: ${item.createdAt}</div>
        </div>
        <a class="btn primary" href="${item.href}">작성하기</a>
      </div>
    `).join('') || `
      <div class="item"><div class="item-col--grow">검색 결과가 없습니다.</div></div>
    `;
  }

  // ===== 이벤트 =====
  searchForm.addEventListener('submit', (e) => {
    e.preventDefault();
    keyword = searchInput.value;
    applyFilter();
  });

  pageSizeSel.addEventListener('change', e => {
    pageSize = parseInt(e.target.value, 10) || 10;
    // 페이지 크기 바뀌면 현재 페이지 유지 가능한 범위에서 다시 그리기
    render();
  });

  sortSel.addEventListener('change', e => {
    sortBy = e.target.value;
    applyFilter();
  });

  prevBtn.addEventListener('click', () => {
    if (page > 1){
      page--;
      render();
    }
  });

  nextBtn.addEventListener('click', () => {
    const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
    if (page < totalPages){
      page++;
      render();
    }
  });

  // 초기 상태
  render();
});
