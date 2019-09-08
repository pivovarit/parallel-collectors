package com.pivovarit.collectors;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOptions;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_ARCHIVES;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_JARS;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class ArchitectureTest {

    private static final ArchRule moduleRules = classes()
      .that()
      .arePublic()
      .should().haveSimpleName("ParallelCollectors")
      .andShould().haveOnlyFinalFields()
      .andShould().haveOnlyPrivateConstructors()
      .andShould().resideInAPackage("com.pivovarit.collectors");

    private static final JavaClasses classes = new ClassFileImporter()
      .importClasspath(new ImportOptions()
        .with(DO_NOT_INCLUDE_JARS)
        .with(DO_NOT_INCLUDE_ARCHIVES)
        .with(DO_NOT_INCLUDE_TESTS));

    @Test
    void validate() {
        moduleRules.check(classes);
    }
}