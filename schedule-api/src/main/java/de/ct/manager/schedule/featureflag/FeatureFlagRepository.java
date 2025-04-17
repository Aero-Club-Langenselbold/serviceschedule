package de.ct.manager.schedule.featureflag;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, String>{

    List<FeatureFlag> findAllByActive(int active); // 1 = true, 0 = false
}
