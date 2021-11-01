package com.bkjk.kgraph.api

import com.bkjk.kgraph.common.ReturnCode
import com.bkjk.kgraph.common.ServiceException
import com.bkjk.kgraph.service.GremlinDriver
import com.bkjk.kgraph.service.PluginService
import net.sf.json.JSONObject
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__
import org.janusgraph.core.attribute.Text
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.springframework.beans.factory.annotation.Autowired

import java.lang.reflect.Method

class GremlinGetEdgeDegreeNodes implements PluginService {
    static final Logger logger = LoggerFactory.getLogger(GremlinGetEdgeDegreeNodes.class)

    @Autowired
    GremlinDriver pool

    private Map predicates = [
        "==": P.class.getDeclaredMethod("eq", Object.class),
        ">=": P.class.getDeclaredMethod("gte", Object.class),
        ">": P.class.getDeclaredMethod("gt", Object.class),
        "<=": P.class.getDeclaredMethod("lte", Object.class),
        "<": P.class.getDeclaredMethod("lt", Object.class)
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
                throw new RuntimeException("Param error: miss node label")
            }

            def edgeName = null
            if (params.containsKey("edge_name")) {
                edgeName = params.getString("edge_name").trim()
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
            if (!predicates.containsKey(edgeDegreePredicate)) {
                throw new RuntimeException("Param error: edge_degree_predicate is wrong")
            }

            Method predicateMethod = (Method)predicates.get(edgeDegreePredicate)

            ArrayList<String> directParam = new ArrayList<String>()
            if (edgeName && edgeName.trim().size() > 0) {
                directParam.add(edgeName)
            }

            def data
            switch (edgeDegreeDirect.trim()) {
                case "out":
                    data = g.V().hasLabel(label).has("graph_label", Text.textContains(label)).where(__.out((String[]) directParam.toArray()). \
                            count().is(predicateMethod.invoke(null, edgeDegreeNum))).range(startIdx, endIdx).elementMap().toList()
                    break
                case "in":
                    data = g.V().hasLabel(label).has("graph_label", Text.textContains(label)).where(__.in((String[]) directParam.toArray()).\
                            count().is(predicateMethod.invoke(null, edgeDegreeNum))).range(startIdx, endIdx).elementMap().toList()
                    break
                case "both":
                    data = g.V().hasLabel(label).has("graph_label", Text.textContains(label)).where(__.both((String[]) directParam.toArray()).\
                            count().is(predicateMethod.invoke(null, edgeDegreeNum))).range(startIdx, endIdx).elementMap().toList()
                    break
            }

            return data
        } catch (Exception e) {
            logger.error("Fail to get nodes by edge degree and direction: " + e)
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR.code, GremlinGetEdgeDegreeNodes.class.toString(), e)
        } finally {
            pool.release(user, graph, g)
        }
    }

}
