<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=no">
        <meta name="format-detection" content="telephone=no" />
        <link rel="stylesheet" type="text/css" href="${Path}/auth/css/button.css">
        <script src="${Path}/auth/js/jquery-3.3.1.min.js"></script>
        <script src="${Path}/auth/js/jsbn.js"></script>
        <script src="${Path}/auth/js/prng4.js"></script>
        <script src="${Path}/auth/js/rng.js"></script>
        <script src="${Path}/auth/js/rsa.js"></script>
        <script src="${Path}/auth/js/aes.js"></script>
        <script src="${Path}/auth/js/common.js"></script>
        <title>确认用户删除</title>
    </head>
    <style>
        body{font-size:80%;font-family:"SimHei",Arial;overflow-x:hidden;overflow-y:auto;}
        input{width:95%;height:22px;}
    </style>
    <script>
        function doPost() {
            var password = randomString(16);
            var rsaPublicKey = "${rsa}";
            var rsa = new RSAKey();
            rsa.setPublic(rsaPublicKey, "10001");
            var encryptedKey = rsa.encrypt(password);

            var data = {
                "username": $("#username").html(),
                "password": $("#password").val()
            };
            var content = JSON.stringify(data);

            var ukey = CryptoJS.enc.Utf8.parse(password);
            var encryptedData = CryptoJS.AES.encrypt(content, ukey, {
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
                    var json = JSON.parse(msg.responseText);
                    if (json.message) {
                        $("#errorMessage").html(htmlEncode(json.message));
                    } else {
                        location.reload(true);
                    }
                } catch (e) {
                    location.reload(true);
                }
            }).done(function(msg) {
                alert("操作成功完成");
                window.location.href = "${Path}/auth/setting"
            });
            return false;
        }
    </script>
    <body bgcolor="#eee">
    <br><br>
    <table style="width:100%; max-width:320px; margin-left:auto; margin-right:auto">
        <thead>
        <tr>
            <th colspan="2"><h1>确认用户删除</h1></th>
        </tr>
        </thead>
        <form name="deleteForm" method="post" onsubmit="return doPost()">
            <tbody>
            <tr>
                <td style="width:80px">将删除用户；</td>
                <td><label id="username">${username}</label></td>
            </tr>
            <tr>
                <td>管理员密码：</td>
                <td><input type="password" id="password" value=""/></td>
            </tr>
            <tr>
                <td colspan=2 style=""><label id="errorMessage" style="color:red;"></label></td>
            </tr>
            <tr>
                <td></td>
                <td><div style="width:97%; text-align:right">
                    <button type="submit" class="button button-primary button-square button-small" style="width:100px;">确认删除</button>
                </div></td>
            </tr>
            </tbody>
        </form>
    </table>
    </body>
</html>