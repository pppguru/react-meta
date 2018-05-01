package org.visallo.core.model.graph;

import org.vertexium.*;
import org.vertexium.mutation.ElementMutation;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.model.workQueue.Priority;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.security.VisibilityTranslator;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper class to create or update graph Elements.
 * <p>
 * Example
 * <pre>
 * {@code
 * try (GraphUpdateContext ctx = graphRepository.beginGraphUpdate(Priority.NORMAL, user, authorizations)) {
 *   ElementMutation<Vertex> m = graph.prepareVertex("v1", visibility);
 *   ctx.update(m, updateContext -> {
 *     VisalloProperties.FILE_NAME.updateProperty(updateContext, "key", fileName, metadata, visibility);
 *   });
 * }
 * }
 * </pre>
 */
public abstract class GraphUpdateContext implements AutoCloseable {
    private static final int DEFAULT_SAVE_QUEUE_SIZE = 1000;
    private final Graph graph;
    private final WorkQueueRepository workQueueRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final Priority priority;
    private final User user;
    private final Authorizations authorizations;
    private final Queue<UpdateFuture<? extends Element>> outstandingFutures = new LinkedList<>();
    private int saveQueueSize = DEFAULT_SAVE_QUEUE_SIZE;
    private boolean pushOnQueue = true;

    protected GraphUpdateContext(
            Graph graph,
            WorkQueueRepository workQueueRepository,
            VisibilityTranslator visibilityTranslator,
            Priority priority,
            User user,
            Authorizations authorizations
    ) {
        this.graph = graph;
        this.workQueueRepository = workQueueRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.priority = priority;
        this.user = user;
        this.authorizations = authorizations;
    }

    /**
     * Saves, flushes, and pushes element on work queue.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void close() {
        flushFutures();
    }

    protected void flushFutures() {
        synchronized (outstandingFutures) {
            saveOutstandingUpdateFutures();
            graph.flush();
            if (isPushOnQueue()) {
                pushOutstandingUpdateFutures();
            }
            outstandingFutures.clear();
        }
    }

    private void pushOutstandingUpdateFutures() {
        outstandingFutures.forEach(f -> {
            try {
                workQueueRepository.pushGraphVisalloPropertyQueue(f.get(), f.getElementUpdateContext().getProperties(), priority);
            } catch (Exception ex) {
                throw new VisalloException("Could not push on queue", ex);
            }
        });
    }

    protected void saveOutstandingUpdateFutures() {
        List<UpdateFuture<?>> futures = outstandingFutures.stream()
                .filter(f -> !f.isDone())
                .collect(Collectors.toList());
        List<ElementMutation> mutations = futures.stream()
                .map(f -> f.getElementUpdateContext().getMutation())
                .collect(Collectors.toList());
        Iterable<Element> results = graph.saveElementMutations(mutations, authorizations);
        int i = 0;
        for (Element result : results) {
            UpdateFuture future = futures.get(i);
            future.setElement(result);
            i++;
        }
    }

    /**
     * Similar to {@link GraphUpdateContext#update(ElementMutation, Update)} but
     * prepares the mutation from the element.
     */
    public <T extends Element> UpdateFuture<T> update(T element, Update<T> updateFn) {
        Date modifiedDate = null;
        VisibilityJson visibilityJson = null;
        String conceptType = null;
        return update(element, modifiedDate, visibilityJson, conceptType, updateFn);
    }

    /**
     * Similar to {@link GraphUpdateContext#update(Element, Update)} but calls
     * {@link ElementUpdateContext#updateBuiltInProperties(Date, VisibilityJson)} before calling
     * updateFn.
     */
    public <T extends Element> UpdateFuture<T> update(
            T element,
            Date modifiedDate,
            VisibilityJson visibilityJson,
            Update<T> updateFn
    ) {
        String conceptType = null;
        return update(element, modifiedDate, visibilityJson, conceptType, updateFn);
    }

    /**
     * Similar to {@link GraphUpdateContext#update(Element, Update)} but calls
     * {@link ElementUpdateContext#updateBuiltInProperties(Date, VisibilityJson)} and
     * {@link ElementUpdateContext#setConceptType(String)} before calling
     * updateFn.
     */
    public <T extends Element> UpdateFuture<T> update(
            T element,
            Date modifiedDate,
            VisibilityJson visibilityJson,
            String conceptType,
            Update<T> updateFn
    ) {
        checkNotNull(element, "element cannot be null");
        return update(element.prepareMutation(), modifiedDate, visibilityJson, conceptType, updateFn);
    }

    /**
     * Calls the update function, saves the element, and adds any updates to
     * the work queue.
     */
    public <T extends Element> UpdateFuture<T> update(ElementMutation<T> m, Update<T> updateFn) {
        Date modifiedDate = null;
        VisibilityJson visibilityJson = null;
        String conceptType = null;
        return update(m, modifiedDate, visibilityJson, conceptType, updateFn);
    }

    /**
     * Similar to {@link GraphUpdateContext#update(ElementMutation, Update)} but calls
     * {@link ElementUpdateContext#updateBuiltInProperties(Date, VisibilityJson)} before calling
     * updateFn.
     */
    public <T extends Element> UpdateFuture<T> update(
            ElementMutation<T> m,
            Date modifiedDate,
            VisibilityJson visibilityJson,
            Update<T> updateFn
    ) {
        String conceptType = null;
        return update(m, modifiedDate, visibilityJson, conceptType, updateFn);
    }

    /**
     * Similar to {@link GraphUpdateContext#update(ElementMutation, Update)} but calls
     * {@link ElementUpdateContext#updateBuiltInProperties(Date, VisibilityJson)} and
     * {@link ElementUpdateContext#setConceptType(String)} before calling
     * updateFn.
     */
    public <T extends Element> UpdateFuture<T> update(
            ElementMutation<T> m,
            Date modifiedDate,
            VisibilityJson visibilityJson,
            String conceptType,
            Update<T> updateFn
    ) {
        checkNotNull(m, "element cannot be null");
        checkNotNull(updateFn, "updateFn cannot be null");

        ElementUpdateContext<T> elementUpdateContext = new ElementUpdateContext<>(visibilityTranslator, m, user);
        if (modifiedDate != null || visibilityJson != null) {
            elementUpdateContext.updateBuiltInProperties(modifiedDate, visibilityJson);
        }
        if (conceptType != null) {
            elementUpdateContext.setConceptType(conceptType);
        }
        try {
            updateFn.update(elementUpdateContext);
        } catch (Exception ex) {
            throw new VisalloException("Could not update element", ex);
        }
        UpdateFuture<T> future = new UpdateFuture<>(elementUpdateContext);
        addToOutstandingFutures(future);
        return future;
    }

    /**
     * Similar to {@link GraphUpdateContext#getOrCreateVertexAndUpdate(String, Long, Visibility, Update)}
     * using the current time as the timestamp.
     */
    public UpdateFuture<Vertex> getOrCreateVertexAndUpdate(String vertexId, Visibility visibility, Update<Vertex> updateFn) {
        return getOrCreateVertexAndUpdate(vertexId, null, visibility, updateFn);
    }

    /**
     * Gets a vertex by id from the graph. If the vertex does not exist prepares a new mutation and
     * calls update.
     */
    public UpdateFuture<Vertex> getOrCreateVertexAndUpdate(
            String vertexId,
            Long timestamp,
            Visibility visibility,
            Update<Vertex> updateFn
    ) {
        Vertex existingVertex = graph.getVertex(vertexId, getAuthorizations());
        ElementMutation<Vertex> m = existingVertex == null
                ? graph.prepareVertex(vertexId, timestamp, visibility)
                : existingVertex.prepareMutation();
        return update(m, updateFn);
    }

    /**
     * Similar to {@link GraphUpdateContext#getOrCreateEdgeAndUpdate(String, String, String, String, Long, Visibility, Update)}
     * using the current time as the timestamp.
     */
    public UpdateFuture<Edge> getOrCreateEdgeAndUpdate(
            String edgeId,
            String outVertexId,
            String inVertexId,
            String label,
            Visibility visibility,
            Update<Edge> updateFn
    ) {
        return getOrCreateEdgeAndUpdate(edgeId, outVertexId, inVertexId, label, null, visibility, updateFn);
    }

    /**
     * Gets a edge by id from the graph. If the edge does not exist prepares a new mutation and
     * calls update.
     */
    public UpdateFuture<Edge> getOrCreateEdgeAndUpdate(
            String edgeId,
            String outVertexId,
            String inVertexId,
            String label,
            Long timestamp,
            Visibility visibility,
            Update<Edge> updateFn
    ) {
        Edge existingEdge = graph.getEdge(edgeId, getAuthorizations());
        ElementMutation<Edge> m = existingEdge == null
                ? graph.prepareEdge(edgeId, outVertexId, inVertexId, label, timestamp, visibility)
                : existingEdge.prepareMutation();
        return update(m, updateFn);
    }

    private <T extends Element> void addToOutstandingFutures(UpdateFuture<T> future) {
        synchronized (outstandingFutures) {
            outstandingFutures.add(future);
            if (outstandingFutures.size() > saveQueueSize) {
                flushFutures();
            }
        }
    }

    public interface Update<T extends Element> {
        void update(ElementUpdateContext<T> elemCtx) throws Exception;
    }

    public Priority getPriority() {
        return priority;
    }

    public User getUser() {
        return user;
    }

    public Authorizations getAuthorizations() {
        return authorizations;
    }

    public int getSaveQueueSize() {
        return saveQueueSize;
    }

    /**
     * Sets the maximum number of element updates to keep in memory before they are flushed
     * and added to the work queue.
     */
    public GraphUpdateContext setSaveQueueSize(int saveQueueSize) {
        this.saveQueueSize = saveQueueSize;
        return this;
    }

    public boolean isPushOnQueue() {
        return pushOnQueue;
    }

    public Graph getGraph() {
        return graph;
    }

    /**
     * By default updates are added to the work queue. If this is false updates will be
     * saved but not added to the work queue.
     */
    public GraphUpdateContext setPushOnQueue(boolean pushOnQueue) {
        this.pushOnQueue = pushOnQueue;
        return this;
    }

    public class UpdateFuture<T extends Element> implements Future<T> {
        private final ElementUpdateContext<T> elementUpdateContext;
        private T element;

        public UpdateFuture(ElementUpdateContext<T> elementUpdateContext) {
            this.elementUpdateContext = elementUpdateContext;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            throw new VisalloException("Not supported");
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return element != null;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            if (element == null) {
                element = this.elementUpdateContext.getMutation().save(authorizations);
            }
            return element;
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return get();
        }

        protected ElementUpdateContext<T> getElementUpdateContext() {
            return elementUpdateContext;
        }

        protected void setElement(T element) {
            this.element = element;
        }
    }
}
