<html lang="cn">
<head>
    <meta charset="UTF-8">
    <title>上传文件到服务器</title>

    <link rel="stylesheet" href="/up/css/jquery.treeview.css" />
    <link rel="stylesheet" href="/up/css/dropzone.css" />
    <link rel="stylesheet" href="/up/css/upload.css" />
    <link rel="stylesheet" href="http://netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css">
    <link rel="stylesheet" href="http://netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap-theme.min.css">

    <script src="/up/js/jquery-3.3.1.min.js"></script>
    <script src="/up/js/jquery.treeview.js"></script>
    <script src="/up/js/jquery.cookie.js"></script>
    <script src="/up/js/spark-md5.min.js"></script>
    <script src="/up/js/upload.js"></script>
    <script src="/up/js/dropzone.js"></script>
</head>
<body>
    <div style="margin-left: 20px; margin-right:20px">
        <div>
            <h3>文件上传：</h3>
            <div style="display: none">
                <div id="template" class="file-row">
                    <!-- This is used as the file preview template -->
                    <div>
                        <span class="preview"><img data-dz-thumbnail /></span>
                    </div>
                    <div>
                        <p class="name" data-dz-name></p>
                        <strong class="error text-danger" data-dz-errormessage></strong>
                    </div>
                    <div>
                        <p class="size" data-dz-size></p>
                        <div class="progress progress-striped active" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="0">
                            <div class="progress-bar progress-bar-success" style="width:0%;" data-dz-uploadprogress></div>
                        </div>
                    </div>
                    <div>
                        <button class="btn btn-primary start">
                            <i class="glyphicon glyphicon-upload"></i>
                            <span>开始上传</span>
                        </button>
                        <button data-dz-remove class="btn btn-warning cancel">
                            <i class="glyphicon glyphicon-ban-circle"></i>
                            <span>取消上传</span>
                        </button>
                        <button data-dz-remove class="btn btn-danger delete">
                            <i class="glyphicon glyphicon-trash"></i>
                            <span>删除</span>
                        </button>
                    </div>
                </div>
            </div>
            <div id="dropzone">
                <span class="btn btn-success fileinput-button dz-clickable">
                    <i class="glyphicon glyphicon-plus"></i>
                    <span>添加文件...</span>
                </span>
                <button type="submit" class="btn btn-primary start">
                    <i class="glyphicon glyphicon-upload"></i>
                    <span>Start upload</span>
                </button>
                <button type="reset" class="btn btn-warning cancel">
                    <i class="glyphicon glyphicon-ban-circle"></i>
                    <span>Cancel upload</span>
                </button>
                <div class="table table-striped files" id="previews">
                </div>
            </div>
            <script>
            </script>
        </div>
        <div>
            <h4>选择一个相对目标路径：<input name="dest" id="dest" value="" readonly/><input type="hidden" name="md5" id="md5"/></h4>
    <#macro child items subtitle>
        <#assign hasSubTitle=(subtitle?length gt 0)/>
        <#if hasSubTitle>
    <ul>
        <#else>
    <ul id="browser" class="filetree">
        </#if>
        <#list items?keys as key>
            <#if subtitle?length gt 0>
                <#assign nextTitle=subtitle+"/"+key/>
            <#else>
                <#assign nextTitle=key/>
            </#if>
        <li class="closed"><span class="folder"><a href="#" onclick='$("#dest").attr("value", "${nextTitle}")'>${key}</a></span>
        <#if items[key]??>
            <@child items=items[key] subtitle=nextTitle></@child>
        </#if>
        </li>
        </#list>
    </ul>
    </#macro>
    <#if tree??>
        <@child items=tree subtitle=""></@child>
    </#if>
        </div>
    </div>
</body>
</html>