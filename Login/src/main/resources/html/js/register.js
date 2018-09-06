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
    if (!checkUsername()) {
        return false;
    }
    if (!checkNewPassword()) {
        return false;
    }
    if (!checkValidation()) {
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
    if (!username.val().match("^[a-zA-Z0-9]{4,20}$")) {
        username.addClass("is-invalid");
        showErrorMessage("用户名只能允许字母和数字，长度在4~20之间");
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
    if (password.val().length < 8 || password.val().length > 32) {
        password.addClass("is-invalid");
        showErrorMessage("密码长度只能在8~32之间");
        return false;
    }
    password.addClass("is-valid");
    checkErrorMessage();
    return true;
}

function checkNewPassword() {
    var password = $("#newPassword");
    if (!password.val()) {
        password.addClass("is-invalid");
        showErrorMessage("请输入密码");
        return false;
    }
    if (password.val().length < 8 || password.val().length > 32) {
        password.addClass("is-invalid");
        showErrorMessage("密码长度只能在8~32之间");
        return false;
    }
    password.addClass("is-valid");
    checkErrorMessage();
    return true;
}

function checkValidation() {
    var password = $("#newPassword");
    var validation = $("#validation");
    if (!validation.val()) {
        validation.addClass("is-invalid");
        showErrorMessage("请输入确认密码");
        return false;
    }
    if (validation.val() !== password.val()) {
        validation.addClass("is-invalid");
        showErrorMessage("两次输入的密码不匹配");
        return false;
    }
    validation.addClass("is-valid");
    checkErrorMessage();
    return true;
}

$(document).ready(function () {
    var password = $("#password");
    var username = $("#username");
    var newPassword = $("#newPassword");
    var validation = $("#validation");

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

    validation.focusin(function () {
        $(this).removeClass("is-invalid");
    });
    validation.focusout(function () {
        checkValidation();
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
            "password": $("#password").val(),
            "username": $("#username").val(),
            "newPassword": $("#newPassword").val(),
            "validation": $("#validation").val(),
            "role": $("#role:checked").val(),
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
            type: "POST",
            url: "user",
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
        }).done(function() {
            alert("操作成功完成");
            window.location.href = "/auth/setting"
        });
        return false;
    }
});
