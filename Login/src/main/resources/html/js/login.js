"use strict";
var usernameKey = "file.login.username";

function showErrorMessage(message) {
    var error = $(".invalid-feedback");
    error.html(message);
    error.css("display", message?"block":"none");
}

function checkErrorMessage() {
    if ($(".is-invalid").length === 0) {
        showErrorMessage("");
    }
}

function inputCheck() {
    if (!checkUsername()) {
        return false;
    }
    if (!checkPassword()) {
        return false;
    }
    return true;
}

function checkUsername() {
    var username = $("#username");
    if (!username.val()) {
        username.addClass("is-invalid");
        showErrorMessage("请输入用户名");
        return false;
    }
    username.addClass("is-valid");
    checkErrorMessage();
    return true;
}

function checkPassword() {
    var password = $("#password");
    if (!password.val()) {
        password.addClass("is-invalid");
        showErrorMessage("请输入密码");
        return false;
    }
    password.addClass("is-valid");
    checkErrorMessage();
    return true;
}

$(document).ready(function () {
    var username = $("#username");
    var password = $("#password");

    username.prop("value", $.cookie(usernameKey))

    username.focusin(function () {
        $(this).removeClass("is-invalid");
    });
    username.focusout(function () {
        checkUsername();
    });

    password.focusin(function () {
        $(this).removeClass("is-invalid");
    });
    password.focusout(function () {
        checkPassword();
    });

    $(".form").submit(function () {
        return doSubmit();
    });

    function doSubmit() {
        if (!inputCheck()) {
            return false;
        }

        var randomPassword = randomString(16);
        var rsa = new RSAKey();
        rsa.setPublic($("#rsaPub").attr("value"), "10001");
        var encryptedKey = rsa.encrypt(randomPassword);
        var username = $("#username").val();
        var password = $("#password").val();

        $.cookie(usernameKey, username);

        var data = {
            "username": username,
            "password": password,
            "timestamp": new Date().getTime(),
            "uuid": $("#uuid").attr("value")
        };
        var content = JSON.stringify(data);

        var ukey = CryptoJS.enc.Utf8.parse(randomPassword);
        var encryptedData = CryptoJS.AES.encrypt(content, ukey, {
            mode: CryptoJS.mode.ECB,
            padding: CryptoJS.pad.Pkcs7
        });
        $.ajax({
            type: "GET",
            url: "user",
            data: {
                data: encryptedData.toString(),
                key: encryptedKey.toString()
            }
        }).fail(function(msg) {
            try {
                if (msg.status === 406) {
                    location.reload(true);
                    return;
                }
                var json = JSON.parse(msg.responseText);
                if (json.message) {
                    showErrorMessage(htmlEncode(json.message));
                } else {
                    location.reload(true);
                }
            } catch (e) {
                location.reload(true);
            }
        }).done(function(msg) {
            if (msg.url) {
                window.location.href = msg.url;
            } else {
                window.location.href = "/auth/";
            }
        });
        return false;
    }
});
