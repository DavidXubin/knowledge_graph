import React, { Component } from 'react';

class TestApp extends React.Component {
    render() {
        return <div>Hello {this.props.name}</div>;
    }
}

export default TestApp;
