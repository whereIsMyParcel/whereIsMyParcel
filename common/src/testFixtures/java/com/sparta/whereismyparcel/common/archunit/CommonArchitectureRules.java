package com.sparta.whereismyparcel.common.archunit;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class CommonArchitectureRules {

    // -------------------------------------------------------------------------
    // 규칙 1. 레이어 의존성
    // -------------------------------------------------------------------------
    
    // Controller -> Repository 직접 접근 금지 (반드시 Service를 거쳐야 함)
    public static final ArchRule CONTROLLER_SHOULD_NOT_ACCESS_REPOSITORY =
            noClasses()
                    .that().resideInAPackage("..presentation.controller..")
                    .should().dependOnClassesThat().resideInAPackage("..domain.repository..")
                    .because("Controller는 Repository에 직접 접근할 수 없으며 Service를 거쳐야 합니다.").allowEmptyShould(true);

    // Controller -> Entity 직접 반환 금지 (DTO로 변환 필수)
    public static final ArchRule CONTROLLER_SHOULD_NOT_RETURN_ENTITY =
            noClasses()
                    .that().resideInAPackage("..presentation.controller..")
                    .should().dependOnClassesThat().resideInAPackage("..domain.entity..")
                    .because("Controller는 Entity를 직접 반환할 수 없으며 DTO를 사용해야 합니다.");

    // Service -> Controller 의존 금지 (역방향 금지)
    public static final ArchRule SERVICE_SHOULD_NOT_ACCESS_CONTROLLER =
            noClasses()
                    .that().resideInAPackage("..application.service..")
                    .should().dependOnClassesThat().resideInAPackage("..presentation.controller..")
                    .because("Service는 Controller에 의존할 수 없습니다.");

    // Service에서 웹 기술(HttpServletRequest 등) 사용 금지
    public static final ArchRule SERVICE_SHOULD_NOT_USE_WEB_LAYER =
            noClasses()
                    .that().resideInAPackage("..application.service..")
                    .should().dependOnClassesThat().haveNameMatching(".*HttpServletRequest.*")
                    .orShould().dependOnClassesThat().haveNameMatching(".*HttpServletResponse.*")
                    .because("Service 레이어는 웹 기술에 종속되어서는 안 됩니다.");

    // Entity에서 application, presentation, infrastructure 패키지 의존 금지
    public static final ArchRule ENTITY_SHOULD_BE_INDEPENDENT =
            noClasses()
                    .that().resideInAPackage("..domain.entity..")
                    .should().dependOnClassesThat().resideInAnyPackage("..application..", "..presentation..", "..infrastructure..")
                    .because("Entity는 도메인 영역 외의 패키지에 의존해서는 안 됩니다.");


    // -------------------------------------------------------------------------
    // 규칙 2. 네이밍 컨벤션
    // -------------------------------------------------------------------------

    // application.service 패키지의 클래스는 Service로 끝나야 함
    // (단, Abstract/Interface/익명 클래스 예외 처리)
    public static final ArchRule SERVICE_NAMING_RULE =
            classes()
                    .that().resideInAPackage("..application.service..")
                    .and().doNotHaveModifier(JavaModifier.ABSTRACT)
                    .and().areNotInterfaces()
                    .and().areNotAnonymousClasses()
                    .should().haveSimpleNameEndingWith("Service")
                    .because("application.service 패키지의 클래스는 이름이 Service로 끝나야 합니다.");

    // presentation.controller 패키지의 클래스는 Controller로 끝나야 함
    public static final ArchRule CONTROLLER_NAMING_RULE =
            classes()
                    .that().resideInAPackage("..presentation.controller..")
                    .should().haveSimpleNameEndingWith("Controller")
                    .because("presentation.controller 패키지의 클래스는 이름이 Controller로 끝나야 합니다.");

    // domain.repository 패키지의 인터페이스는 Repository로 끝나야 함
    public static final ArchRule REPOSITORY_NAMING_RULE =
            classes()
                    .that().resideInAPackage("..domain.repository..")
                    .and().areInterfaces()
                    .should().haveSimpleNameEndingWith("Repository")
                    .because("domain.repository 패키지의 인터페이스는 이름이 Repository로 끝나야 합니다.");

    // infrastructure.client 패키지의 인터페이스는 FeignClient로 끝나야 함
    public static final ArchRule FEIGN_CLIENT_NAMING_RULE =
            classes()
                    .that().resideInAPackage("..infrastructure.client..")
                    .and().areInterfaces()
                    .should().haveSimpleNameEndingWith("FeignClient")
                    .because("infrastructure.client 패키지의 인터페이스는 이름이 FeignClient로 끝나야 합니다.");

    // domain.exception 패키지의 클래스는 Exception 또는 ErrorCode로 끝나야 함
    public static final ArchRule EXCEPTION_NAMING_RULE =
            classes()
                    .that().resideInAPackage("..domain.exception..")
                    .should().haveSimpleNameEndingWith("Exception")
                    .orShould().haveSimpleNameEndingWith("ErrorCode")
                    .because("domain.exception 패키지의 클래스는 이름이 Exception 또는 ErrorCode로 끝나야 합니다.");

    // presentation.dto 패키지의 클래스는 Request 또는 Response로 끝나야 함
    public static final ArchRule DTO_NAMING_RULE =
            classes()
                    .that().resideInAPackage("..presentation.dto..")
                    .should().haveSimpleNameEndingWith("Request")
                    .orShould().haveSimpleNameEndingWith("Response")
                    .because("presentation.dto 패키지의 클래스는 Request 또는 Response로 끝나야 합니다.");


    // -------------------------------------------------------------------------
    // 규칙 3. 패키지 위치 강제
    // -------------------------------------------------------------------------

    // @RestController는 presentation.controller 패키지에만 위치
    public static final ArchRule REST_CONTROLLER_LOCATION_RULE =
            classes()
                    .that().areAnnotatedWith(RestController.class)
                    .should().resideInAPackage("..presentation.controller..")
                    .because("@RestController는 presentation.controller 패키지에 위치해야 합니다.");

    // @Service는 application.service 패키지에만 위치
    public static final ArchRule SERVICE_LOCATION_RULE =
            classes()
                    .that().areAnnotatedWith(Service.class)
                    .should().resideInAPackage("..application.service..")
                    .because("@Service는 application.service 패키지에 위치해야 합니다.");

    // @Configuration은 infrastructure.config 또는 common.config에만 위치
    public static final ArchRule CONFIGURATION_LOCATION_RULE =
            classes()
                    .that().areAnnotatedWith(Configuration.class)
                    .should().resideInAnyPackage("..infrastructure.config..", "..common.config..")
                    .because("@Configuration은 infrastructure.config 또는 common.config 패키지에 위치해야 합니다.");

    // -------------------------------------------------------------------------
    // 규칙 4. Soft Delete (@SQLRestriction 도입)
    // -------------------------------------------------------------------------

    // 모든 엔티티는 @SQLRestriction("deleted_at IS NULL")을 가져야 함
    public static final ArchRule ENTITIES_SHOULD_HAVE_SQL_RESTRICTION =
            classes()
                    .that().resideInAPackage("..domain.entity..")
                    .and().areAnnotatedWith("jakarta.persistence.Entity")
                    .should().beAnnotatedWith("org.hibernate.annotations.SQLRestriction")
                    .because("@SQLRestriction(\"deleted_at IS NULL\") 선언이 없으면 Soft Delete된 데이터가 조회에 포함됩니다. 모든 엔티티에 @SQLRestriction을 선언하세요.");

    // -------------------------------------------------------------------------
    // 규칙 5. 예외 처리 규칙
    // -------------------------------------------------------------------------

    // RuntimeException 직접 throw 금지 (반드시 커스텀 예외 사용)
    // 이 규칙은 메서드 내부의 throw 구문을 검증하기 까다롭지만, 의존성을 통해 일부 강제할 수 있습니다.
    // 하지만 완벽한 메서드 내부 구현 체크는 ArchUnit의 기본 기능만으로는 제한적이므로,
    // Exception 자체를 던지는 것이 아닌, 커스텀 Exception 클래스를 상속받도록 강제하는 방식으로 대체할 수 있습니다.

    // 일반적인 예외보다는 특정 예외 패키지 내부의 예외만 사용하도록 제한 (여기서는 간단한 의존성 규칙으로 예시)
    public static final ArchRule DO_NOT_THROW_GENERIC_EXCEPTIONS =
            noClasses()
                    .should().dependOnClassesThat().haveFullyQualifiedName("java.lang.RuntimeException")
                    .because("RuntimeException을 직접 사용하는 대신 도메인에 맞는 커스텀 예외를 정의하여 사용하세요.");

    // BusinessException 직접 throw 금지 (반드시 구체적인 서브클래스 사용)
    // domain.exception 패키지의 커스텀 예외는 BusinessException을 상속하므로 허용
    public static final ArchRule DO_NOT_THROW_BUSINESS_EXCEPTION_DIRECTLY =
            noClasses()
                    .that().resideOutsideOfPackage("..common.exception..")
                    .and().resideOutsideOfPackage("..domain.exception..")
                    .should().dependOnClassesThat().haveFullyQualifiedName("com.sparta.whereismyparcel.common.exception.BusinessException")
                    .because("BusinessException을 직접 사용하지 말고, 구체적인 커스텀 예외를 사용하세요.");

}
