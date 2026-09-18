package com.br.guirldev.financial_control_api.profile.infrastructure.repository;

import com.br.guirldev.financial_control_api.profile.domain.Profile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class MongoProfileRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest");

    @DynamicPropertySource
    static void configureMongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private MongoProfileRepository mongoProfileRepository;
    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void shouldSaveProfile(){

        Profile profile = new Profile("Test Profile");
        Profile savedProfile = mongoProfileRepository.save(profile);

        ProfileDocument profileDocument =
                mongoTemplate.findById(
                        savedProfile.getId(),
                        ProfileDocument.class
                        );

        assertNotNull(profileDocument);
        assertEquals(savedProfile.getId(), profileDocument.getId());
        assertEquals(savedProfile.getName(), profileDocument.getName());
        assertEquals(savedProfile.getStatus(), profileDocument.getStatus());
    }
}