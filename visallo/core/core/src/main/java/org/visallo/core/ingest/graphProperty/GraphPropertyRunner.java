package org.visallo.core.ingest.graphProperty;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.vertexium.*;
import org.vertexium.property.StreamingPropertyValue;
import org.vertexium.util.IterableUtils;
import org.visallo.core.bootstrap.InjectHelper;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.WorkQueueNames;
import org.visallo.core.model.WorkerBase;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.model.user.AuthorizationRepository;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.status.MetricsManager;
import org.visallo.core.status.StatusRepository;
import org.visallo.core.status.StatusServer;
import org.visallo.core.status.model.GraphPropertyRunnerStatus;
import org.visallo.core.status.model.ProcessStatus;
import org.visallo.core.user.User;
import org.visallo.core.util.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.vertexium.util.IterableUtils.toList;

public class GraphPropertyRunner extends WorkerBase<GraphPropertyWorkerItem> {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(GraphPropertyRunner.class);
    private final StatusRepository statusRepository;
    private final AuthorizationRepository authorizationRepository;
    private Graph graph;
    private Authorizations authorizations;
    private List<GraphPropertyThreadedWrapper> workerWrappers = Lists.newArrayList();
    private User user;
    private UserRepository userRepository;
    private WorkQueueNames workQueueNames;
    private Configuration configuration;
    private VisibilityTranslator visibilityTranslator;
    private AtomicLong lastProcessedPropertyTime = new AtomicLong(0);
    private List<GraphPropertyWorker> graphPropertyWorkers = Lists.newArrayList();
    private boolean prepareWorkersCalled;

    @Inject
    protected GraphPropertyRunner(
            WorkQueueRepository workQueueRepository,
            StatusRepository statusRepository,
            Configuration configuration,
            MetricsManager metricsManager,
            AuthorizationRepository authorizationRepository
    ) {
        super(workQueueRepository, configuration, metricsManager);
        this.statusRepository = statusRepository;
        this.authorizationRepository = authorizationRepository;
    }

    @Override
    protected GraphPropertyWorkerItem tupleDataToWorkerItem(byte[] data) {
        GraphPropertyMessage message = GraphPropertyMessage.create(data);
        return new GraphPropertyWorkerItem(message, getElements(message));
    }

    @Override
    public void process(GraphPropertyWorkerItem workerItem) throws Exception {
        GraphPropertyMessage message = workerItem.getMessage();
        if (message.getProperties() != null && message.getProperties().length > 0) {
            safeExecuteHandlePropertiesOnElements(workerItem);
        } else if (message.getPropertyName() != null) {
            safeExecuteHandlePropertyOnElements(workerItem);
        } else {
            safeExecuteHandleAllEntireElements(workerItem);
        }
    }

    public void prepare(User user) {
        prepare(user, new GraphPropertyWorkerInitializer());
    }

    public void prepare(User user, GraphPropertyWorkerInitializer repository) {
        setUser(user);
        setAuthorizations(authorizationRepository.getGraphAuthorizations(user));
        prepareWorkers(repository);
        this.getWorkQueueRepository().setGraphPropertyRunner(this);
    }

    public void prepareWorkers(GraphPropertyWorkerInitializer initializer) {
        if (prepareWorkersCalled) {
            throw new VisalloException("prepareWorkers should be called only once");
        }
        prepareWorkersCalled = true;

        List<TermMentionFilter> termMentionFilters = loadTermMentionFilters();

        GraphPropertyWorkerPrepareData workerPrepareData = new GraphPropertyWorkerPrepareData(
                configuration.toMap(),
                termMentionFilters,
                this.user,
                this.authorizations,
                InjectHelper.getInjector()
        );
        Collection<GraphPropertyWorker> workers = InjectHelper.getInjectedServices(
                GraphPropertyWorker.class,
                configuration
        );
        for (GraphPropertyWorker worker : workers) {
            try {
                LOGGER.debug("verifying: %s", worker.getClass().getName());
                VerifyResults verifyResults = worker.verify();
                if (verifyResults != null && verifyResults.getFailures().size() > 0) {
                    LOGGER.error("graph property worker %s had errors verifying", worker.getClass().getName());
                    for (VerifyResults.Failure failure : verifyResults.getFailures()) {
                        LOGGER.error("  %s", failure.getMessage());
                    }
                }

                if (initializer != null) {
                    initializer.initialize(worker);
                }
            } catch (Exception ex) {
                LOGGER.error("Could not verify graph property worker %s", worker.getClass().getName(), ex);
            }
        }

        boolean failedToPrepareAtLeastOneGraphPropertyWorker = false;
        List<GraphPropertyThreadedWrapper> wrappers = Lists.newArrayList();
        for (GraphPropertyWorker worker : workers) {
            try {
                LOGGER.debug("preparing: %s", worker.getClass().getName());
                worker.prepare(workerPrepareData);
            } catch (Exception ex) {
                LOGGER.error("Could not prepare graph property worker %s", worker.getClass().getName(), ex);
                failedToPrepareAtLeastOneGraphPropertyWorker = true;
            }

            GraphPropertyThreadedWrapper wrapper = new GraphPropertyThreadedWrapper(worker);
            InjectHelper.inject(wrapper);
            wrappers.add(wrapper);
            Thread thread = new Thread(wrapper);
            String workerName = worker.getClass().getName();
            thread.setName("graphPropertyWorker-" + workerName);
            thread.start();
        }

        this.addGraphPropertyThreadedWrappers(wrappers);
        this.graphPropertyWorkers.addAll(workers);

        if (failedToPrepareAtLeastOneGraphPropertyWorker) {
            throw new VisalloException(
                    "Failed to initialize at least one graph property worker. See the log for more details.");
        }
    }

    public void addGraphPropertyThreadedWrappers(List<GraphPropertyThreadedWrapper> wrappers) {
        this.workerWrappers.addAll(wrappers);
    }

    public void addGraphPropertyThreadedWrappers(GraphPropertyThreadedWrapper... wrappers) {
        this.workerWrappers.addAll(Lists.newArrayList(wrappers));
    }

    private List<TermMentionFilter> loadTermMentionFilters() {
        TermMentionFilterPrepareData termMentionFilterPrepareData = new TermMentionFilterPrepareData(
                configuration.toMap(),
                this.user,
                this.authorizations,
                InjectHelper.getInjector()
        );

        List<TermMentionFilter> termMentionFilters = toList(ServiceLoaderUtil.load(
                TermMentionFilter.class,
                configuration
        ));
        for (TermMentionFilter termMentionFilter : termMentionFilters) {
            try {
                termMentionFilter.prepare(termMentionFilterPrepareData);
            } catch (Exception ex) {
                throw new VisalloException(
                        "Could not initialize term mention filter: " + termMentionFilter.getClass().getName(),
                        ex
                );
            }
        }
        return termMentionFilters;
    }

    @Override
    protected StatusServer createStatusServer() throws Exception {
        return new StatusServer(configuration, statusRepository, "graphProperty", GraphPropertyRunner.class) {
            @Override
            protected ProcessStatus createStatus() {
                GraphPropertyRunnerStatus status = new GraphPropertyRunnerStatus();
                for (GraphPropertyThreadedWrapper graphPropertyThreadedWrapper : workerWrappers) {
                    status.getRunningWorkers().add(graphPropertyThreadedWrapper.getStatus());
                }
                return status;
            }
        };
    }

    private void safeExecuteHandleAllEntireElements(GraphPropertyWorkerItem workerItem) throws Exception {
        for (Element element : workerItem.getElements()) {
            safeExecuteHandleEntireElement(element, workerItem.getMessage());
        }
    }

    private void safeExecuteHandleEntireElement(Element element, GraphPropertyMessage message) throws Exception {
        safeExecuteHandlePropertyOnElement(element, null, message);
        for (Property property : element.getProperties()) {
            safeExecuteHandlePropertyOnElement(element, property, message);
        }
    }

    private ImmutableList<Element> getVerticesFromMessage(GraphPropertyMessage message) {
        ImmutableList.Builder<Element> vertices = ImmutableList.builder();

        for (String vertexId : message.getGraphVertexId()) {
            Vertex vertex;
            if (message.getStatus() == ElementOrPropertyStatus.DELETION || message.getStatus() == ElementOrPropertyStatus.HIDDEN) {
                vertex = graph.getVertex(
                        vertexId,
                        FetchHint.ALL,
                        message.getBeforeActionTimestamp(),
                        this.authorizations
                );
            } else {
                vertex = graph.getVertex(vertexId, this.authorizations);
            }
            if (doesExist(vertex)) {
                vertices.add(vertex);
            } else {
                LOGGER.warn("Could not find vertex with id %s", vertexId);
            }
        }
        return vertices.build();
    }

    private ImmutableList<Element> getEdgesFromMessage(GraphPropertyMessage message) {
        ImmutableList.Builder<Element> edges = ImmutableList.builder();

        for (String edgeId : message.getGraphEdgeId()) {
            Edge edge;
            if (message.getStatus() == ElementOrPropertyStatus.DELETION || message.getStatus() == ElementOrPropertyStatus.HIDDEN) {
                edge = graph.getEdge(edgeId, FetchHint.ALL, message.getBeforeActionTimestamp(), this.authorizations);
            } else {
                edge = graph.getEdge(edgeId, this.authorizations);
            }
            if (doesExist(edge)) {
                edges.add(edge);
            } else {
                LOGGER.warn("Could not find edge with id %s", edgeId);
            }
        }
        return edges.build();
    }

    private boolean doesExist(Element element) {
        return element != null;
    }

    private void safeExecuteHandlePropertiesOnElements(GraphPropertyWorkerItem workerItem) throws Exception {
        GraphPropertyMessage message = workerItem.getMessage();
        for (Element element : workerItem.getElements()) {
            for (GraphPropertyMessage.Property propertyMessage : message.getProperties()) {
                Property property = null;
                String propertyKey = propertyMessage.getPropertyKey();
                String propertyName = propertyMessage.getPropertyName();
                if (StringUtils.isNotEmpty(propertyKey) || StringUtils.isNotEmpty(propertyName)) {
                    if (propertyKey == null) {
                        property = element.getProperty(propertyName);
                    } else {
                        property = element.getProperty(propertyKey, propertyName);
                    }

                    if (property == null) {
                        LOGGER.error(
                                "Could not find property [%s]:[%s] on vertex with id %s",
                                propertyKey,
                                propertyName,
                                element.getId()
                        );
                        continue;
                    }
                }

                safeExecuteHandlePropertyOnElement(
                        element,
                        property,
                        message.getWorkspaceId(),
                        message.getVisibilitySource(),
                        message.getPriority(),
                        message.isTraceEnabled(),
                        propertyMessage.getStatus(),
                        propertyMessage.getBeforeActionTimestampOrDefault()
                );
            }
        }
    }

    private void safeExecuteHandlePropertyOnElements(GraphPropertyWorkerItem workerItem) throws Exception {
        GraphPropertyMessage message = workerItem.getMessage();
        for (Element element : workerItem.getElements()) {
            Property property = getProperty(element, message);

            if (property != null) {
                safeExecuteHandlePropertyOnElement(element, property, message);
            } else {
                LOGGER.error(
                        "Could not find property [%s]:[%s] on vertex with id %s",
                        message.getPropertyKey(),
                        message.getPropertyName(),
                        element.getId()
                );
            }
        }
    }

    private Property getProperty(Element element, GraphPropertyMessage message) {
        if (message.getPropertyName() == null) {
            return null;
        }

        Iterable<Property> properties;

        if (message.getPropertyKey() == null) {
            properties = element.getProperties(message.getPropertyName());
        } else {
            properties = element.getProperties(message.getPropertyKey(), message.getPropertyName());
        }

        Property result = null;
        for (Property property : properties) {
            if (message.getWorkspaceId() != null && property.getVisibility().hasAuthorization(message.getWorkspaceId())) {
                result = property;
            } else if (result == null) {
                result = property;
            }
        }
        return result;
    }

    private void safeExecuteHandlePropertyOnElement(
            Element element,
            Property property,
            GraphPropertyMessage message
    ) throws Exception {
        safeExecuteHandlePropertyOnElement(
                element,
                property,
                message.getWorkspaceId(),
                message.getVisibilitySource(),
                message.getPriority(),
                message.isTraceEnabled(),
                message.getStatus(),
                message.getBeforeActionTimestampOrDefault()
        );
    }

    private void safeExecuteHandlePropertyOnElement(
            Element element,
            Property property,
            String workspaceId,
            String visibilitySource,
            Priority priority,
            boolean traceEnabled,
            ElementOrPropertyStatus status,
            long beforeActionTimestamp
    ) throws Exception {
        String propertyText = getPropertyText(property);

        List<GraphPropertyThreadedWrapper> interestedWorkerWrappers = findInterestedWorkers(element, property, status);
        if (interestedWorkerWrappers.size() == 0) {
            LOGGER.debug(
                    "Could not find interested workers for %s %s property %s (%s)",
                    element instanceof Vertex ? "vertex" : "edge",
                    element.getId(),
                    propertyText,
                    status
            );
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            for (GraphPropertyThreadedWrapper interestedWorkerWrapper : interestedWorkerWrappers) {
                LOGGER.debug(
                        "interested worker for %s %s property %s: %s (%s)",
                        element instanceof Vertex ? "vertex" : "edge",
                        element.getId(),
                        propertyText,
                        interestedWorkerWrapper.getWorker().getClass().getName(),
                        status
                );
            }
        }

        GraphPropertyWorkData workData = new GraphPropertyWorkData(
                visibilityTranslator,
                element,
                property,
                workspaceId,
                visibilitySource,
                priority,
                traceEnabled,
                beforeActionTimestamp,
                status
        );

        LOGGER.debug("Begin work on element %s property %s", element.getId(), propertyText);
        if (property != null && property.getValue() instanceof StreamingPropertyValue) {
            StreamingPropertyValue spb = (StreamingPropertyValue) property.getValue();
            safeExecuteStreamingPropertyValue(interestedWorkerWrappers, workData, spb);
        } else {
            safeExecuteNonStreamingProperty(interestedWorkerWrappers, workData);
        }

        lastProcessedPropertyTime.set(System.currentTimeMillis());

        this.graph.flush();

        LOGGER.debug("Completed work on %s", propertyText);
    }

    private String getPropertyText(Property property) {
        return property == null ? "[none]" : (property.getKey() + ":" + property.getName());
    }

    private void safeExecuteNonStreamingProperty(
            List<GraphPropertyThreadedWrapper> interestedWorkerWrappers,
            GraphPropertyWorkData workData
    ) throws Exception {
        for (GraphPropertyThreadedWrapper interestedWorkerWrapper1 : interestedWorkerWrappers) {
            interestedWorkerWrapper1.enqueueWork(null, workData);
        }

        for (GraphPropertyThreadedWrapper interestedWorkerWrapper : interestedWorkerWrappers) {
            interestedWorkerWrapper.dequeueResult(true);
        }
    }

    private void safeExecuteStreamingPropertyValue(
            List<GraphPropertyThreadedWrapper> interestedWorkerWrappers,
            GraphPropertyWorkData workData,
            StreamingPropertyValue streamingPropertyValue
    ) throws Exception {
        String[] workerNames = graphPropertyThreadedWrapperToNames(interestedWorkerWrappers);
        InputStream in = streamingPropertyValue.getInputStream();
        File tempFile = null;
        try {
            boolean requiresLocalFile = isLocalFileRequired(interestedWorkerWrappers);
            if (requiresLocalFile) {
                tempFile = copyToTempFile(in, workData);
                in = new FileInputStream(tempFile);
            }

            TeeInputStream teeInputStream = new TeeInputStream(in, workerNames);
            for (int i = 0; i < interestedWorkerWrappers.size(); i++) {
                interestedWorkerWrappers.get(i).enqueueWork(teeInputStream.getTees()[i], workData);
            }
            teeInputStream.loopUntilTeesAreClosed();
            for (GraphPropertyThreadedWrapper interestedWorkerWrapper : interestedWorkerWrappers) {
                interestedWorkerWrapper.dequeueResult(false);
            }
        } finally {
            if (tempFile != null) {
                if (!tempFile.delete()) {
                    LOGGER.warn("Could not delete temp file %s", tempFile.getAbsolutePath());
                }
            }
        }
    }

    private File copyToTempFile(InputStream in, GraphPropertyWorkData workData) throws IOException {
        String fileExt = null;
        String fileName = VisalloProperties.FILE_NAME.getOnlyPropertyValue(workData.getElement());
        if (fileName != null) {
            fileExt = FilenameUtils.getExtension(fileName);
        }
        if (fileExt == null) {
            fileExt = "data";
        }
        File tempFile = File.createTempFile("graphPropertyBolt", fileExt);
        workData.setLocalFile(tempFile);
        try (OutputStream tempFileOut = new FileOutputStream(tempFile)) {
            IOUtils.copy(in, tempFileOut);
        } finally {
            in.close();

        }
        return tempFile;
    }

    private boolean isLocalFileRequired(List<GraphPropertyThreadedWrapper> interestedWorkerWrappers) {
        for (GraphPropertyThreadedWrapper worker : interestedWorkerWrappers) {
            if (worker.getWorker().isLocalFileRequired()) {
                return true;
            }
        }
        return false;
    }

    private List<GraphPropertyThreadedWrapper> findInterestedWorkers(
            Element element,
            Property property,
            ElementOrPropertyStatus status
    ) {
        Set<String> graphPropertyWorkerWhiteList = IterableUtils.toSet(VisalloProperties.GRAPH_PROPERTY_WORKER_WHITE_LIST.getPropertyValues(
                element));
        Set<String> graphPropertyWorkerBlackList = IterableUtils.toSet(VisalloProperties.GRAPH_PROPERTY_WORKER_BLACK_LIST.getPropertyValues(
                element));

        List<GraphPropertyThreadedWrapper> interestedWorkers = new ArrayList<>();
        for (GraphPropertyThreadedWrapper wrapper : workerWrappers) {
            String graphPropertyWorkerName = wrapper.getWorker().getClass().getName();
            if (graphPropertyWorkerWhiteList.size() > 0 && !graphPropertyWorkerWhiteList.contains(
                    graphPropertyWorkerName)) {
                continue;
            }
            if (graphPropertyWorkerBlackList.contains(graphPropertyWorkerName)) {
                continue;
            }
            GraphPropertyWorker worker = wrapper.getWorker();
            if (status == ElementOrPropertyStatus.DELETION) {
                addDeletedWorkers(interestedWorkers, worker, wrapper, element, property);
            } else if (status == ElementOrPropertyStatus.HIDDEN) {
                addHiddenWorkers(interestedWorkers, worker, wrapper, element, property);
            } else if (status == ElementOrPropertyStatus.UNHIDDEN) {
                addUnhiddenWorkers(interestedWorkers, worker, wrapper, element, property);
            } else if (worker.isHandled(element, property)) {
                interestedWorkers.add(wrapper);
            }
        }

        return interestedWorkers;
    }

    private void addDeletedWorkers(
            List<GraphPropertyThreadedWrapper> interestedWorkers,
            GraphPropertyWorker worker,
            GraphPropertyThreadedWrapper wrapper,
            Element element,
            Property property
    ) {
        if (worker.isDeleteHandled(element, property)) {
            interestedWorkers.add(wrapper);
        }
    }

    private void addHiddenWorkers(
            List<GraphPropertyThreadedWrapper> interestedWorkers,
            GraphPropertyWorker worker,
            GraphPropertyThreadedWrapper wrapper,
            Element element,
            Property property
    ) {
        if (worker.isHiddenHandled(element, property)) {
            interestedWorkers.add(wrapper);
        }
    }

    private void addUnhiddenWorkers(
            List<GraphPropertyThreadedWrapper> interestedWorkers,
            GraphPropertyWorker worker,
            GraphPropertyThreadedWrapper wrapper,
            Element element,
            Property property
    ) {
        if (worker.isUnhiddenHandled(element, property)) {
            interestedWorkers.add(wrapper);
        }
    }

    private String[] graphPropertyThreadedWrapperToNames(List<GraphPropertyThreadedWrapper> interestedWorkerWrappers) {
        String[] names = new String[interestedWorkerWrappers.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = interestedWorkerWrappers.get(i).getWorker().getClass().getName();
        }
        return names;
    }

    private ImmutableList<Element> getElements(GraphPropertyMessage message) {
        ImmutableList.Builder<Element> results = ImmutableList.builder();
        if (message.getGraphVertexId() != null && message.getGraphVertexId().length > 0) {
            results.addAll(getVerticesFromMessage(message));
        }
        if (message.getGraphEdgeId() != null && message.getGraphEdgeId().length > 0) {
            results.addAll(getEdgesFromMessage(message));
        }
        return results.build();
    }

    public void shutdown() {
        for (GraphPropertyThreadedWrapper wrapper : this.workerWrappers) {
            wrapper.stop();
        }

        super.stop();
    }

    public UserRepository getUserRepository() {
        return this.userRepository;
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Inject
    public void setWorkQueueNames(WorkQueueNames workQueueNames) {
        this.workQueueNames = workQueueNames;
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Inject
    public void setVisibilityTranslator(VisibilityTranslator visibilityTranslator) {
        this.visibilityTranslator = visibilityTranslator;
    }


    public void setAuthorizations(Authorizations authorizations) {
        this.authorizations = authorizations;
    }

    public long getLastProcessedTime() {
        return this.lastProcessedPropertyTime.get();
    }

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    protected String getQueueName() {
        return workQueueNames.getGraphPropertyQueueName();
    }

    public boolean isStarted() {
        return this.shouldRun();
    }

    public boolean canHandle(Element element, Property property, ElementOrPropertyStatus status) {
        if (!this.isStarted()) {
            //we are probably on a server and want to submit it to the architecture
            return true;
        }

        for (GraphPropertyWorker worker : this.getAllGraphPropertyWorkers()) {
            try {
                if (status == ElementOrPropertyStatus.DELETION && worker.isDeleteHandled(element, property)) {
                    return true;
                } else if (status == ElementOrPropertyStatus.HIDDEN && worker.isHiddenHandled(element, property)) {
                    return true;
                } else if (status == ElementOrPropertyStatus.UNHIDDEN && worker.isUnhiddenHandled(element, property)) {
                    return true;
                } else if (worker.isHandled(element, property)) {
                    return true;
                }
            } catch (Throwable t) {
                LOGGER.warn(
                        "Error checking to see if workers will handle graph property message.  Queueing anyways in case there was just a local error",
                        t
                );
                return true;
            }
        }

        if (property == null) {
            LOGGER.debug(
                    "No interested workers for %s so did not queue it",
                    element.getId()
            );
        } else {
            LOGGER.debug(
                    "No interested workers for %s %s %s so did not queue it",
                    element.getId(),
                    property.getKey(),
                    property.getValue()
            );
        }

        return false;
    }

    public boolean canHandle(Element element, String propertyKey, String propertyName, ElementOrPropertyStatus status) {
        if (!this.isStarted()) {
            //we are probably on a server and want to submit it to the architecture
            return true;
        }

        Property property = element.getProperty(propertyKey, propertyName);

        return canHandle(element, property, status);
    }

    private Collection<GraphPropertyWorker> getAllGraphPropertyWorkers() {
        return Lists.newArrayList(this.graphPropertyWorkers);
    }

    public static List<StoppableRunnable> startThreaded(int threadCount, User user) {
        List<StoppableRunnable> stoppables = new ArrayList<>();

        LOGGER.info("Starting GraphPropertyRunners on %d threads", threadCount);
        for (int i = 0; i < threadCount; i++) {
            StoppableRunnable stoppable = new StoppableRunnable() {
                private GraphPropertyRunner graphPropertyRunner = null;

                @Override
                public void run() {
                    try {
                        graphPropertyRunner = InjectHelper.getInstance(GraphPropertyRunner.class);
                        graphPropertyRunner.prepare(user);
                        graphPropertyRunner.run();
                    } catch (Exception ex) {
                        LOGGER.error("Failed running GraphPropertyRunner", ex);
                    }
                }

                @Override
                public void stop() {
                    try {
                        if (graphPropertyRunner != null) {
                            LOGGER.debug("Stopping GraphPropertyRunner");
                            graphPropertyRunner.stop();
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Failed stopping GraphPropertyRunner", ex);
                    }
                }
            };
            stoppables.add(stoppable);
            Thread t = new Thread(stoppable);
            t.setName("graph-property-runner-" + t.getId());
            t.setDaemon(true);
            LOGGER.debug("Starting GraphPropertyRunner thread: %s", t.getName());
            t.start();
        }

        return stoppables;
    }
}
