package com.visco.backend.config;

import org.hibernate.boot.model.function.FunctionContributions;
import org.hibernate.boot.model.function.FunctionContributor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Tells Hibernate's HQL parser that the PostgreSQL `unaccent()` function
 * returns a String, not an unknown Object.
 *
 * <p>Without this, `FUNCTION('unaccent', p.name) ILIKE …` fails validation
 * at startup with `SemanticException: Operand of 'like' is of type
 * 'java.lang.Object' which is not a string` on Hibernate 6+/7+.
 *
 * <p>Loaded via {@code META-INF/services/org.hibernate.boot.model.function.spi.FunctionContributor}.
 */
public class UnaccentFunctionContributor implements FunctionContributor {

  @Override
  public void contributeFunctions(FunctionContributions functionContributions) {
    TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
    functionContributions
      .getFunctionRegistry()
      .registerPattern(
        "unaccent",
        "unaccent(?1)",
        typeConfiguration
          .getBasicTypeRegistry()
          .resolve(StandardBasicTypes.STRING)
      );
  }
}
