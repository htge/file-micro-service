<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=no">
    <meta name="format-detection" content="telephone=no" />

    <link rel="stylesheet" href="/auth/css/common.css">
    <link rel="stylesheet" href="/auth/css/bootstrap.min.css">
    <link rel="stylesheet" href="/auth/css/bootstrap-grid.min.css">
    <link rel="stylesheet" href="/auth/css/bootstrap-reboot.min.css">

    <script type="text/javascript" src="/auth/js/jquery-3.3.1.min.js"></script>
    <script type="text/javascript" src="/auth/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="/auth/js/jquery.cookie.js"></script>
    <script type="text/javascript" src="/auth/js/jsbn.js"></script>
    <script type="text/javascript" src="/auth/js/prng4.js"></script>
    <script type="text/javascript" src="/auth/js/rng.js"></script>
    <script type="text/javascript" src="/auth/js/rsa.js"></script>
    <script type="text/javascript" src="/auth/js/aes.js"></script>
    <script type="text/javascript" src="/auth/js/common.js"></script>
    <script type="text/javascript" src="/auth/js/login.js"></script>
    <title>系统登录</title>
</head>
<body class="body">
<br><br>
<div style="width:100%; max-width:320px; margin-left:auto; margin-right:auto;">
    <form class="form" name="loginForm" method="post">
        <div class="text-center">
            <h3>系统登录</h3><br/>
        </div>
        <div class="form-group row no-gutters">
            <label class="col-3 col-form-label-sm" for="username">用户名：</label>
            <div class="col-9">
                <input class="form-control form-control-sm" type="text" id="username" value="" maxlength="20"/>
            </div>
            <br/>
            <label class="col-3 col-form-label-sm" for="password">密码：</label>
            <div class="col-9">
                <input class="form-control form-control-sm" type="password" id="password" value="" maxlength="32"/>
            </div>
            <br/>
            <div class="col-12 invalid-feedback" id="errorMessage">
            </div>
            <br/><br/>
            <div class="col-12" align="center">
                <button type="submit" class="btn btn-primary">登录</button>
            </div>
            <input id="rsaPub" type="hidden" value="${rsa}"/>
            <input id="uuid" type="hidden" value="${uuid}"/>
        </div>
    </form>
</div>
</body>
</html>