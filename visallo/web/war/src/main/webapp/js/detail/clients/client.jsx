define([
    'react',
    'react-redux'
], function(
    React,
    redux) {
    'use strict';

    const K_CIRCLE_SIZE = 15;
    const K_STICK_SIZE = 10;
    const K_STICK_WIDTH = 3;
    const K_CIRCLE_PIN = 1;
    const K_RECT_PIN = 2;
    const K_TRI_PIN = 3;

    const greatPlaceStyle = {
        // initially any map object has left top corner at lat lng coordinates
        // it's on you to set object origin to 0,0 coordinates
        position: 'relative',
        width: K_CIRCLE_SIZE,
        height: K_CIRCLE_SIZE + K_STICK_SIZE
    };

    const greatPlaceCircleStyle = {
        position: 'absolute',
        left: 0,
        top: 0,
        width: K_CIRCLE_SIZE,
        height: K_CIRCLE_SIZE,
        lineHeight: `${K_CIRCLE_SIZE}px`,
        borderRadius: K_CIRCLE_SIZE,
        backgroundColor: '#ab7421',
        textAlign: 'center',
        color: '#fff',
        fontSize: 12,
        fontWeight: 'bold',
        padding: 0,
        cursor: 'pointer',
        boxShadow: '0 0 0 1px white'
    };


    const greatPlaceCircleStyleHover = {
        ...greatPlaceCircleStyle,
        backgroundColor: '#ffae34',
    };

    const greatPlaceRectStyle = {
        position: 'absolute',
        left: 0,
        top: 0,
        width: K_CIRCLE_SIZE,
        height: K_CIRCLE_SIZE,
        lineHeight: `${K_CIRCLE_SIZE}px`,
        borderRadius: 0,
        backgroundColor: '#ab7421',
        textAlign: 'center',
        color: '#fff',
        fontSize: 12,
        fontWeight: 'bold',
        padding: 0,
        cursor: 'pointer',
        boxShadow: '0 0 0 1px white'
    };

    const greatPlaceStickStyleShadow = {
        position: 'absolute',
        left: K_CIRCLE_SIZE / 2 - K_STICK_WIDTH / 2,
        top: K_CIRCLE_SIZE,
        width: K_STICK_WIDTH,
        height: K_STICK_SIZE,
        backgroundColor: '#ab7421',
        boxShadow: '0 0 0 1px white'
    };


    const greatPlaceStickStyle = {
        position: 'absolute',
        left: K_CIRCLE_SIZE / 2 - K_STICK_WIDTH / 2,
        top: K_CIRCLE_SIZE,
        width: K_STICK_WIDTH,
        height: K_STICK_SIZE,
        backgroundColor: '#ab7421'
    };

    const PinForClient = React.createClass({
        render() {
            const {text, zIndex, pinType} = this.props;
            const style = {
                ...greatPlaceStyle,
                display: 'inline-block',
                verticalAlign: 'top',
                margin: '4px 10px',
                zIndex: this.props.$hover ? 1000 : zIndex
            };

            return (
                <div style={style}>
                    <div style={greatPlaceStickStyleShadow} />
                    <div style={pinType === K_CIRCLE_PIN ? greatPlaceCircleStyle : greatPlaceRectStyle}>
                        {text}
                    </div>
                    <div style={greatPlaceStickStyle} />
                </div>
            );
        }
    });

    const Client = React.createClass({
        render() {
            const { props } = this;
            const style = {
                display: 'inline-block',
                verticalAlign: 'top',
                margin: '6px 0'
            }
            return (
                <div className="client-item" {...props}>
                    <PinForClient pinType={K_CIRCLE_PIN}/>
                    <p style={style}>One Item of Clients</p>
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

    )(Client);
})
