/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.server;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import org.apache.tinkerpop.gremlin.driver.MessageSerializer;
import org.apache.tinkerpop.gremlin.jsr223.GremlinPlugin;
import org.apache.tinkerpop.gremlin.jsr223.GremlinScriptEngine;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy;
import org.apache.tinkerpop.gremlin.server.auth.AllowAllAuthenticator;
import org.apache.tinkerpop.gremlin.server.auth.Authenticator;
import org.apache.tinkerpop.gremlin.server.channel.WebSocketChannelizer;
import org.apache.tinkerpop.gremlin.server.handler.AbstractAuthenticationHandler;
import org.apache.tinkerpop.gremlin.server.util.DefaultGraphManager;
import org.apache.tinkerpop.gremlin.server.util.LifeCycleHook;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;

import javax.net.ssl.TrustManager;

/**
 * Server settings as configured by a YAML file.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class Settings {

    public Settings() {
        // setup some sensible defaults like gremlin-groovy
        scriptEngines = new HashMap<>();
        scriptEngines.put("gremlin-groovy", new ScriptEngineSettings());
    }

    /**
     * Host to bind the server to. Defaults to localhost.
     */
    public String host = "localhost";

    /**
     * Port to bind the server to.  Defaults to 8182.
     */
    public int port = 8182;

    /**
     * Size of the worker thread pool.   Defaults to 1 and should generally not exceed 2 * number of cores. A
     * worker thread performs non-blocking read and write for one or more Channels in a non-blocking mode.
     */
    public int threadPoolWorker = 1;

    /**
     * detect if the OS is linux, then use epoll instead of NIO which causes less GC
     */
    public boolean useEpollEventLoop = false;

    /**
     * Size of the Gremlin thread pool. This pool handles Gremlin script execution and other related "long-run"
     * processing. Defaults to a setting of 0 which indicates the value should be set to
     * {@code Runtime#availableProcessors()}.
     */
    public int gremlinPool = 0;

    /**
     * Size of the boss thread pool.  Defaults to 1 and should likely stay at 1.  The bossy thread accepts incoming
     * connections on a port until it is unbound. Once a connection is accepted successfully, the boss thread
     * passes processing to the worker threads.
     */
    public int threadPoolBoss = 1;

    /**
     * Time in milliseconds to wait for a request (script or bytecode) to complete execution.  Defaults to -1 and
     * thus defers to {@link #evaluationTimeout} for this configuration. When set to something greater than -1 then
     * this configuration is used.
     *
     * @deprecated As of release 3.3.9, replaced by {@link #evaluationTimeout}.
     */
    @Deprecated
    public long scriptEvaluationTimeout = -1L;

    /**
     * Time in milliseconds to wait for a request (script or bytecode) to complete execution. Defaults to 30000.
     */
    public long evaluationTimeout = 30000L;

    /**
     * Number of items in a particular resultset to iterate and serialize prior to pushing the data down the wire
     * to the client.
     */
    public int resultIterationBatchSize = 64;

    /**
     * The maximum length of the initial line (e.g. {@code "GET / HTTP/1.0"}) processed in a request, which essentially
     * controls the maximum length of the submitted URI. This setting ties to the Netty {@code HttpRequestDecoder}.
     */
    public int maxInitialLineLength = 4096;

    /**
     * The maximum length of all headers.  This setting ties to the Netty {@code HttpRequestDecoder}
     */
    public int maxHeaderSize = 8192;

    /**
     * The maximum length of the content or each chunk.  If the content length exceeds this value, the transfer
     * encoding of the decoded request will be converted to 'chunked' and the content will be split into multiple
     * {@code HttpContent}s.  If the transfer encoding of the HTTP request is 'chunked' already, each chunk will be
     * split into smaller chunks if the length of the chunk exceeds this value.
     */
    public int maxChunkSize = 8192;

    /**
     * The maximum length of the aggregated content for a message.  Works in concert with {@link #maxChunkSize} where
     * chunked requests are accumulated back into a single message.  A request exceeding this size will
     * return a 413 - Request Entity Too Large status code.  A response exceeding this size will raise an internal
     * exception.
     */
    public int maxContentLength = 1024 * 64;

    /**
     * Maximum number of request components that can be aggregated for a message.
     */
    public int maxAccumulationBufferComponents = 1024;

    /**
     * If the number of bytes in the network send buffer exceeds this value then the channel is no longer writeable,
     * accepting no additional writes until buffer is drained and the {@link #writeBufferLowWaterMark} is met.
     */
    public int writeBufferHighWaterMark = 1024 * 64;

    /**
     * Once the number of bytes queued in the network send buffer exceeds the high water mark, the channel will not
     * become writeable again until the buffer is drained and it drops below this value.
     */
    public int writeBufferLowWaterMark = 1024 * 32;

    /**
     * Time in milliseconds that the server will allow a channel to not receive requests from a client before it
     * automatically closes. If enabled, the value provided should typically exceed the amount of time given to
     * {@link #keepAliveInterval}. Note that while this value is to be provided as milliseconds it will resolve to
     * second precision. Set this value to 0 to disable this feature.
     */
    public long idleConnectionTimeout = 0;

    /**
     * Time in milliseconds that the server will allow a channel to not send responses to a client before it sends
     * a "ping" to see if it is still present. If it is present, the client should respond with a "pong" which will
     * thus reset the {@link #idleConnectionTimeout} and keep the channel open. If enabled, this number should be
     * smaller than the value provided to the {@link #idleConnectionTimeout}. Note that while this value is to be
     * provided as milliseconds it will resolve to second precision. Set this value to 0 to disable this feature.
     */
    public long keepAliveInterval = 0;

    /**
     * If set to {@code true} the {@code aliases} option is required on requests and Gremlin Server will use that
     * information to control which {@link Graph} instances are transaction managed for that request.  If this
     * setting is false (which is the default), specification of {@code aliases} is not required and Gremlin
     * Server will simply apply a commit for all graphs.  This setting only applies to sessionless requests.
     * With either setting the user is responsible for their own transaction management for in-session requests.
     */
    public boolean strictTransactionManagement = false;

    /**
     * The full class name of the {@link Channelizer} to use in Gremlin Server.
     */
    public String channelizer = WebSocketChannelizer.class.getName();

    /**
     * The full class name of the {@link GraphManager} to use in Gremlin Server.
     */
    public String graphManager = DefaultGraphManager.class.getName();

    /**
     * Configured metrics for Gremlin Server.
     */
    public ServerMetrics metrics = null;

    /**
     * {@link Map} of {@link Graph} objects keyed by their binding name.
     */
    public Map<String, String> graphs = new HashMap<>();

    /**
     * {@link Map} of settings for {@code ScriptEngine} setting objects keyed by the name of the {@code ScriptEngine}
     * implementation.
     */
    public Map<String, ScriptEngineSettings> scriptEngines;

    /**
     * List of {@link MessageSerializer} to configure. If no serializers are specified then default serializers for
     * the most current versions of "application/json" and "application/vnd.gremlin-v1.0+gryo" are applied.
     */
    public List<SerializerSettings> serializers = Collections.emptyList();

    /**
     * Configures settings for SSL.
     */
    public SslSettings ssl = null;

    public AuthenticationSettings authentication = new AuthenticationSettings();

    /**
     * Custom settings for {@link OpProcessor} implementations. Implementations are loaded via
     * {@link ServiceLoader} but custom configurations can be supplied through this configuration.
     */
    public List<ProcessorSettings> processors = new ArrayList<>();

    /**
     * Find the {@link ProcessorSettings} related to the specified class. If there are multiple entries then only the
     * first is returned.
     */
    public Optional<ProcessorSettings> optionalProcessor(final Class<? extends OpProcessor> clazz) {
        return processors.stream()
                .filter(p -> p.className.equals(clazz.getCanonicalName()))
                .findFirst();
    }

    public Optional<ServerMetrics> optionalMetrics() {
        return Optional.ofNullable(metrics);
    }

    public Optional<SslSettings> optionalSsl() {
        return Optional.ofNullable(ssl);
    }

    public long getEvaluationTimeout() {
        return -1 == scriptEvaluationTimeout ? evaluationTimeout : scriptEvaluationTimeout;
    }

    /**
     * Read configuration from a file into a new {@link Settings} object.
     *
     * @param file the location of a Gremlin Server YAML configuration file
     * @return a new {@link Optional} object wrapping the created {@link Settings}
     */
    public static Settings read(final String file) throws Exception {
        final InputStream input = new FileInputStream(new File(file));
        return read(input);
    }

    /**
     * Read configuration from a file into a new {@link Settings} object.
     *
     * @param stream an input stream containing a Gremlin Server YAML configuration
     * @return a new {@link Optional} object wrapping the created {@link Settings}
     */
    public static Settings read(final InputStream stream) {
        Objects.requireNonNull(stream);

        final Constructor constructor = new Constructor(Settings.class);
        final TypeDescription settingsDescription = new TypeDescription(Settings.class);
        settingsDescription.putMapPropertyType("graphs", String.class, String.class);
        settingsDescription.putMapPropertyType("scriptEngines", String.class, ScriptEngineSettings.class);
        settingsDescription.putListPropertyType("serializers", SerializerSettings.class);
        settingsDescription.putListPropertyType("plugins", String.class);
        settingsDescription.putListPropertyType("processors", ProcessorSettings.class);
        constructor.addTypeDescription(settingsDescription);

        final TypeDescription serializerSettingsDescription = new TypeDescription(SerializerSettings.class);
        serializerSettingsDescription.putMapPropertyType("config", String.class, Object.class);
        constructor.addTypeDescription(serializerSettingsDescription);

        final TypeDescription scriptEngineSettingsDescription = new TypeDescription(ScriptEngineSettings.class);
        scriptEngineSettingsDescription.putListPropertyType("imports", String.class);
        scriptEngineSettingsDescription.putListPropertyType("staticImports", String.class);
        scriptEngineSettingsDescription.putListPropertyType("scripts", String.class);
        scriptEngineSettingsDescription.putMapPropertyType("config", String.class, Object.class);
        scriptEngineSettingsDescription.putMapPropertyType("plugins", String.class, Object.class);
        constructor.addTypeDescription(scriptEngineSettingsDescription);

        final TypeDescription sslSettings = new TypeDescription(SslSettings.class);
        constructor.addTypeDescription(sslSettings);

        final TypeDescription authenticationSettings = new TypeDescription(AuthenticationSettings.class);
        constructor.addTypeDescription(authenticationSettings);

        final TypeDescription serverMetricsDescription = new TypeDescription(ServerMetrics.class);
        constructor.addTypeDescription(serverMetricsDescription);

        final TypeDescription consoleReporterDescription = new TypeDescription(ConsoleReporterMetrics.class);
        constructor.addTypeDescription(consoleReporterDescription);

        final TypeDescription csvReporterDescription = new TypeDescription(CsvReporterMetrics.class);
        constructor.addTypeDescription(csvReporterDescription);

        final TypeDescription jmxReporterDescription = new TypeDescription(JmxReporterMetrics.class);
        constructor.addTypeDescription(jmxReporterDescription);

        final TypeDescription slf4jReporterDescription = new TypeDescription(Slf4jReporterMetrics.class);
        constructor.addTypeDescription(slf4jReporterDescription);

        final TypeDescription gangliaReporterDescription = new TypeDescription(GangliaReporterMetrics.class);
        constructor.addTypeDescription(gangliaReporterDescription);

        final TypeDescription graphiteReporterDescription = new TypeDescription(GraphiteReporterMetrics.class);
        constructor.addTypeDescription(graphiteReporterDescription);

        final Yaml yaml = new Yaml(constructor);
        return yaml.loadAs(stream, Settings.class);
    }

    /**
     * Custom configurations for any {@link OpProcessor} implementations.  These settings will not be relevant
     * unless the referenced {@link OpProcessor} is actually loaded via {@link ServiceLoader}.
     */
    public static class ProcessorSettings {
        /**
         * The fully qualified class name of an {@link OpProcessor} implementation.
         */
        public String className;

        /**
         * A set of configurations as expected by the {@link OpProcessor}.  Consult the documentation of the
         * {@link OpProcessor} for information on what these configurations should be.
         */
        public Map<String, Object> config;
    }

    /**
     * Settings for the {@code ScriptEngine}.
     */
    public static class ScriptEngineSettings {
        /**
         * A comma separated list of classes/packages to make available to the {@code ScriptEngine}.
         */
        public List<String> imports = new ArrayList<>();

        /**
         * A comma separated list of "static" imports to make available to the {@code ScriptEngine}.
         */
        public List<String> staticImports = new ArrayList<>();

        /**
         * A comma separated list of script files to execute on {@code ScriptEngine} initialization. {@link Graph} and
         * {@link TraversalSource} instance references produced from scripts will be stored globally in Gremlin
         * Server, therefore it is possible to use initialization scripts to add {@link TraversalStrategy} instances
         * or create entirely new {@link Graph} instances all together. Instantiating a {@link LifeCycleHook} in a
         * script provides a way to execute scripts when Gremlin Server starts and stops.
         */
        public List<String> scripts = new ArrayList<>();

        /**
         * A Map of configuration settings for the {@code ScriptEngine}. These settings are dependent on the
         * {@code ScriptEngine} implementation being used.
         */
        public Map<String, Object> config = null;

        /**
         * A set of configurations for {@link GremlinPlugin} instances to apply to this {@link GremlinScriptEngine}.
         * Plugins will be applied in the order they are listed.
         */
        public Map<String,Map<String,Object>> plugins = new LinkedHashMap<>();
    }

    /**
     * Settings for the {@link MessageSerializer} implementations.
     */
    public static class SerializerSettings {

        public SerializerSettings() {}

        SerializerSettings(final String className, final Map<String, Object> config) {
            this.className = className;
            this.config = config;
        }

        /**
         * The fully qualified class name of the {@link MessageSerializer} implementation. This class name will be
         * used to load the implementation from the classpath.
         */
        public String className;

        /**
         * A {@link Map} containing {@link MessageSerializer} specific configurations. Consult the
         * {@link MessageSerializer} implementation for specifics on what configurations are expected.
         */
        public Map<String, Object> config = Collections.emptyMap();
    }

    /**
     * Settings for the {@link Authenticator} implementation.
     */
    public static class AuthenticationSettings {
        /**
         * The fully qualified class name of the {@link Authenticator} implementation. This class name will be
         * used to load the implementation from the classpath. Defaults to {@link AllowAllAuthenticator} when
         * not specified.
         */
        public String authenticator = AllowAllAuthenticator.class.getName();

        /**
         * The fully qualified class name of the {@link AbstractAuthenticationHandler} implementation.
         * This class name will be used to load the implementation from the classpath.
         * Defaults to null when not specified.
         */
        public String authenticationHandler = null;

        /**
         * Enable audit logging of authenticated users and gremlin evaluation requests.
         */
        public boolean enableAuditLog = false;

        /**
         * A {@link Map} containing {@link Authenticator} specific configurations. Consult the
         * {@link Authenticator} implementation for specifics on what configurations are expected.
         */
        public Map<String, Object> config = null;
    }

    /**
     * Settings to configure SSL support.
     */
    public static class SslSettings {
        /**
         * Enables SSL. Other SSL settings will be ignored unless this is set to true.
         */
        public boolean enabled = false;

        /**
         * The X.509 certificate chain file in PEM format.
         *
         * @deprecated As of release 3.2.10, replaced by {@link #keyStore}
         */
        @Deprecated
        public String keyCertChainFile = null;

        /**
         * The PKCS#8 private key file in PEM format.
         *
         * @deprecated As of release 3.2.10, replaced by {@link #keyStore}
         */
        @Deprecated
        public String keyFile = null;

        /**
         * The password of the {@link #keyFile}, or {@code null} if it's not
         * password-protected.
         *
         * @deprecated As of release 3.2.10, replaced by {@link #keyStorePassword}
         */
        @Deprecated
        public String keyPassword = null;

        /**
         * Trusted certificates for verifying the remote endpoint's certificate. The
         * file should contain an X.509 certificate chain in PEM format. {@code null}
         * uses the system default.
         *
         * @deprecated As of release 3.2.10, replaced by {@link #trustStore}
         */
        @Deprecated
        public String trustCertChainFile = null;

        /**
         * The file location of the private key in JKS or PKCS#12 format.
         */
        public String keyStore;

        /**
         * The password of the {@link #keyStore}, or {@code null} if it's not password-protected.
         */
        public String keyStorePassword;

        /**
         * Trusted certificates for verifying the remote client's certificate. If
         * this value is not provided and SSL is enabled, the default {@link TrustManager} will be used.
         */
        public String trustStore;

        /**
         * The password of the {@link #trustStore}, or {@code null} if it's not password-protected.
         */
        public String trustStorePassword;

        /**
         * The format of the {@link #keyStore}, either {@code JKS} or {@code PKCS12}
         */
        public String keyStoreType;

        /**
         * A list of SSL protocols to enable. @see <a href=
         *      "https://docs.oracle.com/javase/8/docs/technotes/guides/security/SunProviders.html#SunJSSE_Protocols">JSSE
         *      Protocols</a>
         */
        public List<String> sslEnabledProtocols = new ArrayList<>();

        /**
         * A list of cipher suites to enable. @see <a href=
         *      "https://docs.oracle.com/javase/8/docs/technotes/guides/security/SunProviders.html#SupportedCipherSuites">Cipher
         *      Suites</a>
         */
        public List<String> sslCipherSuites = new ArrayList<>();

        /**
         * Require client certificate authentication
         */
        public ClientAuth needClientAuth = ClientAuth.NONE;

        private SslContext sslContext;

        /**
         * When this value is set, the other settings for SSL are ignored. This option provides for a programmatic
         * way to configure more complex SSL configurations. The {@link #enabled} setting should still be set to
         * {@code true} for this setting to take effect.
         */
        public void overrideSslContext(final SslContext sslContext) {
            this.sslContext = sslContext;
        }

        public Optional<SslContext> getSslContext() {
            return Optional.ofNullable(sslContext);
        }
    }

    /**
     * Settings for {@code Metrics} recorded by Gremlin Server.
     */
    public static class ServerMetrics {
        public ConsoleReporterMetrics consoleReporter = null;
        public CsvReporterMetrics csvReporter = null;
        public JmxReporterMetrics jmxReporter = null;
        public Slf4jReporterMetrics slf4jReporter = null;
        public GangliaReporterMetrics gangliaReporter = null;
        public GraphiteReporterMetrics graphiteReporter = null;

        public Optional<ConsoleReporterMetrics> optionalConsoleReporter() {
            return Optional.ofNullable(consoleReporter);
        }

        public Optional<CsvReporterMetrics> optionalCsvReporter() {
            return Optional.ofNullable(csvReporter);
        }

        public Optional<JmxReporterMetrics> optionalJmxReporter() {
            return Optional.ofNullable(jmxReporter);
        }

        public Optional<Slf4jReporterMetrics> optionalSlf4jReporter() {
            return Optional.ofNullable(slf4jReporter);
        }

        public Optional<GangliaReporterMetrics> optionalGangliaReporter() {
            return Optional.ofNullable(gangliaReporter);
        }

        public Optional<GraphiteReporterMetrics> optionalGraphiteReporter() {
            return Optional.ofNullable(graphiteReporter);
        }
    }

    /**
     * Settings for a {@code Metrics} reporter that writes to the console.
     */
    public static class ConsoleReporterMetrics extends IntervalMetrics {
    }


    /**
     * Settings for a {@code Metrics} reporter that writes to a CSV file.
     */
    public static class CsvReporterMetrics extends IntervalMetrics {
        public String fileName = "metrics.csv";
    }

    /**
     * Settings for a {@code Metrics} reporter that writes to JMX.
     */
    public static class JmxReporterMetrics extends BaseMetrics {
        public String domain = null;
        public String agentId = null;
    }

    /**
     * Settings for a {@code Metrics} reporter that writes to the SL4J output.
     */
    public static class Slf4jReporterMetrics extends IntervalMetrics {
        public String loggerName = Slf4jReporterMetrics.class.getName();
    }

    /**
     * Settings for a {@code Metrics} reporter that writes to Ganglia.
     */
    public static class GangliaReporterMetrics extends HostPortIntervalMetrics {
        public String addressingMode = null;
        public int ttl = 1;
        public boolean protocol31 = true;
        public UUID hostUUID = null;
        public String spoof = null;

        public GangliaReporterMetrics() {
            // default ganglia port
            this.port = 8649;
        }
    }

    /**
     * Settings for a {@code Metrics} reporter that writes to Graphite.
     */
    public static class GraphiteReporterMetrics extends HostPortIntervalMetrics {
        public String prefix = "";

        public GraphiteReporterMetrics() {
            // default graphite port
            this.port = 2003;
        }
    }

    public static abstract class HostPortIntervalMetrics extends IntervalMetrics {
        public String host = "localhost";
        public int port;
    }

    public static abstract class IntervalMetrics extends BaseMetrics {
        public long interval = 60000;
    }

    public static abstract class BaseMetrics {
        public boolean enabled = false;
    }
}
