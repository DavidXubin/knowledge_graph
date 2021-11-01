#!/bin/bash

./run.sh import /data/software/janusgraph_release/janusgraph-full-0.5.1/conf/gremlin-server/dcc-janusgraph-hbase-server.properties DCC/system DCC/system/schema.json DCC/system/datamapper.json vertex
./run.sh import /data/software/janusgraph_release/janusgraph-full-0.5.1/conf/gremlin-server/dcc-janusgraph-hbase-server.properties DCC/Interface/ DCC/Interface/schema.json DCC/Interface/datamapper.json vertex
./run.sh import /data/software/janusgraph_release/janusgraph-full-0.5.1/conf/gremlin-server/dcc-janusgraph-hbase-server.properties DCC/dcc_edge/ DCC/dcc_edge/schema.json DCC/dcc_edge/datamapper.json edge

