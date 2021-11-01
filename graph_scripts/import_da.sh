#!/bin/bash

./run.sh import /data/software/janusgraph_release/janusgraph-full-0.5.1/conf/gremlin-server/da-janusgraph-hbase-server.properties DA/app DA/app/schema.json DA/app/datamapper.json vertex
./run.sh import /data/software/janusgraph_release/janusgraph-full-0.5.1/conf/gremlin-server/da-janusgraph-hbase-server.properties DA/Interface/ DA/Interface/schema.json DA/Interface/datamapper.json vertex
./run.sh import /data/software/janusgraph_release/janusgraph-full-0.5.1/conf/gremlin-server/da-janusgraph-hbase-server.properties DA/tenant/ DA/tenant/schema.json DA/tenant/datamapper.json vertex
./run.sh import /data/software/janusgraph_release/janusgraph-full-0.5.1/conf/gremlin-server/da-janusgraph-hbase-server.properties DA/app_api/ DA/app_api/schema.json DA/app_api/datamapper.json edge
./run.sh import /data/software/janusgraph_release/janusgraph-full-0.5.1/conf/gremlin-server/da-janusgraph-hbase-server.properties DA/tenant_app/ DA/tenant_app/schema.json DA/tenant_app/datamapper.json edge

