package com.bkjk.kgraph.web;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.extern.slf4j.Slf4j;

import net.sf.json.JSONObject;
import net.sf.json.JSONArray;
import java.text.SimpleDateFormat;

import com.bkjk.kgraph.model.QueryHistory;
import com.bkjk.kgraph.common.ReturnCode;
import com.bkjk.kgraph.common.ServiceException;
import com.bkjk.kgraph.common.UserPermResult;
import com.bkjk.kgraph.dao.mysql.QueryHistoryMapper;
import com.bkjk.kgraph.model.QueryHistoryExample;
import com.bkjk.kgraph.permission.MetaResult;
import com.bkjk.kgraph.permission.PermissionsManager;
import com.bkjk.kgraph.service.GremlinDriver;
import com.bkjk.kgraph.service.PluginEngine;
import com.bkjk.kgraph.common.AppResult;
import com.bkjk.kgraph.service.RedisService;
import com.bkjk.kgraph.utils.Toolkit;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.amqp.core.MessageProperties;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;



@RestController
@Slf4j
public class kgraphController {

    public static final Logger logger = LoggerFactory.getLogger(kgraphController.class);

    @Autowired
    private PluginEngine pluginEngine;

    @Autowired
    private GremlinDriver driver;

    @Autowired
    private PermissionsManager permissionsManager;

    @Autowired
    RedisService redisService;

    @Autowired
    private QueryHistoryMapper queryHistoryMapper;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Value("${gdb.auth.username}")
    private String gdbUser;

    @Value("${rabbitmq.direct_exchange_name}")
    private String mqDirectExchange;

    @Value("${rabbitmq.fanout_exchange_name}")
    private String mqFanoutExchange;

    @Value("${rabbitmq.rpc_direct_exchange_name}")
    private String mqRPCDirectExchange;


    @RequestMapping(value = "/graph/api", method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public AppResult graphAPI(@RequestBody JSONObject params) {

        try {
            if (!params.containsKey("user")) {
                throw new ServiceException(ReturnCode.SERVICE_PARAM_ERROR, "user missed");
            }
            String user = params.getString("user").trim();

            if (!params.containsKey("graph")) {
                throw new ServiceException(ReturnCode.SERVICE_PARAM_ERROR, "graph missed");
            }
            String graph = params.getString("graph").trim();

            if (!params.containsKey("token")) {
                throw new ServiceException(ReturnCode.SERVICE_PARAM_ERROR, "user token missed");
            }
            String token = params.getString("token").trim();

            String userKey = user + "_" + graph + "_token";
            String expectedToken = redisService.get(userKey, String.class);
            if (expectedToken == null || !expectedToken.equals(token)) {
                throw new ServiceException(ReturnCode.SERVICE_PERMISSION_ERROR, "permission error: " + graph);
            }

            List results = pluginEngine.run(params);
            return AppResult.ok(results);
        } catch (ServiceException e) {
            logger.error("Request /graph/api error: " + e.toString());
            return AppResult.error(500, e.toString());
        }
    }

    @RequestMapping(value = "/graph/connect", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public AppResult connectGraph(@RequestParam(value = "user", required = true) String user,
                                   @RequestParam(value = "graph", required = true) String graph) {

        GraphTraversalSource g = null;
        UserPermResult perm = new UserPermResult();
        String userKey = user + "_" + graph + "_token";

        try {
            String url = "select * from " + graph;

            MetaResult result = permissionsManager.validate(user, url, "spark", "kgraph_service");
            if (result.getError() == 0) {

                g = driver.get(user, graph + "_g");
                g.V().range(0, 1).toList();

                String token = redisService.get(userKey, String.class);

                if (token == null) {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
                    Date date = new Date(System.currentTimeMillis());

                    String rawToken = user + graph + formatter.format(date);

                    token = Toolkit.encode(Toolkit.shuffle(rawToken));

                    if (!redisService.set(userKey, token)) {
                        throw new ServiceException(ReturnCode.SERVICE_REDIS_ERROR, "user token failed");
                    }
                }
                perm.setError(0);
                perm.setMessage(graph + " connected!");
                perm.setToken(token);

            } else {
                throw new ServiceException(ReturnCode.SERVICE_PERMISSION_ERROR, result.getMessage());
            }

            return AppResult.ok(perm);
        } catch (ServiceException e) {
            logger.error("Request /graph/connect error: " + e.toString());
            return AppResult.error(500, e.toString());
        } finally {
            driver.release(user, graph + "_g", g);
        }
    }

    @RequestMapping(value = "/graph/update/plugin", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public AppResult updateGraphPlugin(@RequestParam(value = "user", required = true) String user,
                                         @RequestParam(value = "graph", required = true) String graph,
                                         @RequestParam(value = "plugin_name", required = true) String pluginName,
                                         @RequestParam(value = "plugin_content", required = true) String pluginContent,
                                         @RequestParam(value = "plugin_description", required = false) String pluginDescrpt) {

        try {
            String url = "insert into table " + graph + " values('this is virtual table only for authentication')";

            MetaResult result = permissionsManager.validate(user, url, "spark", "kgraph_service");
            if (result.getError() == 0) {
                pluginEngine.updatePlugin(user, pluginName, pluginContent, pluginDescrpt);
            } else {
                throw new ServiceException(ReturnCode.SERVICE_PERMISSION_ERROR, result.getMessage());
            }

            return AppResult.ok();
        } catch (ServiceException e) {
            logger.error("Request /graph/connect error: " + e.toString());
            return AppResult.error(500, e.toString());
        }
    }

    @RequestMapping(value = "/graph_names", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public AppResult getGraphNames(@RequestParam(value = "user", required = true) String user) {

        try {
            JSONObject params = new JSONObject();

            params.put("user", user);
            params.put("plugin_name", "getAllGraphNames");

            List results = pluginEngine.run(params);
            return AppResult.ok(results);
        } catch (ServiceException e) {
            logger.error("Request /graph/api error: " + e.toString());
            return AppResult.error(500, e.toString());
        }
    }

    @RequestMapping(value = "/graph/history_query", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public AppResult getGraphHistoryQuery(@RequestParam(value = "user", required = true) String user,
                                           @RequestParam(value = "graph", required = true) String graph,
                                           @RequestParam(value = "days", required = true) int days) {

        try {
            QueryHistoryExample example = new QueryHistoryExample();
            QueryHistoryExample.Criteria criteria = example.createCriteria();

            criteria.andUserEqualTo(user);
            criteria.andGraphEqualTo(graph);

            Date now = new Date();
            Date start_day = Toolkit.addAndSubtractDays(now, -days);
            criteria.andQueryTimeBetween(start_day, now);

            List<QueryHistory> histories = queryHistoryMapper.selectByExample(example);

            int limit = Math.min(histories.size(), 100);

            return AppResult.ok(histories.subList(histories.size() - limit, histories.size()));
        } catch (ServiceException e) {
            logger.error("Request /graph/history_query: " + e.toString());
            return AppResult.error(500, e.toString());
        }
    }

    @RequestMapping(value = "/graph/add_query", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public AppResult addGraphQuery(@RequestParam(value = "user", required = true) String user,
                                   @RequestParam(value = "graph", required = true) String graph,
                                   @RequestParam(value = "content", required = true) String content) {

        try {
            QueryHistory query = new QueryHistory();

            query.setUser(user);
            query.setGraph(graph);
            query.setContent(content);
            query.setQueryTime(new Date());

            int rowId = queryHistoryMapper.insert(query);

            return AppResult.ok(rowId);
        } catch (ServiceException e) {
            logger.error("Request /graph/add_query error: " + e.toString());
            return AppResult.error(500, e.toString());
        }
    }


    @RequestMapping(value = "/graph/olap_api", method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public AppResult graphOLAPAPI(@RequestBody JSONObject params) {
        try {
            if (!params.containsKey("user")) {
                throw new ServiceException(ReturnCode.SERVICE_PARAM_ERROR, "user missed");
            }
            String user = params.getString("user").trim();

            if (!params.containsKey("graph")) {
                throw new ServiceException(ReturnCode.SERVICE_PARAM_ERROR, "graph missed");
            }
            String graph = params.getString("graph").trim();

            if (!params.containsKey("token")) {
                throw new ServiceException(ReturnCode.SERVICE_PARAM_ERROR, "user token missed");
            }
            String token = params.getString("token").trim();

            /*
            String userKey = user + "_" + graph + "_token";
            String expectedToken = redisService.get(userKey, String.class);
            if (expectedToken == null || !expectedToken.equals(token)) {
                throw new ServiceException(ReturnCode.SERVICE_PERMISSION_ERROR, "permission error: " + graph);
            }
             */

            params.put("datetime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            byte[] bytes = params.toString().getBytes();

            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            Message message = new Message(bytes, props);

            CorrelationData corrData = new CorrelationData(String.valueOf(UUID.randomUUID()));

            String routingKey = graph + "_routing";

            rabbitTemplate.convertAndSend(mqDirectExchange, routingKey, message, corrData);

            return AppResult.ok("ok");
        } catch (Exception e) {
            logger.error("Request /graph/olap_api error: " + e.toString());
            return AppResult.error(500, e.toString());
        }
    }


    @RequestMapping(value = "/graph/query_olap_results", method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public AppResult queryGraphOLAPResult(@RequestBody JSONObject params) {
        try {
            if (!params.containsKey("user")) {
                throw new ServiceException(ReturnCode.SERVICE_PARAM_ERROR, "user missed");
            }
            String user = params.getString("user").trim();

            if (!params.containsKey("graph")) {
                throw new ServiceException(ReturnCode.SERVICE_PARAM_ERROR, "graph missed");
            }
            String graph = params.getString("graph").trim();

            params.put("query", "results");

            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);

            Message message = new Message(params.toString().getBytes(), props);
            CorrelationData corrData = new CorrelationData(String.valueOf(UUID.randomUUID()));

            String routingKey = graph + "_rpc_routing";

            Message replyMessage = rabbitTemplate.sendAndReceive(mqRPCDirectExchange,
                    routingKey, message, corrData);

            JSONArray results = JSONArray.fromObject(replyMessage.getBody());
            logger.info(results.toString());

            return AppResult.ok(results);
        } catch (ServiceException e) {
            logger.error("Request /graph/query_olap_results error: " + e.toString());
            return AppResult.error(500, e.toString());
        }
    }

    @RequestMapping(value = "/graph/query_olap_status", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public AppResult queryGraphOLAPStatus(@RequestBody JSONObject params) {
        try {
            if (!params.containsKey("user")) {
                throw new ServiceException(ReturnCode.SERVICE_PARAM_ERROR, "user missed");
            }
            String user = params.getString("user").trim();

            if (!params.containsKey("graph")) {
                throw new ServiceException(ReturnCode.SERVICE_PARAM_ERROR, "graph missed");
            }
            String graph = params.getString("graph").trim();

            params.put("query", "status");

            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);

            Message message = new Message(params.toString().getBytes(), props);
            CorrelationData corrData = new CorrelationData(String.valueOf(UUID.randomUUID()));

            String routingKey = graph + "_rpc_routing";

            Message replyMessage = rabbitTemplate.sendAndReceive(mqRPCDirectExchange,
                    routingKey, message, corrData);

            String results = new String(replyMessage.getBody());
            results = results.replace("\"", "");

            logger.info(results);

            return AppResult.ok(results);
        } catch (ServiceException e) {
            logger.error("Request /graph/query_olap_status error: " + e.toString());
            return AppResult.error(500, e.toString());
        }
    }

}
