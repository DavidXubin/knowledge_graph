package com.bkjk.kgraph.service;

public interface ConnectionPool<T> {

    T getResource(String host, int port, String authName, String password, String graph) throws Exception;

    void release(T connection) throws Exception;

    void close();
}
