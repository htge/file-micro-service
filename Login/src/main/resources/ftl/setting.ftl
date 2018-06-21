<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=no">
    <meta name="format-detection" content="telephone=no" />
    <link rel="stylesheet" type="text/css" href="${Path}/auth/css/button.css">
    <script src="${Path}/auth/js/jquery-3.3.1.min.js"></script>
    <title>用户信息管理</title>
</head>
<style>
    table,table tr th,table tr td{border:1px solid #aaaaaa;}
    table{width:200px;min-height:25px;line-height:25px;text-align:center;border-collapse:collapse;}
</style>
<body>
<div style="width:100%; max-width:380px; margin-left:auto; margin-right:auto; text-align:center;">
    <h2>用户信息管理</h2>
</div>
<table style="width:100%; max-width:380px; margin-left:auto; margin-right:auto">
    <tbody>
    <tr>
        <td style="text-align: center">用户名</td>
        <td style="width:80px;text-align: center">权限</td>
        <td style="width:120px;text-align: center">操作</td>
    </tr>
    <tr>
        <#if currentPage==0>
            <td style="text-align: left">${username}</td>
            <td>${role}</td>
            <td></td>
        </#if>
    </tr>
	<#list usersrcs as usersrc>
	<tr>
        <td style="text-align: left"> ${usersrc.username}</td>
        <td>${usersrc.role}</td>
        <td>
            <input name="username" type="hidden" value="${usersrc.username}" />
            <a href="#" onclick="window.location.href='${Path}/auth/delete/${usersrc.username}'">删除账户</a>
        </td>
    </tr>
	</#list>
    </tbody>
</table><br>
<div style="width:100%; max-width:380px; margin-left:auto; margin-right:auto; text-align:center;">
    <#if pageIndexes??>
    <p>
        <button class="button button-plain button-square button-small" style="width:37px;" onclick="location.href='?p=${(currentPage-1)?c}'" <#if currentPage lte 0>disabled</#if>>上页</button>
        <#list pageIndexes as pageIndex>
        <#if pageIndex==currentPage>
            <button class="button button-primary button-square button-small" style="width:35px" onclick="location.href='?p=${pageIndex?c}'">${(pageIndex+1)?c}</button>
        <#else>
           <button class="button button-plain button-square button-small" style="width:35px" onclick="location.href='?p=${pageIndex?c}'">${(pageIndex+1)?c}</button>
        </#if>
        </#list>
        <button class="button button-plain button-square button-small" style="width:37px;" onclick="location.href='?p=${(currentPage+1)?c}'" <#if (currentPage+1) gte maxPage>disabled</#if>>下页</button>
    </p>
    </#if>
	<#if role=='管理员'>
		<p>
            <button class="button button-primary button-square button-small" style="width:100px;" onclick="location.href='change'">修改密码</button>
            <button class="button button-primary button-square button-small" style="width:100px;" onclick="location.href='logout'">退出登录</button>
        </p>
        <p>
            <button class="button button-primary button-square button-small" style="width:100px;" onclick="location.href='register'">注册新账号</button>
            <button class="button button-primary button-square button-small" style="width:100px;" onclick="location.href='.'">返回文件列表</button>
        </p>
	<#else>
		<button class="button button-primary button-square button-small" style="width:100px;" onclick="location.href='change'">修改密码</button>
		<button class="button button-primary button-square button-small" style="width:100px;" onclick="location.href='logout'">退出登录</button>
		<button class="button button-primary button-square button-small" style="width:100px;" onclick="location.href='.'">返回文件列表</button>
	</#if>
</div>
</body>
</html>