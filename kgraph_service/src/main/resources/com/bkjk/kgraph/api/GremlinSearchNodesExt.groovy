package com.bkjk.kgraph.api

import com.bkjk.kgraph.common.ReturnCode
import com.bkjk.kgraph.common.ServiceException
import com.bkjk.kgraph.service.GremlinDriver
import com.bkjk.kgraph.service.PluginService
import com.bkjk.kgraph.utils.Toolkit
import net.sf.json.JSONObject
import org.janusgraph.core.attribute.Text
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

import java.lang.reflect.Method
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__

class GremlinSearchNodesExt implements PluginService {

    static final Logger logger = LoggerFactory.getLogger(GremlinSearchNodesExt.class)

    @Autowired
    GremlinDriver pool

    private Map predicates = [
        "eq": null,
        "contains": Text.class.getDeclaredMethod("textContains", Object.class),
        "textContains": Text.class.getDeclaredMethod("textContains", Object.class),
        "textContainsPrefix": Text.class.getDeclaredMethod("textContainsPrefix", Object.class),
        "textContainsRegex": Text.class.getDeclaredMethod("textContainsRegex", Object.class),
        "textContainsFuzzy": Text.class.getDeclaredMethod("textContainsFuzzy", Object.class),
        "neq": P.class.getDeclaredMethod("neq", Object.class),
        "gt": P.class.getDeclaredMethod("gt", Object.class),
        "gte": P.class.getDeclaredMethod("gte", Object.class),
        "lt": P.class.getDeclaredMethod("lt", Object.class),
        "lte": P.class.getDeclaredMethod("lte", Object.class)
    ]

    private Map edge_degree_predicate_map = [
        "==": P.class.getDeclaredMethod("eq", Object.class),
        ">": P.class.getDeclaredMethod("gt", Object.class),
        ">=": P.class.getDeclaredMethod("gte", Object.class),
        "<": P.class.getDeclaredMethod("lt", Object.class),
        "<=": P.class.getDeclaredMethod("lte", Object.class),
    ]


    List run(JSONObject params) {
        String user
        String graph
        GraphTraversalSource g

        try {
            user = params.getString("user").trim()
            graph = params.getString("graph").trim() + "_g"

            def label = null
            if (params.containsKey("label")) {
                label = params.getString("label").trim()
            } else {
                throw new RuntimeException("Param error: miss label")
            }

            def property = null
            if (params.containsKey("property")) {
                property = params.getString("property").trim()
            }

            def value = null
            if (params.containsKey("value")) {
                value = params.getString("value").trim()
                if (value.length() == 0) {
                    value = null
                }
            }

            if (Toolkit.isDouble(value)) {
                value = Double.parseDouble(value)
            } else if (Toolkit.isInteger(value)) {
                value = Integer.parseInt(value)
            }

            def predicate = null
            if (params.containsKey("predicate")) {
                predicate = params.getString("predicate").trim()
            }

            def edgeName = null
            if (params.containsKey("edge_name")) {
                edgeName = params.getString("edge_name").trim()
            }

            def edgeProperty = null
            if (params.containsKey("edge_property")) {
                edgeProperty = params.getString("edge_property").trim()
            }

            def edgePredicate = null
            if (params.containsKey("edge_predicate")) {
                edgePredicate = params.getString("edge_predicate").trim()
            }

            def edgeValue = null
            if (params.containsKey("edge_value")) {
                edgeValue = params.getString("edge_value").trim()
                if (edgeValue.length() == 0) {
                    edgeValue = null
                }
            }

            if (Toolkit.isDouble(edgeValue)) {
                edgeValue = Double.parseDouble(edgeValue)
            } else if (Toolkit.isInteger(edgeValue)) {
                edgeValue = Integer.parseInt(edgeValue)
            }

            def edgeDegreeDirect = null
            if (params.containsKey("edge_degree_direct")) {
                edgeDegreeDirect = params.getString("edge_degree_direct").trim()
            } else {
                edgeDegreeDirect = "both"
            }

            def edgeDegreePredicate = null
            if (params.containsKey("edge_degree_predicate")) {
                edgeDegreePredicate = params.getString("edge_degree_predicate").trim()
            } else {
                edgeDegreePredicate = ">="
            }

            int edgeDegreeNum = 0
            if (params.containsKey("edge_degree_num")) {
                edgeDegreeNum = params.getInt("edge_degree_num")
            }

            int startIdx = 0
            if (params.containsKey("start_idx")) {
                startIdx = params.getInt("start_idx")
            }

            int limit = pool.getPageSize()
            if (params.containsKey("limit")) {
                limit = params.getInt("limit")
                if (limit > pool.getPageSize()) {
                    limit = pool.getPageSize()
                }
            }

            int endIdx = startIdx + limit

            g = pool.get(user, graph)

            if (predicate != null && !predicates.containsKey(predicate)) {
                throw new RuntimeException("Param error: predicate is invalid: " + predicates.keySet().toList())
            }

            Method predicateMethod = (Method)edge_degree_predicate_map.get(edgeDegreePredicate)

            ArrayList<String> directParam = new ArrayList<String>()
            if (edgeName && edgeName.trim().size() > 0) {
                directParam.add(edgeName)
            }

            def data

            if (edgeName == null || edgeName.trim().size() == 0 || edgeProperty == null || edgeProperty.trim().size() == 0
                    || edgeValue == null) {
                if (value == null && edgeDegreeNum == 0) {
                    data = g.V().hasLabel(label).has("graph_label", Text.textContains(label))
                } else {
                    data = g.V().hasLabel(label)
                }
            } else {
                Method method = (Method)predicates.get(edgePredicate)

                switch (edgeDegreeDirect.trim()) {
                    case "out":
                        if (predicates.get(edgePredicate) == null) {
                            data = g.V().hasLabel(label).has("graph_label", Text.textContains(label)).
                                    as('a').outE().has(edgeName, edgeProperty, edgeValue).select('a')

                        } else {
                            data = g.V().hasLabel(label).has("graph_label", Text.textContains(label)).
                                    as('a').outE().has(edgeName, edgeProperty, method.invoke(null, edgeValue)).select('a')
                        }
                        break
                    case "in":
                        if (predicates.get(edgePredicate) == null) {
                            data = g.V().hasLabel(label).has("graph_label", Text.textContains(label)).
                                    as('a').inE().has(edgeName, edgeProperty, edgeValue).select('a')

                        } else {
                            data = g.V().hasLabel(label).has("graph_label", Text.textContains(label)).
                                    as('a').inE().has(edgeName, edgeProperty, method.invoke(null, edgeValue)).select('a')
                        }
                        break
                    case "both":
                        if (predicates.get(edgePredicate) == null) {
                            data = g.V().hasLabel(label).has("graph_label", Text.textContains(label)).
                                    as('a').bothE().has(edgeName, edgeProperty, edgeValue).select('a')

                        } else {
                            data = g.V().hasLabel(label).has("graph_label", Text.textContains(label)).
                                    as('a').bothE().has(edgeName, edgeProperty, method.invoke(null, edgeValue)).select('a')
                        }
                        break
                }
            }

            Method method = (Method)predicates.get(predicate)

            switch (edgeDegreeDirect.trim()) {
                case "out":
                    if (value == null) {
                        data = data.where(__.out((String[]) directParam.toArray()).count().
                                is(predicateMethod.invoke(null, edgeDegreeNum))).range(startIdx, endIdx).elementMap().toList()
                    } else if (predicates.get(predicate) == null) {
                        data = data.has(property, value).where(__.out((String[]) directParam.toArray()).count().
                                is(predicateMethod.invoke(null, edgeDegreeNum))).range(startIdx, endIdx).elementMap().toList()

                    } else {
                        data = data.has(property, method.invoke(null, value)).
                                where(__.out((String[]) directParam.toArray()).count().
                                is(predicateMethod.invoke(null, edgeDegreeNum))).range(startIdx, endIdx).elementMap().toList()
                    }
                    break
                case "in":
                    if (value == null) {
                        data = data.where(__.in((String[]) directParam.toArray()).count().
                                is(predicateMethod.invoke(null, edgeDegreeNum))).range(startIdx, endIdx).elementMap().toList()
                    } else if (predicates.get(predicate) == null) {
                        data = data.has(property, value).where(__.in((String[]) directParam.toArray()).count().
                                is(predicateMethod.invoke(null, edgeDegreeNum))).range(startIdx, endIdx).elementMap().toList()

                    } else {
                        data = data.has(property, method.invoke(null, value)).
                                where(__.in((String[]) directParam.toArray()).count().
                                is(predicateMethod.invoke(null, edgeDegreeNum))).range(startIdx, endIdx).elementMap().toList()
                    }
                    break
                case "both":
                    if (value == null) {
                        data = data.where(__.both((String[]) directParam.toArray()).count().
                                is(predicateMethod.invoke(null, edgeDegreeNum))).range(startIdx, endIdx).elementMap().toList()

                    } else if (predicates.get(predicate) == null) {
                        data = data.has(property, value).where(__.both((String[]) directParam.toArray()).count().
                                is(predicateMethod.invoke(null, edgeDegreeNum))).range(startIdx, endIdx).elementMap().toList()

                    } else {
                        data = data.has(property, method.invoke(null, value)).
                                where(__.both((String[]) directParam.toArray()).count().
                                is(predicateMethod.invoke(null, edgeDegreeNum))).range(startIdx, endIdx).elementMap().toList()
                    }
                    break
            }

            return data
        } catch (Exception e) {
            logger.error("Fail to search node by property: " + e)
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR.code, GremlinSearchNodesExt.class.toString(), e)
        } finally {
            pool.release(user, graph, g)
        }
    }

}

