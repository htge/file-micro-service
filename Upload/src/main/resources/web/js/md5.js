(function () {
    "use strict";

    self.importScripts("spark-md5.min.js");

    onmessage = function(e) {
        var params = e.data;
        var arraybuffer = params.arraybuffer;
        var uuid = params.uuid;
        var spark = new SparkMD5.ArrayBuffer();
        spark.append(arraybuffer);
        var result = {
            "part": spark.end(),
            "uuid": uuid
        };
        postMessage(result);
    };
}());

