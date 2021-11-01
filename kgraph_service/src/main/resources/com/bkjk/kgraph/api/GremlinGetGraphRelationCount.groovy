package com.bkjk.kgraph.api

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import net.sf.json.JSONObject

import java.util.stream.Collectors
import com.bkjk.kgraph.common.ReturnCode
import com.bkjk.kgraph.common.ServiceException
import com.bkjk.kgraph.service.GremlinDriver
import com.bkjk.kgraph.service.PluginService
import org.springframework.beans.factory.annotation.Autowired
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource


class GremlinGetGraphRelationCount implements PluginService{
    static final Logger logger = LoggerFactory.getLogger(GremlinGetGraphRelationCount.class)

    @Autowired
    GremlinDriver pool

    //Now janusgraph has slow performance on getting label count for large graph,
    //so we temporally fix the count until final solution is found
    private Map real_estate_company = [
        "entity": [
                    '联系人': 95174,
                    '产品': 2,
                    'uus用户': 159531,
                    '法人': 97411,
                    '理房通商户': 125541,
                    '开发商': 90492,
                    '商户': 86326,
                    '授权人': 22030,
                    '房产项目': 9699
                    ],
        "edge": [
                '担任法人': 81669,
                '4级出资': 22964,
                '5级出资': 14154,
                '3级出资': 27333,
                '2级出资': 10682,
                '7级出资': 2550,
                '6级出资': 6310,
                '8级出资': 1539,
                '9级出资': 559,
                '支付渠道': 28381,
                '访问uus': 12048,
                '提供金融产品': 81546,
                '台账决议': 128,
                '联系': 81669,
                '授权': 22116,
                '订购': 260039,
                '开发': 6615,
                '10级出资': 155,
                '11级出资': 68
        ]
    ]

    private Map risk_radar = [
        "entity": [
                '房屋': 343038,
                '交易人配偶': 323888,
                '手机号': 647867,
                '交易人': 895796,
                '参与人': 87200,
                '门店': 26023,
                '金融单': 378673
                ],
        "edge": [
                '反担保人': 1381,
                '签约专员': 53699,
                '复审专员': 3659,
                '赎楼解押专员': 135056,
                '交易顾问': 195548,
                '金融解押专员': 139,
                '金融顾问': 376161,
                '城市运营': 46102,
                '风控高级审批': 51,
                '配偶': 307184,
                '赎楼还款专员': 150275,
                '所属门店': 85248,
                '代理买入': 4519,
                '联系号码': 1023093,
                '内勤专员': 261058,
                '卖出': 367292,
                '渠道拓展专员': 630,
                '集中作业人员': 86021,
                '金融抵押专员': 422,
                '合同审核专员': 83739,
                '经纪人': 263013,
                '借款': 348646,
                '标的物': 343004,
                '买入': 360794,
                '初审专员': 259567,
                '赎楼内勤': 148326,
                '风控外勤专员（成都）': 7,
                '代理卖出': 7059,
                '风控内勤专员(大连)': 1
        ]
    ];

    private Map loan_agent = [
        entity: [
            '联系人': 32593,
            '借款用户': 87137,
            '手机': 92078,
            '市场': 848,
            '设备ip': 249535,
            '团队': 18140,
            '大区': 454,
            '设备': 135565,
            '地址': 15195,
            '门店': 16812,
            '区域': 2720
        ],
        "edge": [
            '居住': 15195,
            '联系人': 32592,
            '所属团队': 65383,
            '所属市场': 2399,
            '借款人号码': 60238,
            '所属门店': 17304,
            'ip地址': 37418,
            '所属区域': 18042,
            '所属大区': 841,
            '使用设备': 37418,
            '联系人号码': 31840
        ]
    ]

    private Map columnFamily = [
            "vertex_pairs": ["start", "end"],
            "shortest_path": ["path"]
    ]


    List run(JSONObject params) {
        String user
        String graph
        GraphTraversalSource g

        try {
            user = params.getString("user").trim()
            graph = params.getString("graph").trim()

            def label = null
            if (params.containsKey("label")) {
                label = params.getString("label").trim()
            }

            def relationType = null
            if (params.containsKey("relationType")) {
                relationType = params.getString("relationType").trim()

                if (relationType != "entity" && relationType != "edge") {
                    throw new RuntimeException("Param error: relationType is either entity or edge")
                }
            } else {
                throw new RuntimeException("Param error: miss relationType")
            }

            g = pool.get(user, graph + "_g")

            if (graph == "real_estate_company") {
                if (label) {
                    def result = [:]
                    result[label] = real_estate_company[relationType][label]
                    return [result]
                } else {
                    return real_estate_company[relationType].collect()
                }
            }  else if (graph == "risk_radar") {
                if (label) {
                    def result = [:]
                    result[label] = risk_radar[relationType][label]
                    return [result]
                } else {
                    return risk_radar[relationType].collect()
                }
            } else if (graph == "loan_agent") {
                if (label) {
                    def result = [:]
                    result[label] = loan_agent[relationType][label]
                    return [result]
                } else {
                    return loan_agent[relationType].collect()
                }
            } else {
                def data = null
                if (relationType == "entity") {
                    data = g.V().label().groupCount().toList()
                } else {
                    data = g.E().label().groupCount().toList()
                }

                if (data == null) {
                    return []
                }

                if (label) {
                    return data[0].entrySet().stream().filter({t -> t.getKey() == label}).
                            collect(Collectors.toList())
                } else {
                    return data
                }
            }

        } catch (Exception e) {
            logger.error("Fail to get cluster graph names: " + e)
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR.code, GremlinGetGraphRelationCount.class.toString(), e)
        } finally {
            if (pool) {
                pool.release(user, graph + "_g", g)
            }
        }
    }
}
