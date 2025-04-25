package de.ct.manager.schedule.settings;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CommonSettingsRepository extends JpaRepository<CommonSettings, String> {
@Query("SELECT s FROM CommonSettings s where s.settingKey = ?1")
    Optional<CommonSettings> findSettingByKey(String key);
}
