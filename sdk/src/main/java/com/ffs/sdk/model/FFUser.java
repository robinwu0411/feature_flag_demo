package com.ffs.sdk.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FFUser {
    private final String id;
    private final Map<String, String> attributes;

    private FFUser(Builder builder) {
        this.id = builder.id;
        this.attributes = Collections.unmodifiableMap(new HashMap<>(builder.attributes));
    }

    public String getId() { return id; }
    public String getAttribute(String key) { return attributes.get(key); }
    public Map<String, String> getAttributes() { return attributes; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private final Map<String, String> attributes = new HashMap<>();

        public Builder id(String id) { this.id = id; return this; }
        public Builder region(String region) { attributes.put("region", region); return this; }
        public Builder country(String country) { attributes.put("country", country); return this; }
        public Builder plan(String plan) { attributes.put("plan", plan); return this; }
        public Builder email(String email) { attributes.put("email", email); return this; }
        public Builder custom(String key, String value) { attributes.put(key, value); return this; }

        public FFUser build() {
            if (id == null) throw new IllegalArgumentException("id is required");
            return new FFUser(this);
        }
    }
}
