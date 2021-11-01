package com.bkjk.kgraph.service;

        import org.apache.tinkerpop.gremlin.process.remote.RemoteConnection;
        import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;


public class MyGraphTraversalSource extends GraphTraversalSource {

    public RemoteConnection getConnection() {
        return connection;
    }

    public MyGraphTraversalSource(RemoteConnection connection) {
        super(connection);
    }

}
