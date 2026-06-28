package com.parentcontrolapp.agent.data.model;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("id")
    public String id;

    @SerializedName("email")
    public String email;

    @SerializedName("user_metadata")
    public UserMetadata metadata;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("updated_at")
    public String updatedAt;

    public static class UserMetadata {
        @SerializedName("full_name")
        public String fullName;
    }

    public String getFullName() {
        return metadata != null ? metadata.fullName : null;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public UserMetadata getMetadata() {
        return metadata;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}