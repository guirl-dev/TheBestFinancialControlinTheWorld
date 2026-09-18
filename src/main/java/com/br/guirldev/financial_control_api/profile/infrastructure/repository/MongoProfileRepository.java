package com.br.guirldev.financial_control_api.profile.infrastructure.repository;

import com.br.guirldev.financial_control_api.profile.application.repository.ProfileRepository;
import com.br.guirldev.financial_control_api.profile.domain.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MongoProfileRepository implements  ProfileRepository  {

    private final MongoTemplate mongoTemplate;

    public MongoProfileRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Profile save(Profile profile) {
        ProfileDocument profileDocument = new ProfileDocument(profile);
        ProfileDocument savedProfileDocument = mongoTemplate.save(profileDocument);
        return savedProfileDocument.toDomain();
    }
}