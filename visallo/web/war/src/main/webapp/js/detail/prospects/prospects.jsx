define([
    'react',
    'react-dom',
    'react-redux',
    './prospect',
    'skylight'
], function(
    React,
    ReactDOM,
    redux,
    Prospect,
    SkyLight) {
    'use strict';

    SkyLight = SkyLight.default;

    const LayeredComponentMixin = {
        componentDidMount: function() {
            // Appending to the body is easier than managing the z-index of
            // everything on the page.  It's also better for accessibility and
            // makes stacking a snap (since components will stack in mount order).
            this._layer = document.createElement('div');
            document.body.appendChild(this._layer);
            this._renderLayer();
        },

        componentDidUpdate: function() {
            this._renderLayer();
        },

        componentWillUnmount: function() {
            this._unrenderLayer();
            document.body.removeChild(this._layer);
        },

        _renderLayer: function() {
            // By calling this method in componentDidMount() and
            // componentDidUpdate(), you're effectively creating a "wormhole" that
            // funnels React's hierarchical updates through to a DOM node on an
            // entirely different part of the page.

            const layerElement = this.renderLayer();
            // Renders can return null, but React.render() doesn't like being asked
            // to render null. If we get null back from renderLayer(), just render
            // a noscript element, like React does when an element's render returns
            // null.
            if (layerElement === null) {
                ReactDOM.render(<noscript />, this._layer);
            } else {
                ReactDOM.render(layerElement, this._layer);
            }

            if (this.layerDidMount) {
                this.layerDidMount(this._layer);
            }
        },

        _unrenderLayer: function() {
            if (this.layerWillUnmount) {
                this.layerWillUnmount(this._layer);
            }

            ReactDOM.unmountComponentAtNode(this._layer);
        },
    };

    var ButtonWithDialog = React.createClass({
        mixins: [LayeredComponentMixin],
        render: function() {
            return <button onClick={this.handleClick}>
                Click Me!
            </button>;
        },
        renderLayer: function() {
            if (this.state.clicked) {
                return <div style={{ width: '300px', height: '300px'}}>
                    sdfasdfsfdasf
                </div>
            } else {
                return <div />;
            }
        },
        // {{{
        handleClose: function() {
            this.setState({ clicked: false });
        },
        handleClick: function() {
            this.setState({ clicked: !this.state.clicked });
        },
        getInitialState: function() {
            return { clicked: false };
        }
        // }}}
    });

    const AnyReactComponent = () => <div></div>;

    const Survey = React.createClass({
        componentDidMount() {
            this.refs.prospectWizard.show();
        },

        componentWillUnmount() {
            this.props.setCloseState();
        },

        render() {
            var styles = {
                modal: {
                    backgroundColor: 'rgb(239, 239, 239)',
                    color: '#ffffff',
                    width: '70%',
                    height: '90%',
                    padding: '0',
                    marginTop: '-400px',
                    marginLeft: '-35%',
                },
                survey: {
                    width: 'calc(100% - 1px)',
                    height: 'calc(100% - 30px)',
                    border: 'none',
                }
            }
            return (
                <div>
                    <SkyLight dialogStyles={styles.modal} hideOnOverlayClicked ref="prospectWizard">
                        <iframe style={styles.survey} src='https://www.surveygizmo.com/s3/3601309/prescreen'></iframe>
                    </SkyLight>
                </div>
            )
        }
    });

    const Prospects = React.createClass({
        mixins: [LayeredComponentMixin],
        getInitialState: function() {
            return { clicked: false };
        },
        showSurvey() {
            this.setState({ clicked: !this.state.clicked});
        },
        render() {
            var { props } = this;
            return (
                <div className="prospects-detail" {...props}>
                    <div className="title">
                        <h3>Prospects</h3>
                        <p>Map View</p>
                    </div>
                    <div className="functions">
                        <button className="add-event"><div>+</div> Add Prospect</button>
                        <button className="search"></button>
                        <button className="drop-menu"></button>
                    </div>
                    <div className="map-functions">
                        <button className="map-open"></button>
                        <button className="map-close"></button>
                        <button className="map-pin" onClick={this.showSurvey}></button>
                    </div>
                    <div className="prospect">
                        <Prospect />
                        <Prospect />
                        <Prospect />
                        <Prospect />
                        <Prospect />
                        <Prospect />
                        <Prospect />
                        <Prospect />
                        <Prospect />
                    </div>
                </div>
            );
        },
        renderLayer: function() {
            if (this.state.clicked) {
                return (
                    <Survey setCloseState={this.showSurvey}/>
                )
            } else {
                return null;
            }
        }
    })

    return redux.connect(

        (state, props) => {
            return {}
        },

        (dispatch) => {
            return {}
        }

    )(Prospects);
});
