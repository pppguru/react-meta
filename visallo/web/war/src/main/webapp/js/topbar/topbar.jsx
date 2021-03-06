define([
    'react',
    'react-redux',
    './recentProspects',
    '../components/layout/actions/FullScreen',
    '../components/layout/actions/ToggleMenu',
    // '../components/layout/actions/SpeechButton',
    '../components/layout/actions/SearchMobile',
    '../components/layout/tools/DeviceDetect',
    '../components/activities/ActivitiesDropdown',
    '../components/i18n/LanguageSelector'
], function(
    React,
    redux,
    RecentProspects,
    FullScreen,
    ToggleMenu,
    // SpeechButton,
    SearchMobile,
    DeviceDetect,
    ActivitiesDropdown,
    LanguageSelector) {
    'use strict';

    let Topbar = React.createClass({
        render: function () {
            return (
                <header id="header">
                    <div id="logo-group">
                        <span id="logo">
                            <img src="../img/Images/ssvf_logo.svg" alt="SSVF" />
                        </span>

                        <ActivitiesDropdown url={'api/activities/activities.json'} />

                    </div>

                    <RecentProspects />
                    <div className="pull-right"  /*pulled right: nav area*/ >


                        <ToggleMenu className="btn-header pull-right"  /* collapse menu button */ />


                        {/* #MOBILE */}
                        {/*  Top menu profile link : this shows only when top menu is active */}
                        <ul id="mobile-profile-img" className="header-dropdown-list hidden-xs padding-5">
                            <li className="">
                                <a href-void className="dropdown-toggle no-margin userdropdown" data-toggle="dropdown">
                                    <img src="../img/smartadmin/avatars/sunny.png" alt="John Doe" className="online"/>
                                </a>
                                <ul className="dropdown-menu pull-right">
                                    <li>
                                        <a href-void className="padding-10 padding-top-0 padding-bottom-0"><i
                                            className="fa fa-cog"/> Setting</a>
                                    </li>
                                    <li className="divider"/>
                                    <li>
                                        <a href="#/views/profile"
                                           className="padding-10 padding-top-0 padding-bottom-0"> <i className="fa fa-user"/>
                                            <u>P</u>rofile</a>
                                    </li>
                                    <li className="divider"/>
                                    <li>
                                        <a href-void className="padding-10 padding-top-0 padding-bottom-0"
                                           data-action="toggleShortcut"><i className="fa fa-arrow-down"/> <u>S</u>hortcut</a>
                                    </li>
                                    <li className="divider"/>
                                    <li>
                                        <a href-void className="padding-10 padding-top-0 padding-bottom-0"
                                           data-action="launchFullscreen"><i className="fa fa-arrows-alt"/> Full
                                            <u>S</u>creen</a>
                                    </li>
                                    <li className="divider"/>
                                    <li>
                                        <a href="#/login" className="padding-10 padding-top-5 padding-bottom-5"
                                           data-action="userLogout"><i
                                            className="fa fa-sign-out fa-lg"/> <strong><u>L</u>ogout</strong></a>
                                    </li>
                                </ul>
                            </li>
                        </ul>

                        {/* logout button */}
                        <div id="logout" className="btn-header transparent pull-right">
                        <span> <a href="#/login" title="Sign Out"
                                  data-logout-msg="You can improve your security further after logging out by closing this opened browser"><i
                            className="fa fa-sign-out"/></a> </span>
                        </div>

                        {/* search mobile button (this is hidden till mobile view port) */}
                        <SearchMobile className="btn-header transparent pull-right"/>


                        {/* input: search field */}
                        <form action="#/misc/search.html" className="header-search pull-right">
                            <input id="search-fld" type="text" name="param" placeholder="Find reports and more"
                                   data-autocomplete='[
                        "ActionScript",
                        "AppleScript",
                        "Asp",
                        "BASIC",
                        "C",
                        "C++",
                        "Clojure",
                        "COBOL",
                        "ColdFusion",
                        "Erlang",
                        "Fortran",
                        "Groovy",
                        "Haskell",
                        "Java",
                        "JavaScript",
                        "Lisp",
                        "Perl",
                        "PHP",
                        "Python",
                        "Ruby",
                        "Scala",
                        "Scheme"]'/>
                            <button type="submit">
                                <i className="fa fa-search"/>
                            </button>
                            <a href="$" id="cancel-search-js" title="Cancel Search"><i className="fa fa-times"/></a>
                        </form>


                        {/*<SpeechButton className="btn-header transparent pull-right hidden-sm hidden-xs" />*/}

                        <FullScreen className="btn-header transparent pull-right" />

                        {/* multiple lang dropdown : find all flags in the flags page */}
                        <LanguageSelector />


                    </div>
                    {/* end pulled right: nav area */}

                    <DeviceDetect />


                </header>
            );
        }
    });

    return redux.connect(

        (state, props) => {
            return {}
        },

        (dispatch) => {
            return {}
        }

    )(Topbar);
});
