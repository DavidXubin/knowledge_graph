function RiskRadarGraph() {
};

RiskRadarGraph.prototype = new GraphMetaData();

RiskRadarGraph.prototype.populate_graph_nodes = function() {
    var graph_nodes = new GraphDictionary();

    graph_nodes.add("金融单", [new Property("订单编号", "composite"),
        new Property("交易编号", "composite"), new Property("tag"),
        new Property("产品名称"), new Property("城市"), new Property("是否在途"),
        new Property("用资金额", "mixed", "number"), new Property("订单时间"),
        new Property("订单状态"), new Property("订单编号"),
        new Property("风险原因"), new Property("风险状态")]);

    graph_nodes.add("交易人", [new Property("证件号", "composite"),
        new Property("tag"), new Property("姓名"), new Property("性别"),
        new Property("年龄", "mixed", "number"), new Property("手机号"),
        new Property("角色形态"), new Property("婚姻状态"), new Property("通讯地址")]);

    graph_nodes.add("交易人配偶", [new Property("证件号", "composite"),
        new Property("交易者证件号","composite"), new Property("tag"),
        new Property("交易者姓名"), new Property("性别"), new Property("姓名"), new Property("手机号")]);

    graph_nodes.add("参与人", [new Property("系统号", "composite"),
        new Property("部门id", "composite"), new Property("大区id", "composite"),
        new Property("门店编码", "composite"), new Property("tag"), new Property("姓名"),
        new Property("部门"), new Property("大区"), new Property("大部"),
        new Property("手机号"), new Property("性别"), new Property("婚姻状态"),
        new Property("职位名称"), new Property("职位等级"), new Property("入职日期"),
        new Property("离职日期"), new Property("品牌"), new Property("学历"), new Property("门店名称")]);

    graph_nodes.add("房屋", [new Property("房屋地址", "composite"),
        new Property("金融单号", "composite"), new Property("交易单号", "composite"),
        new Property("省份编码", "composite"), new Property("城市编码", "composite"),
        new Property("区县编码", "composite"),
        new Property("房屋id", "composite", "number"),
        new Property("商圈id", "composite", "number"),
        new Property("楼盘id", "composite", "number"),
        new Property("成交店面id", "composite"),
        new Property("tag"), new Property("楼盘名称"), new Property("建筑面积"),
        new Property("抵押情况"), new Property("评估价", "mixed", "number"),
        new Property("房屋成交价", "mixed", "number"), new Property("商圈名称"),
        new Property("楼盘名称2"), new Property("成交店面"), new Property("套内面积")]);

    graph_nodes.add("门店", [new Property("门店编码", "composite"),
        new Property("tag"), new Property("门店名称"), new Property("城市")]);

    graph_nodes.add("手机号", [new Property("手机号码",  "composite"), new Property("tag")]);

    return graph_nodes;
};

RiskRadarGraph.prototype.populate_graph_relations = function() {

    let graph_relations = new GraphDictionary();

    graph_relations.add("金融单", ['卖出', '买入', '金融顾问', '标的物', '经纪人', '初审专员', '内勤专员', '借款', '合同审核专员', '交易顾问', '赎楼还款专员',
        '赎楼内勤', '赎楼解押专员', '签约专员', '集中作业人员', '代理卖出', '代理买入', '城市运营', '复审专员',
        '反担保人', '金融抵押专员', '渠道拓展专员', '金融解押专员', '风控外勤专员（成都）', '风控高级审批', '风控内勤专员(大连)']);
    graph_relations.add("交易人", ['卖出', '买入', '代理卖出', '代理买入', '借款', '反担保人', '配偶', '联系号码']);
    graph_relations.add("交易人配偶", ['配偶', '联系号码']);
    graph_relations.add("参与人", ['所属门店', '联系号码', '金融顾问', '经纪人', '初审专员', '内勤专员', '合同审核专员', '交易顾问', '赎楼还款专员',
        '赎楼内勤', '赎楼解押专员', '签约专员', '集中作业人员', '城市运营', '复审专员', '金融抵押专员', '渠道拓展专员', '金融解押专员',
        '风控外勤专员（成都）', '风控高级审批', '风控内勤专员(大连)']);
    graph_relations.add("房屋", ['标的物']);
    graph_relations.add("门店", ['所属门店']);
    graph_relations.add("手机号", ['联系号码']);

    return graph_relations;
};

RiskRadarGraph.prototype.populate_graph_display_properties = function() {
    let display_properties = new GraphDictionary();

    display_properties.add("金融单", ["产品名称"]);
    display_properties.add("交易人", ["姓名"]);
    display_properties.add("交易人配偶", ["姓名"]);
    display_properties.add("参与人", ["姓名"]);
    display_properties.add("房屋", ["房屋地址"]);
    display_properties.add("门店", ["门店名称"]);
    display_properties.add("手机号", ["手机号码"]);

    return display_properties;
};
