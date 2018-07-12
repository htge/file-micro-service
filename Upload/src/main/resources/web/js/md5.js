(function () {
    "use strict";

    self.importScripts("spark-md5.min.js");

    var totalSparks = {};

    onmessage = function(e) {
        var params = e.data;
        var chunk = params.chunk;
        var totalSpark;
        var uuid = chunk.uuid;
        if (chunk.index === 0) {
            totalSpark = new SparkMD5.ArrayBuffer();
            totalSparks[uuid] = totalSpark;
        } else {
            totalSpark = totalSparks[uuid];
        }
        var fileReader = new FileReader();
        fileReader.onload = function (ev) {
            var arraybuffer = ev.target.result;
            var spark = new SparkMD5.ArrayBuffer();
            spark.append(arraybuffer);
            totalSpark.append(arraybuffer);
            var result = {
                "part": spark.end(),
                "total": null,
                "uuid": uuid
            };
            if (chunk.index+1 === chunk.total) {
                result.total = totalSpark.end();
            }
            postMessage(result);
        };
        fileReader.readAsArrayBuffer(params.blob);
    };
}());

