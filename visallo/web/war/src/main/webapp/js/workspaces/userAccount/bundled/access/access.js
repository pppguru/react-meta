define([
    'flight/lib/component',
    './template.hbs'
], function(
    defineComponent,
    template) {
    'use strict';

    return defineComponent(Access);

    function Access() {

        this.after('initialize', function() {
            var user = visalloData.currentUser,
                order = 'EDIT COMMENT PUBLISH ADMIN'.split(' ');

            this.$node.html(template({
                privileges: _.chain(user.privileges)
                    .without('READ')
                    .sortBy(function(p) {
                        return order.indexOf(p);
                    })
                    .value(),
                authorizations: user.authorizations
            }));
        });

    }
});
