import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {Avatar, Button, Card, Comment, Form, Input, List, notification, Spin, Table, Divider} from 'antd';
import styled from 'styled-components';
import Store from "@/store";
import InquireGraphService from '@/service/inquireGraph'
import MetaService from "@/service/permission";

Date.prototype.Format = function(format) {
    const date = {
        "M+": this.getMonth() + 1,
        "d+": this.getDate(),
        "h+": this.getHours(),
        "m+": this.getMinutes(),
        "s+": this.getSeconds(),
        "q+": Math.floor((this.getMonth() + 3) / 3),
        "S+": this.getMilliseconds()
    };
    if (/(y+)/i.test(format)) {
        format = format.replace(RegExp.$1, (this.getFullYear() + '').substr(4 - RegExp.$1.length));
    }
    for (const k in date) {
        if (new RegExp("(" + k + ")").test(format)) {
            format = format.replace(RegExp.$1, RegExp.$1.length === 1
                ? date[k] : ("00" + date[k]).substr(("" + date[k]).length));
        }
    }
    return format;
};

const TextArea = Input.TextArea;

const Wrapper = styled.div`
  .cell-max-width {
    max-width: 200px;
  }
`;

const HistoryQueryList = ({historyQueries}) => (

    <List
        dataSource={historyQueries}
        // header={`${comments.length} ${comments.length > 1 ? 'replies' : 'reply'}`}
        itemLayout="horizontal"
        renderItem={props => <Comment {...props} />}
    />
);

const Editor = ({
                    onChange, onSubmit, submitting, value,
                }) => (
    <div>
        <Form.Item>
            <TextArea rows={4} onChange={onChange} value={value}/>
        </Form.Item>
        <Form.Item>
            <Button
                htmlType="submit"
                loading={submitting}
                onClick={onSubmit}
                type="primary"
            >
                查询
            </Button>
        </Form.Item>
    </div>
);

@Store.storeConsumer()
export default class CustomizedQueryPanel extends PureComponent {
    static propTypes = {
        params: PropTypes.object
    };

    constructor(props) {
        super(props);
        const currentUserInf = this.props.store.profile;

        this.state = {
            currentUserInf: currentUserInf,
            data: {},
            historyQueries: [],
            default_avatar: 'data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBzdGFuZGFsb25lPSJubyI/PjwhRE9DVFlQRSBzdmcgUFVCTElDICItLy9XM0MvL0RURCBTVkcgMS4xLy9FTiIgImh0dHA6Ly93d3cudzMub3JnL0dyYXBoaWNzL1NWRy8xLjEvRFREL3N2ZzExLmR0ZCI+PHN2ZyB0PSIxNTM5OTM3NDAwNTg3IiBjbGFzcz0iaWNvbiIgc3R5bGU9IiIgdmlld0JveD0iMCAwIDEwMjQgMTAyNCIgdmVyc2lvbj0iMS4xIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHAtaWQ9IjExNDUiIHhtbG5zOnhsaW5rPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5L3hsaW5rIiB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCI+PGRlZnM+PHN0eWxlIHR5cGU9InRleHQvY3NzIj48L3N0eWxlPjwvZGVmcz48cGF0aCBkPSJNNTEyIDEwMjRhNTEyIDUxMiAwIDEgMSA1MTItNTEyIDUxMiA1MTIgMCAwIDEtNTEyIDUxMnogbTAtMTcuOTY0OTEyQTQ5NC4wMzUwODggNDk0LjAzNTA4OCAwIDEgMCAxNy45NjQ5MTIgNTEyYTQ5NC4wMzUwODggNDk0LjAzNTA4OCAwIDAgMCA0OTQuMDM1MDg4IDQ5NC4wMzUwODh6IiBmaWxsPSIjRDNERkVGIiBwLWlkPSIxMTQ2Ij48L3BhdGg+PHBhdGggZD0iTTUxMiA1MTJtLTQ1OC4xMDUyNjMgMGE0NTguMTA1MjYzIDQ1OC4xMDUyNjMgMCAxIDAgOTE2LjIxMDUyNiAwIDQ1OC4xMDUyNjMgNDU4LjEwNTI2MyAwIDEgMC05MTYuMjEwNTI2IDBaIiBmaWxsPSIjRjRGOUZGIiBwLWlkPSIxMTQ3Ij48L3BhdGg+PHBhdGggZD0iTTUxNi4wNDIxMDUgOTg4LjA3MDE3NUE0NTQuMzMyNjMyIDQ1NC4zMzI2MzIgMCAwIDAgOTA3LjIyODA3IDc2My41MDg3NzJjLTIzLjA4NDkxMi0zNS45Mjk4MjUtMTgzLjA2MjQ1Ni04OS44MjQ1NjEtMjQ2LjExOTI5OC04OS44MjQ1NjEtMTI0LjU4NjY2NyAyMTguODEyNjMyLTI5Mi4xOTkyOTggMzcuMDA3NzE5LTI4OS4xNDUyNjMgMS41MjcwMTctODYuMDUxOTMtNi4xMDgwNy0xOTYuNzE1Nzg5IDU5LjE5NDM4Ni0yNTUuMTkxNTc5IDk5LjM0NTk2NUMxOTguMjQyODA3IDkwNS41MjE0MDQgMzQ4Ljk2ODQyMSA5ODguMDcwMTc1IDUxMiA5ODguMDcwMTc1IiBmaWxsPSIjRDNERkVGIiBwLWlkPSIxMTQ4Ij48L3BhdGg+PHBhdGggZD0iTTY2MC42NTk2NDkgNjc5LjQzMjk4MmwyLjMzNTQzOS01LjI5OTY0OWMtMjEuMjg4NDIxLTIxLjY0NzcxOS0zNS45Mjk4MjUtNjQuMjI0NTYxLTMwLjI3MDg3Ny0xMjUuNzU0Mzg2LTU2LjIzMDE3NSA5MC45MDI0NTYtMTY1LjYzNjQ5MSA5NS40ODM1MDktMjIyLjA0NjMxNiAxLjA3Nzg5NSA0LjU4MTA1MyAyNy44NDU2MTQtNC4zMTE1NzkgNjguNTM2MTQtMzkuMzQzMTU4IDEyNS43NTQzODYgMi45NjQyMTEgNTAuNjYxMDUzIDE3NS4yNDc3MTkgMjA4LjQ4MjgwNyAyODkuMzI0OTEyIDQuMjIxNzU0eiIgZmlsbD0iI0VERTdFNiIgcC1pZD0iMTE0OSI+PC9wYXRoPjxwYXRoIGQ9Ik00MDkuMTUwODc3IDU1OS4xNTc4OTVjNC40OTEyMjggMTcuOTY0OTEyIDEzMy45Mjg0MjEgMTQ0Ljg4NzAxOCAyNTEuNTA4NzcyIDExNC44ODU2MTQtMzQuNjcyMjgxLTQyLjEyNzcxOS0yNy4zOTY0OTEtMTEzLjM1ODU5Ni0yNC40MzIyODEtMTUyLjcwMTc1NSAxMi4wMzY0OTEtMjguNTY0MjExLTIyNi45ODY2NjcgOC45ODI0NTYtMjMwLjA0MDcwMSAzMy4zMjQ5MTN6IiBmaWxsPSIjRTNEQ0RCIiBwLWlkPSIxMTUwIj48L3BhdGg+PHBhdGggZD0iTTY0Ni43MzY4NDIgNTEyYzcxLjA1MTIyOC0xOS4yMjI0NTYgODMuOTg1OTY1LTE2Mi42NzIyODEgMTcuOTY0OTEyLTEwMi4xMzA1MjZBMzE3LjE3MDUyNiAzMTcuMTcwNTI2IDAgMCAxIDY0Ni43MzY4NDIgNTEyeiIgZmlsbD0iI0VGRTdFNiIgcC1pZD0iMTE1MSI+PC9wYXRoPjxwYXRoIGQ9Ik02MzAuOTI3NzE5IDMxNi4yNzIyODFjLTEzMC40MjUyNjMgMjguNjU0MDM1LTE4NC4xNDAzNTEtMzAuMTgxMDUzLTIxMC4xODk0NzMtNDYuNzk4NTk3LTQyLjkzNjE0IDMzLjIzNTA4OC0zMi4yNDcwMTggMTEwLjEyNDkxMi00NS45OTAxNzYgMTI1LjIxNTQzOS0xLjUyNzAxOC0zLjA1NDAzNS00LjU4MTA1My02LjAxODI0Ni02LjEwODA3LTguOTgyNDU2LTEwLjc3ODk0NyAzMTkuOTU1MDg4IDI5OS4yMDU2MTQgMjk4Ljg0NjMxNiAyOTYuNDIxMDUzIDE2LjYxNzU0NC0yNy41NzYxNC00My43NDQ1NjEtMjIuOTk1MDg4LTQ0LjkxMjI4MS0zMy43NzQwMzUtODYuMDUxOTMiIGZpbGw9IiNGN0VERUIiIHAtaWQ9IjExNTIiPjwvcGF0aD48cGF0aCBkPSJNNjI3Ljk2MzUwOSAzMTcuNDRjMTAuNTk5Mjk4IDQyLjc1NjQ5MSA2LjI4NzcxOSA0MC42OTA1MjYgMzUuMTIxNDAzIDg2LjU5MDg3NyAyMi44MTU0MzkgNy42MzUwODggODUuMDYzODYtMjA5LjM4MTA1My0xMDAuMjQ0MjEtMjM4LjM5NDM4Ni0yMTUuNTc4OTQ3LTMzLjU5NDM4Ni0yNDIuNTI2MzE2IDE2MS42ODQyMTEtMTg3LjkxMjk4MyAyMzAuMTMwNTI3IDEzLjY1MzMzMy0xMy43NDMxNTggNC4yMjE3NTQtOTEuMjYxNzU0IDQ0LjkxMjI4MS0xMjQuODU2MTQxIDI2LjEzODk0NyAxOC4zMjQyMTEgNzkuMDQ1NjE0IDc3LjE1OTI5OCAyMDguMTIzNTA5IDQ2LjUyOTEyM3oiIGZpbGw9IiNBNkIxQkYiIHAtaWQ9IjExNTMiPjwvcGF0aD48L3N2Zz4=',
            submitting: false,
            value: '',
            queryData: null
        };
    }

    buildQueryHistoryList = (data) => {
        let histories = [];
        if (data == null || data.length === 0) return histories;

        for (let i = 0; i < data.length; i++) {
            const item = {};
            item["author"] = data[i].user;
            item["avatar"] = this.state.default_avatar;
            item["content"] = <p>{data[i].content}</p>;
            item["datetime"] = new Date(data[i].queryTime).Format("yyyy-MM-dd hh:mm:ss");

            histories.push(item);
        }
        return histories;
    };

    handleSubmit = () => {
        if (!this.state.value) {
            return;
        }

        this.setState({
            submitting: true,
        });
        const params = {};
        params["graph"] = this.props.graph;
        params["user"] = this.state.currentUserInf.account.toLowerCase();
        params["content"] = this.state.value;
        params["days"] = 90;

        console.log("== start to submit query to janusgraph ==");

        MetaService.checkPermission(params).then(result => {
            if (result != null && result.length > 0) {

                InquireGraphService.customizedQuery(params).then(result => {

                    if (result.totalCount > 0) {

                        this.setState({ queryData: result });

                        notification.success({
                            message: '查询成功',
                        });

                        InquireGraphService.addQuery(params).then(result => {
                            this.setState({
                                submitting: false,
                            });

                            if (result.length > 0) {
                                this.setState({
                                    historyQueries: this.buildQueryHistoryList(result)
                                });
                            } else {
                                notification.error({
                                    message: '添加查询记录失败',
                                    description: '请稍后再试'
                                });
                            }

                        });

                    } else {
                        this.setState({ submitting: false });

                        notification.error({
                            message: '查询语句错误或者数据为空',
                            description: '请检查查询语句'
                        });
                    }
                });
            } else {
                this.setState({
                    submitting: false,
                });

                notification.error({
                    message: "权限错误",
                    description: '请到datamap上去申请访问图谱的权限'
                });
            }
        });

    };

    handleChange = (e) => {
        this.setState({
            value: e.target.value,
        });
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

    componentDidMount = () => {
        const params = {};
        params["user"] = this.state.currentUserInf.account.toLowerCase();
        params["graph"] = this.props.graph;
        params["days"] = 7;
        InquireGraphService.loadHistoryQueries(params).then(result => {
            if (result) {
                this.setState({
                    historyQueries: this.buildQueryHistoryList(result)
                });
            } else {
                notification.error({
                    message: '初始化查询列表失败',
                    description: '请稍后再试'
                });
            }
            this.setState({
                submitting: false,
            });
        });

    };

    render() {
        const {historyQueries, submitting, value} = this.state;

        return (
            <div>
                {historyQueries.length > 0 && <HistoryQueryList historyQueries={historyQueries}/>}
                <Comment
                    avatar={(
                        <Avatar
                            src={this.state.default_avatar}
                            alt={this.state.currentUserInf.name}
                        />
                    )}
                    content={(
                        <Editor
                            onChange={this.handleChange}
                            onSubmit={this.handleSubmit}
                            submitting={submitting}
                            value={value}
                        />
                    )}
                />
                {this.state.submitting && !this.state.queryData ? (
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
                {this.state.queryData && this.state.queryData.isTable ? (
                    <Table
                        title={() => '查询结果：'}
                        columns={this.columnsList()}
                        loading={this.state.submitting}
                        bordered
                        pagination={{
                            pageSize: 100,
                            current: 1,
                            total: this.state.queryData.totalCount,
                            showTotal: total => `共${this.state.queryData.totalCount}条`
                        }}
                        dataSource={this.state.queryData.recordBean.valueList}
                        rowKey={record => JSON.stringify(record)}
                        style={{ marginTop: '10px', overflow: 'auto' }}
                        //scroll={{ y: 600, x: 1600 }}
                    />
                ) : (this.state.queryData && this.state.queryData.isTable  === false ? (
                    <List
                        header={<div>查询结果：</div>}
                        bordered
                        dataSource={this.state.queryData.recordBean.valueList}
                        renderItem={item => <List.Item>{item}</List.Item>}
                    />
                ) : null) }
            </div>
        );

    }
}
