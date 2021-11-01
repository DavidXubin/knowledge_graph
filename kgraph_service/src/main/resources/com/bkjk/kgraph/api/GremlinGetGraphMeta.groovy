package com.bkjk.kgraph.api

import org.janusgraph.core.EdgeLabel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import net.sf.json.JSONObject
import com.bkjk.kgraph.service.GremlinDriver
import org.janusgraph.core.JanusGraph
import org.janusgraph.core.JanusGraphFactory
import org.janusgraph.core.PropertyKey
import org.janusgraph.core.VertexLabel
import com.bkjk.kgraph.common.ReturnCode
import com.bkjk.kgraph.common.ServiceException
import com.bkjk.kgraph.service.PluginService
import org.springframework.beans.factory.annotation.Autowired
import com.google.common.collect.Lists
import java.util.stream.Collectors
import org.janusgraph.graphdb.database.management.ManagementSystem


class GremlinGetGraphMeta implements PluginService{
    static final Logger logger = LoggerFactory.getLogger(GremlinGetGraphMeta.class)

    @Autowired
    GremlinDriver pool

    List run(JSONObject params) {
        JanusGraph graph

        try {
            String graphName = params.getString("graph").trim()

            graph = JanusGraphFactory.open(pool.getConfRootPath() + graphName + "-janusgraph-hbase-server.properties")

            ManagementSystem mgmt = (ManagementSystem)graph.openManagement()
            List<VertexLabel> vertexlabels = Lists.newArrayList(mgmt.getVertexLabels())
            List<String> vertexlabelNames = vertexlabels.stream().map({ p -> p.name() }).collect(Collectors.toList())

            List<EdgeLabel> edgeLabels = Lists.newArrayList(mgmt.getRelationTypes(EdgeLabel.class))
            List<String> edgeLabelNames = edgeLabels.stream().map({ p -> p.name() }).collect(Collectors.toList())

            List<PropertyKey> properties = Lists.newArrayList(mgmt.getRelationTypes(PropertyKey.class))
            List<String> propertyNames = properties.stream().map({ p -> p.name() }).collect(Collectors.toList())

            return [vertexlabelNames, edgeLabelNames, propertyNames]
        } catch (Exception e) {
            logger.error("Fail to get cluster graph names: " + e)
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR.code, GremlinGetGraphMeta.class.toString(), e)
        } finally {
            if (graph) {
                graph.close()
            }
        }
    }
}
