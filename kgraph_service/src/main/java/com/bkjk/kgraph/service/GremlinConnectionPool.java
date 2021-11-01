package com.bkjk.kgraph.service;

import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.MessageSerializer;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.driver.ser.GryoLiteMessageSerializerV1d0;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.io.gryo.GryoMapper;
import org.janusgraph.graphdb.tinkerpop.JanusGraphIoRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal;

public class GremlinConnectionPool implements ConnectionPool<GraphTraversalSource> {

    public static final Logger logger = LoggerFactory.getLogger(GremlinConnectionPool.class);

    private int maxActive;

    private long maxWait;

    private int  min_connection_pool_size;

    private int max_connection_pool_size;

    private int max_content_size;

    private LinkedBlockingQueue<GraphTraversalSource> idle = new LinkedBlockingQueue<>();

    private LinkedBlockingQueue<GraphTraversalSource> busy = new LinkedBlockingQueue<>();

    private AtomicInteger activeSize = new AtomicInteger(0);

    private AtomicBoolean isClosed = new AtomicBoolean(false);

    private AtomicInteger createCount = new AtomicInteger(0);

    public GremlinConnectionPool(int maxActive, long maxWait, int min_connection_pool_size,
                                 int max_connection_pool_size, int max_content_size) {
        this.maxActive = maxActive;
        this.maxWait = maxWait;
        this.min_connection_pool_size = min_connection_pool_size;
        this.max_connection_pool_size = max_connection_pool_size;
        this.max_content_size = max_content_size;
    }

    @Override
    public GraphTraversalSource getResource(String host, int port, String authName, String password, String graph) {
        GraphTraversalSource client;
        Long nowTime = System.currentTimeMillis();

        if ((client = idle.poll()) == null) {
            if (activeSize.get() < maxActive) {
                if (activeSize.incrementAndGet() <= maxActive) {
                    GryoMapper.Builder kryoBuilder = GryoMapper.build().addRegistry(JanusGraphIoRegistry.instance());
                    MessageSerializer serializer = new GryoLiteMessageSerializerV1d0(kryoBuilder);

                    final Cluster cluster = Cluster.build(host).port(port).serializer(serializer)
                                            .credentials(authName, password)
                                            .minConnectionPoolSize(min_connection_pool_size)
                                            .maxConnectionPoolSize(max_connection_pool_size)
                                            .maxContentLength(max_content_size).
                                            create();
                    client = traversal(MyGraphTraversalSource.class).withRemote(DriverRemoteConnection.using(cluster, graph));
                    logger.info("Thread: " + Thread.currentThread().getId() + " get connection：" + createCount.incrementAndGet());
                    busy.offer(client);
                    return client;

                } else {
                    activeSize.decrementAndGet();
                }
            }

            try {
                logger.info("Thread: " + Thread.currentThread().getId() + " wait for idle connection");
                Long waitTime = maxWait - (System.currentTimeMillis() - nowTime);
                client = idle.poll(waitTime, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException("Fail to wait for connection: " + e.getMessage());
            }

            if (client != null) {
                logger.info("Thread: " + Thread.currentThread().getId() + " get connection：" + createCount.incrementAndGet());
                busy.offer(client);
                return client;
            } else {
                logger.error("Thread: " + Thread.currentThread().getId() + " fail to get idle connection");
                throw new RuntimeException("Thread: " + Thread.currentThread().getId() + " fail to get idle connection");
            }
        }

        busy.offer(client);
        return client;
    }

    @Override
    public void release(GraphTraversalSource client) {
        if (client == null) {
            return;
        }
        if (busy.remove(client)){
            logger.info("Release traversal resource, active size is " + activeSize.get());
            idle.offer(client);

            logger.info("busy queue size: " + busy.size());
            logger.info("ilde queue size: " + idle.size());

        } else {
            activeSize.decrementAndGet();
            throw new RuntimeException("Release connection error: " + client.toString());
        }
    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            idle.forEach((client) -> {
                try {
                    client.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            });
            busy.forEach((client) -> {
                try {
                    client.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}