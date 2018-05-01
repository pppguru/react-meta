(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(['module', 'exports', 'url'], factory);
  } else if (typeof exports !== "undefined") {
    factory(module, exports, require('url'));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod, mod.exports, global.url);
    global.tokenizeUrl = mod.exports;
  }
})(this, function (module, exports, _url2) {
  'use strict';

  Object.defineProperty(exports, "__esModule", {
    value: true
  });

  var _url3 = _interopRequireDefault(_url2);

  function _interopRequireDefault(obj) {
    return obj && obj.__esModule ? obj : {
      default: obj
    };
  }

  function tokenizeUrl(_url, token) {
    var parsed = _url3.default.parse(_url, true);
    var protocol = parsed.protocol,
        host = parsed.host,
        pathname = parsed.pathname;


    return protocol + '//' + host + '/trusted/' + token + pathname;
  }

  exports.default = tokenizeUrl;
  module.exports = exports['default'];
});