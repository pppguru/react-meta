define([
    'react',
    'react-redux'
], function(
    React,
    redux) {
    'use strict';

    const Event = React.createClass({
        getInitialState() {
          return { isCollapsed: false }
        },
        onCollapse() {
            this.setState({ isCollapsed: !this.state.isCollapsed })
        },
        render() {
            var { props } = this;
            var styles = {
              hidden: {
                  display: 'none',
              },
              show: {
                  display: 'block',
              },
              rotate: {
                  transform: 'rotate(180deg)',
                  top: '6px',
              }
            };
            var testID = [Math.floor((Math.random() * 99)), Math.floor((Math.random() * 99)), Math.floor((Math.random() * 99))];
            return (
                <div className="event" {...props}>
                    <div className="header">
                        <div style={{ border: '1px solid white' }}>
                            <h4>Registration</h4>
                            <div style={{ float: 'right' }}>
                                <button className="event-collapse" onClick={this.onCollapse} style={this.state.isCollapsed ? styles.rotate : null}></button>
                                <button className="event-drop"></button>
                            </div>
                        </div>
                    </div>
                    <div className="items">
                        <div className="item" style={this.state.isCollapsed ? styles.hidden : styles.show}>
                            <div style={{ margin: '10px 0' }}>
                                <div className="field">Confirm Email</div>
                                <span style={{ display: 'inline-block', verticalAlign: 'top' }}>
                                    <input type="checkbox" id={testID[0]} />
                                    <label htmlFor={testID[0]}></label>
                                </span>
                            </div>
                            <div style={{ margin: '10px 0' }}>
                                <div className="field">Sign In</div>
                                <span style={{ display: 'inline-block', verticalAlign: 'top' }}>
                                    <input type="checkbox" id={testID[1]} />
                                    <label htmlFor={testID[1]}></label>
                                </span>
                            </div>
                            <div style={{ margin: '10px 0' }}>
                                <div className="field">Fill out Form</div>
                                <span style={{ display: 'inline-block', verticalAlign: 'top' }}>
                                    <input type="checkbox" id={testID[2]} />
                                    <label htmlFor={testID[2]}></label>
                                </span>
                            </div>
                        </div>
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

    )(Event);
});
