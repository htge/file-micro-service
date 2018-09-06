(function () {
    "use strict";

    self.importScripts("spark-md5.min.js");

    var totalSparks = {};

    onmessage = function(e) {
        var params = e.data;
        var chunk = params.chunkInfo;
        var totalSpark;
        var uuid = params.uuid;
        if (chunk.index === 0) {
            totalSpark = new SparkMD5.ArrayBuffer();
            totalSparks[uuid] = totalSpark;
        } else {
            totalSpark = totalSparks[uuid];
        }

        var arraybuffer = params.arraybuffer;
        totalSpark.append(arraybuffer);
        var result = {
            "total": null,
            "uuid": uuid
        };
        if (chunk.index+1 === chunk.total) {
            result.total = totalSpark.end();
        }
        postMessage(result);
    };
}());

