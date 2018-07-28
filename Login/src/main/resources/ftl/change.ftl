<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=no">
        <meta name="format-detection" content="telephone=no" />

        <link rel="stylesheet" href="${Path}/auth/css/common.css">
        <link rel="stylesheet" href="${Path}/auth/css/bootstrap.min.css">
        <link rel="stylesheet" href="${Path}/auth/css/bootstrap-grid.min.css">
        <link rel="stylesheet" href="${Path}/auth/css/bootstrap-reboot.min.css">

        <script type="text/javascript" src="${Path}/auth/js/jquery-3.3.1.min.js"></script>
        <script type="text/javascript" src="${Path}/auth/js/bootstrap.min.js"></script>
        <script type="text/javascript" src="${Path}/auth/js/jsbn.js"></script>
        <script type="text/javascript" src="${Path}/auth/js/prng4.js"></script>
        <script type="text/javascript" src="${Path}/auth/js/rng.js"></script>
        <script type="text/javascript" src="${Path}/auth/js/rsa.js"></script>
        <script type="text/javascript" src="${Path}/auth/js/aes.js"></script>
        <script type="text/javascript" src="${Path}/auth/js/common.js"></script>
        <script type="text/javascript" src="${Path}/auth/js/change.js"></script>
        <title>修改密码</title>
    </head>
    <script>
        "use strict";
        setRsaPublicKey("${rsa}");
    </script>
    <body class="body">
        <br/><br/>
        <form class="form" name="changeForm" method="post">
            <div class="text-center">
                <h3>修改密码</h3><br/>
            </div>
            <div class="form-group row">
                <label class="col-sm-3 col-form-label-sm" for="username">原密码：</label>
                <div class="col-sm-9">
                    <input class="form-control form-control-sm" type="password" id="password" value="" maxlength="20"/>
                </div>
                <br/>
                <label class="col-sm-3 col-form-label-sm" for="password">新密码：</label>
                <div class="col-sm-9">
                    <input class="form-control form-control-sm" type="password" id="newPassword" value="" maxlength="32"/>
                </div>
                <br/>
                <label class="col-sm-3 col-form-label-sm" for="validation">确认：</label>
                <div class="col-sm-9">
                    <input class="form-control form-control-sm" type="password" id="validation" value="" maxlength="32"/>
                </div>
                <br/>
                <div class="col-sm-12 invalid-feedback" id="errorMessage">
                </div>
                <br/>
                <div class="col-sm-12" align="right">
                    <button type="submit" class="btn btn-primary">修改密码</button>
                </div>
            </div>
        </form>
    </body>
</html>