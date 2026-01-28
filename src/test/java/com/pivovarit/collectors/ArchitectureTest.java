/*
 * Copyright 2014-2026 Grzegorz Piwowarek, https://4comprehension.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pivovarit.collectors;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.GeneralCodingRules;
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
    static final ArchRule shouldHaveSingletonFacade = classes()
      .that().haveSimpleName("ParallelCollectors")
      .should().haveOnlyFinalFields()
      .andShould().haveOnlyPrivateConstructors()
      .andShould().haveModifier(FINAL)
      .as("ParallelCollectors should be a singleton")
      .because("ParallelCollectors is never supposed to be instantiated");

    @ArchTest
    static final ArchRule shouldLimitPublicAPI = classes()
      .that().arePublic()
      .and().areNotNestedClasses()
      .should().haveSimpleName("ParallelCollectors")
      .orShould().haveSimpleName("Grouped")
      .orShould().haveSimpleName("StreamingConfigurer")
      .orShould().haveSimpleName("CollectingConfigurer")
      .as("limit public API surface")
      .because("only the designated API types should be public; everything else should remain internal");

    @ArchTest
    static final ArchRule shouldHaveZeroDependencies = classes()
      .that().resideInAPackage("com.pivovarit.collectors")
      .should()
      .onlyDependOnClassesThat()
      .resideInAnyPackage("com.pivovarit.collectors", "org.jspecify.annotations", "java..")
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

    @ArchTest
    private static final ArchRule noClassesShouldAccessStandardStreams = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    @ArchTest
    private static final ArchRule noClassesShouldUseJavaUtilLogging = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
}
