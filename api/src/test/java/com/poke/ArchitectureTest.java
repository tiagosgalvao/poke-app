package com.poke;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.poke", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	private static final String DOMAIN = "com.poke..domain..";
	private static final String SERVICE = "com.poke..service..";
	private static final String CONTROLLER = "com.poke..controller..";
	private static final String CLIENT = "com.poke..client..";
	private static final String REPOSITORY = "com.poke..repository..";
	private static final String ENTITY = "com.poke..entity..";
	private static final String SECURITY = "com.poke..security..";

	@ArchTest
	static final ArchRule domainIsFrameworkFree = noClasses()
		.that().resideInAPackage(DOMAIN)
		.should().dependOnClassesThat().resideInAnyPackage(
			"org.springframework..", "jakarta.persistence..", "jakarta.servlet..",
			"com.fasterxml.jackson..", "tools.jackson..")
		.allowEmptyShould(true);

	@ArchTest
	static final ArchRule domainDependsOnNoOtherLayer = noClasses()
		.that().resideInAPackage(DOMAIN)
		.should().dependOnClassesThat().resideInAnyPackage(SERVICE, CONTROLLER, CLIENT, REPOSITORY, ENTITY, SECURITY)
		.allowEmptyShould(true);

	@ArchTest
	static final ArchRule servicesDependOnTheDomainOnly = noClasses()
		.that().resideInAPackage(SERVICE)
		.should().dependOnClassesThat().resideInAnyPackage(CONTROLLER, CLIENT, REPOSITORY, ENTITY, SECURITY)
		.allowEmptyShould(true);

	@ArchTest
	static final ArchRule controllersOnlyCallServices = noClasses()
		.that().resideInAPackage(CONTROLLER)
		.should().dependOnClassesThat().resideInAnyPackage(CLIENT, REPOSITORY, ENTITY, SECURITY)
		.allowEmptyShould(true);

	@ArchTest
	static final ArchRule clientsAndRepositoriesDoNotTouchControllersOrServices = noClasses()
		.that().resideInAnyPackage(CLIENT, REPOSITORY, ENTITY, SECURITY)
		.should().dependOnClassesThat().resideInAnyPackage(CONTROLLER, SERVICE)
		.allowEmptyShould(true);

	@ArchTest
	static final ArchRule sharedKernelDependsOnNoFeature = noClasses()
		.that().resideInAPackage("com.poke.shared..")
		.should().dependOnClassesThat().resideInAnyPackage("com.poke.catalog..", "com.poke.localpokemon..", "com.poke.identity..")
		.allowEmptyShould(true);

	@ArchTest
	static final ArchRule featuresAreFreeOfCycles = slices()
		.matching("com.poke.(*)..")
		.should().beFreeOfCycles()
		.allowEmptyShould(true);
}
