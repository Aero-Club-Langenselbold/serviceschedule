package de.ct.manager.schedule.settings;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SettingsRepository extends JpaRepository<SelectedState, String> {

    @Query("SELECT c FROM Country c order by c.name ASC")
    Optional<List<Country>> findAllCountries();

    @Query("SELECT c FROM Country c where c.id = ?1")
    Optional<Country> findCountryById(String countryId);

    @Query("SELECT s FROM State s where s.country = ?1 order by s.name ASC")
    Optional<List<State>> findAllStatesByCountry(Country country);
}
