import os
import time
import math
import json
import hashlib
import datetime
import pandas as pd
import numpy as np
from run_pyspark import PySparkMgr


graph_type = "loan_agent/"


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


spark_args = {}

pysparkmgr = PySparkMgr(spark_args)
_, spark, sc = pysparkmgr.start('xubin.xu')

# 申请表
apply_loan_df = spark.sql("select * from adm.adm_credit_apply_quota_doc").toPandas()

# 支用表
zhiyong_loan_df = spark.sql("select * from adm.adm_credit_loan_apply_doc").toPandas()
zhiyong_loan_df.quota_apply_id = zhiyong_loan_df.quota_apply_id.astype("int")

# 逾期表
overdue_sql = """select 
*
from adm.adm_credit_apply_quota_doc        t1
--逾期关联，存在一个客户不同时间多笔申请，不同申请会对应不同的逾期状态
--当前逾期天数和历史最大逾期天数
left join 
(
        select 
        quota_apply_id,
        max(overdue_days_now) as overdue_days_now,
        max(his_max_overdue_days) as his_max_overdue_days
        from 
        (
        select 
        c4.quota_apply_id,
        c3.overdue_days_now,
        c3.his_max_overdue_days
        from 
                adm.adm_credit_loan_apply_doc c4
        left join
                (
                select 
                c2.business_id,
                max(overdue_days_now) as overdue_days_now,
                max(overdue_day_calc) as his_max_overdue_days
                from
                (
                        select 
                        c1.*,
                        (case when (overdue_day_calc>0 and latest_actual_repay_date is not null) then 0 else overdue_day_calc end) as overdue_days_now
                        FROM adm.adm_credit_rpt_risk_overdue_bill c1
                ) c2
                group by c2.business_id
                ) c3
        on c4.loan_no=c3.business_id
        ) c5
        group by quota_apply_id
) t4
on t1.quota_apply_id=t4.quota_apply_id
--首逾天数:当前首逾天数，历史最大首逾天数----------------------------------------------------------
left join
(
        select 
        quota_apply_id,
        max(fpd) as fpd,
        max(fpd_ever) as fpd_ever
        from
        (
        select 
        a1.*,a2.*
        from 
        adm.adm_credit_loan_apply_doc a1
        left join
        (
        select 
        c1.business_id,
        (case when (overdue_day_calc>0 and latest_actual_repay_date is null) then overdue_day_calc else 0 end) as fpd,--当前首逾天数
        c1.overdue_day_calc as fpd_ever--历史首逾天数
        from 
        adm.adm_credit_rpt_risk_overdue_bill c1
        where periods=1
        ) a2
        on a1.loan_no=a2.business_id
        ) a3
        group by quota_apply_id
) t5
on t1.quota_apply_id=t5.quota_apply_id"""

overday_df = spark.sql(overdue_sql).toPandas()


# 构建借款者实体
def make_borrower_entity():
    shouxin_zhiyong_df = pd.merge(apply_loan_df, zhiyong_loan_df[
        ["quota_apply_id", "apply_id", "apply_status_risk", "loan_status", "loan_amount", "repayment_principal"]],
                                  how='left', on='quota_apply_id')

    borrower_basic_df = shouxin_zhiyong_df[
        ["name", "uus_id", "employee_no", "identity_no", "sex", "age", "zociac", "educate_level", "marital_status",
         "city", "access_role", "entry_date",
         "resign_date", "on_job_status", "current_working_days", "uc_job_level_name", "store_city", "apply_id",
         "team_code", "shop_code", "area_code", "marketing_code", "region_code"]]

    borrower = shouxin_zhiyong_df.groupby("identity_no")

    borrower_ext_df = pd.DataFrame([], columns=["identity_no", "累计贷款笔数", "未结清贷款笔数", "累计贷款金额", "当前贷款余额"])
    idx = 0

    for group, df in borrower:
        loans_cnt = df[(~pd.isnull(df.apply_id)) & (df.apply_status_risk_y == "放款成功")].apply_id.count()

        unclosed_loans_cnt = df[(~pd.isnull(df.apply_id)) & (df.apply_status_risk_y == "放款成功") & (
                df.loan_status == "REPAYING")].apply_id.count()

        loans_amt = df[(~pd.isnull(df.apply_id)) & (df.apply_status_risk_y == "放款成功")].loan_amount_y.sum()

        unpayed_amt = loans_amt - df[
            (~pd.isnull(df.apply_id)) & (df.apply_status_risk_y == "放款成功")].repayment_principal.sum()

        borrower_ext_df.loc[idx] = {"identity_no": group, "累计贷款笔数": loans_cnt, "未结清贷款笔数": unclosed_loans_cnt,
                                    "累计贷款金额": loans_amt, "当前贷款余额": unpayed_amt}

        idx += 1

    borrower_basic_df.drop_duplicates(borrower_basic_df.columns, keep='first', inplace=True)

    borrower_entity_df = pd.merge(borrower_basic_df, borrower_ext_df, on="identity_no")

    borrower_entity_df = borrower_entity_df.fillna(0)

    overday_gp = overday_df[(~pd.isnull(overday_df.overdue_days_now))].groupby("identity_no")["overdue_days_now"].max()
    overday_now_df = pd.DataFrame({"identity_no": overday_gp.index, "overdue_days_now": overday_gp.values})

    borrower_entity_df = pd.merge(borrower_entity_df, overday_now_df, how="left", on="identity_no")

    his_overday_gp = overday_df[(~pd.isnull(overday_df.his_max_overdue_days))].groupby("identity_no")[
        "his_max_overdue_days"].max()
    his_overday_df = pd.DataFrame({"identity_no": his_overday_gp.index, "his_max_overdue_days": his_overday_gp.values})

    borrower_entity_df = pd.merge(borrower_entity_df, his_overday_df, how="left", on="identity_no")

    borrower_entity_df = borrower_entity_df.fillna(0)

    borrower_entity_df["tag"] = ""

    for idx in borrower_entity_df.index:

        max_overday = borrower_entity_df.loc[idx, "overdue_days_now"]

        his_max_overday = borrower_entity_df.loc[idx, "his_max_overdue_days"]

        loan_amt = borrower_entity_df.loc[idx, "累计贷款金额"]

        job_status = borrower_entity_df.loc[idx, "on_job_status"]

        tag = borrower_entity_df.loc[idx, "tag"]

        if his_max_overday > 90:
            tag = tag + ",坏客户"

        if max_overday > 30:
            tag = tag + ",首逾30+"

        if job_status == "离职":
            tag = tag + ",离职"

        if loan_amt > 0:
            tag = tag + ",放款"
        else:
            tag = tag + ",未放款"

        p = tag.find(",")
        if p == 0:
            tag = tag[1:]

        borrower_entity_df.loc[idx, "tag"] = tag

    borrower_entity_df.drop(["apply_id"], axis=1, inplace=True)

    borrower_entity_df.drop_duplicates(borrower_entity_df.columns, inplace=True)

    return borrower_entity_df


borrower_entity_df = make_borrower_entity()

borrower_entity_df.columns = ["姓名", "uus_id", "员工号", "身份证号", "性别", "年龄", "星座", "教育程度", "婚姻状态", "城市", "角色", "入职日期",
                              "离职日期",
                              "当前在职状态", "当前在职天数", "当前职级", "门店所在城市", "team_code", "shop_code", "area_code",
                              "marketing_code", "region_code",
                              "累计贷款笔数", "未结清贷款笔数", "累计贷款金额", "当前贷款余额", "当前逾期天数", "历史最大逾期天数", "tag"]


# 构建联系人实体
def make_contact_entity():
    contact_df = spark.sql("select * from credit_loan_api_service.personal_contact_info").toPandas()
    contact_df = contact_df[contact_df.product_id == "ELOAN_AGENT"]

    contact_df = contact_df[["contact_name", "contact_way", "contact_relationship", "uid"]]

    contact_df.columns = ["姓名", "联系方式", "关系", "uid"]

    contact_df.drop_duplicates(contact_df.columns, inplace=True)

    return contact_df


contact_entity_df = make_contact_entity()

contact_entity_df["ext_id"] = contact_entity_df["姓名"] + contact_entity_df["联系方式"] + contact_entity_df["关系"] + \
                              contact_entity_df["uid"]

contact_entity_df.ext_id = contact_entity_df.ext_id.apply(lambda x: make_md5(x))


# 构建地址实体
def make_address_entity():

    address_df = spark.sql("select * from credit_loan_api_service.credit_personal_info").toPandas()
    address_df = address_df[address_df.product_id == "ELOAN_AGENT"]

    address_df = address_df[["address", "province", "city", "district", "uid"]]

    address_df.columns = ["地址", "省份", "城市", "区", "uid"]

    address_df.drop_duplicates(address_df.columns, inplace=True)

    return address_df


address_entity_df = make_address_entity()


# 构建手机实体
def make_phone_entity():

    phones_df = apply_loan_df[["uus_id", "telephone"]]
    phones_df = pd.concat([phones_df, zhiyong_loan_df[["uus_id", "telephone"]]])

    phones_df = pd.merge(borrower_entity_df[["uus_id"]], phones_df, how="left", on="uus_id")

    phones_df = phones_df[~pd.isnull(phones_df.telephone)]

    phones_df["tag"] = "借款人"

    contact_phones_df = contact_entity_df[["uid", "联系方式"]]

    contact_phones_df.rename(columns={"uid": "uus_id", "联系方式": "telephone"}, inplace=True)

    contact_phones_df = contact_phones_df[~pd.isnull(contact_phones_df.telephone)]

    contact_phones_df["tag"] = "联系人"

    phones_df = pd.concat([phones_df, contact_phones_df])

    phones_df.rename(columns={"telephone": "手机号"}, inplace=True)

    phones_df.drop_duplicates(phones_df.columns, keep='first', inplace=True)

    return phones_df


phones_entity_df = make_phone_entity()


# 构建团队，门店，区域，市场，大区实体
def build_teams(code):

    team_gp = borrower_entity_df.groupby(code)

    team_df = pd.DataFrame([], columns=["编号", "名称", "放款总人数", "放款总金额", "当前总贷款余额", "总坏客户人数"])

    idx = 0

    for group, df in team_gp:

        loan_cnt = df[df["累计贷款笔数"] > 0]["累计贷款笔数"].count()

        loan_amt = df["累计贷款金额"].sum()

        unpaid_amt = df["当前贷款余额"].sum()

        bad_cnt = df[df.tag.str.contains("坏客户")]["身份证号"].count()

        team_df.loc[idx] = {"编号": group, "名称": "", "放款总人数": loan_cnt, "放款总金额": loan_amt,
                            "当前总贷款余额": unpaid_amt, "总坏客户人数": bad_cnt}

        idx += 1

    team_df.drop_duplicates(team_df.columns, inplace=True)

    return team_df


def make_shop_entity():
    shop_df = build_teams("shop_code")

    shop_df = shop_df[(shop_df["编号"].str.strip().str.len() > 0) & (shop_df["编号"]!=0)]

    shop_address_df = spark.sql("select shop_id, shop_code, shop_name, address, city_name from spark_dw.dw_ke_bkjf_shh_house_shop_base_da").toPandas()

    shop_df = pd.merge(shop_df, shop_address_df[["shop_code", "shop_name", "address", "city_name"]],
                       how = "left", left_on="编号", right_on="shop_code")

    shop_df["名称"] = shop_df.shop_name
    shop_df.drop(["shop_name", "shop_code"], axis=1, inplace=True)

    shop_df.rename(columns={"address": "地址", "city_name": "城市"}, inplace=True)

    shop_df.drop_duplicates(shop_df.columns, inplace=True)

    return shop_df


def make_group_entity(group):

    team_df = build_teams(group + "_code")

    team_df = team_df[(team_df["编号"].str.strip().str.len() > 0) & (team_df["编号"]!=0)]

    tmp_df = apply_loan_df[[group + "_code", group + "_name"]]

    team_df = pd.merge(team_df, tmp_df, how="left", left_on="编号", right_on=group + "_code")

    team_df["名称"] = team_df[group + "_name"]

    team_df.drop([group + "_code", group + "_name"], axis=1, inplace=True)

    team_df.drop_duplicates(team_df.columns, inplace=True)

    return team_df


team_df = make_group_entity("team")
team_df['tag'] = np.where(team_df['总坏客户人数'] > 1, '高风险组', '正常组')

shop_entity_df = make_shop_entity()
shop_entity_df['tag'] = np.where(shop_entity_df['总坏客户人数'] > 2, '高风险门店', '正常门店')

area_df = make_group_entity("area")

marketing_df = make_group_entity("marketing")

region_df = make_group_entity("region")


# 构建设备ip实体
def make_device_ip():
    ip_df = spark.sql("""select ip, udid, union_id, event_time from credit_biz_metrics.device_fingerprint 
    where date(event_time)>=date('2020-08-24') and udid!='2408c710977177815f01fbc344dedc8b'""").toPandas()

    ip_df.sort_values(by="event_time", inplace=True)
    ip_df.drop_duplicates(list(set(ip_df.columns).difference({"event_time"})), keep='first', inplace=True)

    return ip_df


ip_df = make_device_ip()


# 构建设备实体
def make_device_entity():
    device_df = spark.sql("""select udid, union_id, imei, idfa, meid, event_time from credit_biz_metrics.device_fingerprint  
where date(event_time)>=date('2020-08-24') and udid!='2408c710977177815f01fbc344dedc8b'""").toPandas()

    device_df.sort_values(by="event_time", inplace=True)
    device_df.drop_duplicates(list(set(device_df.columns).difference({"event_time"})), keep='first', inplace=True)

    return device_df


device_df = make_device_entity()


# 构建借款者-联系人关系
def make_borrower_contact():

    borrower_contact_df = pd.merge(borrower_entity_df[["uus_id"]], contact_entity_df, left_on="uus_id", right_on="uid")[["uus_id", "关系", "uid", "ext_id"]]

    borrower_contact_df.rename(columns={"uus_id": "Left", "关系": "Type", "ext_id": "Right"}, inplace=True)

    borrower_contact_df = borrower_contact_df[["Left", "Type", "Right"]]

    borrower_contact_df.drop_duplicates(borrower_contact_df.columns, inplace=True)

    return borrower_contact_df


borrower_contact_df = make_borrower_contact()


# 构建借款者-手机关系
def make_borrower_phones():

    borrower_phones = phones_entity_df[phones_entity_df.tag == "借款人"]

    borrower_phones.rename(columns={"uus_id": "Left", "手机号": "Right"}, inplace=True)

    borrower_phones["Type"] = "借款人号码"

    borrower_phones = borrower_phones[["Left", "Type", "Right"]]

    borrower_phones.drop_duplicates(borrower_phones.columns, inplace=True)

    return borrower_phones


borrower_phones_df = make_borrower_phones()


# 构建联系人-手机关系
def make_contact_phones():

    contact_phones = phones_entity_df[phones_entity_df.tag == "联系人"]

    contact_phones.rename(columns={"uus_id": "Left", "手机号": "Right"}, inplace=True)

    contact_phones["Type"] = "联系人号码"

    contact_phones = contact_phones[["Left", "Type", "Right"]]

    contact_phones.drop_duplicates(contact_phones.columns, inplace=True)

    return contact_phones


contact_phones_df = make_contact_phones()


# 构建借款人-地址关系
def make_borrower_address():

    borrower_address = pd.merge(borrower_entity_df[["uus_id"]], address_entity_df["uid"], left_on="uus_id", right_on="uid")

    borrower_address["Type"] = "居住"

    borrower_address.rename(columns={"uus_id": "Left", "uid": "Right"}, inplace=True)

    borrower_address = borrower_address[["Left", "Type", "Right"]]

    borrower_address.drop_duplicates(borrower_address.columns, inplace=True)

    return borrower_address


borrower_address_df = make_borrower_address()


# 构建借款者-团队关系
def make_borrower_team():

    tmp_gp = zhiyong_loan_df.groupby(["identity_no", "team_code"])

    borrower_team = pd.DataFrame([], columns=['Left', 'Type', 'Right', '放款时间', '放款状态'])
    idx = 0

    for group, df in tmp_gp:
        loans = df[(~pd.isnull(df.apply_id)) & (df.apply_status_risk=="放款成功")]
        if loans.shape[0] == 0:
            borrower_team.loc[idx] = {"Left": group[0], "Type": "所属团队", "Right": group[1], "放款时间": "", "放款状态": df.apply_status_risk.values[0]}
            idx += 1
            continue

        min_loan_time = loans.loan_success_time.min()

        team_code = loans[loans.loan_success_time == min_loan_time].team_code.values[0]

        borrower_team.loc[idx] = {"Left": group[0], "Type": "所属团队", "Right": team_code, "放款时间": min_loan_time, "放款状态": "放款成功"}
        idx += 1

    borrower_team.drop_duplicates(borrower_team.columns, keep='first', inplace=True)

    apply_no_zhiyong = pd.merge(borrower_entity_df[["身份证号", "team_code"]], borrower_team["Left"], how="left", left_on="身份证号", right_on="Left")
    apply_no_zhiyong = apply_no_zhiyong[pd.isnull(apply_no_zhiyong.Left)]
    apply_no_zhiyong.drop_duplicates(apply_no_zhiyong.columns, inplace=True)
    apply_no_zhiyong.drop(["Left"], axis=1, inplace=True)

    apply_no_zhiyong.rename(columns={"身份证号": "Left", "team_code": "Right"}, inplace=True)
    apply_no_zhiyong["Type"] = "所属团队"
    apply_no_zhiyong["放款时间"] = ""
    apply_no_zhiyong["放款状态"] = "未支用"

    apply_no_zhiyong = apply_no_zhiyong[["Left", "Type", "Right", "放款时间", "放款状态"]]

    return pd.concat([borrower_team, apply_no_zhiyong])


borrower_team = make_borrower_team()


# 构建团队-门店关系
def make_team_shop():

    tmp_gp = zhiyong_loan_df.groupby(["team_code", "shop_code"])

    team_shop = pd.DataFrame([], columns=['Left', 'Type', 'Right', '放款时间', '放款状态'])
    idx = 0

    for group, df in tmp_gp:
        if pd.isnull(group):
            continue

        loans = df[(~pd.isnull(df.apply_id)) & (df.apply_status_risk=="放款成功")]
        if loans.shape[0] == 0:
            team_shop.loc[idx] = {"Left": group[0], "Type": "所属门店", "Right": group[1], "放款时间": "", "放款状态": ",".join(df.apply_status_risk.unique())}
            idx += 1
            continue

        min_loan_time = loans.loan_success_time.min()

        shop_code = loans[loans.loan_success_time == min_loan_time].shop_code.values[0]

        team_shop.loc[idx] = {"Left": group[0], "Type": "所属门店", "Right": shop_code, "放款时间": min_loan_time, "放款状态": "放款成功"}
        idx += 1

    tmp_df = pd.merge(team_df, borrower_entity_df[['team_code', 'shop_code']], how="left", left_on="编号", right_on="team_code")
    tmp_df.drop_duplicates(tmp_df.columns, inplace=True)

    apply_no_zhiyong = pd.merge(tmp_df[["编号", 'shop_code']], team_shop["Left"], how="left", left_on="编号", right_on="Left")
    apply_no_zhiyong = apply_no_zhiyong[pd.isnull(apply_no_zhiyong.Left)]
    apply_no_zhiyong.drop_duplicates(apply_no_zhiyong.columns, inplace=True)
    apply_no_zhiyong.drop(["Left"], axis=1, inplace=True)

    apply_no_zhiyong.rename(columns={"编号": "Left", "shop_code": "Right"}, inplace=True)
    apply_no_zhiyong["Type"] = "所属门店"
    apply_no_zhiyong["放款时间"] = ""
    apply_no_zhiyong["放款状态"] = "未支用"

    apply_no_zhiyong = apply_no_zhiyong[["Left", "Type", "Right", "放款时间", "放款状态"]]

    return pd.concat([team_shop, apply_no_zhiyong])


team_shop = make_team_shop()


# 构建门店-区域关系
def make_shop_area():

    tmp_gp = zhiyong_loan_df.groupby(["shop_code", "area_code"])

    shop_area = pd.DataFrame([], columns=['Left', 'Type', 'Right', '放款时间', '放款状态'])
    idx = 0

    for group, df in tmp_gp:
        if pd.isnull(group):
            continue

        loans = df[(~pd.isnull(df.apply_id)) & (df.apply_status_risk=="放款成功")]
        if loans.shape[0] == 0:
            shop_area.loc[idx] = {"Left": group[0], "Type": "所属区域", "Right": group[1], "放款时间": "", "放款状态": ",".join(df.apply_status_risk.unique())}
            idx += 1
            continue

        min_loan_time = loans.loan_success_time.min()

        area_code = loans[loans.loan_success_time == min_loan_time].area_code.values[0]

        shop_area.loc[idx] = {"Left": group[0], "Type": "所属区域", "Right": area_code, "放款时间": min_loan_time, "放款状态": "放款成功"}
        idx += 1

    tmp_df = pd.merge(shop_entity_df, borrower_entity_df[['shop_code','area_code']], how="left", left_on="编号", right_on="shop_code")
    tmp_df.drop_duplicates(tmp_df.columns, inplace=True)

    apply_no_zhiyong = pd.merge(tmp_df[["编号", 'area_code']], shop_area["Left"], how="left", left_on="编号", right_on="Left")
    apply_no_zhiyong = apply_no_zhiyong[pd.isnull(apply_no_zhiyong.Left)]
    apply_no_zhiyong.drop_duplicates(apply_no_zhiyong.columns, inplace=True)
    apply_no_zhiyong.drop(["Left"], axis=1, inplace=True)

    apply_no_zhiyong.rename(columns={"编号": "Left", "area_code": "Right"}, inplace=True)
    apply_no_zhiyong["Type"] = "所属区域"
    apply_no_zhiyong["放款时间"] = ""
    apply_no_zhiyong["放款状态"] = "未支用"

    apply_no_zhiyong = apply_no_zhiyong[["Left", "Type", "Right", "放款时间", "放款状态"]]

    return pd.concat([shop_area, apply_no_zhiyong])


shop_area = make_shop_area()


# 构建区域-市场关系
def make_area_marketing():

    tmp_gp = zhiyong_loan_df.groupby(["area_code", "marketing_code"])

    area_marketing = pd.DataFrame([], columns=['Left', 'Type', 'Right', '放款时间', '放款状态'])
    idx = 0

    for group, df in tmp_gp:
        if pd.isnull(group):
            continue

        loans = df[(~pd.isnull(df.apply_id)) & (df.apply_status_risk=="放款成功")]
        if loans.shape[0] == 0:
            area_marketing.loc[idx] = {"Left": group[0], "Type": "所属市场", "Right": group[1], "放款时间": "", "放款状态": ",".join(df.apply_status_risk.unique())}
            idx += 1
            continue

        min_loan_time = loans.loan_success_time.min()

        marketing_code = loans[loans.loan_success_time == min_loan_time].marketing_code.values[0]

        area_marketing.loc[idx] = {"Left": group[0], "Type": "所属市场", "Right": marketing_code, "放款时间": min_loan_time, "放款状态": "放款成功"}
        idx += 1

    tmp_df = pd.merge(area_df, borrower_entity_df[['area_code','marketing_code']], how="left", left_on="编号", right_on="area_code")
    tmp_df.drop_duplicates(tmp_df.columns, inplace=True)

    apply_no_zhiyong = pd.merge(tmp_df[["编号", 'marketing_code']], area_marketing["Left"], how="left", left_on="编号", right_on="Left")
    apply_no_zhiyong = apply_no_zhiyong[pd.isnull(apply_no_zhiyong.Left)]
    apply_no_zhiyong.drop_duplicates(apply_no_zhiyong.columns, inplace=True)
    apply_no_zhiyong.drop(["Left"], axis=1, inplace=True)

    apply_no_zhiyong.rename(columns={"编号": "Left", "marketing_code": "Right"}, inplace=True)
    apply_no_zhiyong["Type"] = "所属市场"
    apply_no_zhiyong["放款时间"] = ""
    apply_no_zhiyong["放款状态"] = "未支用"

    apply_no_zhiyong = apply_no_zhiyong[["Left", "Type", "Right", "放款时间", "放款状态"]]

    return pd.concat([area_marketing, apply_no_zhiyong])


area_marketing = make_area_marketing()


# 构建市场-大区关系
def make_marketing_region():

    tmp_gp = zhiyong_loan_df.groupby(["marketing_code", "region_code"])

    marketing_region = pd.DataFrame([], columns=['Left', 'Type', 'Right', '放款时间', '放款状态'])
    idx = 0

    for group, df in tmp_gp:
        if pd.isnull(group):
            continue

        loans = df[(~pd.isnull(df.apply_id)) & (df.apply_status_risk=="放款成功")]
        if loans.shape[0] == 0:
            marketing_region.loc[idx] = {"Left": group[0], "Type": "所属大区", "Right": group[1], "放款时间": "", "放款状态": ",".join(df.apply_status_risk.unique())}
            idx += 1
            continue

        min_loan_time = loans.loan_success_time.min()

        region_code = loans[loans.loan_success_time == min_loan_time].region_code.values[0]

        marketing_region.loc[idx] = {"Left": group[0], "Type": "所属大区", "Right": region_code, "放款时间": min_loan_time, "放款状态": "放款成功"}
        idx += 1

    tmp_df = pd.merge(marketing_df, borrower_entity_df[['marketing_code','region_code']], how="left", left_on="编号", right_on="marketing_code")
    tmp_df.drop_duplicates(tmp_df.columns, inplace=True)

    apply_no_zhiyong = pd.merge(tmp_df[["编号", 'region_code']], marketing_region["Left"], how="left", left_on="编号", right_on="Left")
    apply_no_zhiyong = apply_no_zhiyong[pd.isnull(apply_no_zhiyong.Left)]
    apply_no_zhiyong.drop_duplicates(apply_no_zhiyong.columns, inplace=True)
    apply_no_zhiyong.drop(["Left"], axis=1, inplace=True)

    apply_no_zhiyong.rename(columns={"编号": "Left", "region_code": "Right"}, inplace=True)
    apply_no_zhiyong["Type"] = "所属大区"
    apply_no_zhiyong["放款时间"] = ""
    apply_no_zhiyong["放款状态"] = "未支用"

    apply_no_zhiyong = apply_no_zhiyong[["Left", "Type", "Right", "放款时间", "放款状态"]]

    return pd.concat([marketing_region, apply_no_zhiyong])


marketing_region = make_marketing_region()


# 构建借款者-设备ip关系
def get_borrower_ip():

    borrower_ip_df = pd.merge(borrower_entity_df["uus_id"], ip_df, how="left", left_on="uus_id", right_on="union_id")

    borrower_ip_df = borrower_ip_df[~pd.isnull(borrower_ip_df.union_id)]

    borrower_ip_df = borrower_ip_df[["uus_id", "udid", "event_time"]]

    borrower_ip_df.rename(columns={"uus_id": "Left", "udid": "Right"}, inplace=True)

    borrower_ip_df["Type"] = "ip地址"

    borrower_ip_df = borrower_ip_df[["Left", "Type", "Right", "event_time"]]

    borrower_ip_df.sort_values(by="event_time", inplace=True)

    borrower_ip_df.drop_duplicates(["Left", "Type", "Right"], inplace=True)

    return borrower_ip_df[~pd.isnull(borrower_ip_df.Right)]


borrower_ip_df = get_borrower_ip()


# 构建借款人-设备关系
def get_borrower_device():

    borrower_device_df = pd.merge(borrower_entity_df["uus_id"], device_df, how="left", left_on="uus_id", right_on="union_id")

    borrower_device_df = borrower_device_df[~pd.isnull(borrower_device_df.union_id)]

    borrower_device_df.rename(columns={"uus_id": "Left", "udid": "Right"}, inplace=True)

    borrower_device_df["Type"] = "使用设备"

    borrower_device_df = borrower_device_df[["Left", "Type", "Right", "event_time"]]

    borrower_device_df.sort_values(by="event_time", inplace=True)

    borrower_device_df.drop_duplicates(["Left", "Type", "Right"], inplace=True)

    return borrower_device_df[~pd.isnull(borrower_device_df.Right)]


borrower_device_df = get_borrower_device()


# 解析借款人实体schema并存储
borrower_entity_df.drop(["team_code", "shop_code", "area_code", "marketing_code", "region_code"], axis=1, inplace=True)

borrower_entity_df["graph_label"] = "借款用户"

borrower_schema = make_node_schema("借款用户", borrower_entity_df,
                                   comp_index_properties = ["身份证号", "uus_id"],
                                   mix_index_properties = ["tag", "员工号", "姓名", '性别', '年龄', '星座', '教育程度', '婚姻状态', '城市',
                                                           '角色', '入职日期', '离职日期', '当前在职状态', '当前在职天数', '当前职级', '门店所在城市', '累计贷款笔数',
                                                           '未结清贷款笔数', '累计贷款金额', '当前贷款余额', '当前逾期天数', '历史最大逾期天数'])

borrower_schema['propertyKeys'][5]["dataType"] = "Float"
borrower_schema['propertyKeys'][14]["dataType"] = "Float"
borrower_schema['propertyKeys'][17]["dataType"] = "Float"
borrower_schema['propertyKeys'][18]["dataType"] = "Float"
borrower_schema['propertyKeys'][19]["dataType"] = "Float"
borrower_schema['propertyKeys'][20]["dataType"] = "Float"
borrower_schema['propertyKeys'][21]["dataType"] = "Float"
borrower_schema['propertyKeys'][22]["dataType"] = "Float"

borrower_mapper = make_node_mapper("借款用户", borrower_entity_df)

dump_schema(borrower_schema, borrower_mapper, "borrower")

borrower_entity_df.to_csv(graph_type + "borrower/gra_借款用户.csv", sep=',', header=True, index=False)


# 解析联系人实体schema并存储
contact_entity_df["tag"] = "联系人"
contact_entity_df["graph_label"] = "联系人"

contact_schema = make_node_schema("联系人", contact_entity_df,
                                  comp_index_properties = ["uid", "联系方式", "ext_id"],
                                  mix_index_properties = ["姓名", '关系', 'tag'])

contact_mapper = make_node_mapper("联系人", contact_entity_df)

dump_schema(contact_schema, contact_mapper, "contact")

contact_entity_df.to_csv(graph_type + "contact/gra_联系人.csv", sep=',', header=True, index=False)


# 解析手机实体schema并存储
phones_entity_df["graph_label"] = "手机"

phones_schema = make_node_schema("手机", phones_entity_df,
                                 comp_index_properties = ["uus_id", "手机号"],
                                 mix_index_properties = ["tag"])

phones_mapper = make_node_mapper("手机", phones_entity_df)

dump_schema(phones_schema, phones_mapper, "phone")

phones_entity_df.to_csv(graph_type + "phone/gra_手机.csv", sep=',', header=True, index=False)


# 解析地址实体schema并存储
address_entity_df["tag"] = "地址"
address_entity_df["graph_label"] = "地址"

address_schema = make_node_schema("地址", address_entity_df,
                                  comp_index_properties = ["uid"],
                                  mix_index_properties = ["地址", "省份", "城市", "区", "tag"])

address_mapper = make_node_mapper("地址", address_entity_df)

dump_schema(address_schema, address_mapper, "address")

address_entity_df.to_csv(graph_type + "address/gra_地址.csv", sep=',', header=True, index=False)


# 解析团队实体schema并存储
team_df["graph_label"] = "团队"

team_schema = make_node_schema("团队", team_df,
                               comp_index_properties = ["编号"],
                               mix_index_properties = ["名称", "放款总人数", "放款总金额", "当前总贷款余额", "总坏客户人数", "tag"])

team_schema['propertyKeys'][2]["dataType"] = "Float"
team_schema['propertyKeys'][3]["dataType"] = "Float"
team_schema['propertyKeys'][4]["dataType"] = "Float"
team_schema['propertyKeys'][5]["dataType"] = "Float"

team_mapper = make_node_mapper("团队", team_df)

dump_schema(team_schema, team_mapper, "team")

team_df.to_csv(graph_type + "team/gra_团队.csv", sep=',', header=True, index=False)


# 解析门店实体schema并存储
shop_entity_df["graph_label"] = "门店"

shop_schema = make_node_schema("门店", shop_entity_df,
                               comp_index_properties = ["编号"],
                               mix_index_properties = ["名称", "放款总人数", "放款总金额", "当前总贷款余额", "总坏客户人数", "地址", "城市", "tag"])

shop_schema['propertyKeys'][2]["dataType"] = "Float"
shop_schema['propertyKeys'][3]["dataType"] = "Float"
shop_schema['propertyKeys'][4]["dataType"] = "Float"
shop_schema['propertyKeys'][5]["dataType"] = "Float"

shop_mapper = make_node_mapper("门店", shop_entity_df)

dump_schema(shop_schema, shop_mapper, "shop")

shop_entity_df.to_csv(graph_type + "shop/gra_门店.csv", sep=',', header=True, index=False)


# 解析区域实体schema并存储
area_df["tag"] = "区域"
area_df["graph_label"] = "区域"

area_schema = make_node_schema("区域", area_df,
                               comp_index_properties=["编号"],
                               mix_index_properties=["名称", "放款总人数", "放款总金额", "当前总贷款余额", "总坏客户人数", "tag"])

area_schema['propertyKeys'][2]["dataType"] = "Float"
area_schema['propertyKeys'][3]["dataType"] = "Float"
area_schema['propertyKeys'][4]["dataType"] = "Float"
area_schema['propertyKeys'][5]["dataType"] = "Float"

area_mapper = make_node_mapper("区域", area_df)

dump_schema(area_schema, area_mapper, "area")

area_df.to_csv(graph_type + "area/gra_区域.csv", sep=',', header=True, index=False)


# 解析市场实体schema并存储
marketing_df["tag"] = "市场"
marketing_df["graph_label"] = "市场"

marketing_schema = make_node_schema("市场", marketing_df,
                                    comp_index_properties=["编号"],
                                    mix_index_properties=["名称", "放款总人数", "放款总金额", "当前总贷款余额", "总坏客户人数", "tag"])

marketing_schema['propertyKeys'][2]["dataType"] = "Float"
marketing_schema['propertyKeys'][3]["dataType"] = "Float"
marketing_schema['propertyKeys'][4]["dataType"] = "Float"
marketing_schema['propertyKeys'][5]["dataType"] = "Float"

marketing_mapper = make_node_mapper("市场", marketing_df)

dump_schema(marketing_schema, marketing_mapper, "market")

marketing_df.to_csv(graph_type + "market/gra_市场.csv", sep=',', header=True, index=False)


# 解析大区实体schema并存储
region_df["tag"] = "大区"
region_df["graph_label"] = "大区"

region_schema = make_node_schema("大区", region_df,
                                 comp_index_properties=["编号"],
                                 mix_index_properties=["名称", "放款总人数", "放款总金额", "当前总贷款余额", "总坏客户人数", "tag"])

region_schema['propertyKeys'][2]["dataType"] = "Float"
region_schema['propertyKeys'][3]["dataType"] = "Float"
region_schema['propertyKeys'][4]["dataType"] = "Float"
region_schema['propertyKeys'][5]["dataType"] = "Float"

region_mapper = make_node_mapper("大区", region_df)

dump_schema(region_schema, region_mapper, "region")

region_df.to_csv("loan_agent/region/gra_大区.csv", sep=',', header=True, index=False)


# 解析设备ip实体schema并存储
ip_df["tag"] = "设备ip"
ip_df["graph_label"] = "设备ip"

ip_schema = make_node_schema("设备ip", ip_df,
                             comp_index_properties=["ip", "udid", "union_id"],
                             mix_index_properties=["event_time", "tag"])

ip_mapper = make_node_mapper("设备ip", ip_df)

dump_schema(ip_schema, ip_mapper, "ip")

ip_df.to_csv("loan_agent/ip/gra_设备ip.csv", sep=',', header=True, index=False)


# 解析设备实体schema并存储
device_df["tag"] = "设备"
device_df["graph_label"] = "设备"

device_schema = make_node_schema("设备", device_df,
                                 comp_index_properties=["udid", "union_id", "imei", "idfa", "meid"],
                                 mix_index_properties=["event_time", "tag"])

device_mapper = make_node_mapper("设备", device_df)

dump_schema(device_schema, device_mapper, "device")

device_df.to_csv("loan_agent/device/gra_设备.csv", sep=',', header=True, index=False)


# 定义关系schema
entity_relations = {
    "所属团队": [("借款用户.身份证号", "团队.编号")],
    "所属门店": [("团队.编号", "门店.编号")],
    "所属区域":  [("门店.编号", "区域.编号")],
    "所属市场": [("区域.编号", "市场.编号")],
    "所属大区": [("市场.编号", "大区.编号")],
    "联系人": [("借款用户.uus_id", "联系人.ext_id")],
    "借款人号码": [("借款用户.uus_id", "手机.手机号")],
    "联系人号码": [("联系人.uid", "手机.手机号")],
    "居住": [("借款用户.uus_id", "地址.uid")],
    "ip地址": [("借款用户.uus_id", "设备ip.udid")],
    "使用设备": [("借款用户.uus_id", "设备.udid")],
}


# 解析借款人-联系人关系schema并存储
borrower_contact_schema = make_edge_schema(borrower_contact_df)

borrower_contact_mapper = make_edge_mapper(entity_relations, borrower_contact_df, "联系人")

dump_schema(borrower_contact_schema, borrower_contact_mapper, "borrower_contact")

borrower_contact_df.to_csv("loan_agent/borrower_contact/gra_联系人.csv", sep=',', header=True, index=False)


# 解析借款人-手机号关系schema并存储
borrower_phone_schema = make_edge_schema(borrower_phones_df)

borrower_phone_mapper = make_edge_mapper(entity_relations, borrower_phones_df, "借款人号码")

dump_schema(borrower_phone_schema, borrower_phone_mapper, "borrower_phone")

borrower_phones_df.to_csv("loan_agent/borrower_phone/gra_借款人号码.csv", sep=',', header=True, index=False)


# 解析联系人-手机号关系schema并存储
contact_phones_schema = make_edge_schema(contact_phones_df)

contact_phones_mapper = make_edge_mapper(entity_relations, contact_phones_df, "联系人号码")

dump_schema(contact_phones_schema, contact_phones_mapper, "contact_phone")

contact_phones_df.to_csv("loan_agent/contact_phone/gra_联系人号码.csv", sep=',', header=True, index=False)


# 解析借款人-地址关系schema并存储
borrower_address_schema = make_edge_schema(borrower_address_df)

borrower_address_mapper = make_edge_mapper(entity_relations, borrower_address_df, "居住")

dump_schema(borrower_address_schema, borrower_address_mapper, "borrower_address")

borrower_address_df.to_csv("loan_agent/borrower_address/gra_居住.csv", sep=',', header=True, index=False)


# 解析借款人-设备ip的schema并存储
borrower_ip_schema = make_edge_schema(borrower_ip_df, relation_mix_index_properties=["event_time"])

borrower_ip_mapper = make_edge_mapper(entity_relations, borrower_ip_df, "ip地址")

dump_schema(borrower_ip_schema, borrower_ip_mapper, "borrower_ip")

borrower_ip_df.to_csv("loan_agent/borrower_ip/gra_ip地址.csv", sep=',', header=True, index=False)


# 解析借款人-设备schema并存储
borrower_device_schema = make_edge_schema(borrower_device_df, relation_mix_index_properties=["event_time"])

borrower_device_mapper = make_edge_mapper(entity_relations, borrower_device_df, "使用设备")

dump_schema(borrower_device_schema, borrower_device_mapper, "borrower_device")

borrower_device_df.to_csv("loan_agent/borrower_device/gra_使用设备.csv", sep=',', header=True, index=False)


# 解析借款人-团队关系schema并存储
borrower_team_schema = make_edge_schema(borrower_team, relation_mix_index_properties=["放款时间", "放款状态"])

borrower_team_mapper = make_edge_mapper(entity_relations, borrower_team, "所属团队")

dump_schema(borrower_team_schema, borrower_team_mapper, "borrower_team")

borrower_team.to_csv("loan_agent/borrower_team/gra_所属团队.csv", sep=',', header=True, index=False)


# 解析团队-门店关系schema并存储
team_shop_schema = make_edge_schema(team_shop, relation_mix_index_properties=["放款时间", "放款状态"])

team_shop_mapper = make_edge_mapper(entity_relations, team_shop, "所属门店")

dump_schema(team_shop_schema, team_shop_mapper, "team_shop")

team_shop.to_csv("loan_agent/team_shop/gra_所属门店.csv", sep=',', header=True, index=False)


# 解析门店-区域关系schema并存储
shop_area_schema = make_edge_schema(shop_area, relation_mix_index_properties=["放款时间", "放款状态"])

shop_area_mapper = make_edge_mapper(entity_relations, shop_area, "所属区域")

dump_schema(shop_area_schema, shop_area_mapper, "shop_area")

shop_area.to_csv("loan_agent/shop_area/gra_所属区域.csv", sep=',', header=True, index=False)


# 解析区域-市场关系schema并存储
area_marketing_schema = make_edge_schema(area_marketing, relation_mix_index_properties=["放款时间", "放款状态"])

area_marketing_mapper = make_edge_mapper(entity_relations, area_marketing, "所属市场")

dump_schema(area_marketing_schema, area_marketing_mapper, "area_market")

area_marketing.to_csv("loan_agent/area_market/gra_所属市场.csv", sep=',', header=True, index=False)


# 解析市场-大区关系schema并存储
marketing_region_schema = make_edge_schema(marketing_region, relation_mix_index_properties=["放款时间", "放款状态"])

marketing_region_mapper = make_edge_mapper(entity_relations, marketing_region, "所属大区")

dump_schema(marketing_region_schema, marketing_region_mapper, "market_region")

marketing_region.to_csv("loan_agent/market_region/gra_所属大区.csv", sep=',', header=True, index=False)

