package com.visco.backend.config;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;

/**
 * Registers the PostgreSQL `unaccent()` function as returning a String.
 *
 * <p>Without this, `FUNCTION('unaccent', p.name) ILIKE …` fails validation
 * at startup with `SemanticException: Operand of 'like' is of type
 * 'java.lang.Object' which is not a string` on Hibernate 6+/7+.
 *
 * <p>Loaded via {@code META-INF/services/org.hibernate.boot.spi.MetadataBuilderContributor}.
 */
public class UnaccentFunctionContributor implements MetadataBuilderContributor {

  @Override
  public void contribute(MetadataBuilder metadataBuilder) {
    metadataBuilder.applySqlFunction(
      "unaccent",
      new StandardSQLFunction("unaccent", StandardBasicTypes.STRING)
    );
  }
}
