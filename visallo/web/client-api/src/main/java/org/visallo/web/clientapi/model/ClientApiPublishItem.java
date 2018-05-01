package org.visallo.web.clientapi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.visallo.web.clientapi.util.ClientApiConverter;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ClientApiVertexPublishItem.class, name = "vertex"),
        @JsonSubTypes.Type(value = ClientApiPropertyPublishItem.class, name = "property"),
        @JsonSubTypes.Type(value = ClientApiRelationshipPublishItem.class, name = "relationship")
})
public abstract class ClientApiPublishItem implements ClientApiObject {
    private Action action;
    private String errorMessage;

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public abstract String getType();

    @Override
    public String toString() {
        return ClientApiConverter.clientApiToString(this);
    }

    public enum Action {
        DELETE, ADD_OR_UPDATE;

        @JsonCreator
        public static Action create(String value) {
            if (value == null) {
                return ADD_OR_UPDATE;
            }
            if (value.equalsIgnoreCase(DELETE.name())) {
                return DELETE;
            }
            return ADD_OR_UPDATE;
        }
    }
}
