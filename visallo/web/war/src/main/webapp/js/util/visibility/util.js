define(['configuration/plugins/registry', 'util/promise'], function(registry) {
    'use strict';

    /**
     * Plugin to configure the user interface for displaying and editing visibility authorization strings.
     *
     * The visibility component requires two FlightJS components registered for viewing and editing:
     *
     * @param {string} editorComponentPath The path to {@link org.visallo.visibility~Editor} component
     * @param {string} viewerComponentPath The path to {@link org.visallo.visibility~Viewer} component
     */
    registry.documentExtensionPoint('org.visallo.visibility',
        'Implement custom interface for visibility display and editing',
        function(e) {
            return _.isString(e.editorComponentPath) ||
                _.isString(e.viewerComponentPath)
        },
        'http://docs.visallo.org/extension-points/front-end/visibility'
    );

    var defaultVisibility = {
            editorComponentPath: 'util/visibility/default/edit',
            viewerComponentPath: 'util/visibility/default/view'
        },
        point = 'org.visallo.visibility',
        visibilityExtensions = registry.extensionsForPoint(point),
        components = {
            editor: undefined,
            viewer: undefined
        },
        setComponent = function(type, Component) {
            components[type] = Component;
        };


    if (visibilityExtensions.length === 0) {
        registry.registerExtension(point, defaultVisibility);
        visibilityExtensions = [defaultVisibility];
    }

    if (visibilityExtensions.length > 1) {
        console.warn('Multiple visibility extensions loaded', visibilityExtensions);
    }

    var promises = {
            editor: Promise.require(
                visibilityExtensions[0].editorComponentPath || defaultVisibility.editorComponentPath
            ).then(_.partial(setComponent, 'editor')),
            viewer: Promise.require(
                visibilityExtensions[0].viewerComponentPath || defaultVisibility.viewerComponentPath
            ).then(_.partial(setComponent, 'viewer'))
        },
        internalAttach = function(Component, node, attrs) {
            $(node).teardownComponent(Component);
            Component.attachTo(node, attrs);
        };

    return {
        attachComponent: function(type, node, attrs) {
            var promise;
            if (components[type]) {
                internalAttach(components[type], node, attrs);
                promise = Promise.resolve();
            } else {
                promise = promises[type].then(function(C) {
                    internalAttach(C, node, attrs);
                });
            }
            return promise;
        }
    };
});
