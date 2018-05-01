/**
 * Created by griga on 11/30/15.
 */
define([
    'react',
    'react-redux'
], function(
    React,
    redux) {
    'use strict';

    let $body = $('body');

    const MinifyMenu = React.createClass({
        toggle: function () {
            if (!$body.hasClass('menu-on-top')) {
                $body.toggleClass('minified');
                $body.removeClass('hidden-menu');
                $('html').removeClass('hidden-menu-mobile-lock');
            }
        },
        render: function () {
            return (
                <span className="minifyme" data-action="minifyMenu" onClick={this.toggle}>
                   <i className="fa fa-arrow-circle-left hit"/>
               </span>
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

    )(MinifyMenu);
});
