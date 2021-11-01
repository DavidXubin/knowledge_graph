#!/bin/bash

./run.sh import /data/software/janusgraph_release/janusgraph-full-0.5.1/conf/gremlin-server/lineage-janusgraph-hbase-server.properties lineage/table lineage/table/schema.json lineage/table/datamapper.json vertex
./run.sh import /data/software/janusgraph_release/janusgraph-full-0.5.1/conf/gremlin-server/lineage-janusgraph-hbase-server.properties lineage/report/ lineage/report/schema.json lineage/report/datamapper.json vertex
./run.sh import /data/software/janusgraph_release/janusgraph-full-0.5.1/conf/gremlin-server/lineage-janusgraph-hbase-server.properties lineage/lineage/ lineage/lineage/schema.json lineage/lineage/datamapper.json edge
./run.sh import /data/software/janusgraph_release/janusgraph-full-0.5.1/conf/gremlin-server/lineage-janusgraph-hbase-server.properties lineage/table_report/ lineage/table_report/schema.json lineage/table_report/datamapper.json edge
