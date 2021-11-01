package com.bkjk.kgraph.service;

import com.bkjk.kgraph.common.ReturnCode;
import com.bkjk.kgraph.common.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;


@Component
public class GremlinDriver {

    private static final Logger logger = LoggerFactory.getLogger(GremlinDriver.class);

    private HashMap<String, HashMap<String, GremlinConnectionPool>> pool;

    @Value("${gdb.auth.username}")
    String authName;

    @Value("${gdb.auth.password}")
    String password;

    @Value("${gdb.host}")
    String host;

    @Value("${gdb.port}")
    int port;

    @Value("${gdb.min_connection_pool_size}")
    int min_connection_pool_size;

    @Value("${gdb.max_connection_pool_size}")
    int max_connection_pool_size;

    @Value("${gdb.max_content_size}")
    int max_content_size;

    @Value("${gremlin.pool.maxActive}")
    private int maxActive;

    @Value("${gremlin.pool.maxWait}")
    private long maxWait;

    @Value("${gdb.page.size}")
    private int page_size;

    @Value("${gdb.root_path}")
    private String root_path;

    @PostConstruct
    public void init() {
        pool = new HashMap<>();
    }

    public int getPageSize() {
        return page_size;
    }

    public String getConfRootPath() { return root_path + "/conf/gremlin-server/"; }

    public String getOLAPConfRootPath() { return root_path + "/conf/hadoop-graph/"; }

    public String getOLAPLibPath() { return root_path + "/lib/"; }

    public synchronized GraphTraversalSource get(String user, String graph) {

        GraphTraversalSource client;

        try {
            if (!pool.containsKey(user)) {
                HashMap<String, GremlinConnectionPool> graphPool = new HashMap<>();
                graphPool.put(graph, new GremlinConnectionPool(maxActive, maxWait, min_connection_pool_size,
                        max_connection_pool_size, max_content_size));

                pool.put(user, graphPool);

            } else if (!pool.get(user).containsKey(graph)) {
                HashMap<String, GremlinConnectionPool> graphPool = pool.get(user);
                graphPool.put(graph, new GremlinConnectionPool(maxActive, maxWait, min_connection_pool_size,
                        max_connection_pool_size, max_content_size));
            }

            client = pool.get(user).get(graph).getResource(host, port, authName, password, graph);
        } catch (Exception e) {
            logger.error("Fail to connect to gremlin server: " + e.getMessage(), e);
            throw new ServiceException(ReturnCode.SERVICE_ERROR, e);
        }

        return client;
    }

    public synchronized void release(String user, String graph, GraphTraversalSource client) {
        try {
            if (pool.containsKey(user) && pool.get(user).containsKey(graph)) {
                pool.get(user).get(graph).release(client);
            }
        } catch (Exception e) {
            logger.error("Fail to close gremlin server connection: " + e.getMessage(), e);
            throw new ServiceException(ReturnCode.SERVICE_ERROR, e);
        }
    }

}

