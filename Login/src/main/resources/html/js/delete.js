"use strict";

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
    if (!checkPassword()) {
        return false;
    }
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
    var password = $("#password");

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

        var password = randomString(16);
        var rsa = new RSAKey();
        rsa.setPublic($("#rsaPub").attr("value"), "10001");
        var encryptedKey = rsa.encrypt(password);

        var data = {
            "username": $('#username').attr("value"),
            "password": $("#password").val(),
            "timestamp": new Date().getTime(),
            "uuid": $("#uuid").attr("value")
        };
        var content = JSON.stringify(data);

        var ukey = CryptoJS.enc.Utf8.parse(password);
        var encryptedData = CryptoJS.AES.encrypt(content, ukey, {
            mode: CryptoJS.mode.ECB,
            padding: CryptoJS.pad.Pkcs7
        });
        $.ajax({
            type: "DELETE",
            url: "../user",
            contentType: "application/json;charset=utf-8;",
            data: JSON.stringify({
                data: encryptedData.toString(),
                key: encryptedKey.toString()
            })
        }).fail(function(msg) {
            try {
                if (msg.status === 401) {
                    location.href = "/auth/";
                    return;
                }
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
            alert("操作成功完成");
            window.location.href = "/auth/setting"
        });
        return false;
    }
});
