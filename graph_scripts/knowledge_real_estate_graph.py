import os
import json
import time
import datetime
import numpy as np
import pandas as pd
import base64
import requests
import hashlib
from run_pyspark import PySparkMgr

spark_args = {'spark.driver.memory': "5g", "spark.executor.memory": "5g"}
pysparkmgr = PySparkMgr(spark_args)
_, spark, sc = pysparkmgr.start('xubin.xu')


#graph_type = "tenant/"
graph_type = "real_estate/"


def make_md5(x):

    md5 = hashlib.md5()

    md5.update(x.encode('utf-8'))

    return md5.hexdigest()


def make_node_schema(entity_name, entity_df, comp_index_properties = None, mix_index_properties = None):

    properties = {"propertyKeys": []}

    for col in entity_df.columns:
        if entity_df[col].dtype == np.float:
            prop = {"name": col, "dataType": "Float", "cardinality": "SINGLE"}
        elif entity_df[col].dtype == np.integer:
            prop = {"name": col, "dataType": "Integer", "cardinality": "SINGLE"}
        else:
            prop = {"name": col, "dataType": "String", "cardinality": "SINGLE"}
        properties["propertyKeys"].append(prop)

    vertexLabels = {"vertexLabels": []}

    vertexLabels["vertexLabels"].append({"name" : entity_name})

    vertexIndexes = {"vertexIndexes": []}

    if comp_index_properties is not None:
        for prop in comp_index_properties:
            vertexIndexes["vertexIndexes"].append({
                "name" : entity_name + "_" + prop + "_comp",
                "propertyKeys" : [ prop ],
                "composite" : True,
                "unique" : False
            })

    if mix_index_properties is not None:
        for prop in mix_index_properties:
            vertexIndexes["vertexIndexes"].append({
                "name" : entity_name + "_" + prop + "_mixed",
                "propertyKeys" : [ prop ],
                "composite" : False,
                "unique" : False,
                "mixedIndex" : "search"
            })

    vertexIndexes["vertexIndexes"].append({
        "name" : entity_name + "_graph_label_mixed",
        "propertyKeys" : [ "graph_label" ],
        "composite" : False,
        "unique" : False,
        "mixedIndex" : "search"
    })

    return {**properties, **vertexLabels, **vertexIndexes}


def make_node_mapper(entity_name, entity_df):
    entity_file = "gra_" + entity_name + ".csv"

    vertexMap = {"vertexMap": {entity_file: {}}}

    vertexMap["vertexMap"][entity_file] =  {
        "[VertexLabel]" : entity_name
    }

    for col in entity_df.columns:
        vertexMap["vertexMap"][entity_file][col] = col

    return vertexMap


def make_vertex_centric_schema(edge_name, index_property, direction, order):
    if direction not in ["BOTH", "IN", "OUT"]:
        print("direction should be in {}".format(["BOTH", "IN", "OUT"]))
        return None

    if order not in ["incr", "decr"]:
        print("order should be in {}".format(["incr", "decr"]))
        return None

    vertexCentricIndexes = {"vertexCentricIndexes": []}

    vertexCentricIndexes["vertexIndexes"].append({
        "name" : edge_name + "_" + index_property,
        "edge" : edge_name,
        "propertyKeys" : [ index_property ],
        "order": order,
        "direction": direction
    })

    return vertexCentricIndexes


def make_edge_schema(relation_df = None, relation_comp_index_properties = None, relation_mix_index_properties = None):

    properties = {"propertyKeys": []}

    relation_columns = relation_df.columns.tolist()
    if "Left" not in relation_columns or "Right" not in relation_columns:
        print("relation df lacks Left and Right columns ")

    for col in relation_df.columns:
        if col in ["Left", "Right", "Type"]:
            continue

        if relation_df[col].dtype == np.float:
            prop = {"name": col, "dataType": "Float", "cardinality" : "SINGLE"}
        elif relation_df[col].dtype == np.integer:
            prop = {"name": col, "dataType": "Integer", "cardinality" : "SINGLE"}
        else:
            prop = {"name": col, "dataType": "String", "cardinality" : "SINGLE"}

        properties["propertyKeys"].append(prop)

    print(properties)


    relation_names = relation_df["Type"].value_counts().index.tolist()

    edgeLabels = {"edgeLabels": []}

    for relation in relation_names:
        edgeLabels["edgeLabels"].append({
            "name" : relation,
            "multiplicity" : "MULTI",
            "unidirected" : False
        })


    edgeIndexes = {"edgeIndexes": []}

    for relation_name in relation_names:
        if relation_comp_index_properties is not None:
            for prop in relation_comp_index_properties:
                edgeIndexes["edgeIndexes"].append({
                    "name" : relation_name + "_" + prop + "_comp",
                    "propertyKeys" : [ prop ],
                    "composite" : True,
                    "unique" : False,
                    "indexOnly": relation_name
                })

        if relation_mix_index_properties is not None:
            for prop in relation_mix_index_properties:
                edgeIndexes["edgeIndexes"].append({
                    "name" : relation_name + "_" + prop + "_mixed",
                    "propertyKeys" : [ prop ],
                    "composite" : False,
                    "unique" : False,
                    "mixedIndex" : "search",
                    "indexOnly": relation_name
                })


    return {**properties, **edgeLabels, **edgeIndexes}


def make_edge_mapper(entity_relations, relation_df=None, specific_relation=None):

    edgeMap = {"edgeMap": {}}

    for relation_name, entity_pairs in entity_relations.items():
        if specific_relation is not None and relation_name != specific_relation:
            continue

        for pair in entity_pairs:

            relation_file = "gra_" + relation_name + ".csv"

            edge = {"[edge_left]": {"Left": pair[0]},
                    "[EdgeLabel]": relation_name,
                    "[edge_right]": {"Right": pair[1]}}

            if relation_df is not None:
                relation_columns = relation_df.columns.tolist()
                if "Left" not in relation_columns or "Right" not in relation_columns:
                    print("relation df lacks Left and Right columns ")

                for col in relation_df.columns:
                    if col in ["Left", "Right", "Type"]:
                        continue

                    edge[col] = col

            edgeMap["edgeMap"][relation_file] = edge

    return edgeMap


def dump_schema(schema, datamapper, folder):
    if not os.path.exists(graph_type + folder):
        os.makedirs(graph_type + folder)

    f = open(graph_type + folder + "/schema.json", 'w')
    f.write(json.dumps(schema))
    f.close()

    f = open(graph_type + folder + "/datamapper.json", 'w')
    f.write(json.dumps(datamapper))
    f.close()


def get_longest_relation(df):
    path = 0

    for col in df.columns:
        if df[col].isnull().all():
            break

        if col.find("level_") >= 0 and col.find("_company") > 0:
            path += 1

    return path


def extract_company_names(df, longest_path):

    all_companies = list()

    for i in range(longest_path + 1):

        all_companies.extend(df["level_{}_company".format(i + 1)].unique().tolist())

    all_companies = [x for x in all_companies if not pd.isnull(x)]

    return list(set(all_companies))


def make_company_entity(all_companies, company_detail_df):

    company_df = pd.DataFrame({"name": all_companies})

    company_df = pd.merge(company_df, company_detail_df, how="left", on="name")

    company_df.drop_duplicates(company_df.columns, inplace=True)

    company_df.rename(columns={"name": "公司名", "type": "tag", "legal_representative": "法人代表",
                               "social_credit_code": "社会信用号"}, inplace=True)

    return company_df


def make_company_invest(data_df, longest_path):

    relation_df = pd.DataFrame([], columns=["Left", "Type", "Right", "percentage"])

    for i in range(longest_path + 1):

        tmp_df = pd.DataFrame([], columns=["Left", "Type", "Right", "percentage"])

        tmp_df["Left"] = data_df["level_{}_company".format(i + 1)]
        tmp_df["Right"] = data_df["level_{}_company".format(i + 2)]
        tmp_df["Type"] = "{}级出资".format(i + 2)
        tmp_df["percentage"] = data_df["level_{}_invest".format(i + 2)]

        relation_df = pd.concat([relation_df, tmp_df])

    return relation_df


#商户实体
sql = """select t.id as `商户id`
,register_email as `注册邮箱`
,tenant_no as `商户编号`
,platform_channel as `商户渠道`
,channel_merchant_num as `渠道商户号`
,t.company_short_name as `商户公司简称`
,tenant_type as `商户类型`
,platform_source as `平台来源`
,contract_status as `签约状态`
,account_status as `开户状态`
,t.status as `用户状态`
,company_name as `商户公司名称`
,social_credit_code as `统一社会信用代码`
,org_code as `组织机构代码`
,company_type as `公司类型`
,found_date as `成立日期`
,legal_representative as `法定代表人`
,life_span_end as `营业期限结束时间`
,life_span_start as `营业期限开始时间`
,registered_capital as `注册资本`
,issured_date as `发照日期`
,registered_org as `登记机关`
,three_to_one as `是否三证合一`
,address as `公司地址`
,tax_registra_num as `税务登记号`
,business_scope as `经营范围`
,company_email as `企业邮箱`
,website as `公司网址`
,company_operate_state as `公司运营状况`
,company_liabilities_state as `公司负债状况`
,company_income_state as `公司收入状况`
,attachments_cooperation as `合作协议附件`
,attachments_authorization as `授权协议附件`
,attachments_signature as `垫佣贝企业电子签章授权书`
,open_bank as `开户银行`
,open_bank_branch as `开户支行`
,bank_link_number as `银行联行号`
,bank_code as `总行编号`
,bank_card_number as `银行卡号`
,acct_name as `登录账号`
,open_account_permit_code as `开户许可证核准号`
,open_province_id as `开户省id`
,open_province_name as `开户省名称`
,open_city_id  as `开户市id`
,open_city_name as `开户市名称`
,s.created_on as `创建日期` 
from
cf_tenant.tn_tenant  t
left join cf_tenant.tn_product_use_relation s  on s.tenant_id = t.id
left join cf_tenant.tn_enterprise e on e.tenant_id = t.id
left join cf_tenant.tn_product_apply p on p.apply_type= 10 and p.status = 30 and p.soft_deleted='N' and s.tenant_id=p.tenant_id
left join cf_tenant.tn_product_apply_extend_info ae on ae.product_apply_id=p.id and p.tenant_id = ae.tenant_id and ae.soft_deleted='N'
where s.soft_deleted='N'
"""

tenant_df = spark.sql(sql).toPandas().drop_duplicates()

tenant_df["商户类型"] = tenant_df["商户类型"].map({"enterprise":"企业", "individuals": "个体工商户"})
tenant_df["商户渠道"] = tenant_df["商户渠道"].map({"00":"未知", "10": "ke-二手房", "20": "ke-租赁", "30": "ke-装修"})
tenant_df["用户状态"] = tenant_df["用户状态"].map({"unactivated":"邮箱未激活", "applying":"待申请", "termination":"申请失效",
                                           "submitted":"待审核", "passed":"审核通过", "failed": "申请失败"})

tenant_df["平台来源"] = tenant_df["平台来源"].map({"portal": "自注册", "api": "API方式对接"})

tenant_df["开户状态"] = tenant_df["开户状态"].astype("str")

tenant_df["开户状态"] = tenant_df["开户状态"].map({"0": "未开通", "1": "已开通"})

tenant_df["签约状态"] = tenant_df["签约状态"].astype("str")
tenant_df["签约状态"] = tenant_df["签约状态"].map({"0": "未签约", "1": "已签约"})

gra_tenant = tenant_df.copy()

gra_tenant["tag"] = np.where(((gra_tenant["公司运营状况"].str.contains("BAD")) | (gra_tenant["公司负债状况"].str.contains("BAD")) | (gra_tenant["公司收入状况"].str.contains("BAD"))),
                             "风险商户", "正常商户")

gra_tenant["graph_label"] = "商户"

tenant_schema = make_node_schema("商户", gra_tenant,
                                 comp_index_properties=["商户id", "商户编号", "渠道商户号", "统一社会信用代码", "组织机构代码", "税务登记号", "银行联行号", "总行编号", "银行卡号",
                                                        "登录账号", "开户许可证核准号", "开户省id", "开户市id"],
                                 mix_index_properties=["tag", "注册邮箱", "商户渠道", '商户公司简称', '商户类型', '平台来源', '签约状态', '开户状态', '用户状态',
                                                       '商户公司名称', '公司类型', '成立日期', '法定代表人', '营业期限结束时间', '营业期限开始时间', '注册资本','发照日期', '登记机关',
                                                       '是否三证合一', '公司地址', '经营范围', '企业邮箱', '公司网址', '公司运营状况', '公司负债状况', '公司收入状况', '合作协议附件',
                                                       '授权协议附件', '垫佣贝企业电子签章授权书', '开户银行', '开户支行', '开户省名称', '开户市名称', '创建日期'])


tenant_mapper = make_node_mapper("商户", gra_tenant)

dump_schema(tenant_schema, tenant_mapper, "tenant")

gra_tenant.to_csv(graph_type + "tenant/gra_商户.csv", sep=',', header=True, index=False)

########################################################################################
## UUS实体

sql = """select
uus_id  
,acct_name as `登录账号`
,acct_type as `账号类型`
,position as `职务`
,work_num as `工号`
,status as `用户状态`
,store_id as `门店ID`
,unit_id as `运营中心ID`
,source_type as `用户来源`
,old_uus_id as `旧uus注册ID`
 From cf_tenant.tn_user where uus_id is not null
union all
select
old_uus_id as uus_id
,acct_name as `登录账号`
,acct_type as `账号类型`
,position as `职务`
,work_num as `工号`
,status as `用户状态`
,store_id as `门店ID`
,unit_id as `运营中心ID`
,source_type as `用户来源`
,cast(null as string) as `旧uus注册ID` 
 From cf_tenant.tn_user where old_uus_id not in (select uus_id from cf_tenant.tn_user where uus_id is not null)"""


uus_user_df = spark.sql(sql).toPandas()
gra_uus_user = uus_user_df.copy()

gra_uus_user["账号类型"] = gra_uus_user["账号类型"].map({"email":"邮箱", "username":"用户名", "phone":"手机号"})

gra_uus_user["用户状态"] = gra_uus_user["用户状态"].map({"effective":"生效", "invalid":"失效"})

gra_uus_user["用户来源"] = gra_uus_user["用户来源"].astype("str")

gra_uus_user["用户来源"] = gra_uus_user["用户来源"].map({"1": "portal", "2": "理房通API同步"})

gra_uus_user = gra_uus_user[["uus_id", "登录账号", "账号类型", "用户状态", "用户来源"]]

gra_uus_user["tag"] = "uus用户"

gra_uus_user["graph_label"] = "uus用户"

uus_schema = make_node_schema("uus用户", gra_uus_user,
                              comp_index_properties=["uus_id", "登录账号"],
                              mix_index_properties=["tag", "账号类型", "用户状态", '用户来源'])

uus_mapper = make_node_mapper("uus用户", gra_uus_user)

dump_schema(uus_schema, uus_mapper, "uus")

gra_uus_user.to_csv(graph_type + "uus/gra_uus用户.csv", sep=',', header=True, index=False)

###################################################################################################################

# 授权人

sql = """select distinct auth_user_card_no as `授权人身份证号码`,
auth_user_name as `授权人姓名`,
auth_user_mobile as `授权人手机`
from cf_tenant.tn_enterprise s
left join cf_tenant.tn_product_apply_extend_info t on s.tenant_id = t.tenant_id
where auth_user_card_no is not null"""

auth_user = spark.sql(sql).toPandas()

gra_auth_user = auth_user.copy()

gra_auth_user["tag"] = "授权人"

gra_auth_user["graph_label"] = "授权人"

auth_schema = make_node_schema("授权人", gra_auth_user,
                               comp_index_properties = ["授权人身份证号码", "授权人手机"],
                               mix_index_properties = ["tag", "授权人姓名"])

auth_mapper = make_node_mapper("授权人", gra_auth_user)

dump_schema(auth_schema, auth_mapper, "auth_user")

gra_auth_user.to_csv(graph_type + "auth_user/gra_授权人.csv", sep=',', header=True, index=False)

###################################################################################################

# 法人

sql = """select distinct
s.legal_credential_no as `法人证件号码`
,s.legal_credential_type as `法人证件类型`
,s.legal_name as `法人姓名`
,s.legal_credential_start as `法人证件有效开始日期`
,s.legal_credential_end as `法人证件有效结束日期`
,legal_mobile as `法人手机`
,legal_bank_card_num as `法人银行账号`
,legal_acct_name as `法人账户名称`
,legal_open_bank as `法人开户银行名称`
,legal_open_bank_branch as `法人开户支行名称`
,legal_bank_link_number as `法人银行联行号`
,legal_bank_code as `法人总行编号`
,legal_open_province_id as `法人开户省id`
,legal_open_province_name as `法人开户省名称`
,legal_open_city_id as `法人开户市id`
,legal_open_city_name as `法人开户市名称`
,legal_branch_bank_flag as `法人支行信息输入标识`
,legal_bank_reserve_mobile as `法人银行预留手机号`
from cf_tenant.tn_enterprise s
left join cf_tenant.tn_product_apply_extend_info t on s.tenant_id = t.tenant_id"""


legal_user = spark.sql(sql).toPandas()

gra_legal_user = legal_user.copy()

gra_legal_user = gra_legal_user[list(set(gra_legal_user.columns.tolist()).difference(set(["etl_time", "pt"])))]

gra_legal_user["tag"] = "法人"

gra_legal_user["graph_label"] = "法人"

gra_legal_user["法人证件类型"] = gra_legal_user["法人证件类型"].astype("str")
gra_legal_user["法人证件类型"] = gra_legal_user["法人证件类型"].map({"1":"身份证", "2":"护照", "3":"港澳居民来往内地通行证", "4":"台胞证", "5":"其他"})

legal_schema = make_node_schema("法人", gra_legal_user,
                                comp_index_properties = ["法人证件号码", "法人手机", "法人银行账号", "法人账户名称", "法人银行联行号", "法人总行编号", "法人开户省id", "法人开户市id", "法人银行预留手机号"],
                                mix_index_properties = ["tag", "法人证件类型", "法人姓名", "法人证件有效开始日期", "法人证件有效结束日期",
                                                        "法人开户银行名称", "法人开户支行名称", "法人开户省名称", "法人开户市名称", "法人支行信息输入标识"])

legal_mapper = make_node_mapper("法人", gra_legal_user)

dump_schema(legal_schema, legal_mapper, "legal")

gra_legal_user.to_csv(graph_type + "legal/gra_法人.csv", sep=',', header=True, index=False)


#####################################################################################################################

# 联系人

sql = """select
distinct mobile as `联系人手机`,
email as `联系人邮箱`,
name as `联系人名称`
From cf_tenant.tn_contact"""

contact_df = spark.sql(sql).toPandas()

gra_contact = contact_df.copy()

gra_contact["tag"] = "联系人"

gra_contact["graph_label"] = "联系人"

contact_schema = make_node_schema("联系人", gra_contact,
                                  comp_index_properties = ["联系人手机"],
                                  mix_index_properties = ["tag", "联系人邮箱", "联系人名称"])

contact_mapper = make_node_mapper("联系人", gra_contact)

dump_schema(contact_schema, contact_mapper, "contact")

gra_contact.to_csv(graph_type + "contact/gra_联系人.csv", sep=',', header=True, index=False)

#####################################################################################################################

# 理房通

sql = """select s.lft_merchant_num as `理房通商户号`
,ehp_acc as `理房通ehp账号`
,pro_type as `服务产品`
,cust_id as `用户id`
,merchant_type as `账户类型`
,merchantnum as `商户号`
,action_status as `状态`
,remark as `备注`
,contract_id as `合同号` 
,email as `账户邮箱`
,city_id as `城市id`
,alias_name as `关联商户名称`
,account_use_name as `账户用途`
,drawing as `允许提现`
,auto_drawing as `自动提现`
,account_name as `账户名`
,bank_card as `银行账号`
,bank_branch as `开户行支行名称`
,cycle as `手续费结算方式`
,pay_type as `支付方式`
,city_name as `城市名字`
,account_type as `分账账户类型`
,parentname as `服务产品-汇总`
,alias_merchant as `商编`
,audit_time as `开户日期`
,tag as `平台方标签`
,tag1 as `二级归属`
,tag2 as `三级归属`
,city_name2 as `城市名字2` 
From cf_tenant.tn_tenant s
inner join lft_dw.dw_ptp_merchant t on t.merchantnum = s.lft_merchant_num"""

lft_df = spark.sql(sql).toPandas()

gra_lft = lft_df.copy()

gra_lft["tag"] = "理房通商户"

gra_lft["graph_label"] = "理房通商户"

gra_lft["状态"] = gra_lft["状态"].astype("str")
gra_lft["状态"] = gra_lft["状态"].map({"1":"正常", "2":"止入", "3":"冻结","4":"止入止出","5":"注销","7":"司法冻结", "8":"休眠", "9":"异常"})

gra_lft["分账账户类型"] = gra_lft["分账账户类型"].astype("str")
gra_lft["分账账户类型"] = gra_lft["分账账户类型"].map({"1": "分账A类账户", "2": "全国普通账户", "3": "分账C类账户", "4": "分账B类账户", "5": "D账户",
                                           "6": "E账户代理服务费-分账（其他）", "7": "F账户 代理服务费（其他）"})

lft_schema = make_node_schema("理房通商户", gra_lft,
                              comp_index_properties = ["理房通商户号", "理房通ehp账号", "用户id", "商户号", "合同号", "城市id", "账户名", "银行账号", "商编"],
                              mix_index_properties = ["tag", "服务产品", "账户类型", "状态", "备注", "账户邮箱", "关联商户名称", "账户用途", "允许提现", "自动提现",
                                                      "开户行支行名称", "手续费结算方式", "支付方式", "城市名字", "分账账户类型", "服务产品-汇总", "开户日期",
                                                      "平台方标签", "二级归属", "三级归属", "城市名字2"])

lft_schema['propertyKeys'][19]["dataType"] = "Float"

lft_mapper = make_node_mapper("理房通商户", gra_lft)

dump_schema(lft_schema, lft_mapper, "lft")

gra_lft.to_csv(graph_type + "lft/gra_理房通商户.csv", sep=',', header=True, index=False)
####################################################################################################################

#产品

sql = """select distinct template_product_id as `金融产品ID`
,product_type_name as `产品类别名称`
,product_type as `产品类别`
,city_name as `城市名称`
,city_id as `城市id`
,capital_product_code as `资金平台产品代码`
,product_code as `金融产品编码`
,product_name as `商户产品名称`
,state as `产品状态`
        From  cf_tenant.tn_product
"""

product_df = spark.sql(sql).toPandas()

gra_product = product_df.copy()

gra_product["tag"] = "产品"

gra_product["graph_label"] = "产品"

product_schema = make_node_schema("产品", gra_product,
                                  comp_index_properties = ["金融产品ID", "城市id", "金融产品编码", "资金平台产品代码"],
                                  mix_index_properties = ["tag", "产品类别名称", "产品类别", "城市名称", "商户产品名称", "产品状态"])

product_mapper = make_node_mapper("产品", gra_product)

dump_schema(product_schema, product_mapper, "product")

gra_product.to_csv(graph_type + "product/gra_产品.csv", sep=',', header=True, index=False)

################################################################################################################

# 项目开发商

sql = """select
       property_company_social_credit_code as `项目公司统一社会信用代码`,
       property_company_name as `项目公司名称`
from cf_receivables_financing.property
where property_company_social_credit_code is not null or property_company_name is not null"""


company_df = spark.sql(sql).toPandas().drop_duplicates()

company_df = company_df[~pd.isnull(company_df["项目公司名称"])]

gra_company = company_df.copy()

for idx in gra_company.index:

    company_name = gra_company.loc[idx, "项目公司名称"]

    if pd.isnull(gra_company.loc[idx, "项目公司统一社会信用代码"]) or len(gra_company.loc[idx, "项目公司统一社会信用代码"].strip()) == 0:
        gra_company.loc[idx, "项目公司统一社会信用代码"] = make_md5(company_name)

gra_company["tag"] = "开发商"

gra_company["graph_label"] = "开发商"


cur_date = spark.sql("select max(pt) as max_date from spark_dw.real_estate_company_invest_graph").toPandas()
cur_date = cur_date.loc[0, "max_date"]

real_estate_invest_df = spark.sql("select * from spark_dw.real_estate_company_invest_graph where pt='{}'".format(cur_date)).toPandas()

data_df = real_estate_invest_df.copy()

data_df.replace('nan', np.nan, inplace=True)

longest_path = get_longest_relation(data_df)

cur_date = spark.sql("select max(pt) as max_date from spark_dw.dw_ke_bkjf_newhouse_service_contract_pisces_company_da").toPandas()
cur_date = cur_date.loc[0, "max_date"]

company_detail_df = spark.sql("""select distinct t.name, 
                              (case 
                              when t.type=1 then '开发商'
                              when t.type=0 then '非开发商'
                              when t.type=2 then '开发商控股'
                              when t.type=3 then '第三方电商平台'
                              else null end) as type,
                              t.legal_representative,
                              t.social_credit_code
                              from spark_dw.dw_ke_bkjf_newhouse_service_contract_pisces_company_da t where pt = {} and
                              ISNULL(t.legal_representative)=0 and LENGTH(trim(t.legal_representative))>0
                              """.format(cur_date)).toPandas()

company_detail_df.drop_duplicates(["social_credit_code"], inplace=True)

company_detail_df.drop_duplicates(["name"], inplace=True)

all_companies = extract_company_names(data_df, longest_path)

company_df = make_company_entity(all_companies, company_detail_df)

company_df = pd.merge(company_df, data_df[["level_1_company"]], how="left", left_on="公司名", right_on="level_1_company")
company_df.drop_duplicates(company_df.columns, inplace=True)

group_index = company_df[company_df["公司名"] == company_df["level_1_company"]].index.values

company_df.loc[group_index, "tag"] = "集团公司"

company_df = pd.merge(company_df, gra_company[["项目公司名称", "项目公司统一社会信用代码", "graph_label"]], how="left",
                      left_on="公司名", right_on="项目公司名称")

company_df.drop_duplicates(company_df.columns, inplace=True)

index = company_df[(pd.isnull(company_df["社会信用号"])) & (~pd.isnull(company_df["项目公司统一社会信用代码"]))].index.values

company_df.loc[index, "社会信用号"] = company_df.loc[index, "项目公司统一社会信用代码"]

index = company_df[(~pd.isnull(company_df["项目公司名称"])) & (pd.isnull(company_df.tag))].index.values

company_df.loc[index, "tag"] = "开发商"

index = company_df[pd.isnull(company_df.tag)].index.values

company_df.loc[index, "tag"] = "未知"

company_df = company_df[["公司名", "tag", "法人代表", "社会信用号", "graph_label"]]

company_df.drop_duplicates(["公司名"], inplace=True)

company_diff = set(gra_company["项目公司名称"].unique().tolist()).difference(company_df["公司名"].unique().tolist())

gra_company_diff = gra_company[gra_company["项目公司名称"].isin(list(company_diff))]

gra_company_diff.rename(columns={"项目公司名称": "公司名", "项目公司统一社会信用代码": "社会信用号"}, inplace=True)
gra_company_diff["法人代表"] = np.nan
gra_company_diff = gra_company_diff[["公司名", "tag", "法人代表", "社会信用号", "graph_label"]]

company_df = pd.concat([company_df, gra_company_diff])

company_df["graph_label"] = "开发商"

company_df.index = np.arange(company_df.shape[0])

index = company_df[pd.isnull(company_df["社会信用号"])].index.values
company_df.loc[index, "社会信用号"] = company_df.loc[index, "公司名"].apply(lambda x: make_md5(x))

company_df["公司名全称"] = company_df["公司名"]

company_schema = make_node_schema("开发商", company_df,
                                  comp_index_properties = ["公司名", "法人代表", "社会信用号"],
                                  mix_index_properties = ["tag", "公司名全称"])

company_mapper = make_node_mapper("开发商", company_df)

dump_schema(company_schema, company_mapper, "company")

company_df.to_csv(graph_type + "company/gra_开发商.csv", sep=',', header=True, index=False)

##################################################################################################################

# 房产项目

sql = """select 
       property_code as `项目编号`,
       property_name as `项目名称`,
       CASE
                                               WHEN city in ('大理市','大理') THEN '大理白族自治州'
                                               WHEN city in ('西双版纳市','西双版纳') THEN '西双版纳傣族自治州'
                                               WHEN city not in ('西双版纳','大理') and city LIKE '%市%' or city LIKE '%自治州%' THEN city
                                               WHEN city is null then '-1'
                                               ELSE concat(city,'市')
       END as `城市`,
       city_code as `城市代号`,
       property_basic_type as `基本情况`,
       status as `状态`,
       create_time as `创建时间`,
       manual_approve_time as `人工审核时间`,
       repay_month as `预计回款账期`
from cf_receivables_financing.property
where status in (40,60) and deleted = 0
 and deleted= 0
"""

property_df = spark.sql(sql).toPandas().drop_duplicates()

gra_property = property_df.copy()

#gra_property.drop(["id"], axis=1, inplace=True)

gra_property.rename(columns={"所属城市的code": "城市代号"}, inplace=True)

gra_property["tag"] = "房产项目"

gra_property["graph_label"] = "房产项目"

gra_property["基本情况"]=gra_property["基本情况"].astype("str")
gra_property["基本情况"] = gra_property["基本情况"].map({"0": "A代", "1": "其他"})

gra_property["状态"]=gra_property["状态"].astype("str")
gra_property["状态"]=gra_property["状态"].map({"10":"待审核", "20":"自动审核未通过", "30":"自动审核通过", "40":"审核驳回",
                                           "50":"审核拒绝", "60":"审核通过"})

property_schema = make_node_schema("房产项目", gra_property,
                                   comp_index_properties = ["项目编号", "城市代号"],
                                   mix_index_properties = ["tag", "项目名称", "城市", "基本情况", "状态", "创建时间", "人工审核时间", "预计回款账期"])

property_mapper = make_node_mapper("房产项目", gra_property)

dump_schema(property_schema, property_mapper, "property")

gra_property.to_csv(graph_type + "property/gra_房产项目.csv", sep=',', header=True, index=False)

####################################################################################################################

#关系

entity_relations = {
    "授权": [("商户.商户id", "授权人.授权人身份证号码")],
    "访问uus": [("商户.商户id", "uus用户.uus_id")],
    "开发":  [("开发商.社会信用号", "房产项目.项目编号")],
    "联系": [("商户.商户id", "联系人.联系人手机")],
    "提供金融产品": [("商户.商户id", "产品.金融产品ID")],
    "支付渠道": [("商户.商户id", "理房通商户.理房通商户号")],
    "担任法人": [("商户.商户id", "法人.法人证件号码")],
    "订购": [("商户.商户id", "房产项目.项目编号")]
}


def get_relation(sql, relation_name):
    gra_df = spark.sql(sql).toPandas().drop_duplicates()
    gra_df.columns = ["Left", "Right"]
    gra_df = gra_df[(~pd.isnull(gra_df.Left)) & (~pd.isnull(gra_df.Right)) & (gra_df.Left.str.strip().str.len()> 0) & (gra_df.Right.str.strip().str.len()> 0)]
    gra_df["Type"] = relation_name
    gra_df = gra_df[["Left", "Type", "Right"]]
    return gra_df


def make_schema_mapper(relation_df, relation_name, folder, relation_comp_index_properties=None, relation_mix_index_properties=None):
    schema = make_edge_schema(relation_df, relation_comp_index_properties, relation_mix_index_properties)
    mapper = make_edge_mapper(entity_relations, relation_df, relation_name)
    dump_schema(schema, mapper, folder)

    relation_df.to_csv(graph_type + folder + "/gra_" + relation_name + ".csv", sep=',', header=True, index=False)


## 授权关系
sql = """select distinct tenant_id,auth_user_card_no from cf_tenant.tn_product_apply_extend_info where auth_user_card_no is not null"""
gra_tenant_auth = get_relation(sql, "授权")
make_schema_mapper(gra_tenant_auth, "授权", "tenant_auth")


## 访问uus
sql = """select distinct tenant_id,uus_id from cf_tenant.tn_user_login_log where uus_id is not null"""
gra_tenant_uus = get_relation(sql, "访问uus")
make_schema_mapper(gra_tenant_uus, "访问uus", "tenant_uus")

## 开发商项目
sql = """select distinct property_company_social_credit_code, property_code, property_company_name
From cf_receivables_financing.property where (property_company_social_credit_code is not null and property_company_social_credit_code<>'') or property_company_name is not null"""

gra_company_property = spark.sql(sql).toPandas().drop_duplicates()
for idx in gra_company_property.index:

    company_name = gra_company_property.loc[idx, "property_company_name"]

    if pd.isnull(gra_company_property.loc[idx, "property_company_social_credit_code"]) or \
            len(gra_company_property.loc[idx, "property_company_social_credit_code"].strip()) == 0:
        gra_company_property.loc[idx, "property_company_social_credit_code"] = make_md5(company_name)

gra_company_property = gra_company_property[["property_company_social_credit_code", "property_code"]]

gra_company_property.columns = ["Left", "Right"]
gra_company_property["Type"] = "开发"
gra_company_property = gra_company_property[["Left", "Type", "Right"]]

make_schema_mapper(gra_company_property, "开发", "company_property")

## 联系
sql = """select distinct tenant_id,mobile  from cf_tenant.tn_contact"""
gra_tenant_contact = get_relation(sql, "联系")
make_schema_mapper(gra_tenant_contact, "联系", "tenant_contact")

## 提供金融产品
sql = """select distinct tenant_id,template_product_id  from cf_tenant.tn_product_use_relation"""
gra_tenant_product = get_relation(sql, "提供金融产品")
make_schema_mapper(gra_tenant_product, "提供金融产品", "tenant_product")

## 支付渠道
sql = """select distinct id as tenant_id,lft_merchant_num from cf_tenant.tn_tenant where lft_merchant_num is not null"""
gra_tenant_lft = get_relation(sql, "支付渠道")
make_schema_mapper(gra_tenant_lft, "支付渠道", "tenant_lft")


## 担任法人
sql = """select distinct tenant_id,legal_credential_no from cf_tenant.tn_enterprise where legal_credential_no is not null"""
gra_tenant_legal = get_relation(sql, "担任法人")
make_schema_mapper(gra_tenant_legal, "担任法人", "tenant_legal")

#订购
sql = """select rzsqfbh as tenant_id,xm_lpbh as property_code,sum(fkje) as loan_amt,count(1)  as loan_cnt 
from bfinance_dw.dw_bfin_dyb_newhousebasicperf_cons group by  rzsqfbh,xm_lpbh"""
gra_tenant_property = spark.sql(sql).toPandas().drop_duplicates()
gra_tenant_property.columns = ["Left", "Right", "订购总金额", "订购次数"]
gra_tenant_property = gra_tenant_property[(~pd.isnull(gra_tenant_property.Left)) & (~pd.isnull(gra_tenant_property.Right)) &
                                          (gra_tenant_property.Left.str.strip().str.len()> 0) & (gra_tenant_property.Right.str.strip().str.len()> 0)]
gra_tenant_property["Type"] = "订购"

gra_tenant_property = gra_tenant_property[["Left", "Type", "Right", "订购总金额", "订购次数"]]
gra_tenant_property["订购总金额"] = gra_tenant_property["订购总金额"].astype("float")
gra_tenant_property["订购次数"] = gra_tenant_property["订购次数"].astype("float")
make_schema_mapper(gra_tenant_property, "订购", "tenant_property", relation_mix_index_properties=["订购总金额", "订购次数"])


#投资关系
relation_df = make_company_invest(data_df, longest_path)

relation_df = relation_df[(~pd.isnull(relation_df.Left)) & (~pd.isnull(relation_df.Right))]

relation_df.rename(columns={"percentage": "出资比例"}, inplace=True)

relation_df.drop_duplicates(relation_df.columns, inplace=True)

entity_relations = {}

for i in range(longest_path - 1):

    entity_relations["{}级出资".format(i + 2)] = [("开发商.公司名", "开发商.公司名")]

company_invest_schema = make_edge_schema(relation_df, relation_mix_index_properties=["出资比例"])

company_invest_schema['propertyKeys'][0]['dataType'] = "Float"

company_invest_mapper = make_edge_mapper(entity_relations, relation_df)

dump_schema(company_invest_schema, company_invest_mapper, "company_invest")

for i in range(longest_path - 1):
    tmp_df = relation_df[relation_df["Type"] == "{}级出资".format(i + 2)]
    tmp_df.to_csv(graph_type + "company_invest/gra_{}级出资.csv".format(i + 2), sep=',', header=True, index=False)

#台账关系
taizhang_df = real_estate_invest_df[real_estate_invest_df.level=="台账决议"]

taizhang_relation_df = taizhang_df[["level_1_company", "level", "company"]]

taizhang_relation_df.columns = ["Left", "Type", "Right"]

taizhang_relation_df["出资比例"] = -911.0

taizhang_entity_relations = {}
taizhang_entity_relations["台账决议"] = [("开发商.公司名", "开发商.公司名")]

company_taizhang_schema = make_edge_schema(taizhang_relation_df, relation_mix_index_properties=["出资比例"])

company_taizhang_mapper = make_edge_mapper(taizhang_entity_relations, taizhang_relation_df)

dump_schema(company_taizhang_schema, company_taizhang_mapper, "company_taizhang")
taizhang_relation_df.to_csv(graph_type + "company_taizhang/gra_台账决议.csv", sep=',', header=True, index=False)




