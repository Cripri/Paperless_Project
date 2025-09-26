/* global $ */
$(function () {
  const $allmenu = $('#allmenu');
  const $groups  = $('.menu-group');

  function openMenuFor(group){
    // 1) 모든 그룹 숨기고 대상만 show
    $groups.hide();
    const $target = $groups.filter('[data-group="'+group+'"]').show();

    // 2) 컨테이너는 auto-height로 부드럽게
    if (!$allmenu.is(':visible')) {
      $allmenu.stop(true, true).slideDown(160);
    }
  }

  // 상단 GNB hover/focus
  $('.menu-list > li').on({
    mouseenter: function(){
      const g = $(this).data('group');
      $('.menu-list > li .bar, .menu-list > li').removeClass('selected');
      $(this).find('.bar').addClass('selected');
      $(this).addClass('selected');
      if (g) openMenuFor(g);
    },
    focusin: function(){
      const g = $(this).data('group');
      $('.menu-list > li .bar, .menu-list > li').removeClass('selected');
      $(this).find('.bar').addClass('selected');
      $(this).addClass('selected');
      if (g) openMenuFor(g);
    },
    click: function(){
      const href = $(this).find('a').attr('href');
      if (href && href !== 'javascript:void(0)') location.href = href;
    }
  });

  // 전체메뉴 바깥으로 나가면 닫기
  $allmenu.on('mouseleave', function(){
    $('.menu-list > li .bar, .menu-list > li').removeClass('selected');
    $allmenu.stop(true, true).slideUp(120);
  });

  // 서브메뉴 클릭 이동
  $('.all-menu-list > li').on('click', function(){
    const href = $(this).find('a').attr('href');
    if (href) location.href = href;
  });

  // 키보드 탭으로 마지막 항목 빠져나오면 닫기
  $('.all-menu > div > div.menu:last-child > ul > li:last-child').on('focusout', function () {
    $('.menu-list > li .bar, .menu-list > li').removeClass('selected');
    $allmenu.hide();
  });
});
