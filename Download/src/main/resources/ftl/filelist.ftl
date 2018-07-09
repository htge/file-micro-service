<!DOCTYPE html>
<html>
	<head>
		<META http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no, minimum-scale=1.0, maximum-scale=1.0"/>
		<#if title?exists>
		<title>${title}文件夹</title>
		<#else>
		<title>根目录</title>
		</#if>
		<style>
		h1{color:white;background-color:#0086b2;}
		h3{color:white;background-color:#0086b2;}
		body{font-family:sans-serif,Arial,Tahoma;color:black;background-color:white;}
		b{color:white;background-color:#0086b2;}
		a{color:black;word-wrap:break-word;word-break:break-all;}
		HR{color:#0086b2;}
		table td{padding:5px;}
		</style>
	</head>
	<body>
	<h1>
		<#if title?exists>
			${title}文件夹
			<a style="color: white; text-decoration: none" href="../"> 返回到${back}/</a>
		<#else>
  			根目录
			<#if settingPath?exists>
 				<a style="color: white; text-decoration: none" href="${settingPath}"> 设置</a>
			<#else>
				<#if logoutPath?exists>
 					<a style="color: white; text-decoration: none" href="${logoutPath}"> 注销</a>
				</#if>
			</#if>
		</#if>
	</h1>
	<hr style="height: 1px;">
	<table style="width: 100%;">
	<tr>
		<th style="text-align: left;">&nbsp;文件名</th>
		<th style="text-align: right;">文件大小&nbsp;&nbsp;</th>
		<th style="text-align: right;">上次修改时间</th>
	</tr>
	<#list datasrcs as datasrc>
	<tr>
        <td style="text-align: left;font-family:Arial;">
            <a href="${contextpath}${datasrc.encodedName}">${datasrc.name}</a>
        </td>
        <td style="text-align: right;min-width: 90px;font-family:Arial;">${datasrc.size}</td>
        <td style="text-align: right;min-width: 90px;font-family:Arial;">${datasrc.lastmodified}</td>
    </tr>
	</#list>
</table>
<hr style="height: 1px;"><h3>文件列表<version-major-minor></version-major-minor></h3>
</body>
</html>