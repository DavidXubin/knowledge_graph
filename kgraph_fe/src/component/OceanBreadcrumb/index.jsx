import React from "react";
import { message } from "ocean-utils";

const OceanBreadcrumb = (config) => WrappedComponent => {
  return class component extends React.PureComponent {
    componentDidMount() {
      message.MsgTrigger('EVENT:BREADCRUMB_SET', config);
    }

    componentWillUnmount() {
      message.MsgTrigger('EVENT:BREADCRUMB_CLEAR');
    }

    render() {
      return <WrappedComponent {...this.props} />;
    }
  }
}

export default OceanBreadcrumb;
