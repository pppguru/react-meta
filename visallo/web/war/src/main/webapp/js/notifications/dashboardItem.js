define([
    'flight/lib/component',
    './notifications'
], function(
    defineComponent,
    Notifications) {
    'use strict';

    return defineComponent(NotificationsDashboardItem);

    function NotificationsDashboardItem() {

        this.after('initialize', function() {
            Notifications.attachTo(this.node, {
                allowSystemDismiss: false,
                animated: false,
                showUserDismissed: true
            });
            this.$node.addClass('list');
        });

    }
});
