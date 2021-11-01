package com.bkjk.kgraph.api

import com.bkjk.kgraph.hbase.src.HBaseService
import com.bkjk.kgraph.service.PluginProperties
import org.apache.tinkerpop.gremlin.process.traversal.Path
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.bkjk.kgraph.common.ReturnCode
import com.bkjk.kgraph.common.ServiceException
import com.bkjk.kgraph.service.GremlinDriver
import com.bkjk.kgraph.service.PluginService
import net.sf.json.JSONObject
import org.apache.tinkerpop.gremlin.process.computer.VertexProgram
import org.apache.tinkerpop.gremlin.process.computer.search.path.ShortestPathVertexProgram
import org.springframework.beans.factory.annotation.Autowired
import org.apache.tinkerpop.gremlin.spark.process.computer.SparkGraphComputer
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.hadoop.conf.Configuration

import javax.annotation.Resource
import java.util.stream.Collectors


class GremlinOLAPShortestPath implements PluginService {
    static final Logger logger = LoggerFactory.getLogger(GremlinOLAPShortestPath.class)

    @Autowired
    GremlinDriver pool

    @Resource
    HBaseService hBaseService

    PluginProperties properties = new PluginProperties()

    static private Map columnFamily = [
        "vertex_pairs": ["start", "end"],
        "shortest_path": ["path"]
    ]

    static List parsePath(Object obj) {
        if (obj instanceof Vertex) {
            Vertex v = (Vertex)obj
            return ["V", v.id(), v.label()]
        }

        if (obj instanceof Edge) {
            Edge e = (Edge)obj
            return ["e", e.id(), e.label()]
        }

        return ["undefined"]
    }

    static Map getInverseColumnFamily() {
        Map<String, String> inverseColumnFamily = new HashMap<>()

        for (String key: columnFamily.keySet()) {
            List values = columnFamily.get(key) as List

            inverseColumnFamily.putAll(values.stream().collect(
                    Collectors.toMap({ item -> item }, { key })) as Map)
        }

        return inverseColumnFamily
    }

    List run(JSONObject params) {
        Graph graph
        String graphName = ""

        try {
            properties.setStatus(PluginProperties.Status.RUNNING)

            System.setProperty("HADOOP_GREMLIN_LIBS", pool.getOLAPLibPath())
            //System.setProperty("hadoop.home.dir", "/data/software/hadoop-2.9.1/")

            Configuration conf = new Configuration()
            logger.info("fs.defaultFS: "+ conf.get("fs.defaultFS"))

            graphName = params.getString("graph").trim()
            graph = GraphFactory.open(pool.getOLAPConfRootPath() + graphName + "-janusgraph-hbase-server.properties")
            logger.info("Open graph[{}]", graphName)

            VertexProgram spvp = ShortestPathVertexProgram.build().includeEdges(true).create()
            def result = graph.compute(SparkGraphComputer).program(spvp).submit().get()
            List<Path> shortestPaths
            if (result.memory().exists(ShortestPathVertexProgram.SHORTEST_PATHS)) {
                shortestPaths = result.memory().get(ShortestPathVertexProgram.SHORTEST_PATHS)
            }

            //List shortestResults = shortestPaths.stream().map({path -> path.objects()}).collect(Collectors.toList())
            //logger.info("-------Shortest path: " + shortestResults.subList(0, 10) as String)

            String tableName = "kgraph:" + graphName + "_shortest_path"
            List<String> families = columnFamily.keySet().toList()

            Map inverseColumnFamily = getInverseColumnFamily()
            logger.info(inverseColumnFamily as String)

            hBaseService.createTable(tableName, families)

            int batchSize = 10000
            int currentSize = 0

            Map<String, Map<String, Object>> batchData = new HashMap<>()

            Map<String, Integer> vertex2PathId = new HashMap<>()

            for (Path path: shortestPaths) {

                List singlePath = path.objects().stream().map({o -> parsePath(o)}).collect(Collectors.toList())

                logger.info(singlePath as String)

                String vertexId = singlePath.first()[1]
                if (!vertex2PathId.containsKey(vertexId)) {
                    vertex2PathId.put(vertexId, 0)
                } else {
                    vertex2PathId.put(vertexId, vertex2PathId.get(vertexId) + 1)
                }

                String rowKey = singlePath.first()[1] + "_" + vertex2PathId.get(vertexId)

                batchData.put(rowKey, ["start": singlePath.first()[1], "end": singlePath.last()[1], "path": singlePath])

                currentSize += 1

                if (currentSize >= batchSize) {
                    hBaseService.addBatchRecords(tableName, batchData, inverseColumnFamily)

                    currentSize = 0
                    batchData.clear()
                }
            }

            if (batchData.size() > 0) {
                hBaseService.addBatchRecords(tableName, batchData, inverseColumnFamily)
            }

            properties.setStatus(PluginProperties.Status.SUCCESS)

            return ["total {} inserted into {}",shortestPaths.size(), tableName]
        } catch (Exception e) {
            properties.setStatus(PluginProperties.Status.FAILURE)

            logger.error("Fail to get shortest path for graph[{}]: {}", graphName, e)
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR.code, GremlinOLAPShortestPath.class.toString(), e)
        } finally {
            if (graph) {
                graph.close()
            }
        }
    }
}
