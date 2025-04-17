package de.ct.manager.schedule.featureflag;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class FeatureFlagService {

    private final FeatureFlagRepository featureFlagRepository;

    public FeatureFlagService(FeatureFlagRepository featureFlagRepository) {
        this.featureFlagRepository = featureFlagRepository;
    }

    public List<FeatureFlag> getActiveFeatureFlags() {
        return featureFlagRepository.findAllByActive(1); // 1 = true, 0 = false
    }
}
