package com.br.guirldev.financial_control_api.profile.infrastructure.repository;

import com.br.guirldev.financial_control_api.profile.domain.Profile;
import com.br.guirldev.financial_control_api.profile.domain.ProfileStatus;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document("profiles")
@Getter
public class ProfileDocument {

    @Id
    private UUID id;
    private String name;
    private ProfileStatus status;

    public ProfileDocument(Profile profile) {
        this.id = profile.getId();
        this.name = profile.getName();
        this.status = profile.getStatus();
    }

    @PersistenceCreator
    public ProfileDocument(UUID id, String name, ProfileStatus status) {
        this.id = id;
        this.name = name;
        this.status = status;
    }

    public Profile toDomain() {
        return new Profile(this.id, this.name, this.status);
    }
}
