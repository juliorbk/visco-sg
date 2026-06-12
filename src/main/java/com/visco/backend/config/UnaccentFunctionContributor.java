package com.visco.backend.config;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Tells Hibernate's HQL/SQM parser that the PostgreSQL `unaccent()`
 * function returns a String, not an unknown Object.
 *
 * <p>Without this, `FUNCTION('unaccent', p.name) ILIKE …` fails validation
 * at startup with `SemanticException: Operand of 'like' is of type
 * 'java.lang.Object' which is not a string` on Hibernate 6+/7+.
 *
 * <p>Hibernate 7 keeps the class at {@code org.hibernate.boot.model.FunctionContributor}
 * (no {@code .function.spi}) and it is NOT registered as a ServiceLoader SPI
 * in hibernate-core, so we apply it programmatically through the
 * {@link MetadataBuilderContributor} SPI (which IS a real ServiceLoader SPI
 * at {@code META-INF/services/org.hibernate.boot.spi.MetadataBuilderContributor}).
 */
public class UnaccentFunctionContributor implements MetadataBuilderContributor {

  @Override
  public void contribute(MetadataBuilder metadataBuilder) {
    metadataBuilder.applyFunctionContributor(new FunctionContributor() {
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
    });
  }
}
