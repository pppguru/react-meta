package org.visallo.web.clientapi.model;

import java.util.ArrayList;
import java.util.List;

public class ClientApiWorkspace implements ClientApiObject {
    private String workspaceId;
    private String title;
    private String createdBy;
    private boolean isSharedToUser;
    private boolean isEditable;
    private boolean isCommentable;
    private List<User> users = new ArrayList<User>();

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isSharedToUser() {
        return isSharedToUser;
    }

    public void setSharedToUser(boolean isSharedToUser) {
        this.isSharedToUser = isSharedToUser;
    }

    public boolean isEditable() {
        return isEditable;
    }

    public void setEditable(boolean isEditable) {
        this.isEditable = isEditable;
    }

    public boolean isCommentable() {
        return isCommentable;
    }

    public void setCommentable(boolean isCommentable) {
        this.isCommentable = isCommentable;
    }

    public List<User> getUsers() {
        return users;
    }

    public void addUser(User user) {
        this.users.add(user);
    }

    @Override
    public String toString() {
        return "Workspace{" +
                "workspaceId='" + workspaceId + '\'' +
                ", title='" + title + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", isSharedToUser=" + isSharedToUser +
                ", isEditable=" + isEditable +
                '}';
    }

    public static class Vertex {
        private String vertexId;
        private boolean visible;

        public String getVertexId() {
            return vertexId;
        }

        public void setVertexId(String vertexId) {
            this.vertexId = vertexId;
        }

        public boolean isVisible() {
            return visible;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        @Override
        public String toString() {
            return "Vertex{" +
                    "vertexId='" + vertexId + '\'' +
                    ", visible=" + visible +
                    '}';
        }
    }

    public static class User {
        private String userId;
        private WorkspaceAccess access;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public WorkspaceAccess getAccess() {
            return access;
        }

        public void setAccess(WorkspaceAccess access) {
            this.access = access;
        }

        @Override
        public String toString() {
            return "User{" +
                    "userId='" + userId + '\'' +
                    ", access=" + access +
                    '}';
        }
    }
}
