define([
    'flight/lib/component',
    'configuration/plugins/registry',
    './template.hbs',
    'util/component/attacher',
    './bundled/index'
], function(
    defineComponent,
    registry,
    template,
    attacher) {
    'use strict';

    /**
     * Renders a new link to an admin form in the admin list (opened from
     * menubar icon.)
     *
     * The component is placed in a pane next to the admin list and
     * automatically will scroll if the height is larger than window.
     *
     * One of `Component`, `componentPath`, or `url` is required.
     *
     * @param {org.visallo.admin~Component} [Component]
     * @param {string} [componentPath] Path to {@link org.visallo.admin~Component}
     * @param {string} [url] Open a url in a new window instead of rendering a
     * form
     * @param {string} section The section to place new admin link. Either
     * existing or new group
     * @param {string} name The title of the admin link
     * @param {string} subtitle The subtitle of the admin link displayed below
     * `name`
     * @param {string|org.visallo.admin~requiredPrivilege} [requiredPrivilege] Only show link if user has the given
     * privilege
     * @param {object} [options]
     * @param {number} [options.sortHint] A number indicating the admin tool's position within the section. If not
    included, admin items will be sorted within a section by `name`.
     */
    registry.documentExtensionPoint('org.visallo.admin',
        'Add admin tools to admin pane',
        function(e) {
            return (e.Component || e.componentPath || e.url) &&
                e.section && e.name && e.subtitle
        },
        'http://docs.visallo.org/extension-points/front-end/admin'
    );

    var adminList = defineComponent(AdminList);

    adminList.getExtensions = function() {
        var userPrivileges = visalloData.currentUser.privileges;
        return registry.extensionsForPoint('org.visallo.admin')
            .filter(function(extension) {
                /**
                 * @callback org.visallo.admin~requiredPrivilege
                 * @param {Array.<object>} userPrivileges The current users
                 * privileges
                 * @return {boolean} If the admin tool should be shown to user
                 */
                if (_.isFunction(extension.requiredPrivilege)) {
                    if (!extension.requiredPrivilege(userPrivileges)) {
                        return false;
                    }
                } else if (!extension.requiredPrivilege) {
                    if (userPrivileges.indexOf('ADMIN') < 0) {
                        return false;
                    }
                } else {
                    if (userPrivileges.indexOf(extension.requiredPrivilege) < 0) {
                        return false;
                    }
                }
                return true;
            });
    };

    return adminList;

    function AdminList() {
        this.defaultAttrs({
            listSelector: '.admin-list',
            pluginItemSelector: '.admin-list > li a',
            formSelector: '.admin-form'
        });

        this.after('initialize', function() {
            this.loadingAdminExtension = Promise.resolve();
            this.on(document, 'showAdminPlugin', this.onShowAdminPlugin);
            this.on(document, 'didToggleDisplay', this.didToggleDisplay);
            this.on('click', {
                pluginItemSelector: this.onClickPluginItem
            });
            this.$node.html(template({}));
            this.update();
        });

        this.didToggleDisplay = function(event, data) {
            if (data.name === 'admin' && !data.visible) {
                this.$node.find('.admin-list .active').removeClass('active');
                this.loadingAdminExtension.cancel();
                var form = this.select('formSelector').hide().find('.content').removePrefixedClasses('admin_less_cls')
                attacher().node(form).teardown();
                form.empty();
            }
        };

        this.onClickPluginItem = function(event) {
            event.preventDefault();
            this.trigger('showAdminPlugin', $(event.target).closest('li').data('component'));
        };

        this.onShowAdminPlugin = function(event, data) {
            var self = this;

            if (data && data.name && data.section) {
                data.name = data.name.toLowerCase();
                data.section = data.section.toLowerCase();
            }

            var $adminListItem = this.select('listSelector').find('li').filter(function() {
                    return _.isEqual($(this).data('component'), data);
                }),
                container = this.select('formSelector'),
                form = container.find('.content');

            if ($adminListItem.hasClass('active')) {
                attacher().node(form).teardown();
                $adminListItem.removeClass('active');
                self.select('formSelector').hide();
                self.trigger(container, 'paneResized');
                return;
            }
            this.loadingAdminExtension.cancel();
            $adminListItem.addClass('active').siblings('.active').removeClass('active loading').end();

            container.resizable({
                handles: 'e',
                minWidth: 120,
                maxWidth: 500,
                resize: function() {
                    self.trigger(document, 'paneResized');
                }
            });
            var extension = _.find(adminList.getExtensions(), function(e) {
                    return e.name.toLowerCase() === data.name &&
                        e.section.toLowerCase() === data.section;
                });

            form.removePrefixedClasses('admin_less_cls');

            if (extension) {
                if (extension.url) {
                    window.open(extension.url, 'ADMIN_OPEN_URL');
                    container.hide();
                    self.trigger(document, 'paneResized');
                    _.delay(function() {
                        $adminListItem.removeClass('active');
                    }, 100)
                } else {
                    $adminListItem.addClass('loading');

                    /**
                     * FlightJS or React component displaying the admin
                     * interface.
                     *
                     * @typedef org.visallo.admin~Component
                     * @property {object} data Admin-tool specific data passed from router
                     */
                    var promise = attacher()
                        .node(form)
                        .component(extension.component)
                        .path(extension.componentPath)
                        .params(data)
                        .attach({ teardown: true, empty: true })
                        .then(function() {
                            self.trigger(container.show(), 'paneResized');
                        });

                    promise.finally(function() {
                        $adminListItem.removeClass('loading');
                    });
                    this.loadingAdminExtension = promise;
                }
            } else {
                this.trigger(container, 'paneResized');
            }
        };

        this.update = function() {
            var self = this,
                extensions = adminList.getExtensions();

            require(['d3'], function(d3) {
                d3.select(self.select('listSelector').get(0))
                    .selectAll('li')
                    .data(
                        _.chain(extensions)
                        .groupBy('section')
                        .pairs()
                        .sortBy(function(d) {
                            return d[0];
                        })
                        .each(function(d) {
                            d[1] = _.chain(d[1])
                                .sortBy('name')
                                .sortBy(function sortHint({ options }) {
                                    return options && Number.isInteger(options.sortHint) ?
                                        options.sortHint : Number.MAX_VALUE;
                                })
                                .value();
                        })
                        .flatten()
                        .value()
                    )
                    .call(function() {
                        this.exit().remove();
                        this.enter().append('li')
                            .attr('class', function(component) {
                                if (_.isString(component)) {
                                    return 'nav-header';
                                }
                            }).each(function(component) {
                                if (!_.isString(component)) {
                                    d3.select(this).append('a');
                                }
                            });

                        this.each(function(component) {
                            if (_.isString(component)) {
                                this.textContent = component;
                                return;
                            }

                            d3.select(this)
                                .attr('data-component', JSON.stringify(
                                    _.chain(component)
                                    .pick('section', 'name')
                                    .tap(function(c) {
                                        c.name = c.name.toLowerCase();
                                        c.section = c.section.toLowerCase();
                                    }).value()
                                ))
                                .select('a')
                                .call(function() {
                                    this.append('div')
                                        .attr('class', 'nav-list-title')
                                        .text(component.name);

                                    this.append('div')
                                        .attr('class', 'nav-list-subtitle')
                                        .attr('title', component.subtitle)
                                        .text(component.subtitle)
                                });
                        });
                    });
            });
        };
    }
});
