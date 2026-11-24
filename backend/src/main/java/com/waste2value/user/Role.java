package com.waste2value.user;

public enum Role {
    GENERATOR,
    COLLECTOR,
    PROCESSOR,
    ADMIN;

    public String toAuthority() {
        return "ROLE_" + this.name();
    }
}
