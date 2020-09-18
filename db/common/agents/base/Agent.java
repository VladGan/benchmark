package grakn.simulation.db.common.agents.base;

import grabl.tracing.client.GrablTracingThreadStatic.ThreadContext;
import grakn.simulation.db.common.agents.interaction.InteractionAgent;
import grakn.simulation.db.common.agents.interaction.RandomValueGenerator;
import grakn.simulation.db.common.agents.utils.CheckMethod;
import grakn.simulation.db.common.context.DatabaseContext;
import grakn.simulation.db.common.world.Region;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static grabl.tracing.client.GrablTracingThreadStatic.contextOnThread;

/**
 * An agent that performs some unit of work across an object in the simulation world. Agent definitions must extend
 * this class.
 *
 * This class is instantiated via reflection by {@link AgentRunner} and initialized using
 * {@link #init(IterationContext, Random, Object, Object, String, String, Logger, Boolean)}.
 *
 * The protected methods of this class provide useful simple methods for writing Agents as concisely as possible.
 */
public abstract class Agent<REGION extends Region, CONTEXT extends DatabaseContext> implements InteractionAgent<REGION>, AutoCloseable {

    private Random random;
    private CONTEXT backendContext;
    private String sessionKey;
    private String tracker;
    private LogWrapper logWrapper;
    private ThreadContext context;
    private HashSet<String> tracedMethods = new HashSet<>();

    void init(int simulationStep, Random random, CONTEXT backendContext, String sessionKey, String tracker, Logger logger, Boolean trace) {
        this.random = random;
        this.backendContext = backendContext;
        this.sessionKey = sessionKey;
        this.tracker = tracker;
        this.logWrapper = new LogWrapper(logger);
        if (trace) {
            context = contextOnThread(tracker(), simulationStep);
        }
    }

    public LogWrapper log() {
        return logWrapper;
    }

    public RandomValueGenerator randomAttributeGenerator() {
        return new RandomValueGenerator(random);
    }

    protected CONTEXT backendContext() {
        return backendContext;
    }

    protected String tracker() {
        return tracker;
    }

    protected String getSessionKey() {
        return sessionKey;
    }

    /////////////////////////////////////////////////
    // Helper methods called from agent interfaces //
    /////////////////////////////////////////////////

    // TODO Use Autoclosable classes for these actions
    public abstract void startAction();

    public abstract void stopAction();

    public abstract void commitAction();

    public <U> U pickOne(List<U> list) { // TODO can be a util
        return list.get(random().nextInt(list.size()));
    }

    public Random random() {
        return random;
    }

    protected void shuffle(List<?> list) {
        Collections.shuffle(list, random());
    }

    /**
     * Create a unique identifier, useful for creating keys without risk of collision
     * @param iterationScopeId An id that uniquely identifies a concept within the scope of the agent at a particular iteration
     * @return
     */
    public int uniqueId(IterationContext iterationContext, int iterationScopeId) {
        String id = iterationContext.simulationStep() + tracker() + iterationScopeId;
        return id.hashCode();
    }

    ///////////////////////////////////////////////////////
    // [End] Helper methods called from agent interfaces //
    ///////////////////////////////////////////////////////

    @Override
    public void close() {
//        closeTx();
        if (context != null) {
            context.close();
        }
    }

    public class LogWrapper {
        private final Logger logger;

        private LogWrapper(Logger logger) {
            this.logger = logger;
        }

        public void query(String scope, Object query) {
            query(scope, query.toString());
        }

        public void query(String scope, String query) {
            logger.info("({}):{}:\n{}", tracker, scope, query);
        }

        public void message(String scope, String message) {
            logger.info("({}):{}:\n{}", tracker, scope, message);
        }
    }

    public String registerMethodTrace(String methodName) {
        CheckMethod.checkMethodExists(this, methodName);
        if (tracedMethods.contains(methodName)) {
            throw new RuntimeException(String.format("Method %s has already been registered for tracing for class %s", methodName, this.getClass().getName()));
        }
        tracedMethods.add(methodName);
        return methodName;
    }

    public String checkMethodTrace(String methodName) {
        CheckMethod.checkMethodExists(this, methodName);
        return methodName;
    }

    public interface ComparableField {}
}