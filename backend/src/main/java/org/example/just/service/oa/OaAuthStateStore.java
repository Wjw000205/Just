package org.example.just.service.oa;

public interface OaAuthStateStore {

    void save(String state, OaAuthState authState);

    OaAuthState consume(String state);
}
