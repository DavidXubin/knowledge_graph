function TenantGraph() {
};

TenantGraph.prototype = new GraphMetaData();

TenantGraph.prototype.populate_graph_nodes = function() {
    var graph_nodes = new GraphDictionary();

    graph_nodes.add("商户", [new Property("商户id", "composite"),
        new Property("商户编号", "composite"), new Property("渠道商户号", "composite"),
        new Property("统一社会信用代码", "composite"), new Property("组织机构代码", "composite"),
        new Property("税务登记号", "composite"), new Property("银行联行号", "composite"),
        new Property("总行编号", "composite"), new Property("银行卡号", "composite"),
        new Property("登录账号", "composite"), new Property("开户许可证核准号", "composite"),
        new Property("开户省id", "composite"), new Property("开户市id", "composite"),
        new Property("tag"), new Property("注册邮箱"), new Property("商户渠道"),
        new Property("商户公司简称"), new Property("商户类型"), new Property("平台来源"),
        new Property("签约状态"), new Property("开户状态"), new Property("用户状态"),
        new Property("商户公司名称"), new Property("公司类型"), new Property("成立日期"),
        new Property("法定代表人"), new Property("营业期限结束时间"), new Property("营业期限开始时间"),
        new Property("注册资本"), new Property("发照日期"), new Property("登记机关"),
        new Property("是否三证合一"), new Property("公司地址"), new Property("经营范围"),
        new Property("企业邮箱"), new Property("公司网址"), new Property("公司运营状况"),
        new Property("公司负债状况"), new Property("公司收入状况"), new Property("合作协议附件"),
        new Property("授权协议附件"), new Property("垫佣贝企业电子签章授权书"), new Property("开户银行"),
        new Property("开户支行"), new Property("开户省名称"), new Property("开户市名称"),
        new Property("创建日期")
    ]);

    graph_nodes.add("uus用户", [new Property("uus_id", "composite"),
        new Property("登录账号", "composite"), new Property("tag"),
        new Property("账号类型"), new Property("用户状态"), new Property("用户来源")
    ]);

    graph_nodes.add("授权人", [new Property("授权人身份证号码", "composite"),
        new Property("授权人手机", "composite"),
        new Property("tag"), new Property("授权人姓名")
    ]);

    graph_nodes.add("法人", [new Property("法人证件号码", "composite"),
        new Property("法人手机", "composite"), new Property("法人银行账号", "composite"),
        new Property("法人账户名称", "composite"), new Property("法人银行联行号", "composite"),
        new Property("法人总行编号", "composite"), new Property("法人开户省id", "composite"),
        new Property("法人开户市id", "composite"), new Property("法人银行预留手机号", "composite"),
        new Property("tag"), new Property("法人证件类型"), new Property("法人姓名"),
        new Property("法人证件有效开始日期"), new Property("法人证件有效结束日期"), new Property("法人开户银行名称"),
        new Property("法人开户支行名称"), new Property("法人开户省名称"), new Property("法人开户市名称"),
        new Property("法人支行信息输入标识")
    ]);

    graph_nodes.add("联系人", [new Property("联系人手机", "composite"),
        new Property("tag"), new Property("联系人邮箱"), new Property("联系人名称")
    ]);

    graph_nodes.add("理房通商户", [new Property("理房通商户号", "composite"),
        new Property("理房通ehp账号", "composite"), new Property("用户id", "composite"),
        new Property("商户号", "composite"), new Property("合同号", "composite"),
        new Property("城市id", "composite"), new Property("账户名", "composite"),
        new Property("银行账号", "composite"), new Property("商编", "composite"),
        new Property("tag"), new Property("服务产品"), new Property("账户类型"),
        new Property("状态"), new Property("备注"), new Property("账户邮箱"),
        new Property("关联商户名称"), new Property("账户用途"), new Property("允许提现"),
        new Property("自动提现"), new Property("开户行支行名称"), new Property("手续费结算方式"),
        new Property("支付方式"), new Property("城市名字"), new Property("分账账户类型"),
        new Property("服务产品-汇总"), new Property("开户日期"), new Property("平台方标签"),
        new Property("二级归属"), new Property("三级归属"), new Property("城市名字2")
    ]);

    graph_nodes.add("产品", [new Property("金融产品ID", "composite"),
        new Property("城市id", "composite"), new Property("金融产品编码", "composite"),
        new Property("资金平台产品代码", "composite"), new Property("tag"),
        new Property("产品类别名称"), new Property("产品类别"), new Property("城市名称"),
        new Property("商户产品名称"), new Property("产品状态"),
    ]);

    graph_nodes.add("开发商", [new Property("项目公司统一社会信用代码", "composite"),
        new Property("tag"), new Property("项目公司名称")
    ]);

    graph_nodes.add("房产项目", [new Property("项目编号", "composite"),
        new Property("城市代号", "composite"), new Property("tag"),
        new Property("项目名称"), new Property("城市"), new Property("基本情况"),
        new Property("状态"), new Property("创建时间"), new Property("人工审核时间"),
        new Property("预计回款账期")
    ]);

    return graph_nodes;
};

TenantGraph.prototype.populate_graph_relations = function() {

    let graph_relations = new GraphDictionary();

    graph_relations.add("商户", ['授权', '提供金融产品', '支付渠道', '访问uus', '担任法人', '联系', '订购']);
    graph_relations.add("uus用户", ['访问uus']);
    graph_relations.add("授权人", ['授权']);
    graph_relations.add("法人", ['担任法人']);
    graph_relations.add("联系人", ['联系']);
    graph_relations.add("理房通商户", ['支付渠道']);
    graph_relations.add("产品", ['提供金融产品']);
    graph_relations.add("开发商", ['开发']);
    graph_relations.add("房产项目", ['开发', '订购']);

    return graph_relations;
};

TenantGraph.prototype.populate_graph_display_properties = function() {
    let display_properties = new GraphDictionary();

    display_properties.add("商户", ['商户公司简称']);
    display_properties.add("uus用户", ['uus_id']);
    display_properties.add("授权人", ['授权人姓名']);
    display_properties.add("法人", ['法人姓名']);
    display_properties.add("联系人", ['联系人名称']);
    display_properties.add("理房通商户", ['理房通商户号']);
    display_properties.add("产品", ['产品类别名称']);
    display_properties.add("开发商", ['项目公司名称']);
    display_properties.add("房产项目", ['项目名称']);

    return display_properties;
};