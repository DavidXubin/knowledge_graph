package com.bkjk.kgraph.api

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import net.sf.json.JSONObject
import com.bkjk.kgraph.common.ReturnCode
import com.bkjk.kgraph.common.ServiceException
import com.bkjk.kgraph.service.GremlinDriver
import com.bkjk.kgraph.service.PluginService
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.springframework.beans.factory.annotation.Autowired

import java.util.stream.Collectors

class GremlinGetCompanyInvestPath implements PluginService {

    static final Logger logger = LoggerFactory.getLogger(GremlinGetCompanyInvestPath.class)

    @Autowired
    GremlinDriver pool

    List run(JSONObject params) {
        String user
        String graph
        GraphTraversalSource g

        try {
            user = params.getString("user").trim()
            graph = params.getString("graph").trim() + "_g"


            def start_company = null
            def end_company = null
            if (params.containsKey("start_company")) {
                start_company = params.getString("start_company").trim()
            } else {
                throw new RuntimeException("Param error: miss start company")
            }

            if (params.containsKey("end_company")) {
                end_company = params.getString("end_company").trim()
            } else {
                throw new RuntimeException("Param error: miss end company")
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

            List data = g.V().has('公司名',start_company).repeat(__.out().simplePath()).
                            until(__.has('公司名',end_company)).path().by('公司名').
                            range(startIdx, endIdx).toList()

            List pathList = data.stream().map({path -> path.objects()}).collect(Collectors.toList())

             return pathList
        } catch (Exception e) {
            logger.error("Fail to get investment path: " + e)
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR.code, GremlinGetCompanyInvestPath.class.toString(), e)
        } finally {
            pool.release(user, graph, g)
        }
    }

}
