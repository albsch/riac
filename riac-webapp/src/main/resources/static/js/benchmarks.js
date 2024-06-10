var cachedResults = {};


$(function() {

    $('#detail-modal').on('show.bs.modal', function () {
        $('.modal .modal-body').css('overflow-y', 'auto');
        $('#modal-pre').css('max-height', $(window).height() * 0.8);
    });

    function isElementInViewport (el) {
        //special bonus for those using jQuery
        if (typeof jQuery === "function" && el instanceof jQuery) {
            el = el[0];
        }

        var rect = el.getBoundingClientRect();

        return (
            rect.top >= 0 &&
            rect.left >= 0 &&
            rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) && /*or $(window).height() */
            rect.right <= (window.innerWidth || document.documentElement.clientWidth) /*or $(window).width() */
        );
    }

    function onVisibilityChange(el, callback) {
        var old_visible;
        return function () {
            var visible = isElementInViewport(el);
            if (visible !== old_visible) {
                old_visible = visible;
                if (typeof callback === 'function') {
                    callback(el, visible);
                }
            }
        }
    }

    function convertTime(time) {
        if(!time) {
            return "timeout";
        }
        //nano seconds
        if(time < 1000) {
            return time + " ns";
        }
        //milli seconds
        else if(time < 1000000) {
            return Math.round(time/1000) + " ms";
        }
        //seconds seconds
        else if(time < 1000000000) {
            return Math.round(time/1000000) + " s";
        }
    }

    function fillData(data) {
        $('#showing-result').show();
        $('#loading-result').hide();

        var res = $('#single-result');
        if(data.included === true) {
            res.html("&#8466;(A) &sube; &#8466;(B)");
            res.css("color","#90c8f9");
        }
        else if(data.included === false) {
            res.html("&#8466;(A) &#8840; &#8466;(B)");
            res.css("color","orangered");
        }
        console.log(data.timeout);
        if(data.timeout) {
            res.html("Timeout");
            res.css("color","whitesmoke");
        }
        console.log(data.memory);
        if(data.memory) {
            res.html("Memory");
            res.css("color","whitesmoke");
        }

        $('#automaton-a-name').html("File: "+data.automatonA);
        $('#automaton-b-name').html("File: "+data.automatonB);

        $('#automaton-a-states').html("States: "+data.automatonAStates);
        $('#automaton-b-states').html("States: "+data.automatonBStates);

        $('#automaton-a-final-states').html("Final States: "+data.automatonAFinal);
        $('#automaton-b-final-states').html("Final States: "+data.automatonBFinal);

        $('#automaton-a-alphabet').html("Alphabet Size: "+data.automatonASymbols);
        $('#automaton-b-alphabet').html("Alphabet Size: "+data.automatonBSymbols);

        $('#result-convert').html("Convert Time: "+convertTime(data.timeConvert));
        $('#result-solved').html("Computing Time: "+convertTime(data.time));
    }

    $(document).ready( function() {
        // $(window).scroll(function(){
        //     $('.top-header').css({
        //         'right': -$(this).scrollLeft() + 300,
        //         'top' : $(this).scrollTop() + 15
        //     });
        // });
        //
        // var handler = onVisibilityChange($('.top-header'), function(el, visible) {
        //     /* your code go here */
        //     console.log("Visibility change -> "+visible);
        //     console.log(el);
        //     if(!visible){
        //         el.css("position","absolute");
        //     } else {
        //         //el.css("position","");
        //     }
        //
        // });
        // $(window).on('DOMContentLoaded load resize scroll', handler);

        $('.resultcell').filter(function(){
            var targetRow = $(this).data('row');
            var targetCol = $(this).data('col');
            return targetRow === targetCol;
        }).css("box-shadow", "0px 0px 5px #fff");

        $('.top-header').css('opacity','0.0');

        var benchCells = $('[data-toggle="bench-cell"]');

        benchCells.click(function (event) {
            $('#detail-modal').modal("show");
            var automatonA = $(this).data('automaton-a');
            var automatonB = $(this).data('automaton-b');

            var requrl = "result?set="+set+"&automatonA="+automatonA+"&automatonB="+automatonB+"&prefix="+prefix+"&includePre=true";
            $.get( requrl, function( data ) {
                $('#modal-pre').text(data.output);
            });

            var aUrl = "result/download?set="+set+"&automaton="+automatonA+"&prefix="+prefix;
            $('#download-a').attr("href", aUrl);
            var bUrl = "result/download?set="+set+"&automaton="+automatonB+"&prefix="+prefix;
            $('#download-b').attr("href", bUrl);
        });

        benchCells.mousemove(function (event) {
            var currentCell = $(this);
            var targetCells = $('.resultcell');


            $('.top-header').css("opacity","0.0");
            $('.top-header').filter(function(){
                var targetCol = $(this).data('header-col');
                return currentCell.data('col') === targetCol;
            }).css("opacity", "1.0");

            targetCells.css("opacity", "1.0");
            targetCells.filter(function(){
                var targetRow = $(this).data('row');
                var targetCol = $(this).data('col');
                return ((currentCell.data('row') === targetRow) && (currentCell.data('col') >= targetCol))
                    || ((currentCell.data('col') === targetCol) && (currentCell.data('row') >= targetRow));
            }).css("opacity", "0.7");

            var offsetLeftTarget = event.pageX + 20;
            var offsetTopTarget = event.pageY + 20;

            var magGlass = $('.magnifying-glass');
            magGlass.show();
            magGlass.css("left",offsetLeftTarget);
            magGlass.css("top",offsetTopTarget);

            $('#showing-result').hide();
            $('#loading-result').show();
            $('#error-result').hide();


            var full = event.currentTarget.id;
            var splitAutomata = full.split("__");

            var a = splitAutomata[0];
            var b = splitAutomata[1];

            var reqdata;
            if(!cachedResults[full]) {
                var requrl = "result?set="+set+"&automatonA="+a+"&automatonB="+b+"&prefix="+prefix;
                $.get( requrl, function( data ) {
                    reqdata = data;
                    cachedResults[full] = data;
                    fillData(data);
                }).fail(function() {
                    cachedResults[full] = "error";
                });
            }
            else if(cachedResults[full] === "error") {
                $('#showing-result').hide();
                $('#loading-result').hide();
                $('#error-result').show();
            }
            else {
                reqdata = cachedResults[full];
                fillData(reqdata);
            }
        });

        benchCells.mouseleave(function (event) {
            $('.magnifying-glass').hide();
            $('.resultcell').css("opacity", "1.0");
            $('.top-header').css("opacity","0.0");
        });

    });
});

