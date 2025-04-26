package com.pivovarit.collectors;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.pivovarit", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule shouldBeFreeOfCycles = slices()
      .matching("com.pivovarit.(*)..")
      .should().beFreeOfCycles()
      .as("the library should be free of cycles")
      .because("cycles are bad");

    @ArchTest
    static final ArchRule shouldHaveSingleFacade = classes()
      .that().arePublic()
      .and().areNotNestedClasses()
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

    @ArchTest
    static final ArchRule noDefaultExecutorInCompletableFuture = ArchRuleDefinition.noClasses()
      .should().callMethod(CompletableFuture.class, "supplyAsync", Supplier.class)
      .orShould().callMethod(CompletableFuture.class, "runAsync", Runnable.class)
      .orShould().callMethod(CompletableFuture.class, "thenRunAsync", Runnable.class)
      .orShould().callMethod(CompletableFuture.class, "thenAcceptAsync", Consumer.class)
      .orShould().callMethod(CompletableFuture.class, "thenApplyAsync", Function.class)
      .orShould().callMethod(CompletableFuture.class, "thenCombineAsync", CompletionStage.class, BiFunction.class)
      .orShould().callMethod(CompletableFuture.class, "runAfterBothAsync", CompletionStage.class, Runnable.class)
      .orShould().callMethod(CompletableFuture.class, "thenAcceptBothAsync", CompletionStage.class, Consumer.class)
      .orShould().callMethod(CompletableFuture.class, "thenComposeAsync", CompletionStage.class)
      .orShould().callMethod(CompletableFuture.class, "completeAsync", Supplier.class)
      .orShould().callMethod(CompletableFuture.class, "exceptionallyComposeAsync", Function.class)
      .orShould().callMethod(CompletableFuture.class, "handleAsync", BiFunction.class)
      .orShould().callMethod(CompletableFuture.class, "applyToEitherAsync", CompletionStage.class, Function.class)
      .orShould().callMethod(CompletableFuture.class, "whenCompleteAsync", BiConsumer.class)
      .because("those default to ForkJoinPool.commonPool() which is a terrible default for most cases - consider Executors#newVirtualThreadPerTaskExecutor instead. If you really need ForkJoinPool, provide it explicitly (ForkJoinPool.commonPool())");
}
