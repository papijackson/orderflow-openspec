package com.orderflow.controller.fixture;

import com.orderflow.repository.fixture.RepositoryFixture;

public class ForbiddenControllerFixture {

    private final RepositoryFixture repository;

    public ForbiddenControllerFixture(RepositoryFixture repository) {
        this.repository = repository;
    }

    public RepositoryFixture repository() {
        return repository;
    }
}
