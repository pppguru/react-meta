define([
    'configuration/plugins/registry',
    'util/messages'
], function(registry, i18n) {
    'use strict';

    var adminExtensionPoint = 'org.visallo.admin';

    /**
     * @undocumented
     */
    registry.documentExtensionPoint(
        'org.visallo.admin.user.privileges',
        "Displays editor for user's privileges.",
        function() {
            return true;
        }
    );

    /**
     * @undocumented
     */
    registry.documentExtensionPoint(
        'org.visallo.admin.user.authorizations',
        "Displays editor for user's authorizations.",
        function() {
            return true;
        }
    );

    registry.registerExtension(adminExtensionPoint, {
        componentPath: 'org/visallo/web/adminUserTools/UserAdminPlugin',
        section: i18n('admin.user.section'),
        name: i18n('admin.user.editor'),
        subtitle: i18n('admin.user.editor.subtitle')
    });
});
