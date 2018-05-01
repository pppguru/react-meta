define([
    'react',
    'react-redux',
], function(
    React,
    redux) {
    'use strict';

    const DeviceDetect = React.createClass({
        render: function () {
            var $body = $('body');

            var isMobile = (/iphone|ipad|ipod|android|blackberry|mini|windows\sce|palm/i.test(navigator.userAgent.toLowerCase()));

            $body.toggleClass('desktop-detected', !isMobile);
            $body.toggleClass('mobile-detected', isMobile);
            return (
                <div />
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

    )(DeviceDetect);
});
