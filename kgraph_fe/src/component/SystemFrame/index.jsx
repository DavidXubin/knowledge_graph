import React from 'react';
import { Layout } from 'antd';
import { withRouter } from 'react-router-dom';
import OceanSider from './OceanSider';
import OceanHeader from './OceanHeader';

const { Content, Footer } = Layout;

const SystemFrame = WrappedComponent => {
  class component extends React.PureComponent {
    handleClick = event => {
      this.props.history.push(event.key);
    };

    render() {
      return (
        <Layout>
          <OceanSider />
          <Layout>
            <OceanHeader />
            <Content>
              <WrappedComponent {...this.props} />
              <Footer style={{ textAlign: 'center', fontSize: '12px', color: '#8E99A9' }}>©2018 powered by 产品技术中心</Footer>
            </Content>
          </Layout>
        </Layout>
      );
    }
  }

  return withRouter(component);
};

export default SystemFrame;
