"use strict";

var w, wt, wf;

$(document).ready(function(){
    initTreeView();
    initDropZone();
});

function initTreeView() {
    var setting = {
        view: {
            dblClickExpand: dblClickExpand
        },
        data: {
            simpleData: {
                enable: true
            }
        },
        callback: {
            beforeExpand: beforeExpand,
            onExpand: onExpand,
            onClick: onTreeClick
        }
    };

    $(document).ready(function(){
        //zNodes来源于FTL的定义
        $.fn.zTree.init($("#treeDemo"), setting, zNodes);
    });

    function onTreeClick(event, treeId, treeNode) {
        if (treeNode) {
            $("#dest").attr("value", treeNode.id);
        }
    }

    function dblClickExpand(treeId, treeNode) {
        return treeNode.level > 0;
    }

    var curExpandNode = null;
    function beforeExpand(treeId, treeNode) {
        var pNode = curExpandNode ? curExpandNode.getParentNode():null;
        var treeNodeP = treeNode.parentTId ? treeNode.getParentNode():null;
        var zTree = $.fn.zTree.getZTreeObj("treeDemo");
        for(var i=0, l=!treeNodeP ? 0:treeNodeP.children.length; i<l; i++ ) {
            if (treeNode !== treeNodeP.children[i]) {
                zTree.expandNode(treeNodeP.children[i], false);
            }
        }
        while (pNode) {
            if (pNode === treeNode) {
                break;
            }
            pNode = pNode.getParentNode();
        }
        if (!pNode) {
            singlePath(treeNode);
        }

    }
    function singlePath(newNode) {
        if (newNode === curExpandNode) return;

        var zTree = $.fn.zTree.getZTreeObj("treeDemo"),
            rootNodes, tmpRoot, tmpTId, i, j, n;

        if (!curExpandNode) {
            tmpRoot = newNode;
            while (tmpRoot) {
                tmpTId = tmpRoot.tId;
                tmpRoot = tmpRoot.getParentNode();
            }
            rootNodes = zTree.getNodes();
            for (i=0, j=rootNodes.length; i<j; i++) {
                n = rootNodes[i];
                if (n.tId != tmpTId) {
                    zTree.expandNode(n, false);
                }
            }
        } else if (curExpandNode && curExpandNode.open) {
            if (newNode.parentTId === curExpandNode.parentTId) {
                zTree.expandNode(curExpandNode, false);
            } else {
                var newParents = [];
                while (newNode) {
                    newNode = newNode.getParentNode();
                    if (newNode === curExpandNode) {
                        newParents = null;
                        break;
                    } else if (newNode) {
                        newParents.push(newNode);
                    }
                }
                if (newParents!=null) {
                    var oldNode = curExpandNode;
                    var oldParents = [];
                    while (oldNode) {
                        oldNode = oldNode.getParentNode();
                        if (oldNode) {
                            oldParents.push(oldNode);
                        }
                    }
                    if (newParents.length>0) {
                        zTree.expandNode(oldParents[Math.abs(oldParents.length-newParents.length)-1], false);
                    } else {
                        zTree.expandNode(oldParents[oldParents.length-1], false);
                    }
                }
            }
        }
        curExpandNode = newNode;
    }

    function onExpand(event, treeId, treeNode) {
        curExpandNode = treeNode;
    }}

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
            if (typeof(wt) === "undefined") {
                wt = new Worker("/up/js/md5Chunk.js");
            }
            if (typeof(wf) === "undefined") {
                wf = new Worker("/up/js/fileReader.js");
            }

            var uuid;
            if (chunk) {
                uuid = chunk.file.upload.uuid;
            } else {
                uuid = blob.upload.uuid;
            }

            w.onmessage = function(e) {
                var json = callbackMap[e.data.uuid];
                json.part = e.data.part;
                json.count++;
                checkCallback(json);
            };

            wt.onmessage = function (e) {
                var json = callbackMap[e.data.uuid];
                json.total = e.data.total;
                json.count++;
                checkCallback(json);
            };

            function checkCallback(json) {
                if (json.count === 2) {
                    var callback = json.callback;
                    if (callback) {
                        callback({
                            part: json.part,
                            total: json.total
                        });
                    } else {
                        alert("could not get callback");
                    }
                }
            }

            wf.onmessage = function (e) {
                var arraybuffer = e.data.arraybuffer;
                var uuid = e.data.uuid;
                var chunkInfo = callbackMap[uuid].chunkInfo;

                w.postMessage({
                    arraybuffer: arraybuffer,
                    uuid: uuid
                });
                wt.postMessage({
                    arraybuffer: arraybuffer,
                    chunkInfo: chunkInfo,
                    uuid: uuid
                });
            };

            //发送消息之前，存储临时信息
            var chunkInfo;
            if (chunk) {
                chunkInfo = {
                    index: chunk.index,
                    total: chunk.file.upload.totalChunkCount
                };
            } else {
                chunkInfo = {
                    index: 0,
                    total: 1
                };
            }

            callbackMap[uuid] = {
                callback: callback,
                chunkInfo: chunkInfo,
                count: 0
            };

            wf.postMessage({
                uuid: uuid,
                blob: blob
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

