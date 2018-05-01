/**
 * Base plugin file that defines a visallo admin ui plugin.
 */
define([
    'flight/lib/component',
    'configuration/plugins/registry',
    'tpl!util/alert',
    'util/messages'
], function(defineComponent,
    registry,
    alertTemplate,
    i18n) {
    'use strict';

    var NODE_CLS_FOR_LESS_CONTAINMENT = 'admin_less_cls_',
        componentInc = 0;

    defineVisalloAdminPlugin.ALL_COMPONENTS = [];

    return defineVisalloAdminPlugin;

    function defineVisalloAdminPlugin(Component, options) {

        var FlightComponent = defineComponent.apply(null, [Component].concat(options && options.mixins || [])),
            attachTo = FlightComponent.attachTo,
            cls = NODE_CLS_FOR_LESS_CONTAINMENT + (componentInc++);

        console.warn('Admin plugin is deprecated, use registry for', options.section + '/' + options.name)
        registry.registerExtension('org.visallo.admin', {
            Component: FlightComponent,
            section: options.section,
            name: options.name,
            subtitle: options.subtitle
        });

        if (options && options.less) {
            options.less.applyStyleForClass(cls);
        }

        FlightComponent.attachTo = function attachToWithLessClass(selector) {
            $(selector).each(function() {
                $(this).addClass(cls)
            });

            var self = this;
            this.prototype.initialize = _.wrap(this.prototype.initialize, function(init) {
                this.showSuccess = function(message) {
                    this.$node.find('.alert').remove();
                    this.$node.prepend(alertTemplate({ message: message || i18n('admin.plugin.success') }));
                };
                this.showError = function(message) {
                    this.hideError();
                    this.$node.prepend(alertTemplate({ error: message || i18n('admin.plugin.error') }));
                };
                this.hideError = function() {
                    this.$node.find('.alert').remove();
                };
                this.handleSubmitButton = function(button, promise) {
                    var $button = $(button),
                        text = $button.text();

                    $button.attr('disabled', true);

                    if (promise.progress) {
                        promise.progress(function(v) {
                            require(['util/formatters'], function(F) {
                                $button.text(F.number.percent(v) + ' ' + text);
                            })
                        })
                    }

                    return promise.finally(function() {
                        $button.removeAttr('disabled').text(text);
                    });
                };
                return init.apply(this, Array.prototype.slice.call(arguments, 1));
            });
            attachTo.apply(this, arguments);
        }

        componentInc++;

        defineVisalloAdminPlugin.ALL_COMPONENTS.push(
            $.extend({},
                _.pick(options || {}, 'section', 'name', 'subtitle'),
                {
                    Component: FlightComponent
                }
            )
        );

        return FlightComponent;
    }
});
