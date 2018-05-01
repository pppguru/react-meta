define([
    'react',
    'react-redux',
    'moment'
], function(
    React,
    redux,
    moment) {
    'use strict';

    const Moment = React.createClass({
        render: function () {
            return (
                <span>{
                    moment(this.props.date).format(this.props.format || 'llll')}</span>
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

    )(Moment);
});
