(() => {
  const slider = document.getElementById('banner-slider');
  if(!slider) return;

  const slidesWrap = slider.querySelector('.slides');
  const slides = Array.from(slidesWrap.children);
  const prevBtn = slider.querySelector('.prev');
  const nextBtn = slider.querySelector('.next');
  const dotsWrap = slider.querySelector('.dots');

  let idx = 0, timer = null, speed = 8000;

  // 도트 생성
  slides.forEach((_, i) => {
    const b = document.createElement('button');
    if(i === 0) b.classList.add('active');
    b.addEventListener('click', () => go(i, true));
    dotsWrap.appendChild(b);
  });

  const update = () => {
    slidesWrap.style.transform = `translateX(-${idx * 100}%)`;
    dotsWrap.querySelectorAll('button').forEach((d, i) => {
      d.classList.toggle('active', i === idx);
    });
  };

  const go = (n, reset=false) => {
    idx = (n + slides.length) % slides.length;
    update();
    if(reset) restart();
  };

  const next = () => go(idx + 1);
  const prev = () => go(idx - 1);

  nextBtn.addEventListener('click', () => go(idx + 1, true));
  prevBtn.addEventListener('click', () => go(idx - 1, true));

  // 자동재생 + 호버 시 일시정지
  const start = () => { timer = setInterval(next, speed); };
  const stop  = () => { if(timer) clearInterval(timer); };
  const restart = () => { stop(); start(); };

  slider.addEventListener('mouseenter', stop);
  slider.addEventListener('mouseleave', start);

  // 초기화
  update(); start();
})();