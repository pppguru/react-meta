/**
 * Routes for dashboards and dashboard cards/items
 *
 * @module services/dashboard
 * @see module:dataRequest
 */
define(['../util/ajax'], function(ajax) {
    'use strict';

    /**
     * @alias module:services/dashboard
     */
    return {

        requestData: function(endpoint, params) {
            return ajax('GET', endpoint, params);
        },

        postData: function(endpoint, params) {
            return ajax('POST', endpoint, params);
        },

        /**
         * Get list of all dashboards (without extendedData)
         */
        dashboards: function() {
            return ajax('GET', '/dashboard/all')
                .then(function(result) {
                    return result.dashboards.map(function(dashboard) {
                        dashboard.items = dashboard.items.map(function(item) {
                            if (item.configuration) {
                                try {
                                    item.configuration = JSON.parse(item.configuration);
                                } catch(e) {
                                    console.error(e);
                                }
                            }
                            return item;
                        })
                        return dashboard;
                    })
                })
        },

        /**
         * Remove an item from a dashboard
         *
         * @param {string} itemId
         */
        dashboardItemDelete: function(itemId) {
            return ajax('DELETE', '/dashboard/item', {
                dashboardItemId: itemId
            });
        },

        /**
         * Create a new dashboard
         *
         * @param {object} [options]
         * @param {string} [options.title='Untitled'] The title of the new dashboard
         * @param {Array.<object>} [options.items=[]] List of item configurations
         * to add to new dashboard
         */
        dashboardNew: function(options) {
            var params = {};
            if (options && options.title) {
                params.title = options.title;
            }
            if (options && options.items) {
                params.items = options.items.map(function(item) {
                    var mapped = _.extend({}, item);
                    if (mapped.configuration) {
                        mapped.configuration = JSON.stringify(mapped.configuration);
                    }
                    return JSON.stringify(mapped);
                })
            }
            return ajax('POST', '/dashboard', params);
        },

        /**
         * Update dashboard
         *
         * @param {object} params
         */
        dashboardUpdate: function(params) {
            return ajax('POST', '/dashboard', params);
        },

        /**
         * Update item on dashboard
         *
         * @param {object} item The configuration to update
         * @param {string} item.id
         * @param {string} item.extensionId
         * @param {string} item.title
         * @param {object} [item.configuration={}]
         */
        dashboardItemUpdate: function(item) {
            return ajax('POST', '/dashboard/item', {
                dashboardItemId: item.id,
                extensionId: item.extensionId,
                title: item.title,
                configuration: JSON.stringify(item.configuration || {})
            });
        },

        /**
         * Create new dashboard item
         *
         * @param {string} dashboardId
         * @param {object} item The new item configuration
         * @param {string} item.id
         * @param {string} item.extensionId
         * @param {string} [item.title]
         * @param {object} [item.configuration={}]
         */
        dashboardItemNew: function(dashboardId, item) {
            if (!dashboardId) throw new Error('dashboardId required if new item');

            var params = {
                dashboardId: dashboardId
            };
            if ('title' in item) {
                params.title = item.title;
            }
            if (item.configuration) {
                params.configuration = JSON.stringify(item.configuration);
            }
            if ('extensionId' in item) {
                params.extensionId = item.extensionId;
            }
            return ajax('POST', '/dashboard/item', params);
        }
    };
});
