
var exec = require('cordova/exec');

exports.scan = function (str_close, str_scaning, camera_num, success, error) {
    exec(success, error, 'lcr', 'scan', [str_close, str_scaning, camera_num]);
};

