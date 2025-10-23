// 민원 검색/정렬/페이지네이션 스크립트 (Vanilla JS)
// 인트로/헤더 테마와 무관, DOMContentLoaded 후 동작

document.addEventListener('DOMContentLoaded', () => {
  // ===== 샘플 데이터 (실제 데이터와 연결 시 이 배열을 서버 데이터로 교체) =====
  // createdAt은 최신순 정렬용 (YYYY-MM-DD)
  const SERVICES = [
    { id: 'resident-copy',        name: '주민등록등본 발급',       href: '/residentregistration/form',     tag:'등본',     createdAt:'2025-09-01' },
    { id: 'resident-abstract',    name: '주민등록초본 발급',       href: '/residentregistration/form', tag:'초본',     createdAt:'2025-08-28' },
    { id: 'passport-reissue',     name: '여권 재발급 신청',        href: '/passport/apply',               tag:'여권',     createdAt:'2025-08-15' }
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
