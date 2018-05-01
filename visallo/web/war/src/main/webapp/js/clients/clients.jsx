define([
    'react',
    'react-redux'
], function(
    React,
    redux) {
    'use strict';

    const Clients = React.createClass({
        render() {
            var { props } = this;

            return (
                <div className="clients-graph" {...props}>
                    Clients Graph Component
                </div>
            );
        }
    })

    return redux.connect(

        (state, props) => {
            return {}
        },

        (dispatch) => {
            return {}
        }

    )(Clients);
});
