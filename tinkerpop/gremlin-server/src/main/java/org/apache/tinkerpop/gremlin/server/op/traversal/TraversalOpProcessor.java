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
package org.apache.tinkerpop.gremlin.server.op.traversal;

import com.codahale.metrics.Timer;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.channel.ChannelHandlerContext;
import org.apache.tinkerpop.gremlin.driver.MessageSerializer;
import org.apache.tinkerpop.gremlin.driver.Tokens;
import org.apache.tinkerpop.gremlin.driver.message.RequestMessage;
import org.apache.tinkerpop.gremlin.driver.message.ResponseMessage;
import org.apache.tinkerpop.gremlin.driver.message.ResponseStatusCode;
import org.apache.tinkerpop.gremlin.jsr223.JavaTranslator;
import org.apache.tinkerpop.gremlin.process.traversal.Bytecode;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalSideEffects;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.util.BytecodeHelper;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalInterruptedException;
import org.apache.tinkerpop.gremlin.server.Context;
import org.apache.tinkerpop.gremlin.server.GraphManager;
import org.apache.tinkerpop.gremlin.server.GremlinServer;
import org.apache.tinkerpop.gremlin.server.OpProcessor;
import org.apache.tinkerpop.gremlin.server.Settings;
import org.apache.tinkerpop.gremlin.server.handler.Frame;
import org.apache.tinkerpop.gremlin.server.handler.StateKey;
import org.apache.tinkerpop.gremlin.server.op.AbstractOpProcessor;
import org.apache.tinkerpop.gremlin.server.op.OpProcessorException;
import org.apache.tinkerpop.gremlin.server.util.MetricManager;
import org.apache.tinkerpop.gremlin.server.util.SideEffectIterator;
import org.apache.tinkerpop.gremlin.server.util.TraverserIterator;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONMapper;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONVersion;
import org.apache.tinkerpop.gremlin.util.function.ThrowingConsumer;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * Simple {@link OpProcessor} implementation that iterates remotely submitted serialized {@link Traversal} objects.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class TraversalOpProcessor extends AbstractOpProcessor {
    private static final Logger logger = LoggerFactory.getLogger(TraversalOpProcessor.class);
    private static final ObjectMapper mapper = GraphSONMapper.build().version(GraphSONVersion.V2_0).create().createMapper();
    public static final String OP_PROCESSOR_NAME = "traversal";
    public static final Timer traversalOpTimer = MetricManager.INSTANCE.getTimer(name(GremlinServer.class, "op", "traversal"));

    public static final Settings.ProcessorSettings DEFAULT_SETTINGS = new Settings.ProcessorSettings();

    /**
     * Configuration setting for how long a cached side-effect will be available before it is evicted from the cache.
     *
     * @deprecated As of release 3.3.8, not directly replaced in the protocol as side-effect retrieval after
     * traversal iteration is not being promoted anymore as a feature.
     */
    @Deprecated
    public static final String CONFIG_CACHE_EXPIRATION_TIME = "cacheExpirationTime";

    /**
     * Default timeout for a cached side-effect is ten minutes.
     *
     * @deprecated As of release 3.3.8, not directly replaced in the protocol as side-effect retrieval after
     * traversal iteration is not being promoted anymore as a feature.
     */
    @Deprecated
    public static final long DEFAULT_CACHE_EXPIRATION_TIME = 600000;

    /**
     * Configuration setting for the maximum number of entries the cache will have.
     *
     * @deprecated As of release 3.3.8, not directly replaced in the protocol as side-effect retrieval after
     * traversal iteration is not being promoted anymore as a feature.
     */
    @Deprecated
    public static final String CONFIG_CACHE_MAX_SIZE = "cacheMaxSize";

    /**
     * Default size of the max size of the cache.
     *
     * @deprecated As of release 3.3.8, not directly replaced in the protocol as side-effect retrieval after
     * traversal iteration is not being promoted anymore as a feature.
     */
    @Deprecated
    public static final long DEFAULT_CACHE_MAX_SIZE = 1000;

    static {
        DEFAULT_SETTINGS.className = TraversalOpProcessor.class.getCanonicalName();
        DEFAULT_SETTINGS.config = new HashMap<String, Object>() {{
            put(CONFIG_CACHE_EXPIRATION_TIME, DEFAULT_CACHE_EXPIRATION_TIME);
            put(CONFIG_CACHE_MAX_SIZE, DEFAULT_CACHE_MAX_SIZE);
        }};
    }

    /**
     * @deprecated As of release 3.3.8, not directly replaced in the protocol as side-effect retrieval after
     * traversal iteration is not being promoted anymore as a feature.
     */
    protected static Cache<UUID, TraversalSideEffects> cache = null;

    private static final Bindings EMPTY_BINDINGS = new SimpleBindings();

    public TraversalOpProcessor() {
        super(false);
    }

    @Override
    public String getName() {
        return OP_PROCESSOR_NAME;
    }

    @Override
    public void close() throws Exception {
        // do nothing = no resources to release
    }

    @Override
    public void init(final Settings settings) {
        final Settings.ProcessorSettings processorSettings = settings.processors.stream()
                .filter(p -> p.className.equals(TraversalOpProcessor.class.getCanonicalName()))
                .findAny().orElse(TraversalOpProcessor.DEFAULT_SETTINGS);
        final long maxSize = Long.parseLong(processorSettings.config.get(TraversalOpProcessor.CONFIG_CACHE_MAX_SIZE).toString());
        final long expirationTime = Long.parseLong(processorSettings.config.get(TraversalOpProcessor.CONFIG_CACHE_EXPIRATION_TIME).toString());

        cache = Caffeine.newBuilder()
                .expireAfterWrite(expirationTime, TimeUnit.MILLISECONDS)
                .maximumSize(maxSize)
                .build();

        logger.info("Initialized cache for {} with size {} and expiration time of {} ms",
                TraversalOpProcessor.class.getSimpleName(), maxSize, expirationTime);
    }

    @Override
    public ThrowingConsumer<Context> select(final Context context) throws OpProcessorException {
        final RequestMessage message = context.getRequestMessage();
        logger.debug("Selecting processor for RequestMessage {}", message);

        final ThrowingConsumer<Context> op;
        switch (message.getOp()) {
            case Tokens.OPS_BYTECODE:
                validateTraversalSourceAlias(context, message, validateTraversalRequest(message));
                op = this::iterateBytecodeTraversal;
                break;
            case Tokens.OPS_GATHER:
                final Optional<String> sideEffectForGather = message.optionalArgs(Tokens.ARGS_SIDE_EFFECT);
                if (!sideEffectForGather.isPresent()) {
                    final String msg = String.format("A message with an [%s] op code requires a [%s] argument.", Tokens.OPS_GATHER, Tokens.ARGS_SIDE_EFFECT);
                    throw new OpProcessorException(msg, ResponseMessage.build(message).code(ResponseStatusCode.REQUEST_ERROR_INVALID_REQUEST_ARGUMENTS).statusMessage(msg).create());
                }

                final Optional<String> sideEffectKey = message.optionalArgs(Tokens.ARGS_SIDE_EFFECT_KEY);
                if (!sideEffectKey.isPresent()) {
                    final String msg = String.format("A message with an [%s] op code requires a [%s] argument.", Tokens.OPS_GATHER, Tokens.ARGS_SIDE_EFFECT_KEY);
                    throw new OpProcessorException(msg, ResponseMessage.build(message).code(ResponseStatusCode.REQUEST_ERROR_INVALID_REQUEST_ARGUMENTS).statusMessage(msg).create());
                }

                validateTraversalSourceAlias(context, message, validatedAliases(message).get());

                op = this::gatherSideEffect;

                break;
            case Tokens.OPS_KEYS:
                final Optional<String> sideEffectForKeys = message.optionalArgs(Tokens.ARGS_SIDE_EFFECT);
                if (!sideEffectForKeys.isPresent()) {
                    final String msg = String.format("A message with an [%s] op code requires a [%s] argument.", Tokens.OPS_GATHER, Tokens.ARGS_SIDE_EFFECT);
                    throw new OpProcessorException(msg, ResponseMessage.build(message).code(ResponseStatusCode.REQUEST_ERROR_INVALID_REQUEST_ARGUMENTS).statusMessage(msg).create());
                }

                op = varRhc -> {
                    final RequestMessage msg = context.getRequestMessage();
                    final Optional<UUID> sideEffect = msg.optionalArgs(Tokens.ARGS_SIDE_EFFECT);
                    final TraversalSideEffects sideEffects = cache.getIfPresent(sideEffect.get());

                    if (null == sideEffects)
                        logger.warn("Request for side-effect keys on {} returned no side-effects in the cache", sideEffect.get());

                    handleIterator(varRhc, null == sideEffects ? Collections.emptyIterator() : sideEffects.keys().iterator());
                };

                break;
            case Tokens.OPS_CLOSE:
                final Optional<String> sideEffectForClose = message.optionalArgs(Tokens.ARGS_SIDE_EFFECT);
                if (!sideEffectForClose.isPresent()) {
                    final String msg = String.format("A message with an [%s] op code requires a [%s] argument.", Tokens.OPS_CLOSE, Tokens.ARGS_SIDE_EFFECT);
                    throw new OpProcessorException(msg, ResponseMessage.build(message).code(ResponseStatusCode.REQUEST_ERROR_INVALID_REQUEST_ARGUMENTS).statusMessage(msg).create());
                }

                op = varRhc -> {
                    final RequestMessage msg = context.getRequestMessage();
                    logger.debug("Close request {} for in thread {}", msg.getRequestId(), Thread.currentThread().getName());

                    final Optional<UUID> sideEffect = msg.optionalArgs(Tokens.ARGS_SIDE_EFFECT);
                    cache.invalidate(sideEffect.get());

                    final String successMessage = String.format("Successfully cleared side effect cache for [%s].", Tokens.ARGS_SIDE_EFFECT);
                    varRhc.writeAndFlush(ResponseMessage.build(message).code(ResponseStatusCode.NO_CONTENT).statusMessage(successMessage).create());
                };

                break;
            case Tokens.OPS_INVALID:
                final String msgInvalid = String.format("Message could not be parsed.  Check the format of the request. [%s]", message);
                throw new OpProcessorException(msgInvalid, ResponseMessage.build(message).code(ResponseStatusCode.REQUEST_ERROR_MALFORMED_REQUEST).statusMessage(msgInvalid).create());
            default:
                final String msgDefault = String.format("Message with op code [%s] is not recognized.", message.getOp());
                throw new OpProcessorException(msgDefault, ResponseMessage.build(message).code(ResponseStatusCode.REQUEST_ERROR_MALFORMED_REQUEST).statusMessage(msgDefault).create());
        }

        return op;
    }

    private static void validateTraversalSourceAlias(final Context ctx, final RequestMessage message, final Map<String, String> aliases) throws OpProcessorException {
        final String traversalSourceBindingForAlias = aliases.values().iterator().next();
        if (!ctx.getGraphManager().getTraversalSourceNames().contains(traversalSourceBindingForAlias)) {
            final String msg = String.format("The traversal source [%s] for alias [%s] is not configured on the server.", traversalSourceBindingForAlias, Tokens.VAL_TRAVERSAL_SOURCE_ALIAS);
            throw new OpProcessorException(msg, ResponseMessage.build(message).code(ResponseStatusCode.REQUEST_ERROR_INVALID_REQUEST_ARGUMENTS).statusMessage(msg).create());
        }
    }

    private static Map<String, String> validateTraversalRequest(final RequestMessage message) throws OpProcessorException {
        if (!message.optionalArgs(Tokens.ARGS_GREMLIN).isPresent()) {
            final String msg = String.format("A message with [%s] op code requires a [%s] argument.", Tokens.OPS_BYTECODE, Tokens.ARGS_GREMLIN);
            throw new OpProcessorException(msg, ResponseMessage.build(message).code(ResponseStatusCode.REQUEST_ERROR_INVALID_REQUEST_ARGUMENTS).statusMessage(msg).create());
        }

        return validatedAliases(message).get();
    }

    private static Optional<Map<String, String>> validatedAliases(final RequestMessage message) throws OpProcessorException {
        final Optional<Map<String, String>> aliases = message.optionalArgs(Tokens.ARGS_ALIASES);
        if (!aliases.isPresent()) {
            final String msg = String.format("A message with [%s] op code requires a [%s] argument.", Tokens.OPS_BYTECODE, Tokens.ARGS_ALIASES);
            throw new OpProcessorException(msg, ResponseMessage.build(message).code(ResponseStatusCode.REQUEST_ERROR_INVALID_REQUEST_ARGUMENTS).statusMessage(msg).create());
        }

        if (aliases.get().size() != 1 || !aliases.get().containsKey(Tokens.VAL_TRAVERSAL_SOURCE_ALIAS)) {
            final String msg = String.format("A message with [%s] op code requires the [%s] argument to be a Map containing one alias assignment named '%s'.",
                    Tokens.OPS_BYTECODE, Tokens.ARGS_ALIASES, Tokens.VAL_TRAVERSAL_SOURCE_ALIAS);
            throw new OpProcessorException(msg, ResponseMessage.build(message).code(ResponseStatusCode.REQUEST_ERROR_INVALID_REQUEST_ARGUMENTS).statusMessage(msg).create());
        }

        return aliases;
    }

    private void gatherSideEffect(final Context context) throws OpProcessorException {
        final RequestMessage msg = context.getRequestMessage();
        logger.debug("Side-effect request {} for in thread {}", msg.getRequestId(), Thread.currentThread().getName());

        // earlier validation in selection of this op method should free us to cast this without worry
        final Optional<UUID> sideEffect = msg.optionalArgs(Tokens.ARGS_SIDE_EFFECT);
        final Optional<String> sideEffectKey = msg.optionalArgs(Tokens.ARGS_SIDE_EFFECT_KEY);
        final Map<String, String> aliases = (Map<String, String>) msg.optionalArgs(Tokens.ARGS_ALIASES).get();

        final GraphManager graphManager = context.getGraphManager();
        final String traversalSourceName = aliases.entrySet().iterator().next().getValue();
        final TraversalSource g = graphManager.getTraversalSource(traversalSourceName);

        final Timer.Context timerContext = traversalOpTimer.time();
        try {
            final ChannelHandlerContext ctx = context.getChannelHandlerContext();
            final Graph graph = g.getGraph();

            context.getGremlinExecutor().getExecutorService().submit(() -> {
                try {
                    beforeProcessing(graph, context);

                    try {
                        final TraversalSideEffects sideEffects = cache.getIfPresent(sideEffect.get());

                        if (null == sideEffects) {
                            final String errorMessage = String.format("Could not find side-effects for %s.", sideEffect.get());
                            logger.warn(errorMessage);
                            context.writeAndFlush(ResponseMessage.build(msg).code(ResponseStatusCode.SERVER_ERROR).statusMessage(errorMessage).create());
                            onError(graph, context);
                            return;
                        }

                        if (!sideEffects.exists(sideEffectKey.get())) {
                            final String errorMessage = String.format("Could not find side-effect key for %s in %s.", sideEffectKey.get(), sideEffect.get());
                            logger.warn(errorMessage);
                            context.writeAndFlush(ResponseMessage.build(msg).code(ResponseStatusCode.SERVER_ERROR).statusMessage(errorMessage).create());
                            onError(graph, context);
                            return;
                        }

                        handleIterator(context, new SideEffectIterator(sideEffects.get(sideEffectKey.get()), sideEffectKey.get()));
                    } catch (Exception ex) {
                        logger.warn(String.format("Exception processing a side-effect on iteration for request [%s].", msg.getRequestId()), ex);
                        context.writeAndFlush(ResponseMessage.build(msg).code(ResponseStatusCode.SERVER_ERROR)
                                                             .statusMessage(ex.getMessage())
                                                             .statusAttributeException(ex).create());
                        onError(graph, context);
                        return;
                    }

                    onSideEffectSuccess(graph, context);
                } catch (Exception ex) {
                    logger.warn(String.format("Exception processing a side-effect on request [%s].", msg.getRequestId()), ex);
                    context.writeAndFlush(ResponseMessage.build(msg).code(ResponseStatusCode.SERVER_ERROR)
                                                         .statusMessage(ex.getMessage())
                                                         .statusAttributeException(ex).create());
                    onError(graph, context);
                } finally {
                    timerContext.stop();
                }
            });

        } catch (Exception ex) {
            timerContext.stop();
            throw new OpProcessorException("Could not iterate the side-effect instance",
                    ResponseMessage.build(msg).code(ResponseStatusCode.SERVER_ERROR)
                            .statusMessage(ex.getMessage())
                            .statusAttributeException(ex).create());
        }
    }

    private void iterateBytecodeTraversal(final Context context) throws Exception {
        final RequestMessage msg = context.getRequestMessage();
        logger.debug("Traversal request {} for in thread {}", msg.getRequestId(), Thread.currentThread().getName());

        // right now the TraversalOpProcessor can take a direct GraphSON representation of Bytecode or directly take
        // deserialized Bytecode object.
        final Object bytecodeObj = msg.getArgs().get(Tokens.ARGS_GREMLIN);
        final Bytecode bytecode = bytecodeObj instanceof Bytecode ? (Bytecode) bytecodeObj :
                mapper.readValue(bytecodeObj.toString(), Bytecode.class);

        // earlier validation in selection of this op method should free us to cast this without worry
        final Map<String, String> aliases = (Map<String, String>) msg.optionalArgs(Tokens.ARGS_ALIASES).get();

        // timeout override - handle both deprecated and newly named configuration. earlier logic should prevent
        // both configurations from being submitted at the same time
        final Map<String, Object> args = msg.getArgs();
        final long seto = args.containsKey(Tokens.ARGS_SCRIPT_EVAL_TIMEOUT) || args.containsKey(Tokens.ARGS_EVAL_TIMEOUT)
                // could be sent as an integer or long
                ? (args.containsKey(Tokens.ARGS_SCRIPT_EVAL_TIMEOUT) ?
                ((Number) args.get(Tokens.ARGS_SCRIPT_EVAL_TIMEOUT)).longValue() : ((Number) args.get(Tokens.ARGS_EVAL_TIMEOUT)).longValue())
                : context.getSettings().getEvaluationTimeout();

        final GraphManager graphManager = context.getGraphManager();
        final String traversalSourceName = aliases.entrySet().iterator().next().getValue();
        final TraversalSource g = graphManager.getTraversalSource(traversalSourceName);

        final Traversal.Admin<?, ?> traversal;
        try {
            final Optional<String> lambdaLanguage = BytecodeHelper.getLambdaLanguage(bytecode);
            if (!lambdaLanguage.isPresent())
                traversal = JavaTranslator.of(g).translate(bytecode);
            else
                traversal = context.getGremlinExecutor().eval(bytecode, EMPTY_BINDINGS, lambdaLanguage.get(), traversalSourceName);
        } catch (ScriptException ex) {
            logger.error("Traversal contains a lambda that cannot be compiled", ex);
            throw new OpProcessorException("Traversal contains a lambda that cannot be compiled",
                    ResponseMessage.build(msg).code(ResponseStatusCode.SERVER_ERROR_SCRIPT_EVALUATION)
                            .statusMessage(ex.getMessage())
                            .statusAttributeException(ex).create());
        } catch (Exception ex) {
            logger.error("Could not deserialize the Traversal instance", ex);
            throw new OpProcessorException("Could not deserialize the Traversal instance",
                    ResponseMessage.build(msg).code(ResponseStatusCode.SERVER_ERROR_SERIALIZATION)
                            .statusMessage(ex.getMessage())
                            .statusAttributeException(ex).create());
        }

        final Timer.Context timerContext = traversalOpTimer.time();
        final FutureTask<Void> evalFuture = new FutureTask<>(() -> {
            final Graph graph = g.getGraph();

            try {
                beforeProcessing(graph, context);

                try {
                    // compile the traversal - without it getEndStep() has nothing in it
                    traversal.applyStrategies();
                    handleIterator(context, new TraverserIterator(traversal), graph);
                } catch (Exception ex) {
                    Throwable t = ex;
                    if (ex instanceof UndeclaredThrowableException)
                        t = t.getCause();

                    if (t instanceof InterruptedException || t instanceof TraversalInterruptedException) {
                        final String errorMessage = String.format("A timeout occurred during traversal evaluation of [%s] - consider increasing the limit given to evaluationTimeout", msg);
                        logger.warn(errorMessage);
                        context.writeAndFlush(ResponseMessage.build(msg).code(ResponseStatusCode.SERVER_ERROR_TIMEOUT)
                                                             .statusMessage(errorMessage)
                                                             .statusAttributeException(ex).create());
                        onError(graph, context);
                    } else {
                        logger.warn(String.format("Exception processing a Traversal on iteration for request [%s].", msg.getRequestId()), ex);
                        context.writeAndFlush(ResponseMessage.build(msg).code(ResponseStatusCode.SERVER_ERROR)
                                                             .statusMessage(ex.getMessage())
                                                             .statusAttributeException(ex).create());
                        onError(graph, context);
                    }
                }
            } catch (Exception ex) {
                logger.warn(String.format("Exception processing a Traversal on request [%s].", msg.getRequestId()), ex);
                context.writeAndFlush(ResponseMessage.build(msg).code(ResponseStatusCode.SERVER_ERROR)
                                                     .statusMessage(ex.getMessage())
                                                     .statusAttributeException(ex).create());
                onError(graph, context);
            } finally {
                timerContext.stop();
            }

            return null;
        });

        final Future<?> executionFuture = context.getGremlinExecutor().getExecutorService().submit(evalFuture);
        if (seto > 0) {
            // Schedule a timeout in the thread pool for future execution
            context.getScheduledExecutorService().schedule(() -> executionFuture.cancel(true), seto, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    protected void iterateComplete(final ChannelHandlerContext ctx, final RequestMessage msg, final Iterator itty) {
        if (itty instanceof TraverserIterator) {
            final Traversal.Admin traversal = ((TraverserIterator) itty).getTraversal();
            if (!traversal.getSideEffects().isEmpty()) {
                cache.put(msg.getRequestId(), traversal.getSideEffects());
            }
        }
    }

    protected void beforeProcessing(final Graph graph, final Context ctx) {
        if (graph.features().graph().supportsTransactions() && graph.tx().isOpen()) graph.tx().rollback();
    }

    protected void onError(final Graph graph, final Context ctx) {
        if (graph.features().graph().supportsTransactions() && graph.tx().isOpen()) graph.tx().rollback();
    }

    protected void onTraversalSuccess(final Graph graph, final Context ctx) {
        if (graph.features().graph().supportsTransactions() && graph.tx().isOpen()) graph.tx().commit();
    }

    protected void onSideEffectSuccess(final Graph graph, final Context ctx) {
        // there was no "writing" here, just side-effect retrieval, so if a transaction was opened then
        // just close with rollback
        if (graph.features().graph().supportsTransactions() && graph.tx().isOpen()) graph.tx().rollback();
    }

    @Override
    protected Map<String, Object> generateMetaData(final ChannelHandlerContext ctx, final RequestMessage msg,
                                                   final ResponseStatusCode code, final Iterator itty) {
        // leaving this overriding the deprecated version of this method because it provides a decent test to those
        // who might have their own OpProcessor implementations that apply meta-data. leaving this alone helps validate
        // that the upgrade path is clean.  it can be removed at the next breaking change 3.5.0
        Map<String, Object> metaData = Collections.emptyMap();
        if (itty instanceof SideEffectIterator) {
            final SideEffectIterator traversalIterator = (SideEffectIterator) itty;
            final String key = traversalIterator.getSideEffectKey();
            if (key != null) {
                metaData = new HashMap<>();
                metaData.put(Tokens.ARGS_SIDE_EFFECT_KEY, key);
                metaData.put(Tokens.ARGS_AGGREGATE_TO, traversalIterator.getSideEffectAggregator());
            }
        } else {
            // this is a standard traversal iterator
            metaData = super.generateMetaData(ctx, msg, code, itty);
        }

        return metaData;
    }

    protected void handleIterator(final Context context, final Iterator itty, final Graph graph) throws InterruptedException {
        final ChannelHandlerContext nettyContext = context.getChannelHandlerContext();
        final RequestMessage msg = context.getRequestMessage();
        final Settings settings = context.getSettings();
        final MessageSerializer serializer = nettyContext.channel().attr(StateKey.SERIALIZER).get();
        final boolean useBinary = nettyContext.channel().attr(StateKey.USE_BINARY).get();
        boolean warnOnce = false;

        // we have an empty iterator - happens on stuff like: g.V().iterate()
        if (!itty.hasNext()) {
            final Map<String, Object> attributes = generateStatusAttributes(nettyContext, msg, ResponseStatusCode.NO_CONTENT, itty, settings);
            // if it was a g.V().iterate(), then be sure to add the side-effects to the cache
            if (itty instanceof TraverserIterator &&
                    !((TraverserIterator)itty).getTraversal().getSideEffects().isEmpty()) {
                cache.put(msg.getRequestId(), ((TraverserIterator)itty).getTraversal().getSideEffects());
            }
            // as there is nothing left to iterate if we are transaction managed then we should execute a
            // commit here before we send back a NO_CONTENT which implies success
            onTraversalSuccess(graph, context);
            context.writeAndFlush(ResponseMessage.build(msg)
                    .code(ResponseStatusCode.NO_CONTENT)
                    .statusAttributes(attributes)
                    .create());
            return;
        }

        // the batch size can be overridden by the request
        final int resultIterationBatchSize = (Integer) msg.optionalArgs(Tokens.ARGS_BATCH_SIZE)
                .orElse(settings.resultIterationBatchSize);
        List<Object> aggregate = new ArrayList<>(resultIterationBatchSize);

        // use an external control to manage the loop as opposed to just checking hasNext() in the while.  this
        // prevent situations where auto transactions create a new transaction after calls to commit() withing
        // the loop on calls to hasNext().
        boolean hasMore = itty.hasNext();

        while (hasMore) {
            if (Thread.interrupted()) throw new InterruptedException();

            // check if an implementation needs to force flush the aggregated results before the iteration batch
            // size is reached.
            final boolean forceFlush = isForceFlushed(nettyContext, msg, itty);

            // have to check the aggregate size because it is possible that the channel is not writeable (below)
            // so iterating next() if the message is not written and flushed would bump the aggregate size beyond
            // the expected resultIterationBatchSize.  Total serialization time for the response remains in
            // effect so if the client is "slow" it may simply timeout.
            //
            // there is a need to check hasNext() on the iterator because if the channel is not writeable the
            // previous pass through the while loop will have next()'d the iterator and if it is "done" then a
            // NoSuchElementException will raise its head. also need a check to ensure that this iteration doesn't
            // require a forced flush which can be forced by sub-classes.
            //
            // this could be placed inside the isWriteable() portion of the if-then below but it seems better to
            // allow iteration to continue into a batch if that is possible rather than just doing nothing at all
            // while waiting for the client to catch up
            if (aggregate.size() < resultIterationBatchSize && itty.hasNext() && !forceFlush) aggregate.add(itty.next());

            // send back a page of results if batch size is met or if it's the end of the results being iterated.
            // also check writeability of the channel to prevent OOME for slow clients.
            //
            // clients might decide to close the Netty channel to the server with a CloseWebsocketFrame after errors
            // like CorruptedFrameException. On the server, although the channel gets closed, there might be some
            // executor threads waiting for watermark to clear which will not clear in these cases since client has
            // already given up on these requests. This leads to these executors waiting for the client to consume
            // results till the timeout. checking for isActive() should help prevent that.
            if (nettyContext.channel().isActive() && nettyContext.channel().isWritable()) {
                if (forceFlush || aggregate.size() == resultIterationBatchSize || !itty.hasNext()) {
                    final ResponseStatusCode code = itty.hasNext() ? ResponseStatusCode.PARTIAL_CONTENT : ResponseStatusCode.SUCCESS;

                    // serialize here because in sessionless requests the serialization must occur in the same
                    // thread as the eval.  as eval occurs in the GremlinExecutor there's no way to get back to the
                    // thread that processed the eval of the script so, we have to push serialization down into that
                    final Map<String, Object> metadata = generateResultMetaData(nettyContext, msg, code, itty, settings);
                    final Map<String, Object> statusAttrb = generateStatusAttributes(nettyContext, msg, code, itty, settings);
                    Frame frame = null;
                    try {
                        frame = makeFrame(context, msg, serializer, useBinary, aggregate, code,
                                          metadata, statusAttrb);
                    } catch (Exception ex) {
                        // a frame may use a Bytebuf which is a countable release - if it does not get written
                        // downstream it needs to be released here
                        if (frame != null) frame.tryRelease();

                        // exception is handled in makeFrame() - serialization error gets written back to driver
                        // at that point
                        onError(graph, context);
                        break;
                    }

                    try {
                        // only need to reset the aggregation list if there's more stuff to write
                        if (itty.hasNext())
                            aggregate = new ArrayList<>(resultIterationBatchSize);
                        else {
                            // iteration and serialization are both complete which means this finished successfully. note that
                            // errors internal to script eval or timeout will rollback given GremlinServer's global configurations.
                            // local errors will get rolledback below because the exceptions aren't thrown in those cases to be
                            // caught by the GremlinExecutor for global rollback logic. this only needs to be committed if
                            // there are no more items to iterate and serialization is complete
                            onTraversalSuccess(graph, context);

                            // exit the result iteration loop as there are no more results left.  using this external control
                            // because of the above commit.  some graphs may open a new transaction on the call to
                            // hasNext()
                            hasMore = false;
                        }
                    } catch (Exception ex) {
                        // a frame may use a Bytebuf which is a countable release - if it does not get written
                        // downstream it needs to be released here
                        if (frame != null) frame.tryRelease();
                        throw ex;
                    }

                    if (!itty.hasNext()) iterateComplete(nettyContext, msg, itty);

                    // the flush is called after the commit has potentially occurred.  in this way, if a commit was
                    // required then it will be 100% complete before the client receives it. the "frame" at this point
                    // should have completely detached objects from the transaction (i.e. serialization has occurred)
                    // so a new one should not be opened on the flush down the netty pipeline
                    context.writeAndFlush(code, frame);
                }
            } else {
                // don't keep triggering this warning over and over again for the same request
                if (!warnOnce) {
                    logger.warn("Pausing response writing as writeBufferHighWaterMark exceeded on {} - writing will continue once client has caught up", msg);
                    warnOnce = true;
                }

                // since the client is lagging we can hold here for a period of time for the client to catch up.
                // this isn't blocking the IO thread - just a worker.
                TimeUnit.MILLISECONDS.sleep(10);
            }
        }
    }
}
