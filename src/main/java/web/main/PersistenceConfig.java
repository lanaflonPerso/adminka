package web.main;

import javax.sql.DataSource;

import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories("org.baeldung.web.dao")
@ComponentScan(basePackages = { "org.baeldung.web.components","org.baeldung.web.service","org.baeldung.web.rest"})
@EntityScan("org.baeldung.web.entity")
@Configuration
@PropertySource({"classpath:${mode:live}.properties"})
public class PersistenceConfig {

    @Autowired
    private Environment env;

    @Bean
    public DataSource dataSource() {
        PGSimpleDataSource ds = new PGSimpleDataSource() ;
        ds.setServerName( env.getProperty("database.host") );
        ds.setDatabaseName( env.getProperty("database.dbname") );
        ds.setUser( env.getProperty("database.username") );
        ds.setPassword( env.getProperty("database.password") );
        ds.setReWriteBatchedInserts(true);
        return ds;
    }

}
