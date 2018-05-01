define([
    'react',
    'react-redux'
], function(
    React,
    redux) {
    'use strict';

    const SearchMobile = React.createClass({
        toggleSearch: function(e){
            $('body').addClass('search-mobile');
            e.preventDefault();
            $('#cancel-search-js').on('click', function (e) {
                $('body').removeClass('search-mobile');
                e.preventDefault();
            });
        },
        render: function () {
            return (
                <div id="search-mobile" className={this.props.className} >
                    <span> <a href-void onClick={this.toggleSearch} title="Search"><i className="fa fa-search"/></a> </span>
                </div>
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

    )(SearchMobile);
});
