package com.poke;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "com.poke", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	@ArchTest
	static final ArchRule layersRespectTheDependencyRule = layeredArchitecture()
			.consideringOnlyDependenciesInLayers()
			.withOptionalLayers(true)
			.layer("Domain").definedBy("..domain..")
			.layer("Application").definedBy("..application..")
			.layer("Adapters").definedBy("..adapter..")
			.layer("Config").definedBy("..config..")
			.whereLayer("Config").mayNotBeAccessedByAnyLayer()
			.whereLayer("Adapters").mayOnlyBeAccessedByLayers("Config")
			.whereLayer("Application").mayOnlyBeAccessedByLayers("Adapters", "Config")
			.whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapters", "Config");

	@ArchTest
	static final ArchRule coreIsFrameworkFree = noClasses()
			.that().resideInAnyPackage("..domain..", "..application..")
			.should().dependOnClassesThat().resideInAnyPackage(
					"org.springframework..", "jakarta.persistence..", "jakarta.servlet..", "com.fasterxml.jackson..", "tools.jackson..")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule inboundAdaptersDoNotTalkToOutboundAdapters = noClasses()
			.that().resideInAPackage("..adapter.in..")
			.should().dependOnClassesThat().resideInAPackage("..adapter.out..")
			.allowEmptyShould(true);
}
