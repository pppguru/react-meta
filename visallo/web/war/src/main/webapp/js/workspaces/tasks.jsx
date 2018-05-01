define([
    'react',
    'react-dom',
    'react-redux',
    'd3'
], function(
    React,
    ReactDOM,
    redux,
    d3) {
    'use strict';

    const LineConnector = React.createClass({
        render : function() {

            const sqSize = this.props.sqSize;
            const viewBox = `0 0 ${this.props.lineWidth} ${sqSize}`;
            const strokeWidth = 3;
            return <svg
                width={this.props.lineWidth}
                height={this.props.sqSize}
                viewBox={viewBox}>
                <line
                    x1="0"
                    y1={`${(sqSize - strokeWidth) / 2}`}
                    x2={`${(this.props.lineWidth)}`}
                    y2={`${(sqSize - strokeWidth) / 2}`}
                    stroke="#555555"
                    strokeWidth={`${(strokeWidth)}`}
                    strokeDasharray = { this.props.type === 'dash' ? '5, 5' : null}
                />
            </svg>;
        }
    });

    LineConnector.defaultProps = {
        sqSize: 200,
        lineWidth: 50
    };


    const CircularProgressBar = React.createClass({

        render : function() {
            // Default outer & inner border width
            const borderWidth = '3';
            // Size of the enclosing square
            const sqSize = this.props.sqSize;
            // SVG centers the stroke width on the radius, subtract out so circle fits in square
            const radius = (this.props.sqSize - this.props.strokeWidth - borderWidth) / 2;
            // Enclose cicle in a circumscribing square
            const viewBox = `0 0 ${sqSize} ${sqSize}`;
            // Arc length at 100% coverage is the circle circumference
            const dashArray = radius * Math.PI * 2;
            // Scale 100% coverage overlay with the actual percent
            const dashOffset = dashArray - dashArray * this.props.percentage / 100;

            let circularType = 'coming';
            if (this.props.type === '0')
                circularType = 'done';
            else if (this.props.type === '1')
                circularType = 'inprogress';
            else
                circularType = 'coming';

            const circularClassName = 'timeline-circle-' + circularType;

            return (
                <svg
                    width={this.props.sqSize}
                    height={this.props.sqSize}
                    viewBox={viewBox}>

                    <circle
                        className={`${circularClassName}-outer`}
                        cx={this.props.sqSize / 2}
                        cy={this.props.sqSize / 2}
                        r={radius + this.props.strokeWidth / 2}
                        strokeDasharray = { circularType === 'coming' ? '5, 5' : null}
                        strokeWidth= {borderWidth}
                    />

                    <circle
                        className={`${circularClassName}-inner`}
                        cx={this.props.sqSize / 2}
                        cy={this.props.sqSize / 2}
                        r={radius - this.props.strokeWidth / 2}
                        strokeDasharray = { circularType === 'coming' ? '5, 5' : null}
                        strokeWidth= {borderWidth}
                    />

                    { circularType === 'inprogress' &&

                    <circle
                        className="circle-progress"
                        cx={this.props.sqSize / 2}
                        cy={this.props.sqSize / 2}
                        r={radius}
                        strokeWidth={`${this.props.strokeWidth}px`}
                        // Start progress marker at 12 O'Clock
                        transform={`rotate(-90 ${this.props.sqSize / 2} ${this.props.sqSize / 2})`}
                        style={{
                            strokeDasharray: dashArray,
                            strokeDashoffset: dashOffset
                        }}
                    />
                    }

                    { circularType === 'done' &&
                    <polyline
                        class="check"
                        fill="none"
                        stroke="#a8a294"
                        strokeWidth="6"
                        strokeMiterlimit="20"
                        points="55,110 80,140 145,55" />
                    }
                    <text
                        className={`${circularClassName}-text`}
                        x="50%"
                        y="50%"
                        dy=".3em"
                        textAnchor="middle">
                        {`${this.props.title}`}
                    </text>

                </svg>
            );
        }
    });

    CircularProgressBar.defaultProps = {
        sqSize: 200,
        percentage: 25,
        strokeWidth: 10
    };

    const Tasks = React.createClass({
        render: function(){
            return (
                <div className="tasks-svg">
                    <div className="left-movement">
                        <i className="fa fa-chevron-circle-left"/>
                    </div>

                    {/* Start the timeline progress */}

                    <div className="main-time-line-view">
                        <CircularProgressBar
                            strokeWidth="10"
                            sqSize="200"
                            title = 'Schedule Assessment'
                            type = "0"
                            percentage= "25" />
                        <LineConnector type="line" />
                        <CircularProgressBar
                            strokeWidth="10"
                            sqSize="200"
                            title = 'Engage Client'
                            type = "1"
                            percentage= "25" />
                        <LineConnector type="dash" />
                        <CircularProgressBar
                            strokeWidth="10"
                            sqSize="200"
                            title = 'Assess Client'
                            type = "2"
                            percentage= "25" />
                    </div>

                    {/* End the timeline progress */}

                    <div className="right-movement">
                        <i className="fa fa-chevron-circle-right"/>
                    </div>

                    <div className="zoom-bar">
                        <div className="line"></div>
                        <div className="zoom-icons">
                            <button className="btn btn-small zoom-plus"><i className="fa fa-plus"/></button>
                            <button className="btn btn-small zoom-zero"><i className="fa fa-crosshairs"/></button>
                            <button className="btn btn-small zoom-minus"><i className="fa fa-minus"/></button>
                        </div>
                    </div>
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

    )(Tasks);
});
