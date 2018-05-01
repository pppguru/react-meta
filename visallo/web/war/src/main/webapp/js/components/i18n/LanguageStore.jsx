define([
    '/libs/reflux/dist/reflux.js',
    './LanguageActions',
], function(
    Reflux,
    LanguageActions) {
    'use strict';

    var LanguageStore = Reflux.createStore({
        data: {
            language: {
                key: 'us',
                alt: 'United States',
                title: 'English (US)'
            },
            languages: [],
            phrases: {}
        },
        listenables: LanguageActions,
        getData: function(){
            return this.data
        },
        onInitCompleted: function (_data) {
            this.data.languages = _data;
            this.trigger(this.data)
        },
        onSelectCompleted: function (_data) {
            this.data.phrases = _data;
            this.trigger(this.data)
        },
        setLanguage: function(_lang){
            this.data.language = _lang
        }
    });

    return LanguageStore;
});
