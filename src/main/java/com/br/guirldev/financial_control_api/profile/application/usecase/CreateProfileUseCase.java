package com.br.guirldev.financial_control_api.profile.application.usecase;

import com.br.guirldev.financial_control_api.profile.application.repository.ProfileRepository;
import com.br.guirldev.financial_control_api.profile.domain.Profile;

public class CreateProfileUseCase {

    private final ProfileRepository profileRepository;

    public CreateProfileUseCase(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public Profile execute(String name) {
        Profile profile = new Profile(name);
        return profileRepository.save(profile);
    }
}