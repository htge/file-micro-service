"use strict";
const usernameKey = "file.login.username";

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
    if (!checkUsername()) {
        return false;
    }
    if (!checkPassword()) {
        return false;
    }
    return true;
}

function checkUsername() {
    const username = $("#username");
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
    const username = $("#username");
    const password = $("#password");

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

        const randomPassword = randomString(16);
        const rsa = new RSAKey();
        rsa.setPublic($("#rsaPub").attr("value"), "10001");
        const encryptedKey = rsa.encrypt(randomPassword);
        const username = $("#username").val();
        const password = $("#password").val();

        $.cookie(usernameKey, username);

        const data = {
            "username": username,
            "password": password,
            "timestamp": new Date().getTime(),
            "uuid": $("#uuid").attr("value")
        };
        const content = JSON.stringify(data);

        const ukey = CryptoJS.enc.Utf8.parse(randomPassword);
        const encryptedData = CryptoJS.AES.encrypt(content, ukey, {
            mode: CryptoJS.mode.ECB,
            padding: CryptoJS.pad.Pkcs7
        });
        $.ajax({
            type: "POST",
            url: "login",
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
            if (msg.url) {
                window.location.href = msg.url;
            } else {
                window.location.href = "/auth/";
            }
        });
        return false;
    }
});
