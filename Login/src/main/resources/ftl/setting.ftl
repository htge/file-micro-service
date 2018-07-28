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
    <link rel="stylesheet" href="${Path}/auth/css/jquery.dataTables.min.css" />

    <script type="text/javascript" src="${Path}/auth/js/jquery-3.3.1.min.js"></script>
    <script type="text/javascript" src="${Path}/auth/js/bootstrap.min.js"></script>
    <script type="text/javascript" src="${Path}/auth/js/jquery.dataTables.min.js"></script>
    <script type="text/javascript" src="${Path}/auth/js/setting.js"></script>
    <title>用户信息管理</title>
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark bg-primary">
    <a class="navbar-brand mb-0 h1" href="#">${username}，${role}</a>
    <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarSupportedContent" aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
        <span class="navbar-toggler-icon"></span>
    </button>

    <div class="collapse navbar-collapse" id="navbarSupportedContent">
        <ul class="navbar-nav mr-auto">
            <li class="nav-item">
                <a id="home" class="nav-link" href="#">返回首页</a>
            </li>
            <li class="nav-item">
                <a id="changepwd" class="nav-link" href="#">修改密码</a>
            </li><#if role=='管理员'>
                <li class="nav-item">
                    <a id="register" class="nav-link" href="#">注册新账户</a>
                </li></#if>
            <li class="nav-item">
                <a id="logoff" class="nav-link" href="#">注销</a>
            </li>
        </ul>
    </div>
</nav>
<br/>
<div class="userList">
    <div class="col_2_3_right">
        <div class="index_viewport">
            <table id="userList" cellpadding="0" cellspacing="0" border="0" width="100%">
                <thead>
                <tr>
                    <th width="40%">用户名</th>
                    <th width="30%">权限</th>
                    <th width="30%">操作</th>
                </tr>
                </thead>
            </table>
        </div>
    </div>
</div>
</body>
</html>