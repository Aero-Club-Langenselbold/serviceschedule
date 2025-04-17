package de.ct.manager.schedule.featureflag;

import javax.sql.DataSource;

import org.ff4j.FF4j;
import org.ff4j.audit.repository.JdbcEventRepository;
import org.ff4j.cache.InMemoryCacheManager;
import org.ff4j.property.store.JdbcPropertyStore;
import org.ff4j.store.JdbcFeatureStore;
import org.ff4j.store.JdbcQueryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FF4jConfig {

    private static final String TABLE_PREFIX = "FEATURE_FLAGS_";

    private static final long CACHE_TTL_IN_SECONDS = 5 * 60L; // 5min

    @Bean
    public FF4j getFF4j(DataSource dataSource) {
        FF4j ff4j = new FF4j();

        // Overwriting the default prefix for tables
        JdbcFeatureStore jdbcFeatureStore = new JdbcFeatureStore();
        jdbcFeatureStore.setQueryBuilder(new JdbcQueryBuilder(TABLE_PREFIX, ""));
        jdbcFeatureStore.setDataSource(dataSource);

        JdbcPropertyStore jdbcPropertyStore = new JdbcPropertyStore();
        jdbcPropertyStore.setQueryBuilder(new JdbcQueryBuilder(TABLE_PREFIX, ""));
        jdbcPropertyStore.setDataSource(dataSource);

        JdbcEventRepository jdbcEventRepository = new JdbcEventRepository();
        jdbcEventRepository.setQueryBuilder(new JdbcQueryBuilder(TABLE_PREFIX, ""));
        jdbcEventRepository.setDataSource(dataSource);

        /*
         * Implementation of each store. Here this is boiler plate as if nothing
         * is specified the inmemory is used. Those are really the one that will
         * change depending on your technology.
         */
        ff4j.setFeatureStore(jdbcFeatureStore);
        ff4j.setPropertiesStore(jdbcPropertyStore);
        ff4j.setEventRepository(jdbcEventRepository);

        // When evaluating not existing features, ff4j will create them but disabled
        ff4j.autoCreate(true);

        // Activate cache so that not every feature check will hit the database
        ff4j.cache(new InMemoryCacheManager(CACHE_TTL_IN_SECONDS));

        return ff4j;
    }
}

