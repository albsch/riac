$(function() {

    // We can attach the `fileselect` event to all file inputs on the page
    $(document).on('change', ':file', function() {
        var input = $(this),
            numFiles = input.get(0).files ? input.get(0).files.length : 1,
            label = input.val().replace(/\\/g, '/').replace(/.*\//, '');
        input.trigger('fileselect', [numFiles, label]);
    });

    // We can watch for our custom `fileselect` event like this
    $(document).ready( function() {
        $(':file').on('fileselect', function(event, numFiles, label) {

            var input = $(this).parents('.input-group').find(':text'),
                log = numFiles > 1 ? numFiles + ' files selected' : label;

            if( input.length ) {
                input.val(log);
            } else {
                if( log ) alert(log);
            }

        });

        $("#submit-normal").click(function () {
            if(formCheck()) {
                $("#input-inverse").val("false");
                $("#form-submit").submit();
            }
        });

        $("#submit-inverse").click(function () {
            if(formCheck()) {
                $("#input-inverse").val("true");
                $("#form-submit").submit();
            }
        });

        $("#use-generator-a").click(function () {
            if(generatorAFormCheck()) {
                var states = $("#number-states").val();
                if(states === "") states = "100";

                var final = $("#number-final-states").val();
                if(final === "") final = "5";

                var connectivity = $("#connectivity").val();
                if(connectivity === "") connectivity = "10";

                var alphabet = $("#number-symbols").val();
                if(alphabet === "") alphabet = "25";

                $.post( "random", { states: states, final: final, connectivity: connectivity, alphabet: alphabet } ).done(function( data ) {
                    $("#file-name-a").val(data);
                    $('#generate-a-modal').modal('hide');
                    $('#download-a').removeClass('disabled');
                    $('#download-a').attr("href", "automaton/"+data);
                });
            }
        });

        $("#use-generator-b").click(function () {
            if(generatorBFormCheck()) {
                var states = $("#number-states-b").val();
                if(states === "") states = "100";

                var final = $("#number-final-states-b").val();
                if(final === "") final = "5";

                var connectivity = $("#connectivity-b").val();
                if(connectivity === "") connectivity = "10";

                var alphabet = $("#number-symbols-b").val();
                if(alphabet === "") alphabet = "25";

                $.post( "random", { states: states, final: final, connectivity: connectivity, alphabet: alphabet } ).done(function( data ) {
                    $("#file-name-b").val(data);
                    $('#generate-b-modal').modal('hide');
                    $('#download-b').removeClass('disabled');
                    $('#download-b').attr("href", "automaton/"+data);
                });
            }
        });
    });
});

function generatorAFormCheck() {
    var valid = true;

    if($("#number-states").val() > 1000 || $("#number-states").val() < 0) {
        $("#number-states").addClass("err");
        valid = false;
    } else {
        $("#number-states").removeClass("err");
    }

    if($("#number-symbols").val() > 5000 || $("#number-symbols").val() < 0) {
        $("#number-symbols").addClass("err");
        valid = false;
    } else {
        $("#number-symbols").removeClass("err");
    }

    if($("#number-final-states").val() < 0) {
        $("#number-final-states").addClass("err");
        valid = false;
    } else {
        $("#number-final-states").removeClass("err");
    }

    if($("#connectivity").val() < 0 || $("#connectivity").val() > 100) {
        $("#connectivity").addClass("err");
        valid = false;
    } else {
        $("#connectivity").removeClass("err");
    }

    return valid;
}

function generatorBFormCheck() {
    var valid = true;

    if($("#number-states-b").val() > 1000 || $("#number-states").val() < 0) {
        $("#number-states-b").addClass("err");
        valid = false;
    } else {
        $("#number-states-b").removeClass("err");
    }

    if($("#number-symbols-b").val() > 5000 || $("#number-symbols-b").val() < 0) {
        $("#number-symbols-b").addClass("err");
        valid = false;
    } else {
        $("#number-symbols-b").removeClass("err");
    }

    if($("#number-final-states-b").val() < 0) {
        $("#number-final-states-b").addClass("err");
        valid = false;
    } else {
        $("#number-final-states-b").removeClass("err");
    }

    if($("#connectivity-b").val() < 0 || $("#connectivity-b").val() > 100) {
        $("#connectivity-b").addClass("err");
        valid = false;
    } else {
        $("#connectivity-b").removeClass("err");
    }

    return valid;
}

function formCheck() {
    console.log("Checking form");
    var valid = true;

    var selectorA = $("#file-name-a");
    var selectorB = $("#file-name-b");

    if(selectorA.val() === "" || (!selectorA.val().endsWith(".dot") && !selectorA.val().endsWith(".vtf"))) {
        $("#file-a").addClass("error");
        valid = false;
    } else {
        $("#file-a").removeClass("error");
    }

    if(selectorB.val() === "" || (!selectorB.val().endsWith(".dot") && !selectorB.val().endsWith(".vtf"))) {
        $("#file-b").addClass("error");
        valid = false
    } else {
        $("#file-a").removeClass("error");
    }

    return valid;
}
