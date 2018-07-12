$(document).ready(function(){
    initTreeView();
    initDropZone();
});

function initTreeView() {
    // first example
    $("#browser").treeview();

    // second example
    $("#navigation").treeview({
        persist: "location",
        collapsed: true,
        unique: true
    });

    // third example
    $("#red").treeview({
        animated: "fast",
        collapsed: true,
        unique: true,
        persist: "cookie",
        toggle: function() {
            window.console && console.log("%o was toggled", this);
        }
    });

    // fourth example
    $("#black, #gray").treeview({
        control: "#treecontrol",
        persist: "cookie",
        cookieId: "treeview-black"
    });
}

function initDropZone() {
    //上传进度相关
    var previewNode = document.querySelector("#template");
    previewNode.id = "";
    var previewTemplate = previewNode.parentNode.innerHTML;
    previewNode.parentNode.removeChild(previewNode);
    var callbackMap = {};

    function calcMD5Async(blob, chunk, callback) {
        if (typeof(Worker) !== "undefined") {
            if (typeof(w) === "undefined") {
                w = new Worker("/up/js/md5.js");
            }
            var uuid;
            if (chunk) {
                uuid = chunk.file.upload.uuid;
            } else {
                uuid = blob.upload.uuid;
            }
            callbackMap[uuid] = callback;
            w.onmessage = function(e) {
                callback = callbackMap[e.data.uuid];
                if (callback) {
                    callback(e.data);
                } else {
                    alert("could not get callback");
                }
            };
            var chunkInfo;
            if (chunk) {
                chunkInfo = {
                    index: chunk.index,
                    total: chunk.file.upload.totalChunkCount,
                    uuid: uuid
                };
            } else {
                chunkInfo = {
                    index: 0,
                    total: 1,
                    uuid: uuid
                };
            }
            w.postMessage({
                "chunk": chunkInfo,
                "blob": blob
            });
        } else {
            alert("Browser not supported");
        }
    }

    var myDropzone = new Dropzone("#dropzone", { // Make the whole body a dropzone
        url: "/up/upload", // Set the url
        thumbnailWidth: 80,
        thumbnailHeight: 80,
        parallelUploads: 20,
        maxFilesize: 10240,
        chunking: true,
        retryChunks: true,
        chunkSize: 2097152,
        previewTemplate: previewTemplate,
        autoQueue: false, // Make sure the files aren't queued until manually added
        previewsContainer: "#previews", // Define the container to display the previews
        clickable: ".fileinput-button", // Define the element that should be used as click trigger to select files.
        params: function(file, xhr, chunk, callback) {
            var object = this;
            //分为全局md5和部分md5，第一个参数为全局md5，最后一次的时候需要执行end
            calcMD5Async(chunk?chunk.dataBlock.data:file[0], chunk, function(hash) {
                var part = hash.part;
                var total = hash.total;
                if (chunk) {
                    callback.call(object, {
                        index: chunk.index,
                        blockSize: object.options.chunkSize,
                        total: chunk.file.upload.totalChunkCount,
                        offset: chunk.index * object.options.chunkSize,
                        relative: $("#dest").attr("value"),
                        hash: part,
                        all: total
                    });
                } else {
                    callback.call(object, {
                        relative: $("#dest").attr("value"),
                        hash: part,
                        all: total
                    });
                }
            });
        }
    });

    myDropzone.on("addedfile", function(file) {
        // Hookup the start button
        file.previewElement.querySelector(".start").onclick = function() {
            myDropzone.enqueueFile(file);
        };
    });

    myDropzone.on("uploadprogress", function(file, progress) {
        file.previewElement.querySelector(".progress").hidden = (progress===100);
    });

    myDropzone.on("sending", function(file) {
        // Show the total progress bar when upload starts
        // document.querySelector("#total-progress").style.opacity = "1";
        // And disable the start button
        file.previewElement.querySelector(".start").setAttribute("disabled", "disabled");
    });

    // Setup the buttons for all transfers
    // The "add files" button doesn't need to be setup because the config
    // `clickable` has already been specified.
    document.querySelector("#dropzone .start").onclick = function() {
        myDropzone.enqueueFiles(myDropzone.getFilesWithStatus(Dropzone.ADDED));
    };
    document.querySelector("#dropzone .cancel").onclick = function() {
        myDropzone.removeAllFiles(true);
    };
}

function getBufferMD5(buffer) {
    var spark = new SparkMD5.ArrayBuffer();
    spark.append(buffer);
    return spark.end();
}
