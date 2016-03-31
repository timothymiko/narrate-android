$(document).ready(function(){
    
    //nojs
    $("body").removeClass("no-js");
    
    //------------------------------------------------------------------------//
    
    //fakelink
    $('a[href="#"]').on('click',function(e){e.preventDefault();});
    
    //------------------------------------------------------------------------//
    
    //placeholder
    $('input[placeholder], textarea[placeholder]').placeholder();
    
    //------------------------------------------------------------------------//
    
    //tab
    $('.tabs').delegate('li:not(.active)','click',function(){$(this).addClass('active').siblings().removeClass('active').parents('.tab').find('.box').hide().eq($(this).index()).fadeIn(250);});
    
    
    
});//document ready