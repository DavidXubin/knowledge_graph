import React from 'react';
import Store from "@/store";
import {
    notification
} from 'antd';
import './index.less';
import OceanBreadcrumb from "@/component/OceanBreadcrumb";
import MetaService from "@/service/permission";

@OceanBreadcrumb([{path: '/', text: '关系图谱'}, {path: '/datamodel', text: '图谱菜单'}])
@Store.storeConsumer()
class DataModel extends React.PureComponent {
    constructor(props) {
        super(props);
        this.state = {currentUserInf: this.props.store.profile};
    }

    GotoGraph = graphName => {
        let params = {
            user: this.state.currentUserInf.account.toLowerCase(),
            graph: graphName
        };

        MetaService.checkPermission(params).then(result => {
            if (result != null && result.length > 0) {
                console.log(window.location.href);
                window.location.href = window.location.origin + "/graph/" + graphName + "?user=" + params.user;
            } else {
                notification.error({
                    message: result,
                    description: '请到datamap上去申请访问图谱的权限'
                });
            }
        })
    };

    render() {
        return (
            <div className="boss">
                <h1 style={{marginTop: 18.7}}>贝壳金服关系图谱:</h1>
                <div className="model">
                    <svg width="800" height="600" xmlns="http://www.w3.org/2000/svg">
                        <g>
                            <title>background</title>
                            <rect x="-1" y="-1" width="802" height="602" id="canvas_background" fill="#fff"/>
                            <g id="canvasGrid" display="none">
                                <rect id="svg_11" width="100%" height="100%" x="0" y="0" strokeWidth="0"
                                      fill="url(#gridpattern)"/>
                            </g>
                        </g>
                        <g>
                            <title>Layer 1</title>
                            <rect stroke="#fff" fill="#1890ff" strokeWidth="1.5" x="83.297934" y="87.488937"
                                  width="144.035101" height="63.508773" id="svg_9"/>
                            <rect stroke="#fff" fill="#1890ff" strokeWidth="1.5" x="611.807226" y="87.488938"
                                  width="144.035101" height="63.508773" id="svg_28"/>
                            <rect stroke="#fff" fill="#1890ff" strokeWidth="1.5" x="342.947303" y="453.717369"
                                  width="144.035101" height="63.508773" id="svg_29"/>
                            <rect stroke="#fff" fill="#1890ff" strokeWidth="1.5" x="342.947321" y="273.804898"
                                  width="144.035101" height="63.508773" id="svg_30"/>
                            <rect stroke="#fff" fill="#1890ff" strokeWidth="1.5" x="83.297939" y="453.717371"
                                  width="144.035101" height="63.508773" id="svg_31"/>
                            <rect stroke="#fff" fill="#1890ff" strokeWidth="1.5" x="83.297932" y="273.804903"
                                  width="144.035101" height="63.508773" id="svg_32"/>
                            <rect stroke="#fff" fill="#1890ff" strokeWidth="1.5" x="342.947306" y="87.488936"
                                  width="144.035101" height="63.508773" id="svg_33"/>
                            <rect stroke="#fff" fill="#1890ff" strokeWidth="1.5" x="611.80725" y="273.804895"
                                  width="144.035101" height="63.508773" id="svg_34"/>
                            <rect stroke="#fff" fill="#1890ff" strokeWidth="1.5" x="611.807227" y="453.717356"
                                  width="144.035101" height="63.508773" id="svg_35"/>

                            <a onClick={() => this.GotoGraph("tenant")}>
                                <text textAnchor="start" fontFamily="Helvetica, Arial, sans-serif" fontSize="18"
                                      id="svg_51"
                                      y="127.586345" x="100.343737" fillOpacity="null" strokeOpacity="null"
                                      strokeWidth="0"
                                      stroke="#000" fill="#fff">商户关系图谱
                                </text>

                            </a>
                            <a onClick={() => this.GotoGraph("risk_radar")}>
                                <text textAnchor="start" fontFamily="Helvetica, Arial, sans-serif" fontSize="18"
                                      id="svg_52"
                                      y="127.586345" x="358.981838" fillOpacity="null" strokeOpacity="null"
                                      strokeWidth="0"
                                      stroke="#000" fill="#fff">风险雷达图谱
                                </text>
                            </a>
                            <a onClick={() => this.GotoGraph("loan_agent")}>
                                <text textAnchor="start" fontFamily="Helvetica, Arial, sans-serif" fontSize="18"
                                      id="svg_53"
                                      y="127.586345" x="622.843159" fillOpacity="null" strokeOpacity="null"
                                      strokeWidth="0"
                                      stroke="#000" fill="#fff">贝用金关系图谱
                                </text>
                            </a>
                            <a onClick={() => this.GotoGraph("dcc")}>
                                <text textAnchor="start" fontFamily="Helvetica, Arial, sans-serif" fontSize="18"
                                      id="svg_54"
                                      y="313.907429" x="100.341811" fillOpacity="null" strokeOpacity="null"
                                      strokeWidth="0"
                                      stroke="#000" fill="#fff">DCC调用图谱
                                </text>
                            </a>
                            <a onClick={() => this.GotoGraph("da")}>
                                <text textAnchor="start" fontFamily="Helvetica, Arial, sans-serif" fontSize="18"
                                      id="svg_55"
                                      y="313.898654" x="363.986792" fillOpacity="null" strokeOpacity="null"
                                      strokeWidth="0"
                                      stroke="#000" fill="#fff">DA关系图谱
                                </text>
                            </a>
                            <a onClick={() => this.GotoGraph("real_estate_company")}>
                                <text textAnchor="start" fontFamily="Helvetica, Arial, sans-serif" fontSize="18"
                                      id="svg_56"
                                      y="313.909623" x="622.853543" fillOpacity="null" strokeOpacity="null"
                                      strokeWidth="0"
                                      stroke="#000" fill="#fff">开发商关系图谱
                                </text>
                            </a>
                            <a onClick={() => this.GotoGraph("lineage")}>
                                <text textAnchor="start" fontFamily="Helvetica, Arial, sans-serif" fontSize="18"
                                      id="svg_57"
                                      y="493.814056" x="100.341811" fillOpacity="null" strokeOpacity="null"
                                      strokeWidth="0"
                                      stroke="#000" fill="#fff">血缘关系图谱
                                </text>
                            </a>
                            <a onClick={() => this.GotoGraph("unknown2")}>
                                <text textAnchor="start" fontFamily="Helvetica, Arial, sans-serif" fontSize="18"
                                      id="svg_58"
                                      y="493.809668" x="391.995269" fillOpacity="null" strokeOpacity="null"
                                      strokeWidth="0"
                                      stroke="#000" fill="#fff">待定2
                                </text>
                            </a>
                            <a onClick={() => this.GotoGraph("unknown3")}>
                                <text textAnchor="start" fontFamily="Helvetica, Arial, sans-serif" fontSize="18"
                                      id="svg_59"
                                      y="493.809906" x="664.845889" fillOpacity="null" strokeOpacity="null"
                                      strokeWidth="0"
                                      stroke="#000" fill="#fff">待定3
                                </text>
                            </a>
                        </g>
                    </svg>
                </div>
            </div>
        )
    }
}

export default DataModel;
