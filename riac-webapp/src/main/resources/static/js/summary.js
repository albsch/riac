$(function() {
    $(document).ready( function() {
        $(".compare-link").mousemove(function () {
            $('.compare-link').css("box-shadow", "");
            var currentText = $(this).html();
            $('.compare-link').filter(function(){
                var targetText = $(this).html();
                return currentText === targetText;
            }).css("box-shadow", "0px 0px 5px #fff");
        });
    });
});

