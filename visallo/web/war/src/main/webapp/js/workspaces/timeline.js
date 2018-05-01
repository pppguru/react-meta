define([
    'flight/lib/component',
    'react',
    'react-dom',
    'react-redux',
    './timeline-tpl.hbs',
    './tasks',
    'util/withDataRequest',
    'util/popovers/withElementScrollingPositionUpdates',
    'require'
], function(
    defineComponent,
    React,
    ReactDom,
    redux,
    template,
    Tasks,
    withDataRequest,
    withElementScrollingPositionUpdates,
    require) {
    'use strict';

    console.log('asdf=============');

    return defineComponent(Timeline, withDataRequest, withElementScrollingPositionUpdates);

    function Timeline() {

        this.defaultAttrs({
            timelineSVGSelector: '.timeline-svg-container',
            timelineConfigSelector: '.timeline-config',
            timelineFitSelector: '.timeline-fit',
            toggleTimelineButton: '.btn-timeline',
            toggleTasksButton: '.btn-tasks'
        });

        this.before('teardown', function() {
            if (this.Histogram) {
                this.$node.children('.timeline-svg-container').teardownComponent(this.Histogram);
            }
        })

        this.after('initialize', function() {
            var self = this;

            this.on('updateHistogramExtent', this.onUpdateHistogramExtent);
            this.on('timelineConfigChanged', this.onTimelineConfigChanged);

            this.ontologyPropertiesPromise = new Promise(function(fulfill) {
                self.on('ontologyPropertiesRenderered', function(event, data) {
                    self.foundOntologyProperties = data.ontologyProperties;
                    self.select('timelineConfigSelector').trigger('ontologyPropertiesChanged', {
                        ontologyProperties: self.foundOntologyProperties
                    });
                    fulfill();
                });
            });

            this.on('click', {
                timelineConfigSelector: this.onTimelineConfigToggle,
                timelineFitSelector: this.onFitTimeline,
                toggleTimelineButton: this.onToggleTimelineButton,
                toggleTasksButton: this.onToggleTasksButton

            });

            this.$node.on(TRANSITION_END, _.once(this.render.bind(this)));

            this.$node.html(template({}));
            Promise.all([
                Promise.require('workspaces/tasks'),
                Promise.resolve()
            ]).done(function(results) {
                var Tasks = results.shift();
                self.attachReactComponentWithStore(Tasks, {}, self.select('timelineSVGSelector'));
            })
        });

        this.onFitTimeline = function() {
            this.$node.children('.timeline-svg-container').trigger('fitHistogram');
        };

        this.onTimelineConfigChanged = function(event, data) {
            this.config = data.config;
            this.$node.children('.timeline-svg-container').trigger('propertyConfigChanged', {
                properties: data.config.properties
            });
        };

        this.onToggleTimelineButton = function() {
            var $timeline = $('.btn-timeline'),
                $tasks = $('.btn-tasks');
            if (!$timeline.hasClass('toggle-down')) {
                $timeline.toggleClass('toggle-down');
                $tasks.toggleClass('toggle-down');
            }
        };

        this.onToggleTasksButton = function() {
            var $timeline = $('.btn-timeline'),
                $tasks = $('.btn-tasks');
            if (!$tasks.hasClass('toggle-down')) {
                $timeline.toggleClass('toggle-down');
                $tasks.toggleClass('toggle-down');
            }
        };

        this.onTimelineConfigToggle = function(event) {
            var self = this,
                $target = $(event.target),
                shouldOpen = $target.lookupAllComponents().length === 0;

            require(['./timeline-config'], function(TimelineConfig) {

                self.ontologyPropertiesPromise.done(function() {
                    if (shouldOpen) {
                        TimelineConfig.teardownAll();
                        TimelineConfig.attachTo($target, {
                            config: self.config,
                            ontologyProperties: self.foundOntologyProperties
                        });
                    } else {
                        $target.teardownComponent(TimelineConfig);
                    }
                })
            });
        };

        this.onUpdateHistogramExtent = function(event, data) {
            this.trigger('selectObjects', {
                vertexIds: data.vertexIds,
                edgeIds: data.edgeIds,
                options: {
                    fromHistogram: true
                }
            })
        };

        this.attachReactComponentWithStore = function(Comp, props, div) {
            return visalloData.storePromise.then(function(store) {
                var component = React.createElement(Comp, props || {}),
                    provider = React.createElement(redux.Provider, { store }, component),
                    node = _.isFunction(div.get) ? div.get(0) : div;

                ReactDom.render(provider, node);
            })
        };

        this.render = function() {
            console.log('timeline render===========');
            var self = this;
            this.$node.html(template({}));

            Promise.all([
                Promise.require('workspaces/tasks'),
                Promise.resolve()
            ]).done(function(results) {
                var Tasks = results.shift();
                self.attachReactComponentWithStore(Tasks, {}, self.select('timelineSVGSelector'));
            })
        }
    }
});
