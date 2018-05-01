define([
    'react',
    'react-redux',
    './client'
], function(
    React,
    redux,
    Client) {
    'use strict';

    const Clients = React.createClass({
        render() {
            var { props } = this;

            return (
                <div className="clients-detail" {...props}>
                    <div className="title">
                        <h3>CLIENTS</h3>
                        <p>Graph View</p>
                    </div>
                    <div className="functions">
                        <button className="add-event"><div>+</div> Add Client</button>
                        <button className="search"></button>
                        <button className="drop-menu"></button>
                    </div>
                    <div className="client">
                        <Client />
                        <Client />
                        <Client />
                        <Client />
                        <Client />
                        <Client />
                    </div>
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
