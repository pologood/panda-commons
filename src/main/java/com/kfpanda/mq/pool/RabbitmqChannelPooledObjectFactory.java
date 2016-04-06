package com.kfpanda.mq.pool;

import com.kfpanda.mq.pool1.PoolConfig;
import com.kfpanda.util.PropertiesUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by kfpanda on 16-4-5.
 */
public class RabbitmqChannelPooledObjectFactory extends BasePooledObjectFactory<Channel> {
    private static Logger logger = LoggerFactory.getLogger(RabbitmqChannelPooledObjectFactory.class);

    protected static String host;
    protected static int port;
    protected static int DEFAULT_PORT = 5672;
    protected static String virtualHost;
    protected static int timeout;
    protected static String userName;
    protected static String password;

    protected static String queue;
    protected static String amqDirect;
    protected static String exchangeType;
    protected static String routeKey;
    private ObjectPool<Connection> connectionPool;
    private static final ConnectionFactory factory = new ConnectionFactory();

    public RabbitmqChannelPooledObjectFactory(RabbitmqConnectionPooledObjectFactory connectionFactory){
        Properties prop = PropertiesUtil.getConfig();
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxIdle(Integer.parseInt(prop.getProperty("rabbitmq.conn.pool.max.idle")));
        poolConfig.setMinIdle(Integer.parseInt(prop.getProperty("rabbitmq.conn.pool.min.idle")));
        poolConfig.setMaxTotal(Integer.parseInt(prop.getProperty("rabbitmq.conn.pool.max.total")));
        poolConfig.setMaxWaitMillis(Integer.parseInt(prop.getProperty("rabbitmq.conn.pool.max.wait")));
        // 在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
        poolConfig.setTestOnBorrow(true);

        connectionPool = new GenericObjectPool<Connection>(new RabbitmqConnectionPooledObjectFactory(), poolConfig);
    }

    @Override
    public Channel create() throws Exception {
        Properties prop = PropertiesUtil.getConfig();

        String tmout = prop.getProperty("rabbitmq.timeout");
        queue = prop.getProperty("rabbitmq.queue");
        amqDirect = prop.getProperty("rabbitmq.amq.direct");
        exchangeType = prop.getProperty("rabbitmq.exchange.type");
        routeKey = prop.getProperty("rabbitmq.route.key");

        Connection conn = null;
        Channel channel = null;
        try {
            conn = connectionPool.borrowObject();
            channel = conn.createChannel();
            channel.exchangeDeclare(amqDirect, exchangeType, true);
            channel.queueBind(queue, amqDirect, routeKey);
        } catch (Exception e) {
            logger.error("rabbitmq borrow connection error.", e);
        }
        return channel;
    }

    @Override
    public PooledObject<Channel> wrap(Channel channel) {
        return new DefaultPooledObject<Channel>(channel);
    }
}