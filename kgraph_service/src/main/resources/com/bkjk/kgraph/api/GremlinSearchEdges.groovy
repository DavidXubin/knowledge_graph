package com.bkjk.kgraph.api

import com.bkjk.kgraph.common.ReturnCode
import com.bkjk.kgraph.common.ServiceException
import com.bkjk.kgraph.service.GremlinDriver
import com.bkjk.kgraph.service.PluginService
import com.bkjk.kgraph.utils.Toolkit
import net.sf.json.JSONObject
import org.apache.tinkerpop.gremlin.structure.Direction
import org.janusgraph.core.attribute.Text
import org.janusgraph.graphdb.relations.RelationIdentifier
import org.apache.tinkerpop.gremlin.structure.T
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

import java.lang.reflect.Method
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.P

import java.util.stream.Collectors

class GremlinSearchEdges implements PluginService{
    static final Logger logger = LoggerFactory.getLogger(GremlinSearchEdges.class)

    @Autowired
    GremlinDriver pool

    private Map predicates = [
            "eq": null,
            "contains": Text.class.getDeclaredMethod("textContains", Object.class),
            "textContains": Text.class.getDeclaredMethod("textContains", Object.class),
            "textContainsPrefix": Text.class.getDeclaredMethod("textContainsPrefix", Object.class),
            "textContainsRegex": Text.class.getDeclaredMethod("textContainsRegex", Object.class),
            "textContainsFuzzy": Text.class.getDeclaredMethod("textContainsFuzzy", Object.class),
            "==": P.class.getDeclaredMethod("eq", Object.class),
            "!=": P.class.getDeclaredMethod("neq", Object.class),
            ">": P.class.getDeclaredMethod("gt", Object.class),
            ">=": P.class.getDeclaredMethod("gte", Object.class),
            "<": P.class.getDeclaredMethod("lt", Object.class),
            "<=": P.class.getDeclaredMethod("lte", Object.class)
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

            def data
            if (property == null || value == null) {
                data = g.E().hasLabel(label).range(startIdx, endIdx).elementMap().toList()
            } else if (predicate == "eq") {
                data = g.E().hasLabel(label).has(property, value).range(startIdx, endIdx).elementMap().toList()
            } else {
                if (!predicates.containsKey(predicate)) {
                    throw new RuntimeException("Param error: predicate is invalid: " + predicates.keySet().toList())
                }
                Method method = (Method)predicates.get(predicate)

                data = g.E().hasLabel(label).has(property, method.invoke(null, value)).range(startIdx, endIdx).elementMap().toList()
            }

            List edgeList = data.parallelStream().map({edge -> [
                                                                "edge_id": ((RelationIdentifier)edge.get(T.id)).getRelationId(),
                                                                "outVertexId": ((Map)edge.get(Direction.OUT)).get(T.id),
                                                                "outVertexLabel": ((Map)edge.get(Direction.OUT)).get(T.label),
                                                                "inVertexId": ((Map)edge.get(Direction.IN)).get(T.id),
                                                                "inVertexLabel": ((Map)edge.get(Direction.IN)).get(T.label),
                                                                "label": edge.get(T.label),
                                                                ] +
                    edge.entrySet().stream().filter({t -> t.getKey() != T.id && t.getKey() != Direction.OUT && t.getKey() != Direction.IN && t.getKey() != T.label}).collect(Collectors.toList())

            }).collect(Collectors.toList())

            return edgeList
        } catch (Exception e) {
            logger.error("Fail to search edge by node: " + e)
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR.code, GremlinSearchEdges.class.toString(), e)
        } finally {
            pool.release(user, graph, g)
        }
    }

}
