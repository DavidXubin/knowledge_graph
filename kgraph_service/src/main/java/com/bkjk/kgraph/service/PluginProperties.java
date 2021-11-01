package com.bkjk.kgraph.service;

public class PluginProperties {

    public enum Status {
        RUNNING, STOPPED, SUCCESS, FAILURE
    }

    protected Status status = Status.STOPPED;

    public synchronized Status getStatus() {
        return status;
    }

    public synchronized void setStatus(Status _status) {
        status = _status;
    }
}
