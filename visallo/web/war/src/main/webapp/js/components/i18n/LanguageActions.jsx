define([
    '/libs/reflux/dist/reflux.js'
], function(
    Reflux) {
    'use strict';

    var LanguageActions = Reflux.createActions({
        init: {
            asyncResult: true
        },
        select: {
            asyncResult: true
        }
    });

    LanguageActions.init.listen( function() {
        $.getJSON('api/langs/languages.json')
            .then( this.completed, this.failed )
    });
    LanguageActions.select.listen( function(language) {
        $.getJSON('api/langs/' + language.key + '.json')
            .then( this.completed, this.failed )
    });

    return LanguageActions;
});
