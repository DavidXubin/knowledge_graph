import React from 'react'
import { Layout, Menu } from 'antd'
import PropTypes from 'prop-types'
import styled from 'styled-components'
import { withRouter } from 'react-router-dom'
import Store from '@/store'

const { Sider } = Layout
const { SubMenu } = Menu

const Logo = styled.div`
  height: 64px;
  color: #fff;
  background: #3d86fa;
  padding: 10px 40px;
  display: flex;
  align-items: center;
  img {
    max-width: 100%;
  }
`

@Store.storeConsumer()
class OceanSider extends React.PureComponent {
  static propTypes = {
    history: PropTypes.object,
    store: PropTypes.object
  }

  constructor(props) {
    super(props)

    this.state = {
      currentAppKey: window.location.pathname.split('/')[1] || '',
      selectedKeys: [window.location.pathname]
    }
  }

  handleClick = event => {
    this.setState({
      selectedKeys: [event.key]
    })
    this.props.history.push(event.key)
  }

  render() {
    return (
      <Sider width="230">
        <Logo>
          <img src={require('./assets/logo.png')} />
        </Logo>
        <Menu
          onClick={this.handleClick}
          mode="inline"
          defaultOpenKeys={['kgraph']}
          selectedKeys={this.state.selectedKeys}
          theme="dark"
          width="256"
        >
          <SubMenu key="kgraph" title={<span>关系图谱</span>}>
            <Menu.Item key="/datamodel">图谱菜单</Menu.Item>
          </SubMenu>
          <SubMenu key="kgraph_query" title={<span>数据查询</span>}>
            <Menu.Item key="/inquireGraph">数据查询</Menu.Item>
          </SubMenu>
        </Menu>
      </Sider>
    )
  }
}

export default withRouter(OceanSider)
