<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/html">
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
        <script type="text/javascript" src="/auth/js/jsbn.js"></script>
        <script type="text/javascript" src="/auth/js/prng4.js"></script>
        <script type="text/javascript" src="/auth/js/rng.js"></script>
        <script type="text/javascript" src="/auth/js/rsa.js"></script>
        <script type="text/javascript" src="/auth/js/aes.js"></script>
        <script type="text/javascript" src="/auth/js/common.js"></script>
        <script type="text/javascript" src="/auth/js/delete.js"></script>
        <title>确认用户删除</title>
    </head>
    <body class="body">
        <br><br>
        <form class="form" name="deleteForm" method="post">
            <div class="text-center">
                <h3>确认用户删除</h3><br/>
            </div>
            <div class="form-group row no-gutters">
                <label class="col-12 col-form-label-sm">将要删除“${username}”用户</label>
                <br/>
                <label class="col-2 col-form-label-sm" for="password">密码：</label>
                <div class="col-10">
                    <input class="form-control form-control-sm" type="password" id="password" value="" maxlength="32" placeholder="当前管理账号的密码"/>
                </div>
                <br/>
                <div class="col-12 invalid-feedback" id="errorMessage">
                </div>
                <br/><br/>
                <div class="col-12" align="center">
                    <button type="submit" class="btn btn-primary">确认删除</button>
                </div>
                <input id="username" type="hidden" value="${username}"/>
                <input id="rsaPub" type="hidden" value="${rsa}"/>
                <input id="uuid" type="hidden" value="${uuid}"/>
            </div>
        </form>
    </body>
</html>