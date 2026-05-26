package com.sparta.whereismyparcel.aislack.archunit;

import com.sparta.whereismyparcel.common.archunit.CommonArchitectureRules;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.sparta.whereismyparcel.aislack",
        importOptions = {ImportOption.DoNotIncludeTests.class}
)
public class ArchitectureTest {

    // -------------------------------------------------------------------------
    // 규칙 1. 레이어 의존성
    // -------------------------------------------------------------------------
    @ArchTest
    static final ArchRule CONTROLLER_SHOULD_NOT_ACCESS_REPOSITORY = CommonArchitectureRules.CONTROLLER_SHOULD_NOT_ACCESS_REPOSITORY;

    @ArchTest
    static final ArchRule CONTROLLER_SHOULD_NOT_RETURN_ENTITY = CommonArchitectureRules.CONTROLLER_SHOULD_NOT_RETURN_ENTITY;

    @ArchTest
    static final ArchRule SERVICE_SHOULD_NOT_ACCESS_CONTROLLER = CommonArchitectureRules.SERVICE_SHOULD_NOT_ACCESS_CONTROLLER;

    @ArchTest
    static final ArchRule SERVICE_SHOULD_NOT_USE_WEB_LAYER = CommonArchitectureRules.SERVICE_SHOULD_NOT_USE_WEB_LAYER;

    @ArchTest
    static final ArchRule ENTITY_SHOULD_BE_INDEPENDENT = CommonArchitectureRules.ENTITY_SHOULD_BE_INDEPENDENT;

    // -------------------------------------------------------------------------
    // 규칙 2. 네이밍 컨벤션
    // -------------------------------------------------------------------------
    @ArchTest
    static final ArchRule SERVICE_NAMING_RULE = CommonArchitectureRules.SERVICE_NAMING_RULE;

    @ArchTest
    static final ArchRule CONTROLLER_NAMING_RULE = CommonArchitectureRules.CONTROLLER_NAMING_RULE;

    @ArchTest
    static final ArchRule REPOSITORY_NAMING_RULE = CommonArchitectureRules.REPOSITORY_NAMING_RULE;

    @ArchTest
    static final ArchRule FEIGN_CLIENT_NAMING_RULE = CommonArchitectureRules.FEIGN_CLIENT_NAMING_RULE;

    @ArchTest
    static final ArchRule EXCEPTION_NAMING_RULE = CommonArchitectureRules.EXCEPTION_NAMING_RULE;

    @ArchTest
    static final ArchRule DTO_NAMING_RULE = CommonArchitectureRules.DTO_NAMING_RULE;

    // -------------------------------------------------------------------------
    // 규칙 3. 패키지 위치 강제
    // -------------------------------------------------------------------------
    @ArchTest
    static final ArchRule REST_CONTROLLER_LOCATION_RULE = CommonArchitectureRules.REST_CONTROLLER_LOCATION_RULE;

    @ArchTest
    static final ArchRule SERVICE_LOCATION_RULE = CommonArchitectureRules.SERVICE_LOCATION_RULE;

    @ArchTest
    static final ArchRule CONFIGURATION_LOCATION_RULE = CommonArchitectureRules.CONFIGURATION_LOCATION_RULE;

    // -------------------------------------------------------------------------
    // 규칙 4. Soft Delete
    // -------------------------------------------------------------------------
    @ArchTest
    static final ArchRule ENTITIES_SHOULD_HAVE_SQL_RESTRICTION = CommonArchitectureRules.ENTITIES_SHOULD_HAVE_SQL_RESTRICTION;

    // -------------------------------------------------------------------------
    // 규칙 5. 예외 처리 규칙
    // -------------------------------------------------------------------------
    @ArchTest
    static final ArchRule DO_NOT_THROW_GENERIC_EXCEPTIONS = CommonArchitectureRules.DO_NOT_THROW_GENERIC_EXCEPTIONS;

    @ArchTest
    static final ArchRule DO_NOT_THROW_BUSINESS_EXCEPTION_DIRECTLY = CommonArchitectureRules.DO_NOT_THROW_BUSINESS_EXCEPTION_DIRECTLY;

}
