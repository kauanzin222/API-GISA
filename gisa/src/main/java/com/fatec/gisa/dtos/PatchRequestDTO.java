package com.fatec.gisa.dtos;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class PatchRequestDTO {

    @JsonIgnore
    private final Set<String> providedFields = new HashSet<>();

    protected void markProvided(String field) {
        providedFields.add(field);
    }

    @JsonIgnore
    public boolean hasField(String field) {
        return providedFields.contains(field);
    }
}
