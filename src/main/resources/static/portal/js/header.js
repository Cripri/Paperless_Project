// $(document).ready(function () {
//   $(".menu-list > li").on({
//     mouseenter: function () {
//       $(".menu-list > li").find("span").removeClass("selected");
//       $(".menu-list > li").removeClass("selected");
//       $(this).find("span").addClass("selected");
//       $(this).addClass("selected");
//       if (!$("#allmenu").is(':animated')) {
//         $("#allmenu").slideDown();
//       }
//     },
//     mouseout: function () {
//       $("#allmenu").on('mouseleave', function () {
//         $(".menu-list > li").find("span").removeClass("selected");
//         $(".menu-list > li").removeClass("selected");
//         $("#allmenu").hide();
//       });
//     },
//     focusin: function () {
//       $(".menu-list > li").find("span").removeClass("selected");
//       $(".menu-list > li").removeClass("selected");
//       $(this).find("span").addClass("selected");
//       $(this).addClass("selected");
//       if (!$("#allmenu").is(':animated')) {
//         $("#allmenu").slideDown();
//       }
//     },
//     click: function () {
//       const hrefurl = $(this).find("a").attr("href");
//       location.href = hrefurl;
//     }
//   });

//   $(".all-menu-list > li").on({
//     click: function () {
//       const hrefurl = $(this).find("a").attr("href");
//       location.href = hrefurl;
//     },
//     focusin: function () {
//       $(".menu-list > li").find("span").removeClass("selected");
//       $(".menu-list > li").removeClass("selected");
//       $(this).find("span").addClass("selected");
//       $(this).addClass("selected");
//       if (!$("#allmenu").is(':animated')) {
//         $("#allmenu").slideDown();
//       }
//     }
//   });

//   $(".all-menu > div > div.menu:last-child > ul > li:last-child").on({
//     focusout: function () {
//       $(".menu-list > li").find("span").removeClass("selected");
//       $(".menu-list > li").removeClass("selected");
//       $("#allmenu").hide();
//     }
//   });
// });
