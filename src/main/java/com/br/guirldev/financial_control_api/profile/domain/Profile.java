package com.br.guirldev.financial_control_api.profile.domain;

import lombok.Getter;

import java.util.UUID;
import com.br.guirldev.financial_control_api.shared.exception.InvalidNameException;

@Getter
public class Profile {

    private UUID id;
    private String name;
    private ProfileStatus status;

    public Profile(String name){
        validateName(name);
        this.id = UUID.randomUUID();
        this.name = name;
        this.status = ProfileStatus.ACTIVE;
    }

    public Profile(UUID id, String name, ProfileStatus status){
        if (id == null || name == null || status == null) {
            throw new IllegalArgumentException("All parameters must be provided");
        }
        validateName(name);
        this.id = id;
        this.name = name;
        this.status = status;
    }

    public void changeName(String name) {
        validateName(name);
        this.name = name;
    }

    public void deactivate() {
        this.status = ProfileStatus.INACTIVE;
    }

    private static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidNameException("Name cannot be null, empty or blank");
        }
        if (name.length() > 100) {
            throw new InvalidNameException("Name cannot be longer than 100 characters");
        }
    }
}