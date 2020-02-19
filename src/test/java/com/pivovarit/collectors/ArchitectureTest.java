package com.pivovarit.collectors;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOptions;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.domain.JavaModifier.*;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_ARCHIVES;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_JARS;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class ArchitectureTest {

    private static final JavaClasses classes = new ClassFileImporter()
      .importClasspath(new ImportOptions()
        .with(DO_NOT_INCLUDE_JARS)
        .with(DO_NOT_INCLUDE_ARCHIVES)
        .with(DO_NOT_INCLUDE_TESTS));

    @Test
    void shouldHaveSingleFacade() {
        classes()
          .that().arePublic()
          .should().haveSimpleName("ParallelCollectors")
          .andShould().haveOnlyFinalFields()
          .andShould().haveOnlyPrivateConstructors()
          .andShould().haveModifier(FINAL)
          .andShould().resideInAPackage("com.pivovarit.collectors")
          .as("all public factory methods should be accessible from the ParallelCollectors class")
          .because("users of ParallelCollectors should have a single entry point")
          .check(classes);
    }

    @Test
    void shouldHaveZeroDependencies() {
        classes()
          .that().resideInAPackage("com.pivovarit.collectors")
          .should()
          .dependOnClassesThat().resideInAPackage("java..")
          .as("the library should depend only on core Java classes")
          .because("users appreciate not experiencing a dependency hell")
          .check(classes);
    }

    @Test
    void shouldHaveSinglePackage() {
        classes()
          .should().resideInAPackage("com.pivovarit.collectors")
          .check(classes);
    }
}