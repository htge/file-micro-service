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
        <script type="text/javascript" src="/auth/js/jsbn.js"></script>
        <script type="text/javascript" src="/auth/js/prng4.js"></script>
        <script type="text/javascript" src="/auth/js/rng.js"></script>
        <script type="text/javascript" src="/auth/js/rsa.js"></script>
        <script type="text/javascript" src="/auth/js/aes.js"></script>
        <script type="text/javascript" src="/auth/js/common.js"></script>
        <script type="text/javascript" src="/auth/js/register.js"></script>

        <title>注册新用户</title>
    </head>
    <body class="body">
        <br/><br/>
        <form class="form" name="registerForm" method="post">
            <div class="text-center">
                <h3>注册新用户</h3><br/>
            </div>
            <div class="form-group row no-gutters">
                <label class="col-3 col-form-label-sm" for="password">原密码：</label>
                <div class="col-9">
                    <input class="form-control form-control-sm" type="password" id="password" value="" maxlength="20" placeholder="当前管理账号的密码"/>
                </div>
                <br/>
                <label class="col-3 col-form-label-sm" for="username">用户名：</label>
                <div class="col-9">
                    <input class="form-control form-control-sm" type="text" id="username" value="" maxlength="20" placeholder="新用户名"/>
                </div>
                <br/>
                <label class="col-3 col-form-label-sm" for="newPassword">密码：</label>
                <div class="col-9">
                    <input class="form-control form-control-sm" type="password" id="newPassword" value="" maxlength="32" placeholder="新用户密码"/>
                </div>
                <br/>
                <label class="col-3 col-form-label-sm" for="validation">确认：</label>
                <div class="col-9">
                    <input class="form-control form-control-sm" type="password" id="validation" value="" maxlength="32" placeholder="新用户确认密码"/>
                </div>
                <br/>
                <div class="col-12 invalid-feedback" id="errorMessage">
                </div>
                <br/>
                <div class="col-4 mt-2 mb-2">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" value="admin" id="role">
                        <label class="form-check-label" for="role">
                            管理员
                        </label>
                    </div>
                </div>
                <div class="col-8" align="right">
                    <button type="submit" class="btn btn-primary">注册</button>
                </div>
                <input id="rsaPub" type="hidden" value="${rsa}"/>
                <input id="uuid" type="hidden" value="${uuid}"/>
            </div>
        </form>
    </body>
</html>