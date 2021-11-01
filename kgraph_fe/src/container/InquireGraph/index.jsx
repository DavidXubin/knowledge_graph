//# sourceURL=dynamicScript.js

import React from 'react'
import {
    Modal,
    notification,
    DatePicker,
    Card,
    Select,
    Input,
    Button,
    Table,
    Tooltip,
    Icon,
    Spin,
    Tabs,
    message
} from 'antd'
import InquireGraphService from '@/service/inquireGraph'
import G2 from '@antv/g2'
import Store from '@/store'
import OceanBreadcrumb from '@/component/OceanBreadcrumb'
import JSONTree from 'react-json-tree'
import CustomizedQueryPanel from './query';
import MetaService from "@/service/permission";

const { TabPane } = Tabs;

@OceanBreadcrumb([
    { path: '/', text: '关系图谱' },
    { path: '/inquireGraph', text: '数据查询' }
])
@Store.storeConsumer()
class InquireGraph extends React.PureComponent {
    constructor(props) {
        super(props);
        this.state = {
            conditions: [],
            graph: '',
            relation: '',
            graphList: [],
            relationList: [],
            loading: false,
            page: 1,
            queryData: null,
            queryDataCount: 0,
            graphLoading: false,
            relationLoading: false,
            screenWidth: document.body.clientWidth,
            currentUserInf: this.props.store.profile
        }
    }

    queryBean = {
        startIndex: 0,
        pageSize: 100,
        graphName: '',
        relationName: '',
        tabKey: "entity"
    };

    componentDidMount() {
        this.setState({ graphLoading: true });

        let params = {
            user: this.state.currentUserInf.account.toLowerCase()
        };

        InquireGraphService.getDB(params).then(result => {
            this.setState({ graphLoading: false });
            
            if (result !== null && result.length > 0) {
                this.setState({ graphList: result })
            }
        })
    }

    graphSelect = value => {
        if (this.queryBean.tabKey === "customized_query") {
            this.queryBean.graphName = value;
            this.setState({graph: value});
            return;
        }

        this.setState({
            relationLoading: true,
            relation: ''
        });

        console.log("graph =", value);

        this.queryBean.graphName = value;

        if (value && value.length > 0) {
            this.setState({graph: value}, () => {
                let params = {
                    user: this.state.currentUserInf.account.toLowerCase(),
                    graph: this.state.graph
                };

                InquireGraphService.getRelation(params).then(result => {
                    this.setState({relationLoading: false});
                    if (result !== null && result.length > 0) {
                        if (this.queryBean.tabKey === "entity") {
                            this.setState({relationList: result[0]})
                        } else if (this.queryBean.tabKey === "edge") {
                            this.setState({relationList: result[1]})
                        }
                    }
                })
            })
        } else {
            this.setState({graph: "", relation: "", relationList: []});
            this.queryBean.graphName = "";
            this.queryBean.relationName = "";
        }

    };

    relationSelect = value => {
        if (value && value.length > 0) {
            this.queryBean.relationName = value;
            this.setState({relation: value})
        } else {
            this.queryBean.relationName = "";
            this.setState({relation: ""});
        }
    };


    inquire = () => {

        if (
            this.queryBean.graphName.length > 0 &&
            this.queryBean.relationName.length > 0
        ) {
            this.setState({ loading: true });

            let params = {
                user: this.state.currentUserInf.account.toLowerCase(),
                graph: this.queryBean.graphName
            };

            MetaService.checkPermission(params).then(result => {
                if (result != null && result.length > 0) {

                    InquireGraphService.inquireGraphRelationCount(this.queryBean).then(result => {
                        if (result !== null) {
                            console.log(result);
                            this.setState({ queryDataCount: result[0][this.queryBean.relationName] });

                            InquireGraphService.inquireGraph(this.queryBean).then(result => {
                                this.setState({ loading: false });

                                if (result !== null) {
                                    this.setState({ queryData: result })
                                }
                            })
                        } else {
                            this.setState({ loading: false });

                            notification.error({
                                message: "数据错误",
                                description: '数据大小获取失败'
                            });
                        }
                    });

                } else {
                    this.setState({ loading: false });

                    notification.error({
                        message: "权限错误",
                        description: '请到datamap上去申请访问图谱的权限'
                    });
                }
            })

        } else {
            message.error('必填字断不能为空')
        }
    };


    columnsList = () => {
        let list = [];

        this.state.queryData.recordBean.keyList.map((item, index) => {
            list.push({
                title: item,
                dataIndex: item,
                key: item
            })
        });
        return list
    };

    columnDetail = render => {
        return (
            <Tabs defaultActiveKey="1" key={JSON.stringify(render)}>
                <TabPane tab="Table" key="1">
                    {this.state.queryData.recordBean.keyList.map(item => {
                        return (
                            <div style={{ margin: '0 0 10px 0', display: 'flex' }} key={item}>
                                <p
                                    style={{
                                        margin: '0 0 10px 0',
                                        backgroundColor: '#ffffff',
                                        width: '100px'
                                    }}
                                >
                                    {item}
                                </p>
                                <p
                                    style={{
                                        margin: '0 0 10px 0',
                                        backgroundColor: '#ffffff',
                                        marginLeft: '20px',
                                        width: this.state.screenWidth - 550
                                    }}
                                >
                                    {render[item]}
                                </p>
                            </div>
                        )
                    })}
                </TabPane>
                <TabPane tab="JSON" key="2">
                    <div
                        style={{
                            margin: '0 0 10px 0',
                            backgroundColor: '#ffffff',
                            color: '#0000FF',
                            width: this.state.screenWidth - 350
                        }}
                    >
                        <JSONTree data={render} />
                    </div>
                </TabPane>
            </Tabs>
        )
    };

    changePage = (page, pageSize) => {
        this.setState({ page: page });
        console.log("page idx: {}, page size: {}", page, pageSize);
        this.queryBean.startIndex = (page  - 1) * pageSize;
        this.inquire()
    };

    changeTab = activeKey => {
        console.log(activeKey);
        this.setState(
            {
                    queryData: null,
                    queryDataCount: 0,
                    graphList: [],
                    relationList: [],
                    graph: "",
                    relation: "",
                    page: 1
                }
                );
        this.queryBean.tabKey = activeKey;
        this.queryBean.startIndex = 0;
        this.queryBean.pageSize = 50;
        this.queryBean.graphName = '';
        this.queryBean.relationName = "";

        let params = {
            user: this.state.currentUserInf.account.toLowerCase()
        };

        InquireGraphService.getDB(params).then(result => {
            this.setState({ graphLoading: false });

            if (result !== null && result.length > 0) {
                this.setState({ graphList: result })
            }
        })
    };

    render() {
        return (
            <Tabs defaultActiveKey="1" onChange={this.changeTab}>
                <TabPane tab="查询实体" key="entity">
                    <Card>
                        <div style={{ margin: '0 0 10px 0', display: 'flex' }}>
                            <Select
                                showSearch
                                size="large"
                                placeholder="请选择图谱(必填)"
                                style={{ flex: 1 }}
                                allowClear={true}
                                onChange={this.graphSelect}
                                loading={this.state.graphLoading}
                            >
                                {this.state.graphList.length > 0
                                    ? this.state.graphList.map(graphName => {
                                        return (
                                            <Select.Option key={graphName} value={graphName}>
                                                {graphName}
                                            </Select.Option>
                                        )
                                    })
                                    : null}
                            </Select>
                            <Select
                                showSearch
                                size="large"
                                optionLabelProp="label"
                                placeholder="请选择实体"
                                style={{ flex: 1, marginLeft: '10px' }}
                                allowClear={true}
                                onChange={this.relationSelect}
                                value={this.state.relation ? this.state.relation : []}
                                loading={this.state.relationLoading}
                            >
                                {this.state.relationList.length > 0
                                    ? this.state.relationList.map(relationName => {
                                        return (
                                            <Select.Option key={relationName} value={relationName} label={relationName}>
                                                {relationName}
                                            </Select.Option>
                                        )
                                    })
                                    : null}
                            </Select>
                            <Button
                                size="large"
                                style={{ flex: 1, marginLeft: '10px' }}
                                type="primary"
                                onClick={this.inquire}
                            >
                                查询
                            </Button>
                        </div>
                        {/*this.condition()*/}
                        {this.state.loading && !this.state.queryData ? (
                            <div
                                style={{
                                    height: '60vh',
                                    display: 'flex',
                                    justifyContent: 'center',
                                    alignItems: 'center'
                                }}
                            >
                                <Spin tip={'数据加载中....'} />
                            </div>
                        ) : null}
                        {/* {this.charDetail()} */}
                        {this.state.queryData ? (
                            <Table
                                columns={this.columnsList()}
                                loading={this.state.loading}
                                bordered
                                pagination={{
                                    pageSize: this.queryBean.pageSize,
                                    current: this.state.page,
                                    total: this.state.queryDataCount,
                                    onChange: this.changePage,
                                    showTotal: total => `共${this.state.queryDataCount}条`
                                }}
                                dataSource={this.state.queryData.recordBean.valueList}
                                expandedRowRender={this.columnDetail}
                                rowKey={record => JSON.stringify(record)}
                                style={{ marginTop: '10px', overflow: 'auto' }}
                                //scroll={{ y: 600, x: 1600 }}
                            />
                        ) : null}
                    </Card>
                </TabPane>
                <TabPane tab="查询关系" key="edge">
                    <Card>
                        <div style={{ margin: '0 0 10px 0', display: 'flex' }}>
                            <Select
                                showSearch
                                size="large"
                                placeholder="请选择图谱(必填)"
                                style={{ flex: 1 }}
                                allowClear={true}
                                onChange={this.graphSelect}
                                loading={this.state.graphLoading}
                            >
                                {this.state.graphList.length > 0
                                    ? this.state.graphList.map(graphName => {
                                        return (
                                            <Select.Option key={graphName} value={graphName}>
                                                {graphName}
                                            </Select.Option>
                                        )
                                    })
                                    : null}
                            </Select>
                            <Select
                                showSearch
                                size="large"
                                optionLabelProp="label"
                                placeholder="请选择关系"
                                style={{ flex: 1, marginLeft: '10px' }}
                                allowClear={true}
                                onChange={this.relationSelect}
                                value={this.state.relation ? this.state.relation : []}
                                loading={this.state.relationLoading}
                            >
                                {this.state.relationList.length > 0
                                    ? this.state.relationList.map(relationName => {
                                        return (
                                            <Select.Option key={relationName} value={relationName} label={relationName}>
                                                {relationName}
                                            </Select.Option>
                                        )
                                    })
                                    : null}
                            </Select>
                            <Button
                                size="large"
                                style={{ flex: 1, marginLeft: '10px' }}
                                type="primary"
                                onClick={this.inquire}
                            >
                                查询
                            </Button>
                        </div>
                        {/*this.condition()*/}
                        {this.state.loading && !this.state.queryData ? (
                            <div
                                style={{
                                    height: '60vh',
                                    display: 'flex',
                                    justifyContent: 'center',
                                    alignItems: 'center'
                                }}
                            >
                                <Spin tip={'数据加载中....'} />
                            </div>
                        ) : null}
                        {/* {this.charDetail()} */}
                        {this.state.queryData ? (
                            <Table
                                columns={this.columnsList()}
                                loading={this.state.loading}
                                bordered
                                pagination={{
                                    pageSize: this.queryBean.pageSize,
                                    current: this.state.page,
                                    total: this.state.queryDataCount,
                                    onChange: this.changePage,
                                    showTotal: total => `共${this.state.queryDataCount}条`
                                }}
                                dataSource={this.state.queryData.recordBean.valueList}
                                expandedRowRender={this.columnDetail}
                                rowKey={record => JSON.stringify(record)}
                                style={{ marginTop: '10px', overflow: 'auto' }}
                                // scroll={{ y: 600, x: 1600 }}
                            />
                        ) : null}
                    </Card>
                </TabPane>
                <TabPane tab="定制查询" key="customized_query">
                    <Card>
                        <div style={{ margin: '0 0 10px 0', display: 'flex', width: '30%'  }}>
                            <Select
                                showSearch
                                size="large"
                                placeholder="请选择图谱(必填)"
                                style={{ flex: 1 }}
                                allowClear={true}
                                onChange={this.graphSelect}
                                loading={this.state.graphLoading}
                            >
                                {this.state.graphList.length > 0
                                    ? this.state.graphList.map(graphName => {
                                        return (
                                            <Select.Option key={graphName} value={graphName}>
                                                {graphName}
                                            </Select.Option>
                                        )
                                    })
                                    : null}
                            </Select>

                        </div>
                        <div>
                            {this.state.graph ? <CustomizedQueryPanel graph={this.state.graph} />: null}
                        </div>
                    </Card>
                </TabPane>
            </Tabs>
        )
    }
}

export default InquireGraph;
