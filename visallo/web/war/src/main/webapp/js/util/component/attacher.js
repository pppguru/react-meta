(function() {
    'use strict';
define([
    'react-dom',
    'react',
    'react-redux',
    'util/promise'
], function(
    ReactDOM,
    React,
    { Provider },
    Promise) {

    var API_VERSIONS = ['v1'],
        self = this,
        cachedApiVersions = null;

    /**
     * Abstracts the attachment of flight and react nodes
     */
    function Attacher(options = {}) {
        return (this === self ? new Attacher() : this).options(options)
    }

    ['path', 'component', 'params', 'behavior', 'legacyMapping', 'options'].forEach(createSetter);

    Attacher.prototype.node = function(node) {
        if (arguments.length === 0) return this._node;
        this._node = _.isFunction(node.get) ? node.get(0) : node;
        return this;
    };

    Attacher.prototype._verifyState = function() {
        if (!this._options.preferDirectReactChildren && !_.isElement(this._node)) {
            throw new Error('Node is not an Element')
        }
        if (!_.isString(this._path) && !this._component) {
            throw new Error('Valid component or path is required')
        }
        if (this._params) {
            if ('visalloApi' in this._params) {
                throw new Error('Refrain from setting visalloApi key in params to avoid collisions');
            }
            if (!_.isObject(this._params) || _.isArray(this._params) || _.isFunction(this._params)) {
                throw new Error('Params must be an object')
            }
        }
    };

    Attacher.prototype.teardown = function(options = {}) {
        if (!this._options.preferDirectReactChildren) {
            const { flight = true, react = true } = options;
            if (!this._node) throw new Error('No node specified');
            if (react) ReactDOM.unmountComponentAtNode(this._node);
            if (flight) $(this._node).teardownAllComponents();
        }
        return this;
    };

    Attacher.prototype.attach = function(options = {}) {
        var self = this,
            params = _.extend({}, this._params) || {};

        return Promise.try(this._verifyState.bind(this))
            .then(function() {
                return Promise.all([
                    self._component || Promise.require(self._path),
                    cachedApiVersions || (cachedApiVersions = loadApiVersions()),
                    visalloData.storePromise
                ]);
            })
            .spread(function(Component, api, store) {
                params.visalloApi = api;
                const flight = isFlight(Component);

                if (options.teardown) {
                    self.teardown(options.teardownOptions || {});
                }
                if (options.empty || (flight && options.emptyFlight) || (!flight && options.emptyReact)) {
                    self._node.textContent = '';
                }

                if (flight) {
                    var eventNode = options && options.legacyFlightEventsNode,
                        addedEvents = addLegacyListeners(self, eventNode);
                    Component.attachTo(self._node, params);
                    removeLegacyListenersOnTeardown(self, eventNode || self._node, Component, addedEvents)
                    self._flightComponent = Component;
                } else {
                    var reactElement = React.createElement(Component, _.extend(params, wrapBehavior(self)));
                    if (self._options.preferDirectReactChildren) {
                        self._reactElement = reactElement;
                    } else {
                        var provider = React.createElement(Provider, { store }, reactElement);
                        ReactDOM.render(provider, self._node);
                        self._reactElement = provider;
                    }
                }
                return self;
            })
    };

    return Attacher;

    function addLegacyListeners(inst, node) {
        var mapping = inst._legacyMapping || {},
            addedEvents = {};
        _.each(inst._behavior, function(callback, name) {
            if (name in mapping) {
                name = mapping[name];
            }
            if (!(name in addedEvents)) {
                addedEvents[name] = function(event, data) {
                    event.stopPropagation();
                    callback(inst, data);
                };
                $(node || inst._node).on(name, addedEvents[name]);
            }
        })
        return addedEvents;
    }

    function wrapBehavior(inst) {
        return _.mapObject(inst._behavior, function(fn) {
            return function(data) {
                return fn.apply(this, [inst].concat(_.toArray(arguments)));
            }
        })
    }

    function removeLegacyListenersOnTeardown(inst, eventNode, Component, addedEvents) {
        var comp = $(inst._node).lookupComponent(Component)
        if (comp) {
            comp.before('teardown', function() {
                var $node = this.$node;
                _.each(addedEvents, function(handler, name) {
                    $(eventNode).off(name, handler);
                })
            })
        }
    }

    function loadApiVersions() {
        return Promise.map(API_VERSIONS, function(version) {
                return Promise.require('public/' + version + '/api')
                    .then(function(api) {
                        return api.connect()
                            .then(function(asyncApi) {
                                var baseApi = _.omit(api, 'connect');
                                return [version, _.extend(baseApi, asyncApi)];
                            });
                    })
            })
            .then(function(apis) {
                return _.object(apis);
            })
    }

    function isFlight(Component) {
        return _.isFunction(Component.attachTo);
    }

    function createSetter(name) {
        Attacher.prototype[name] = function(value) {
            var key = `_${name}`;
            if (arguments.length === 0) {
                return this[key];
            }
            this[key] = value;
            return this;
        }
    }
});
})();
