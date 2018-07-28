<html lang="cn">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=no">
    <meta name="format-detection" content="telephone=no">

    <title>上传文件到服务器</title>

    <link rel="stylesheet" href="/up/css/zTreeStyle.css">
    <link rel="stylesheet" href="/up/css/dropzone.css">
    <link rel="stylesheet" href="/up/css/upload.css">
    <link rel="stylesheet" href="/up/css/bootstrap.min.css">
    <link rel="stylesheet" href="/up/css/bootstrap-theme.min.css">

    <script type="text/javascript">
        var zNodes = ${tree};
    </script>
    <script type="text/javascript" src="/up/js/jquery-1.4.4.min.js"></script>
    <script type="text/javascript" src="/up/js/jquery.ztree.core.min.js"></script>
    <script type="text/javascript" src="/up/js/jquery.cookie.js"></script>
    <script type="text/javascript" src="/up/js/spark-md5.min.js"></script>
    <script type="text/javascript" src="/up/js/upload.js"></script>
    <script type="text/javascript" src="/up/js/dropzone.js"></script>
</head>
<body style="background-color: #eee">
    <div id="page-layout">
        <div id="page-inner-layout">
            <h3>上传文件到服务器：</h3>
            <div id="hidden">
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
                <button class="btn btn-success fileinput-button dz-clickable">
                    <i class="glyphicon glyphicon-plus"></i>
                    <span>添加文件...</span>
                </button>
                <button type="submit" class="btn btn-primary start">
                    <i class="glyphicon glyphicon-upload"></i>
                    <span>开始上传</span>
                </button>
                <button type="reset" class="btn btn-warning cancel">
                    <i class="glyphicon glyphicon-ban-circle"></i>
                    <span>取消上传</span>
                </button>
                <div class="table table-striped files" id="previews">
                </div>
            </div>
            <hr/>

            <h4>上传目标：</h4><input name="dest" id="dest" value="/" readonly placeholder="/"/><input type="hidden" name="md5" id="md5"/>
            <h4>选择路径：</h4>
            <div id="treeview">
                <div class="zTreeDemoBackground left">
                    <ul id="treeDemo" class="ztree"></ul>
                </div>
            </div>
        </div>
    </div>
    <br/>
</body>
</html>