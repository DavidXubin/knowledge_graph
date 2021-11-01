function LoanAgentGraph() {
};

LoanAgentGraph.prototype = new GraphMetaData();

LoanAgentGraph.prototype.populate_graph_nodes = function() {
    var graph_nodes = new GraphDictionary();

    graph_nodes.add("借款用户", [new Property("身份证号", "composite"),
        new Property("uus_id", "composite"), new Property("tag"),
        new Property("员工号"), new Property("姓名"), new Property("性别"),
        new Property("年龄", "mixed", "number"), new Property("星座"),
        new Property("教育程度"), new Property("婚姻状态"), new Property("城市"),
        new Property("角色"), new Property("入职日期"), new Property("离职日期"),
        new Property("当前在职状态"), new Property("当前在职天数", "mixed", "number"),
        new Property("当前职级"), new Property("门店所在城市"),
        new Property("累计贷款笔数", "mixed", "number"),
        new Property("未结清贷款笔数", "mixed", "number"),
        new Property("累计贷款金额", "mixed", "number"),
        new Property("当前贷款余额", "mixed", "number"),
        new Property("当前逾期天数", "mixed", "number"),
        new Property("历史最大逾期天数", "mixed", "number")
    ]);

    graph_nodes.add("联系人", [new Property("uid", "composite"),
        new Property("联系方式", "composite"), new Property("ext_id", "composite"),
        new Property("姓名"), new Property("关系"),
        new Property("tag")
    ]);

    graph_nodes.add("手机", [new Property("uus_id", "composite"),
        new Property("手机号","composite"), new Property("tag")
    ]);

    graph_nodes.add("地址", [new Property("uid", "composite"),
        new Property("地址"), new Property("省份"), new Property("城市"), new Property("区"),
        new Property("tag")
    ]);

    graph_nodes.add("团队", [new Property("编号", "composite"),
        new Property("名称"), new Property("tag"),
        new Property("放款总人数", "mixed", "number"),
        new Property("放款总金额", "mixed", "number"),
        new Property("当前总贷款余额", "mixed", "number"),
        new Property("总坏客户人数", "mixed", "number")
    ]);

    graph_nodes.add("门店", [new Property("编号", "composite"),
        new Property("名称"), new Property("tag"), 
        new Property("地址"), new Property("城市"),
        new Property("放款总人数", "mixed", "number"),
        new Property("放款总金额", "mixed", "number"),
        new Property("当前总贷款余额", "mixed", "number"),
        new Property("总坏客户人数", "mixed", "number")
    ]);

    graph_nodes.add("区域", [new Property("编号", "composite"),  new Property("名称"),
        new Property("放款总人数", "mixed", "number"),
        new Property("放款总金额", "mixed", "number"),
        new Property("当前总贷款余额", "mixed", "number"),
        new Property("总坏客户人数", "mixed", "number"),
        new Property("tag")
    ]);

    graph_nodes.add("市场", [new Property("编号", "composite"),  new Property("名称"),
        new Property("放款总人数", "mixed", "number"),
        new Property("放款总金额", "mixed", "number"),
        new Property("当前总贷款余额", "mixed", "number"),
        new Property("总坏客户人数", "mixed", "number"),
        new Property("tag")
    ]);

    graph_nodes.add("大区", [new Property("编号", "composite"),  new Property("名称"),
        new Property("放款总人数", "mixed", "number"),
        new Property("放款总金额", "mixed", "number"),
        new Property("当前总贷款余额", "mixed", "number"),
        new Property("总坏客户人数", "mixed", "number"),
        new Property("tag")
    ]);

    graph_nodes.add("ip地址", [new Property("ip", "composite"),
        new Property("udid", "composite"), new Property("union_id", "composite"),
        new Property("event_time"), new Property("tag")
    ]);

    graph_nodes.add("设备ip", [new Property("ip", "composite"),
        new Property("udid", "composite"), new Property("union_id", "composite"),
        new Property("event_time"), new Property("tag")
    ]);

    graph_nodes.add("设备", [new Property("udid", "composite"), new Property("union_id", "composite"),
        new Property("imei", "composite"), new Property("idfa", "composite"),
        new Property("meid", "composite"), new Property("event_time"), new Property("tag")
    ]);

    return graph_nodes;
};

LoanAgentGraph.prototype.populate_graph_relations = function() {
    //var edges = ['所属团队', '所属门店', '所属区域', '所属市场', '所属大区', '联系人', '借款人号码', '联系人号码', '居住'];

    var graph_relations = new GraphDictionary();

    graph_relations.add("借款用户", ['联系人', '借款人号码', '居住', '所属团队', 'ip地址', '使用设备']);
    graph_relations.add("联系人", ['联系人号码', '联系人']);
    graph_relations.add("手机", ['联系人号码', '借款人号码']);
    graph_relations.add("地址", ['居住']);
    graph_relations.add("团队", ['所属团队', '所属门店']);
    graph_relations.add("门店", ['所属门店', '所属区域']);
    graph_relations.add("区域", ['名称']);
    graph_relations.add("市场", ['所属市场', '所属大区']);
    graph_relations.add("大区", ['所属大区']);
    graph_relations.add("设备ip", ['ip地址']);
    graph_relations.add("设备", ['使用设备']);

    return graph_relations;
};

LoanAgentGraph.prototype.populate_graph_edges = function() {
    let graph_edges = new GraphDictionary();

    graph_edges.add("所属团队", [
        new Property("放款时间"),
        new Property("放款状态")
    ]);

    graph_edges.add("所属门店", [
        new Property("放款时间"),
        new Property("放款状态")
    ]);

    graph_edges.add("所属区域", [
        new Property("放款时间"),
        new Property("放款状态")
    ]);

    graph_edges.add("所属市场", [
        new Property("放款时间"),
        new Property("放款状态")
    ]);

    graph_edges.add("所属大区", [
        new Property("放款时间"),
        new Property("放款状态")
    ]);

    graph_edges.add("ip地址", [
        new Property("event_time")
    ]);

    graph_edges.add("使用设备", [
        new Property("event_time")
    ]);

    return graph_edges;
};

LoanAgentGraph.prototype.populate_graph_display_properties = function() {
    let display_properties = new GraphDictionary();

    display_properties.add("借款用户", ['姓名']);
    display_properties.add("联系人", ['姓名', '关系']);
    display_properties.add("手机", ['手机号']);
    display_properties.add("地址", ['城市', '地址']);
    display_properties.add("团队", ['名称']);
    display_properties.add("门店", ['名称', '城市', '地址']);
    display_properties.add("区域", ['名称']);
    display_properties.add("市场", ['名称']);
    display_properties.add("大区", ['名称']);
    display_properties.add("设备ip", ['udid', 'ip']);
    display_properties.add("设备", ['udid']);

    return display_properties;
};

