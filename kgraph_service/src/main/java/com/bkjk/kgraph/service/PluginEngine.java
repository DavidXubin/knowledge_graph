package com.bkjk.kgraph.service;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import com.bkjk.kgraph.common.ReturnCode;
import com.bkjk.kgraph.common.ServiceException;
import com.bkjk.kgraph.dao.mysql.PluginMapper;
import com.bkjk.kgraph.model.Plugin;
import com.bkjk.kgraph.model.PluginExample;
import com.google.common.collect.Lists;
import com.alibaba.fastjson.JSONException;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.PostConstruct;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ResourceLoader;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;


import groovy.lang.GroovyClassLoader;

@Service
//@EnableScheduling
public class PluginEngine implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(PluginEngine.class);

    private ApplicationContext parentContext;
    private ClassPathXmlApplicationContext pluginContext;

    private Resource pluginConfig;
    private long lastModified;

    @javax.annotation.Resource
    private ResourceLoader resourceLoader;

    @Autowired
    private PluginMapper pluginMapper;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.fanout_exchange_name}")
    private String mqFanoutExchange;

    private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();


    @PostConstruct
    public void init() {
        try {
            pluginConfig = resourceLoader.getResource("classpath:spring/plugins.xml");
            lastModified = pluginConfig.lastModified();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        reload();
        logger.info("Rule engine initialized.");
    }

    public PluginProperties.Status getPluginStatus(String pluginName) {

        try {
            rwLock.readLock().lock();

            if (!pluginContext.containsBean(pluginName)) {
                logger.warn("Rule[" + pluginName + "] not found.");
                throw new RuntimeException("Rule[" + pluginName + "] not found.");
            }

            PluginService service = pluginContext.getBean(pluginName, PluginService.class);

            BeanUtils.e

            PluginProperties.Status status = service.props.getStatus();

            return status;

        } catch (Exception e) {
            logger.error("Error occur while querying the plugin[" + pluginName + "]：" + e);
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR, e);
        } finally {
            rwLock.readLock().unlock();
        }

    }

    public List run(JSONObject param) {

        String pluginName = null;

        try {
            pluginName = param.getString("plugin_name");

            rwLock.readLock().lock();

            if (!pluginContext.containsBean(pluginName)) {
                logger.warn("Rule[" + pluginName + "] not found.");
                throw new RuntimeException("Rule[" + pluginName + "] not found.");
            }

            PluginService service = pluginContext.getBean(pluginName, PluginService.class);

            if (null != service) {
                return service.run(param);
            }

            return Lists.newArrayList();

        } catch (JSONException e) {
            logger.error("Params :" + param.toString() + "is not valid: " + e);
            throw new ServiceException(ReturnCode.SERVICE_JSON_PARSE_ERROR, e);
        } catch (Exception e) {
            logger.error("Error occur while running the plugin[" + pluginName + "]：" + e);
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR, e);
        } finally {
            rwLock.readLock().unlock();
        }

    }

    //@Scheduled(fixedDelay = 3600000)
    public void checkUpdate() {
        try {
            long currentLastModified = pluginConfig.lastModified();
            if (this.lastModified < currentLastModified) {
                reload();
                this.lastModified = currentLastModified;
                logger.info("Rule engine updated.");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int updatePlugin(String user, String pluginName, String pluginContent, String pluginDescrpt) {
        int rowId;

        try {
            PluginExample example = new PluginExample();
            PluginExample.Criteria criteria = example.createCriteria();
            criteria.andNameEqualTo(pluginName);

            Date date = new Date(System.currentTimeMillis());

            Plugin newPlugin = new Plugin();

            newPlugin.setName(pluginName);
            newPlugin.setUser(user);
            newPlugin.setContent(pluginContent);
            newPlugin.setDecription(pluginDescrpt);
            newPlugin.setUpdateTime(date);

            List<Plugin> plugins = pluginMapper.selectByExample(example);
            if (plugins.isEmpty()) {
                newPlugin.setCreateTime(date);
                rowId = pluginMapper.insertSelective(newPlugin);

            } else {
                rowId = pluginMapper.updateByExampleSelective(newPlugin, example);
            }
            
            JSONObject updateObject = new JSONObject();
            updateObject.element("message_type", "update_plugin");
            updateObject.element("plugin_name", pluginName);

            byte[] bytes = updateObject.toString().getBytes();

            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            Message message = new Message(bytes, props);

            CorrelationData corrData = new CorrelationData(String.valueOf(UUID.randomUUID()));

            rabbitTemplate.convertAndSend(mqFanoutExchange, null, message, corrData);

            return rowId;
        } catch (Exception e) {
            logger.error("Error occur while updating the plugin[" + pluginName + "]：" + e);
            throw new ServiceException(ReturnCode.SERVICE_PLUGIN_ERROR, e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.parentContext = applicationContext;
    }

    private synchronized void reload() {
        if (!pluginConfig.exists()) {
            throw new RuntimeException("Plugin config not exist.");
        }
        ClassPathXmlApplicationContext oldContext = this.pluginContext;

        try {
            String[] config = { pluginConfig.getURI().toString() };
            ClassPathXmlApplicationContext newContext = new ClassPathXmlApplicationContext(config, parentContext);

            this.pluginContext = newContext;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (null != oldContext && oldContext.isActive()) {
            oldContext.close();
        }

        reloadFromDB(null);
    }

    private void registerPluginAsBean(String pluginName, String pluginContent) {
        Class clazz = new GroovyClassLoader().parseClass(pluginContent);

        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
        BeanDefinition beanDefinition = beanDefinitionBuilder.getRawBeanDefinition();

        pluginContext.getAutowireCapableBeanFactory().applyBeanPostProcessorsAfterInitialization(beanDefinition, pluginName);
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) pluginContext.getBeanFactory();
        beanFactory.registerBeanDefinition(pluginName, beanDefinition);
    }

    public void reloadFromDB(String pluginName) {
        PluginExample example = new PluginExample();
        example.setDistinct(true);
        if (pluginName != null) {
            PluginExample.Criteria criteria = example.createCriteria();
            criteria.andNameEqualTo(pluginName);
        }

        List<Plugin> plugins = pluginMapper.selectByExample(example);

        try {
            rwLock.writeLock().lock();

            for (Plugin plugin : plugins) {
                registerPluginAsBean(plugin.getName(), plugin.getContent());
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

}
