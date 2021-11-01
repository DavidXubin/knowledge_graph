import React from 'react';
import {
    Layout,
    Icon,
    Menu,
    Dropdown,
    Avatar,
    Breadcrumb,
} from 'antd';
import {Link, withRouter} from 'react-router-dom';
import {message} from 'ocean-utils';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import Store from '@/store';

const {Header} = Layout;

const menu = (
    <Menu>
        <Menu.Item>
            <a href="/server/auth/logout">
                <Icon type="logout"/> 退出
            </a>
        </Menu.Item>
    </Menu>
);

const FrameHeader = styled.div`
  border-bottom: 1px solid #e8e8e8;
  .breadcrumb {
    margin-left: 20px;
    margin-top: 23px;
    float: left;
    display: flex;
    .anticon {
      margin-right: 13px;
      position: relative;
      top: 3px;
    }

    .ant-breadcrumb > span {
      .ant-breadcrumb-link {
        color: #666;
        a {
          color: #666;
        }
      }
    }

    .ant-breadcrumb > span:last-of-type {
      .ant-breadcrumb-link {
        color: #3d86fa;
      }
    }
  }
  .ant-layout-header {
    background: #fff;
    padding: 0;
    .ant-avatar {
      margin-right: 10px;
      i {
        font-size: 16px !important;
      }
    }

    .personal {
      cursor: pointer;
      display: flex;
      margin-right: 50px;
      float: right;
      .user {
        margin-left: 20px;
        margin-right: 20px;
      }
    }

    .role {
      float: right;
      margin-right: 26px;
      .ant-select-selection {
        background: #f1f7ff;
        border-color: #d8e9ff;
        color: #5a7db4;
        .anticon {
          color: #5a7db4;
        }
      }
    }
  }
`;

@Store.storeConsumer()
class OceanHeader extends React.PureComponent {
    static propTypes = {
        store: PropTypes.object
    };

    constructor(props) {
        super(props);
        const currentUserInf = this.props.store.profile;
        this.state = {
            breadcrumb: null,
            currentUserInf: currentUserInf
        };

    }

    componentDidMount() {
        message.MsgRegister('EVENT:BREADCRUMB_SET', breadcrumb => {
            this.setState({
                breadcrumb
            });
        });

        message.MsgRegister('EVENT:BREADCRUMB_CLEAR', () => {
            this.setState({
                breadcrumb: null
            });
        });
    }


    renderBreadcrumb = () => {
        if (!this.state.breadcrumb) {
            return null;
        }
        return (
            <div className="breadcrumb">
                <Icon type="environment" theme="outlined" style={{color: '#666'}}/>
                <Breadcrumb>
                    {this.state.breadcrumb.map((config, index) => {
                        return <Breadcrumb.Item key={`Breadcrumb-${Number(index)}`}>{config.path ?
                            <Link to={config.path}>{config.text}</Link> : config.text}</Breadcrumb.Item>;
                    })}
                </Breadcrumb>
            </div>
        );
    };

    render() {
        const {
            store: {profile = {}}
        } = this.props;
        const {name, roles, store} = profile;

        return (
            <FrameHeader>
                <Header>
                    {this.renderBreadcrumb()}
                    <div className="personal">
                        <Dropdown overlay={menu}>
                            <div>
                                <Avatar icon="user" src={require('./assets/header.svg')}/>
                                {name}
                            </div>
                        </Dropdown>
                    </div>
                </Header>
            </FrameHeader>
        );
    }
}

export default withRouter(OceanHeader);
