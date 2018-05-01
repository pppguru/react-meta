package org.visallo.web.routes.workspace;

import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Required;
import org.json.JSONObject;
import org.vertexium.Authorizations;
import org.visallo.core.exception.VisalloResourceNotFoundException;
import org.visallo.core.model.notification.ExpirationAge;
import org.visallo.core.model.notification.ExpirationAgeUnit;
import org.visallo.core.model.notification.UserNotificationRepository;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.model.workspace.Workspace;
import org.visallo.core.model.workspace.WorkspaceRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.clientapi.model.ClientApiWorkspace;
import org.visallo.web.clientapi.model.ClientApiWorkspaceUpdateData;
import org.visallo.web.clientapi.model.WorkspaceAccess;
import org.visallo.web.parameterProviders.ActiveWorkspaceId;
import org.visallo.web.parameterProviders.SourceGuid;

import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

public class WorkspaceUpdate implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(WorkspaceUpdate.class);
    private final WorkspaceRepository workspaceRepository;
    private final WorkQueueRepository workQueueRepository;
    private final UserNotificationRepository userNotificationRepository;

    @Inject
    public WorkspaceUpdate(
            final WorkspaceRepository workspaceRepository,
            final WorkQueueRepository workQueueRepository,
            final UserNotificationRepository userNotificationRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workQueueRepository = workQueueRepository;
        this.userNotificationRepository = userNotificationRepository;
    }

    @Handle
    public ClientApiWorkspace handle(
            @Required(name = "data") ClientApiWorkspaceUpdateData updateData,
            @ActiveWorkspaceId String workspaceId,
            @SourceGuid String sourceGuid,
            ResourceBundle resourceBundle,
            User user,
            Authorizations authorizations
    ) throws Exception {
        Workspace workspace = workspaceRepository.findById(workspaceId, user);
        if (workspace == null) {
            throw new VisalloResourceNotFoundException("Could not find workspace: " + workspaceId);
        }

        if (updateData.getTitle() != null) {
            setTitle(workspace, updateData.getTitle(), user);
        }


        updateUsers(workspace, updateData.getUserUpdates(), resourceBundle, user);

        workspace = workspaceRepository.findById(workspaceId, user);
        ClientApiWorkspace clientApiWorkspaceAfterUpdateButBeforeDelete = workspaceRepository.toClientApi(
                workspace,
                user,
                authorizations
        );
        List<ClientApiWorkspace.User> previousUsers = clientApiWorkspaceAfterUpdateButBeforeDelete.getUsers();
        deleteUsers(workspace, updateData.getUserDeletes(), user);

        ClientApiWorkspace clientApiWorkspace = workspaceRepository.toClientApi(workspace, user, authorizations);

        workQueueRepository.pushWorkspaceChange(clientApiWorkspace, previousUsers, user.getUserId(), sourceGuid);

        return workspaceRepository.toClientApi(workspace, user, authorizations);
    }

    private void setTitle(Workspace workspace, String title, User authUser) {
        LOGGER.debug("setting title (%s): %s", workspace.getWorkspaceId(), title);
        workspaceRepository.setTitle(workspace, title, authUser);
    }

    private void deleteUsers(Workspace workspace, List<String> userDeletes, User authUser) {
        for (String userId : userDeletes) {
            LOGGER.debug("user delete (%s): %s", workspace.getWorkspaceId(), userId);
            workspaceRepository.deleteUserFromWorkspace(workspace, userId, authUser);
            workQueueRepository.pushWorkspaceDelete(workspace.getWorkspaceId(), userId);
        }
    }

    private void updateUsers(
            Workspace workspace,
            List<ClientApiWorkspaceUpdateData.UserUpdate> userUpdates,
            ResourceBundle resourceBundle,
            User authUser
    ) {
        for (ClientApiWorkspaceUpdateData.UserUpdate update : userUpdates) {
            LOGGER.debug("user update (%s): %s", workspace.getWorkspaceId(), update.toString());
            String userId = update.getUserId();
            WorkspaceAccess workspaceAccess = update.getAccess();
            WorkspaceRepository.UpdateUserOnWorkspaceResult updateUserOnWorkspaceResults
                    = workspaceRepository.updateUserOnWorkspace(workspace, userId, workspaceAccess, authUser);

            String title;
            String subtitle;
            switch (updateUserOnWorkspaceResults) {
                case UPDATE:
                    title = resourceBundle.getString("workspaces.notification.shareUpdated.title");
                    subtitle = resourceBundle.getString("workspaces.notification.shareUpdated.subtitle");
                    break;
                default:
                    title = resourceBundle.getString("workspaces.notification.shared.title");
                    subtitle = resourceBundle.getString("workspaces.notification.shared.subtitle");
            }
            String message = MessageFormat.format(subtitle, authUser.getDisplayName(), workspace.getDisplayTitle());
            JSONObject payload = new JSONObject();
            payload.put("workspaceId", workspace.getWorkspaceId());
            userNotificationRepository.createNotification(
                    userId,
                    title,
                    message,
                    "switchWorkspace",
                    payload,
                    new ExpirationAge(7, ExpirationAgeUnit.DAY),
                    authUser
            );
        }
    }

}
