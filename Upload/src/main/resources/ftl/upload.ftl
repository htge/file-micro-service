<html lang="cn">
<head>
    <meta charset="UTF-8">
    <title>上传文件到服务器</title>

    <link rel="stylesheet" href="/up/css/jquery.treeview.css" />
    <link rel="stylesheet" href="/up/css/upload.css" />

    <script src="/up/js/jquery-3.3.1.min.js"></script>
    <script src="/up/js/jquery.treeview.js"></script>
    <script src="/up/js/jquery.cookie.js"></script>
    <script src="/up/js/upload.js"></script>
    <script src="/up/js/spark-md5.min.js"></script>
    <script>
        var blockSize = 2097152; //2MB
        var pause = false; //暂停
        var retryCount = 0; //重试计数器

        var startTime, endTime;

        function getBufferMD5(buffer) {
            var spark = new SparkMD5.ArrayBuffer();
            spark.append(buffer);
            return spark.end();
        }

        function get_filemd5sum(ofile, callback) {
            var file = ofile;
            var tmp_md5;
            var size = file.size / 1048576;
            if (size > 1024) { //1GB
                blockSize = 10485760; //10MB
            } else {
                blockSize = 2097152; //2MB
            }
            if (size > 10240) {//10GB
                alert("文件太大，无法传输");
                hideLoading();
                return;
            }
            var blobSlice = File.prototype.slice || File.prototype.mozSlice || File.prototype.webkitSlice,
                    chunkSize = blockSize,
                    chunks = Math.ceil(file.size / chunkSize),
                    currentChunk = 0,
                    spark = new SparkMD5.ArrayBuffer(),
                    fileReader = new FileReader();

            fileReader.onload = function(e) {
                var buffer = e.target.result;
                var partMD5 = getBufferMD5(buffer);
                spark.append(buffer); // Append array buffer
                currentChunk++;
                var md5_progress = Math.floor((currentChunk / chunks) * 100);

                console.log(file.name + "  正在处理，请稍等," + "已完成" + md5_progress + "%");
                var handler_info = document.getElementById("handler_info");
                handler_info.innerHTML="正在处理<br/>" + "已完成" + md5_progress + "%";

                var formData = new FormData();
                formData.append('total', chunks);//总块数
                formData.append('index', currentChunk);//当前上传的块下标
                //起始位置，带个blockSize
                if (currentChunk === 1) {
                    formData.append('blockSize', blockSize);
                }
                formData.append('partMD5', partMD5);

                if (currentChunk < chunks) {
                    formData.append('md5', null);
                    callback(formData, loadNext);
                } else {
                    tmp_md5 = spark.end();
                    console.log(tmp_md5);
                    $("#md5").attr("value", tmp_md5);

                    formData.append('md5', tmp_md5);
                    callback(formData, null);
                }
            };

            fileReader.onerror = function() {
                console.warn('oops, something went wrong.');
            };

            function loadNext() {
                var start = currentChunk * chunkSize,
                        end = ((start + chunkSize) >= file.size) ? file.size : start + chunkSize;
                fileReader.readAsArrayBuffer(blobSlice.call(file, start, end));
            }
            loadNext();
        }

        function UploadPost(file, formData, next) {
            if (pause) {
                return; //暂停
            }
            var totalSize = file.size;
            var index = formData.get("index");
            var blockCount = formData.get("total");
            try {
                var start = (index - 1) * blockSize;
                var end = Math.min(totalSize, start + blockSize);
                var block = file.slice(start, end);
                formData.set('data', block);
                formData.set('fileName', file.name);
                formData.set('relative', $("#dest").attr("value"));

                $.ajax({
                    url: '/up/upload',
                    type: 'post',
                    data: formData,
                    processData: false,
                    contentType: false
                }).done(function (res) {
                    block = null;
                    $('#progress').text((index / blockCount * 100).toFixed(2) + '%');

                    if (next) {
                        next();
                    } else {
                        hideLoading();
                        alert("上传成功！");
                    }
                }).fail(function (res) {
                    if (res.responseJSON.errorCode === 5 && retryCount < 3) {
                        console.log("上传错误，重试...");
                        retryCount++;
                        UploadPost(file, formData, next);
                    } else {
                        hideLoading();
                        alert("上传失败: " + JSON.stringify(res));
                    }
                });
            } catch (e) {
                alert(e);
            }
        }

        function onSubmit() {
            var fileElement = document.getElementById('file');
            var file = fileElement.files[0];
            if (!file) {
                alert("请选择一个文件");
            } else if (!$("#dest").attr("value")) {
                alert("请选择一个目录");
            } else {
                showLoading();
                $.ajax({
                    url: '/up/upload/check?file='+$("#dest").attr("value")+'/'+file.name,
                    type: "GET",
                    cache: false,
                    processsData: false,
                    contentType: false
                }).done(function (res) {
                    var resultJson = JSON.parse(res);
                    if (resultJson.result) {
                        hideLoading();
                        alert("文件已存在");
                    } else {
                        retryCount = 0;
                        get_filemd5sum(file, function(formData, next) {
                            UploadPost(file, formData, next);
                        });
                    }
                }).fail(function (res) {
                    hideLoading();
                    alert("上传失败："+JSON.stringify(res));
                });
            }
            return false;
        }

        function showLoading() {
            startTime = new Date();

            document.getElementById("over").style.display = "block";
            document.getElementById("layout").style.display = "block";
        }
        
        function hideLoading() {
            endTime = new Date();
            var timeDiff = endTime - startTime; //in ms
            // strip the ms
            timeDiff /= 1000;

            // get seconds
            var seconds = Math.round(timeDiff);
            console.log(seconds + " seconds");

            document.getElementById("over").style.display = "none";
            document.getElementById("layout").style.display = "none";
        }
    </script>
</head>
<body>
<div id="over" class="over"></div>
<div id="layout" class="layout"><img src="/up/images/loading.gif" /><br><div id="handler_info"></div></div>
<div style="width:100%; max-width:320px; margin-left:auto; margin-right:auto">
    <!--暂时显示的效果很差，后续处理-->
    <div>
        <h3>文件上传：</h3>
        选择一个要上传的文件：<br />
        <form id="uploadForm" enctype="multipart/form-data" onsubmit="return onSubmit();">
            <input type="file" name="file" id="file"/>
            <br />
            <input type="submit" value="上传" />
        </form>
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