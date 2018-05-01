package org.visallo.web;

import com.codahale.metrics.Counter;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.commons.lang.StringUtils;
import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.cpr.*;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;
import org.atmosphere.interceptor.BroadcastOnPostAtmosphereInterceptor;
import org.atmosphere.interceptor.HeartbeatInterceptor;
import org.atmosphere.interceptor.JavaScriptProtocol;
import org.json.JSONObject;
import org.visallo.core.model.user.UserRepository;
import org.visallo.core.model.user.UserSessionCounterRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.status.JmxMetricsManager;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.UserStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

@AtmosphereHandlerService(
        path = Messaging.PATH,
        broadcasterCache = UUIDBroadcasterCache.class,
        interceptors = {
                AtmosphereResourceLifecycleInterceptor.class,
                BroadcastOnPostAtmosphereInterceptor.class,
                TrackMessageSizeInterceptor.class,
                HeartbeatInterceptor.class,
                JavaScriptProtocol.class
        })
public class Messaging implements AtmosphereHandler { //extends AbstractReflectorAtmosphereHandler {
    public static final String PATH = "/messaging";

    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(Messaging.class);

    private UserRepository userRepository;

    // TODO should we save off this broadcaster? When using the BroadcasterFactory
    //      we always get null when trying to get the default broadcaster
    private static Broadcaster broadcaster;
    private WorkspaceRepository workspaceRepository;
    private WorkQueueRepository workQueueRepository;
    private UserSessionCounterRepository userSessionCounterRepository;
    private boolean subscribedToBroadcast = false;
    private Map<AtmosphereResource.TRANSPORT, Counter> requestsCounters = new HashMap<>();

    @Override
    public void onRequest(AtmosphereResource resource) throws IOException {
        ensureInitialized(resource);

        CurrentUser.setUserInLogMappedDiagnosticContexts(resource.getRequest());
        try {
            Counter requestsCounter = requestsCounters.get(resource.transport());
            if (requestsCounter == null) {
                LOGGER.error("unexpected transport: " + resource.transport());
            } else {
                requestsCounter.inc();
            }

            AtmosphereRequest request = resource.getRequest();
            BufferedReader reader = request.getReader();
            String requestData = org.apache.commons.io.IOUtils.toString(reader);
            try {
                if (!StringUtils.isBlank(requestData)) {
                    processRequestData(resource, requestData);
                }
            } catch (Exception ex) {
                LOGGER.error("Could not handle async message: " + requestData, ex);
            }

            if (request.getMethod().equalsIgnoreCase("GET")) {
                onOpen(resource);
                resource.suspend();
            } else if (request.getMethod().equalsIgnoreCase("POST")) {
                LOGGER.debug("onRequest() POST: %s", requestData);
                resource.getBroadcaster().broadcast(requestData);
            }
        } finally {
            CurrentUser.clearUserFromLogMappedDiagnosticContexts();
        }
    }

    private void ensureInitialized(AtmosphereResource resource) {
        if (userRepository == null) {
            Injector injector = (Injector) resource.getAtmosphereConfig().getServletContext().getAttribute(Injector.class.getName());
            injector.injectMembers(this);
        }

        if (!subscribedToBroadcast) {
            this.workQueueRepository.subscribeToBroadcastMessages(new WorkQueueRepository.BroadcastConsumer() {
                @Override
                public void broadcastReceived(JSONObject json) {
                    if (broadcaster != null) {
                        broadcaster.broadcast(json.toString());
                    }
                }
            });
            subscribedToBroadcast = true;
        }
        broadcaster = resource.getBroadcaster();
    }

    @Override
    public void destroy() {
        LOGGER.debug("destroy");
    }

    @Override
    public void onStateChange(AtmosphereResourceEvent event) throws IOException {
        ensureInitialized(event.getResource());
        AtmosphereResponse response = ((AtmosphereResourceImpl) event.getResource()).getResponse(false);

        if (event.getMessage() != null && List.class.isAssignableFrom(event.getMessage().getClass())) {
            List<String> messages = List.class.cast(event.getMessage());
            for (String t : messages) {
                onMessage(event, response, t);
            }

        } else if (event.isClosedByApplication() || event.isClosedByClient() || event.isCancelled()) {
            onDisconnect(event, response);
        } else if (event.isSuspended()) {
            onMessage(event, response, (String) event.getMessage());
        } else if (event.isResuming()) {
            onResume(event, response);
        } else if (event.isResumedOnTimeout()) {
            onTimeout(event, response);
        }
    }

    public void onOpen(AtmosphereResource resource) throws IOException {
        setStatus(resource, UserStatus.ACTIVE);
        incrementUserSessionCount(resource);
    }

    public void onResume(AtmosphereResourceEvent event, AtmosphereResponse response) throws IOException {
        LOGGER.debug("onResume");
    }

    public void onTimeout(AtmosphereResourceEvent event, AtmosphereResponse response) throws IOException {
        LOGGER.debug("onTimeout");
    }

    public void onDisconnect(AtmosphereResourceEvent event, AtmosphereResponse response) throws IOException {
        onDisconnectOrClose(event);
    }

    public void onClose(AtmosphereResourceEvent event, AtmosphereResponse response) {
        onDisconnectOrClose(event);
    }

    private void onDisconnectOrClose(AtmosphereResourceEvent event) {
        // If POST /logout was called first the session will be invalidated.
        if (event.getResource() == null
                || event.getResource().getRequest() == null
                || event.getResource().getRequest().getSession() == null) {
            return;
        }

        boolean lastSession = decrementUserSessionCount(event.getResource());
        if (lastSession) {
            LOGGER.info("last session for user %s", getCurrentUserId(event.getResource()));
            setStatus(event.getResource(), UserStatus.OFFLINE);
        }
    }

    public void onMessage(AtmosphereResourceEvent event, AtmosphereResponse response, String message) throws IOException {
        try {
            if (!StringUtils.isBlank(message)) {
                processRequestData(event.getResource(), message);
            }
        } catch (Exception ex) {
            LOGGER.error("Could not handle async message: " + message, ex);
        }
        if (message != null) {
            response.write(message);
        } else {
            onDisconnectOrClose(event);
        }
    }

    private void processRequestData(AtmosphereResource resource, String message) {
        JSONObject messageJson = new JSONObject(message);
        String type = messageJson.optString("type", null);

        // Handle posts form client
        if (type != null) {
            switch (type) {
                case MessagingFilter.TYPE_SET_ACTIVE_WORKSPACE:
                    String authUserId = getCurrentUserId(resource);
                    JSONObject dataJson = messageJson.optJSONObject("data");
                    if (dataJson != null) {
                        String workspaceId = dataJson.getString("workspaceId");
                        switchWorkspace(authUserId, workspaceId);
                    }
                    break;
            }
        }
    }

    private void switchWorkspace(String authUserId, String workspaceId) {
        if (!workspaceId.equals(userRepository.getCurrentWorkspaceId(authUserId))) {
            User authUser = userRepository.findById(authUserId);
            Workspace workspace = workspaceRepository.findById(workspaceId, authUser);
            userRepository.setCurrentWorkspace(authUserId, workspace.getWorkspaceId());
            workQueueRepository.pushUserCurrentWorkspaceChange(authUser, workspace.getWorkspaceId());

            LOGGER.debug("User %s switched current workspace to %s", authUserId, workspaceId);
        }
    }

    private void setStatus(AtmosphereResource resource, UserStatus status) {
        broadcaster = resource.getBroadcaster();
        try {
            String authUserId = CurrentUser.getUserId(resource.getRequest());
            checkNotNull(authUserId, "Could not find user in session");
            User authUser = userRepository.findById(authUserId);
            checkNotNull(authUser, "Could not find user with id: " + authUserId);

            if (authUser.getUserStatus() != status) {
                LOGGER.debug("Setting user %s status to %s", authUserId, status.toString());
                userRepository.setStatus(authUserId, status);

                this.workQueueRepository.pushUserStatusChange(authUser, status);
            }
        } catch (Exception ex) {
            LOGGER.error("Could not update status", ex);
        } finally {
            // TODO session is held open by getAppSession
            // session.close();
        }
    }

    private void incrementUserSessionCount(AtmosphereResource resource) {
        String userId = getCurrentUserId(resource);
        boolean autoDelete = !(resource.transport() == AtmosphereResource.TRANSPORT.WEBSOCKET);
        userSessionCounterRepository.updateSession(userId, resource.uuid(), autoDelete);
    }

    private boolean decrementUserSessionCount(AtmosphereResource resource) {
        String userId = getCurrentUserId(resource);
        if (userId == null) {
            LOGGER.debug("userId could not be found in Atmosphere session");
            return false;
        }
        return userSessionCounterRepository.deleteSession(userId, resource.uuid()) < 1;
    }

    private String getCurrentUserId(AtmosphereResource resource) {
        if (resource.getRequest().getSession() == null) {
            return null;
        }

        String userId = CurrentUser.getUserId(resource.getRequest());
        if (userId != null && userId.trim().length() > 0) {
            return userId;
        }

        return null;
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Inject
    public void setWorkspaceRepository(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Inject
    public void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    @Inject
    public void setUserSessionCounterRepository(UserSessionCounterRepository userSessionCounterRepository) {
        this.userSessionCounterRepository = userSessionCounterRepository;
    }

    @Inject
    public void setMetricsManager(JmxMetricsManager metricsManager) {
        String namePrefix = metricsManager.getNamePrefix(this);
        for (AtmosphereResource.TRANSPORT transport : AtmosphereResource.TRANSPORT.values()) {
            requestsCounters.put(transport, metricsManager.counter(namePrefix + transport.name()));
        }
    }
}
