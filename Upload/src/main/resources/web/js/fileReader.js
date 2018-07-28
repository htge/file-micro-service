(function () {
    "use strict";
    onmessage = function (e) {
        var params = e.data;
        var uuid = params.uuid;
        var blob = params.blob;
        var fileReader = new FileReader();
        fileReader.onload = function (ev) {
            postMessage({
                uuid: uuid,
                arraybuffer: ev.target.result
            });
        };
        fileReader.readAsArrayBuffer(blob);
    };
}());
