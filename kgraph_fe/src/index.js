import React from 'react'
import ReactDom from 'react-dom'
import Store from './store'
import { Switch, BrowserRouter, Route, Redirect } from 'react-router-dom'
import { LocaleProvider, Spin } from 'antd'
import zhCN from 'antd/lib/locale-provider/zh_CN'
import styled from 'styled-components'
import { fetch } from 'ocean-utils'
import Guard from './container/Guard'
import './style/global.less'
import DataModel from './container/home/DataModel/index'
import InquireGraph from "@/container/InquireGraph";
//import TestApp from './container/test/index'


const LoadingContainer = styled.div`
  width: 100%;
  display: flex;
  margin-top: 100px;
  .ant-spin-spinning {
    width: 100%;
  }
`

fetch.setResponseInterceptor(
    res => {
        return res.data
    },
    error => {
        return error
    }
)

@Store.storeProvider()
class Root extends React.PureComponent {
    constructor(props) {
        super(props)

        this.state = {
            preDataReady: false
        }
    }

    componentDidMount() {
        this.preload()
    }

    componentDidCatch(error) {
        console.error(error)
    }

    preload = () => {
        this.setState({
            preDataReady: true
        })
    }

    render() {
        if (!this.state.preDataReady) {
            return (
                <LoadingContainer>
                    <Spin size="large" />
                </LoadingContainer>
            )
        }

        return (
            <BrowserRouter>
                <Switch>
                    <Route path="/">
                        <Guard>
                            <Switch>
                                <Redirect from="/" to="/datamodel" exact />
                                <Route path="/datamodel" component={DataModel} exact />
                                <Route path="/inquireGraph" component={InquireGraph} exact />
                            </Switch>
                        </Guard>
                    </Route>
                </Switch>
            </BrowserRouter>
        )
    }
}


ReactDom.render(
    <LocaleProvider locale={zhCN}>
        <Root />
    </LocaleProvider>,
    document.getElementById('app')
)

//only for test
//ReactDom.render(<TestApp name="Jane" />, document.getElementById('app'));
