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
graph_type = "risk_radar/"


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

    vertexLabels["vertexLabels"].append({"name": entity_name})

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
            prop = {"name": col, "dataType": "Float", "cardinality": "SINGLE"}
        elif relation_df[col].dtype == np.integer:
            prop = {"name": col, "dataType": "Integer", "cardinality": "SINGLE"}
        else:
            prop = {"name": col, "dataType": "String", "cardinality": "SINGLE"}

        properties["propertyKeys"].append(prop)

    relation_names = relation_df["Type"].value_counts().index.tolist()

    edgeLabels = {"edgeLabels": []}

    for relation in relation_names:
        edgeLabels["edgeLabels"].append({
            "name": relation,
            "multiplicity": "MULTI",
            "unidirected": False
        })

    edgeIndexes = {"edgeIndexes": []}

    for relation_name in relation_names:
        if relation_comp_index_properties is not None:
            for prop in relation_comp_index_properties:
                edgeIndexes["edgeIndexes"].append({
                    "name": relation_name + "_" + prop + "_comp",
                    "propertyKeys": [ prop ],
                    "composite": True,
                    "unique": False,
                    "indexOnly": relation_name
                })

        if relation_mix_index_properties is not None:
            for prop in relation_mix_index_properties:
                edgeIndexes["edgeIndexes"].append({
                    "name" : relation_name + "_" + prop + "_mixed",
                    "propertyKeys": [ prop ],
                    "composite": False,
                    "unique": False,
                    "mixedIndex": "search",
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


# 交易单
str1='''
select t1.loan_no as `订单编号`,
       t1.transaction_no as `交易编号`,
       t1.product_name as `产品名称`,
       case when t1.secondary_classify=1 then '未报单'
            when t1.secondary_classify=2 then '在途单'
            when t1.secondary_classify=3 then '终结单'
       else t1.secondary_classify end as `是否在途`,
       t1.loan_status as `订单状态`,
       t1.created_date as `订单时间`,

       t2.apply_fund_amount as `用资金额`,
       t3.city_name as `城市`,
       '金融单' as `tag`
from housingloan.loan t1
left join housingloanapply.loan t2
on t1.loan_no=t2.loan_no
left join dw.pub_dim_city t3
on t1.city_code=t3.city_code
'''

data1 = spark.sql(str1).toPandas().drop_duplicates()
map1={11:'未报',12:'重新报单',13:'签约中',14:'已签约',15:'外部预审核中',16:'外部预审核通过',21:'风控审核中',22:'已出告知书',23:'解约中',24:'合同变更',25:'已划款',26:'已过户',27:'合同补充',28:'风控审核通过',29:'已放款',210:'部分回款',211:'全部回款',212:'已解约',213:'未达成',214:'放款失败',215:'放款中',216:'划款失败',217:'外部审核通过',218:'外部审核退回',219:'外部审核中',220:'放款复核中',221:'已划款无需过户',222:'已批贷',223:'划款中',224:'已还款',225:'已领取',226:'已解押',31:'已中止解约',32:'已中止无效',33:'已终结未达成',34:'已终结',35:'已中止放款失败'}
data1['订单状态']=data1['订单状态'].map(map1)
risk=pd.read_excel('金融风险单.xlsx')
data1['tag']=np.where((data1['订单编号'].isin(risk['金融编号'])), '金融单(风险单)', '金融单(正常单)')
data1_temp1=data1[data1['tag']=='金融单(正常单)']
data1_temp2=data1[data1['tag']=='金融单(风险单)']
data1_temp2=pd.merge(data1_temp2, risk, how='left', left_on='订单编号', right_on='金融编号').drop('金融编号', axis=1)

data1=pd.concat([data1_temp1,data1_temp2],axis=0)

data1["graph_label"] = "金融单"
data1["用资金额"] = data1["用资金额"].astype("float")

data1_schema = make_node_schema("金融单", data1,
                                comp_index_properties = ["交易编号", "订单编号"],
                                mix_index_properties = ["tag", "产品名称", "城市", "是否在途", "用资金额", "订单时间", "订单状态", "风险原因", "风险状态"])

data1_mapper = make_node_mapper("金融单", data1)

dump_schema(data1_schema, data1_mapper, "jinrongdan")

data1.to_csv(graph_type + "jinrongdan/gra_金融单.csv", sep=',', header=True, index=False)


# 交易人
str2='''
select business_id_no as `证件号`,

       case when business_role=0 then '买方'
            when business_role=1 then '卖方'
            when business_role=2 then '买方代理人'
            when business_role=3 then '卖方代理人'
            when business_role=4 then '借款人'
            when business_role=10 then '反担保人'
       else business_role end as `tag`,

       business_name as `姓名`,

       case when business_sex=0 then '男'
            when business_sex=1 then '女'
       else business_sex end as `性别`,

       business_age as `年龄`,
       business_tel as `手机号`,

       case when business_type=0 then '个人'
            when business_type=1 then '公司'
       else business_type end as `角色形态`,

       case when business_marriage=0 then '未婚'
            when business_marriage=1 then '已婚'
            when business_marriage=2 then '丧偶'
            when business_marriage=3 then '离异'
       else business_marriage end as `婚姻状态`,

       postal_address as `通讯地址`
from housingloanapply.loan_business_info  
where business_id_no is not null and trim(business_id_no)!=''
'''

data2 = spark.sql(str2).toPandas().drop_duplicates()

data2["graph_label"] = "交易人"

data2_schema = make_node_schema("交易人", data2,
                                comp_index_properties = ["证件号"],
                                mix_index_properties = ["tag", "姓名", "性别", "年龄", "手机号", "角色形态", "婚姻状态", "通讯地址"])

data2_schema["propertyKeys"][4]["dataType"] = "Float"

data2_mapper = make_node_mapper("交易人", data2)

data2.to_csv(graph_type + "jiaoyiren/gra_交易人.csv", sep=',', header=True, index=False)

dump_schema(data2_schema, data2_mapper, "jiaoyiren")


# 交易人配偶
str3='''
select business_mate_id_no as `证件号`,
       business_id_no as `交易者证件号`,
       business_name as `交易者姓名`,
       case when business_sex=0 then '女'
            when business_sex=1 then '男'
       else business_sex end as `性别`,

       business_mate_name as `姓名`,
       business_mate_tel as `手机号`,
       '配偶' as `tag`
from housingloanapply.loan_business_info 
where business_mate_id_no is not null and trim(business_mate_id_no)!=''
'''

data3 = spark.sql(str3).toPandas().drop_duplicates()

data3["graph_label"] = "交易人配偶"

data3_schema = make_node_schema("交易人配偶", data3,
                                comp_index_properties = ["证件号", "交易者证件号"],
                                mix_index_properties = ["tag", "交易者姓名", "性别", "姓名", "手机号"])

data3_mapper = make_node_mapper("交易人配偶", data3)

data3.to_csv(graph_type + "jiaoyiren_couple/gra_交易人配偶.csv", sep=',', header=True, index=False)

dump_schema(data3_schema, data3_mapper, "jiaoyiren_couple")


#参与人
str4='''
select t1.participant_id as `系统号`,
       t1.role as `tag`,
       t1.participant_name as `姓名`,
       t1.department_id as `部门id`,
       t1.department as `部门`,
       t1.region_id as `大区id`,
       t1.region as `大区`,
       t1.part as `大部`,
       t1.phone as `手机号`,
       
       t2.sex as `性别`,
       t2.marriage as `婚姻状态`,
       t2.uc_job_name as `职位名称`,
       t2.uc_job_level_name as `职位等级`,
       t2.entry_date as `入职日期`,
       t2.resign_date as `离职日期`,
       t2.brand_name as `品牌`,
       t2.degree_type as `学历`,
       t2.shop_code as `门店编码`,
       t2.shop_name as `门店名称`
from housingloan.participantrecord t1
left join dw.dw_ke_bkjf_allinfo_hr_employee_da t2
on t1.participant_id=t2.employee_no
'''

data4 = spark.sql(str4).toPandas().drop_duplicates(['系统号'])

data4["graph_label"] = "参与人"

data4_schema = make_node_schema("参与人", data4,
                                comp_index_properties = ["系统号", "部门id", "大区id", "门店编码"],
                                mix_index_properties = list(set(data4.columns.tolist()).difference(set(["系统号", "部门id", "大区id", "门店编码"]))))

data4_mapper = make_node_mapper("参与人", data4)

data4.to_csv(graph_type + "canyuren/gra_参与人.csv", sep=',', header=True, index=False)

dump_schema(data4_schema, data4_mapper, "canyuren")


#标的房屋
str5='''
select t1.property_address as `房屋地址`,
       t1.loan_no as `金融单号`,
       t2.transaction_no as `交易单号`,
       t1.building_name as `楼盘名称`,
       t1.province as `省份编码`,
       t1.city as `城市编码`,
       t1.district as `区县编码`,
       t1.build_area as `建筑面积`,

       case when t1.pledge_type_info=0 then '纯一抵'
            when t1.pledge_type_info=1 then '银行转单'
            when t1.pledge_type_info=2 then '小贷转单'
            when t1.pledge_type_info=3 then '一抵为银行住房按揭贷款抵押'
            when t1.pledge_type_info=4 then '一抵为抵押消费贷款抵押'
            when t1.pledge_type_info=5 then '一抵为小贷抵押'
       else t1.pledge_type_info end as `抵押情况`,

       t1.evaluate_amount as `评估价`,
       t3.fangwucjjxx as `房屋成交价`,
       t3.house_id as `房屋id`,
       t3.bizcircle_id as `商圈id`,
       t3.bizcircle_name as `商圈名称`,
       t3.resblock_id as `楼盘id`,
       t3.resblock_name as `楼盘名称2`,
       t3.shop_code as `成交店面id`,
       t3.shop_name as `成交店面`,
       t3.inside_area as `套内面积`,
       '房屋' as `tag`
from housingloanapply.loan_house_info t1
left join housingloan.loan t2
on t1.loan_no=t2.loan_no
left join dw.dwd_bkjf_tra_jinkong_di t3
on t2.transaction_no=t3.business_code
where property_address is not null and trim(property_address)!=''
'''

data5 = spark.sql(str5).toPandas().drop_duplicates()

data5["graph_label"] = "房屋"

data5_schema = make_node_schema("房屋", data5,
                                comp_index_properties=["金融单号", "房屋地址", "交易单号", "省份编码", "城市编码", "区县编码", "房屋id", "商圈id", "楼盘id", "成交店面id"],
                                mix_index_properties=["楼盘名称", "建筑面积", "抵押情况", "评估价", "房屋成交价", "商圈名称", "楼盘名称2", "成交店面", "套内面积", "tag"])

data5_schema["propertyKeys"][9]["dataType"] = "Float"
data5_schema["propertyKeys"][10]["dataType"] = "Float"
data5_schema["propertyKeys"][11]["dataType"] = "Float"
data5_schema["propertyKeys"][12]["dataType"] = "Float"
data5_schema["propertyKeys"][14]["dataType"] = "Float"

data5_mapper = make_node_mapper("房屋", data5)

data5.to_csv(graph_type + "house_info/gra_房屋.csv", sep=',', header=True, index=False)

dump_schema(data5_schema, data5_mapper, "house_info")


#门店
str6='''select distinct t2.shop_code as `门店编码`,
                        t2.shop_name as `门店名称`,
                        t2.city_name as `城市`,
                        '门店' as `tag`
from housingloan.participantrecord t1
left join dw.dw_ke_bkjf_allinfo_hr_employee_da t2
on t1.participant_id=t2.employee_no'''

data6 = spark.sql(str6).toPandas()

data6["graph_label"] = "门店"

data6_schema = make_node_schema("门店", data6,
                                comp_index_properties = ["门店编码"],
                                mix_index_properties = ["门店名称", "城市", "tag"])

data6_mapper = make_node_mapper("门店", data6)

data6.to_csv(graph_type + "mengdian/gra_门店.csv", sep=',', header=True, index=False)

dump_schema(data6_schema, data6_mapper, "mengdian")

#手机
str7='''
select business_tel as `手机号码`,'手机号' as `tag`
from housingloanapply.loan_business_info  

union 

select business_mate_tel as `手机号码`,'手机号' as `tag`
from housingloanapply.loan_business_info
where business_mate_id_no is not null and trim(business_mate_id_no)!=''

union

select phone as `手机号码`,'手机号' as `tag`
from housingloan.participantrecord
'''

data7 = spark.sql(str7).toPandas().drop_duplicates()

data7["graph_label"] = "手机号"

data7_schema = make_node_schema("手机号", data7,
                                comp_index_properties=["手机号码"],
                                mix_index_properties=["tag"])

data7_mapper = make_node_mapper("手机号", data7)

data7.to_csv(graph_type + "phones/gra_手机号.csv", sep=',', header=True, index=False)

dump_schema(data7_schema, data7_mapper, "phones")


#关系
str8='''
select loan_no,
       '标的物',
       property_address
from housingloanapply.loan_house_info

union

select loan_no,
       case when business_role=0 then '买入'
            when business_role=1 then '卖出'
            when business_role=2 then '代理买入'
            when business_role=3 then '代理卖出'
            when business_role=4 then '借款'
            when business_role=10 then '反担保人'
        else business_role end as `business_role_name`,
       business_id_no 
from housingloanapply.loan_business_info

union

select business_id_no,
       '配偶',
       business_mate_id_no
from housingloanapply.loan_business_info 
where business_id_no is not null and trim(business_id_no)!='' and business_mate_id_no is not null and trim(business_mate_id_no)!=''

union

select loan_no,
       role,
       participant_id
from housingloan.participantrecord t1

union

select t1.participant_id,
       '所属门店',
       t2.shop_code
from housingloan.participantrecord t1
left join dw.dw_ke_bkjf_allinfo_hr_employee_da t2
on t1.participant_id=t2.employee_no

union

select business_id_no,
       '联系号码',
       business_tel
from housingloanapply.loan_business_info  

union 
select business_mate_id_no,
       '联系号码',
       business_mate_tel
from housingloanapply.loan_business_info
where business_mate_id_no is not null and trim(business_mate_id_no)!=''

union
select participant_id,
       '联系号码',
       phone
from housingloan.participantrecord
'''
data8 = spark.sql(str8).toPandas().drop_duplicates()
data8.columns = ['START_ID', 'TYPE', 'END_ID']
data8.rename(columns={"TYPE": "Type"}, inplace=True)

entity_relations = {
    "卖出": [("金融单.订单编号", "交易人.证件号")],
    "买入": [("金融单.订单编号", "交易人.证件号")],
    "代理买入":  [("金融单.订单编号", "交易人.证件号")],
    "代理卖出": [("金融单.订单编号", "交易人.证件号")],
    "借款": [("金融单.订单编号", "交易人.证件号")],
    "反担保人": [("金融单.订单编号", "交易人.证件号")],
    "标的物": [("金融单.订单编号", "房屋.房屋地址")],
    "配偶": [("交易人.证件号", "交易人配偶.证件号")],
    "所属门店": [("参与人.系统号", "门店.门店编码")],
    "联系号码": [("交易人.证件号", "手机号.手机号码"), ("交易人配偶.证件号", "手机号.手机号码"), ("参与人.系统号", "手机号.手机号码")],
    "金融顾问": [("金融单.订单编号", "参与人.系统号")],
    "经纪人": [("金融单.订单编号", "参与人.系统号")],
    "初审专员": [("金融单.订单编号", "参与人.系统号")],
    "内勤专员": [("金融单.订单编号", "参与人.系统号")],
    "合同审核专员": [("金融单.订单编号", "参与人.系统号")],
    "交易顾问": [("金融单.订单编号", "参与人.系统号")],
    "赎楼还款专员": [("金融单.订单编号", "参与人.系统号")],
    "赎楼内勤": [("金融单.订单编号", "参与人.系统号")],
    "赎楼解押专员": [("金融单.订单编号", "参与人.系统号")],
    "签约专员": [("金融单.订单编号", "参与人.系统号")],
    "集中作业人员": [("金融单.订单编号", "参与人.系统号")],
    "城市运营": [("金融单.订单编号", "参与人.系统号")],
    "复审专员": [("金融单.订单编号", "参与人.系统号")],
    "金融抵押专员": [("金融单.订单编号", "参与人.系统号")],
    "渠道拓展专员": [("金融单.订单编号", "参与人.系统号")],
    "金融解押专员": [("金融单.订单编号", "参与人.系统号")],
    "风控外勤专员（成都）": [("金融单.订单编号", "参与人.系统号")],
    "风控高级审批": [("金融单.订单编号", "参与人.系统号")],
    "风控内勤专员(大连)": [("金融单.订单编号", "参与人.系统号")]
}

for key in entity_relations.keys():

    df = data8[data8["Type"] == key][["START_ID", 'END_ID']]

    df.rename(columns={"START_ID": "Left", "END_ID": "Right"}, inplace=True)

    df.to_csv(graph_type + "risk_radar_relations/gra_" + key + ".csv", sep=',', header=True, index=False)

relation_schema = make_edge_schema(relation_df=data8)

relation_mapper = make_edge_mapper(entity_relations)

dump_schema(relation_schema, relation_mapper, "risk_radar_relations")
