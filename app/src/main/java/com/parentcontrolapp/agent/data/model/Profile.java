package com.parentcontrolapp.agent.data.model;

import com.google.gson.annotations.SerializedName;

public class Profile {
    @SerializedName("id")
    public String id;

    @SerializedName("email")
    public String email;

    @SerializedName("full_name")
    public String fullName;

    @SerializedName("created_at")
    public String createdAt;

    @SerializedName("updated_at")
    public String updatedAt;

    public Profile() {
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }
}