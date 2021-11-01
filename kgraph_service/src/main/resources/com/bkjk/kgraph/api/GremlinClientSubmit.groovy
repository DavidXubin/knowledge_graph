package com.bkjk.kgraph.api

import com.bkjk.kgraph.common.ReturnCode
import com.bkjk.kgraph.common.ServiceException
import com.bkjk.kgraph.service.GremlinDriver
import com.bkjk.kgraph.service.MyGraphTraversalSource
import com.bkjk.kgraph.service.PluginService
import net.sf.json.JSONObject
import org.apache.tinkerpop.gremlin.driver.Client
import org.apache.tinkerpop.gremlin.driver.Result
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

import java.lang.reflect.Field
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors

class GremlinClientSubmit implements PluginService {

    static final Logger logger = LoggerFactory.getLogger(GremlinClientSubmit.class)

    @Autowired
    GremlinDriver pool

    String processResult(String result) {
        if (result.indexOf('}') < 0) {
            return ""
        }

        int q = result.indexOf(' class=')
        if (q < 0) {
            return result
        }

        result = result.substring(0, q - 1)

        int p = result.indexOf('{')
        if (p < 0) {
            p = result.indexOf('[')

            if (p < 0) {
                return result
            }
        }

        return result.substring(p + 1, result.length())
    }

    String getKey(String item) {
        int p = item.indexOf("=")

        if (p < 0) {
            return item
        }

        return item.substring(0, p)
    }

    String getValue(String item) {
        int p = item.indexOf("=")

        if (p < 0) {
            return item
        }

        return item.substring(p + 1, item.length())
    }

    Map convertResult(String result) {

        return Arrays.stream(result.split(",")).collect(
                Collectors.toMap({ item -> getKey(item) }, { item -> getValue(item) })
        )
    }


    List run(JSONObject params) {
        String user
        String graph
        GraphTraversalSource g

        try {
            user = params.getString("user").trim()
            graph = params.getString("graph").trim() + "_g"

            def cql = null
            if (params.containsKey("content")) {
                cql = params.getString("content").trim()
            } else {
                throw new RuntimeException("Param error: miss query content")
            }

            g = (MyGraphTraversalSource)pool.get(user, graph)

            Field[] fields = g.getConnection().getClass().getDeclaredFields()

            Field clientField = Arrays.stream(fields).filter({ field -> field.getName() == "client" }).collect(Collectors.toList())[0]

            clientField.setAccessible(true)

            Client client = clientField.get(g.getConnection()) as Client

            clientField.setAccessible(false)

            CompletableFuture<List<Result>> results = client.submit(cql).all()

            String finalresults = results.get().toString()

            List<String> resultList = finalresults.split("object=")

            resultList = resultList.stream().map({ result -> processResult(result) }).
                    filter({result -> result.length() > 0}).collect(Collectors.toList())

            List jsonResults

            if (resultList[0].count("=") == resultList[0].count(",") + 1) {

                jsonResults = resultList.stream().map({ result -> convertResult(result) }).collect(Collectors.toList())

                int limit = jsonResults.size() < 100? jsonResults.size(): 100
                return [["is_table": true], jsonResults.subList(0, limit)]
            }

            int limit = resultList.size() < 100? resultList.size(): 100
            return [["is_table": false], resultList.subList(0, limit)]

        } catch (Exception e) {
            logger.error("Fail to execute remote code: " + e)
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR.code, GremlinClientSubmit.class.toString(), e)
        } finally {
            if (pool) {
                pool.release(user, graph, g)
            }
        }
    }
}
