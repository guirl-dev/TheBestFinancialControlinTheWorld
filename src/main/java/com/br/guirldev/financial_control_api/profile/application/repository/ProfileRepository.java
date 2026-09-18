package com.br.guirldev.financial_control_api.profile.application.repository;

import com.br.guirldev.financial_control_api.profile.domain.Profile;

public interface ProfileRepository {
    Profile save(Profile profile);
}
