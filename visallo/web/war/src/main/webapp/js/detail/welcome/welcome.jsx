define([
    'react',
    'react-redux',
    './event'
], function(
    React,
    redux,
    Event) {
    'use strict';

    const WelcomeBoard = React.createClass({
        render() {
            var { props } = this;

            return (
                <div className="welcome-board" {...props}>
                    <div className="title">
                        <h3>WELCOME ABOARD</h3>
                        <p>Onboarding</p>
                    </div>
                    <div className="functions">
                        <button className="add-event"><div>+</div> Add Event</button>
                        <button className="search"></button>
                        <button className="drop-menu"></button>
                    </div>
                    <div className="events">
                        <Event />
                        <Event />
                        <Event />
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

    )(WelcomeBoard);
});
