(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(['module', 'exports', './TableauReport'], factory);
  } else if (typeof exports !== "undefined") {
    factory(module, exports, require('./TableauReport'));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod, mod.exports, global.TableauReport);
    global.index = mod.exports;
  }
})(this, function (module, exports, _TableauReport) {
  'use strict';

  Object.defineProperty(exports, "__esModule", {
    value: true
  });

  var _TableauReport2 = _interopRequireDefault(_TableauReport);

  function _interopRequireDefault(obj) {
    return obj && obj.__esModule ? obj : {
      default: obj
    };
  }

  exports.default = _TableauReport2.default;
  module.exports = exports['default'];
});