package com.pivovarit.collectors;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Disabled;

import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;


@AnalyzeClasses(packages = "com.pivovarit", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule shouldHaveSingleFacade = classes()
      .that().arePublic()
      .should().haveSimpleName("ParallelCollectors").orShould().haveSimpleName("Batching")
      .andShould().haveOnlyFinalFields()
      .andShould().haveOnlyPrivateConstructors()
      .andShould().haveModifier(FINAL)
      .as("all public factory methods should be accessible from the ParallelCollectors and ParallelCollectors.Batching classes")
      .because("users of ParallelCollectors should have a single entry point");

    @ArchTest
    static final ArchRule shouldHaveBatchingClassesInsideParallelCollectors = classes()
      .that().arePublic().and().haveSimpleName("Batching")
      .should().beNestedClasses()
      .as("all Batching classes are sub namespaces of ParallelCollectors");

    @ArchTest
    static final ArchRule shouldHaveZeroDependencies = classes()
      .that().resideInAPackage("com.pivovarit.collectors")
      .should()
      .onlyDependOnClassesThat()
      .resideInAnyPackage("com.pivovarit.collectors", "java..")
      .as("the library should depend only on core Java classes")
      .because("users appreciate not experiencing a dependency hell");

    @ArchTest
    static final ArchRule shouldHaveSinglePackage = classes()
      .should().resideInAPackage("com.pivovarit.collectors");

    @ArchTest
    static final ArchRule shouldHaveTwoPublicClasses = classes()
      .that().haveSimpleName("ParallelCollectors").or().haveSimpleName("Batching")
      .should().bePublic().andShould().haveModifier(FINAL);
}
