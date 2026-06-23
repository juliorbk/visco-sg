package com.visco.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.net.URISyntaxException;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnProperty("DATABASE_URL")
public class DataSourceConfig {

  @Value("${DATABASE_URL}")
  private String databaseUrl;

  @Bean
  @Primary
  public DataSource dataSource() throws URISyntaxException {
    URI dbUri = new URI(databaseUrl);

    var userInfo = dbUri.getUserInfo().split(":");
    var username = userInfo[0];
    var password = userInfo.length > 1 ? userInfo[1] : "";
    var port = dbUri.getPort() == -1 ? 5432 : dbUri.getPort();
    var jdbcUrl = "jdbc:postgresql://" + dbUri.getHost() + ":" + port + dbUri.getPath();

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
