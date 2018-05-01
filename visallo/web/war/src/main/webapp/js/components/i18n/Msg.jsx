define([
    '/libs/reflux/dist/reflux.js',
    'react',
    'react-redux',
    './LanguageStore',
], function(
    Reflux,
    React,
    redux,
    LanguageStore) {
    'use strict';

    const Msg = React.createClass({
        mixins: [Reflux.listenTo(LanguageStore, '_onChangeLanguage')],
        languageKey: LanguageStore.getData().language.key,

        _onChangeLanguage: function(data){
            if (data.language.key !== this.languageKey){
                this.languageKey = data.language.key;
                this.forceUpdate()
            }
        },
        render: function () {
            let phrase = LanguageStore.getData().phrases[this.props.phrase] || this.props.phrase;
            return (
                <span>{phrase}</span>
            )
        }
    });

    return redux.connect(

        (state, props) => {
            return {}
        },

        (dispatch) => {
            return {}
        }

    )(Msg);
});


