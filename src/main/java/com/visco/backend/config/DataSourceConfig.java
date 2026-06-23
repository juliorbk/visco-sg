package com.visco.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

@Configuration
public class DataSourceConfig {

  @Bean
  @Primary
  public DataSource dataSource(Environment env) {
    var jdbcUrl = env.getRequiredProperty("DB_URL");
    var username = env.getProperty("DB_USERNAME", "");
    var password = env.getProperty("DB_PASSWORD", "");

    var config = new HikariConfig();
    config.setJdbcUrl(jdbcUrl);
    config.setUsername(username);
    config.setPassword(password);
    config.setMaximumPoolSize(7);
    config.setMinimumIdle(3);
    config.setIdleTimeout(300000);
    config.setMaxLifetime(1200000);
    config.setConnectionTimeout(30000);
    config.addDataSourceProperty("stringtype", "unspecified");

    return new HikariDataSource(config);
  }
}
