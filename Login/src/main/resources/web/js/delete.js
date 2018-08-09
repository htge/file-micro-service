"use strict";

function showErrorMessage(message) {
    const error = $(".invalid-feedback");
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
    const password = $("#password");
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
    const password = $("#password");

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

        const password = randomString(16);
        const rsa = new RSAKey();
        rsa.setPublic($("#rsaPub").attr("value"), "10001");
        const encryptedKey = rsa.encrypt(password);

        const data = {
            "username": $('#username').attr("value"),
            "password": $("#password").val(),
            "timestamp": new Date().getTime(),
            "uuid": $("#uuid").attr("value")
        };
        const content = JSON.stringify(data);

        const ukey = CryptoJS.enc.Utf8.parse(password);
        const encryptedData = CryptoJS.AES.encrypt(content, ukey, {
            mode: CryptoJS.mode.ECB,
            padding: CryptoJS.pad.Pkcs7
        });
        $.ajax({
            type: "POST",
            url: "../delete",
            data: {
                data: encryptedData.toString(),
                key: encryptedKey.toString()
            }
        }).fail(function(msg) {
            try {
                const json = JSON.parse(msg.responseText);
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
