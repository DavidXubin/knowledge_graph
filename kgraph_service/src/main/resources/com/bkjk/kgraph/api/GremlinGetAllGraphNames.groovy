package com.bkjk.kgraph.api

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import net.sf.json.JSONObject
import com.bkjk.kgraph.service.GremlinDriver
import com.bkjk.kgraph.common.ReturnCode
import com.bkjk.kgraph.common.ServiceException
import com.bkjk.kgraph.service.PluginService
import org.apache.tinkerpop.gremlin.server.Settings

import org.springframework.beans.factory.annotation.Autowired


class GremlinGetAllGraphNames implements PluginService{
    static final Logger logger = LoggerFactory.getLogger(GremlinGetAllGraphNames.class)

    @Autowired
    GremlinDriver pool

    List run(JSONObject params) {

        try {
            String clusterConfPath = pool.getConfRootPath() + "http-gremlin-server.yaml"

            InputStream input = new FileInputStream(new File(clusterConfPath))
            if (input == null) {
                throw new RuntimeException(clusterConfPath + " can not be accessed.")
            }

            final Settings settings = Settings.read(input)
            final Set<String> graphNames = settings.graphs.keySet()

            return graphNames.toList()
        } catch (Exception e) {
            logger.error("Fail to get cluster graph names: " + e)
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR.code, GremlinGetAllGraphNames.class.toString(), e)
        }
    }
}
