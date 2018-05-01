define([
    'react',
    'react-redux',
    'Survey'
], function(
    React,
    redux,
    Survey) {
    'use strict';

    Survey.Survey.cssType = 'bootstrap';
    const surveyJSON = { surveyId: '56257219-77e9-4977-a36f-5ca2c7855648'};

    const Ideas = React.createClass({
        sendDataToServer(survey) {
            survey.sendResult('9a37d76a-4eaf-41d3-87b8-90692b1a26d5');
        },
        render() {
            var { props } = this;

            return (
                <div className="ideas-survey" {...props}>
                    <Survey.Survey json={ surveyJSON } onComplete={ this.sendDataToServer } />
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

    )(Ideas);
});
