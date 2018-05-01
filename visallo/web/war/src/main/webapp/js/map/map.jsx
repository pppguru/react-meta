define([
    'react',
    'react-redux',
    'google-map-react'
], function(
    React,
    redux,
    GoogleMap) {
    'use strict';

    const AnyReactComponent = ({ text }) => <div>{text}</div>;


    const K_CIRCLE_SIZE = 30;
    const K_STICK_SIZE = 10;
    const K_STICK_WIDTH = 3;
    const K_CIRCLE_PIN = 1;
    const K_RECT_PIN = 2;
    const K_TRI_PIN = 3;

    const greatPlaceStyle = {
        // initially any map object has left top corner at lat lng coordinates
        // it's on you to set object origin to 0,0 coordinates
        position: 'absolute',
        width: K_CIRCLE_SIZE,
        height: K_CIRCLE_SIZE + K_STICK_SIZE,
        left: -K_CIRCLE_SIZE / 2,
        top: -(K_CIRCLE_SIZE + K_STICK_SIZE)
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


    const greatPlaceRectStyleHover = {
        ...greatPlaceRectStyle,
        backgroundColor: '#ffae34',
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

    const greatPlaceStickStyleHover = {
        ...greatPlaceStickStyle,
        backgroundColor: '#ffae34'
    };

    const MyGreatPlaceWithStick = React.createClass({
        render() {
            const {text, zIndex, pinType} = this.props;

            const style = {
                ...greatPlaceStyle,
                zIndex: this.props.$hover ? 1000 : zIndex
            };

            const rectStyle = this.props.$hover ? greatPlaceRectStyleHover : greatPlaceRectStyle;
            const circleStyle = this.props.$hover ? greatPlaceCircleStyleHover : greatPlaceCircleStyle;
            const stickStyle = this.props.$hover ? greatPlaceStickStyleHover : greatPlaceStickStyle;
            return (
                <div style={style}>
                    <div style={greatPlaceStickStyleShadow} />
                    <div style={pinType === K_CIRCLE_PIN ? circleStyle : rectStyle}>
                        {text}
                    </div>
                    <div style={stickStyle} />
                </div>
            );
        }
    });

    const MapContainer = React.createClass({
        _distanceToMouse(markerPos, mousePos, markerProps) {
            const x = markerPos.x;
            // because of marker non symmetric,
            // we transform it central point to measure distance from marker circle center
            // you can change distance function to any other distance measure
            const y = markerPos.y - K_STICK_SIZE - K_CIRCLE_SIZE / 2;

            // and i want that hover probability on markers with text === 'A' be greater than others
            // so i tweak distance function (for example it's more likely to me that user click on 'A' marker)
            // another way is to decrease distance for 'A' marker
            // this is really visible on small zoom values or if there are a lot of markers on the map
            const distanceKoef = markerProps.text !== 'A' ? 1.5 : 1;

            // it's just a simple example, you can tweak distance function as you wish
            return distanceKoef * Math.sqrt((x - mousePos.x) * (x - mousePos.x) + (y - mousePos.y) * (y - mousePos.y));
        },

        render() {
            var { props } = this;

            return (
                <GoogleMap
                    defaultCenter={{lat: 40.74, lng: -73.98}}
                    defaultZoom={14}
                    hoverDistance={K_CIRCLE_SIZE / 2}
                    distanceToMouse={this._distanceToMouse}
                    bootstrapURLKeys={{key: 'AIzaSyCgTQ2y-w1D02ShsRa94FkIV_6ofh9_eGQ'}}
                >
                    <MyGreatPlaceWithStick lat={40.741895} lng={-73.989308} text={'A'} zIndex={3} pinType={K_CIRCLE_PIN} />
                    <MyGreatPlaceWithStick {...{lat: 40.74, lng: -73.98}} text={'B'} zIndex={2} />
                    <MyGreatPlaceWithStick {...{lat: 40.741, lng: -73.984}} text={'C'} zIndex={1} />
                </GoogleMap>
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

    )(MapContainer);
});
