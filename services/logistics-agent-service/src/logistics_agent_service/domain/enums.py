from enum import StrEnum


class OrderStatus(StrEnum):
    """order-service의 Order 상태 계약을 미러링한다(내부 API로 수신하는 값)."""

    PENDING = "PENDING"
    STOCK_RESERVED = "STOCK_RESERVED"
    CONFIRMED = "CONFIRMED"
    CANCELLED = "CANCELLED"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    COMPENSATION_FAILED = "COMPENSATION_FAILED"


class ShipmentStatus(StrEnum):
    """shipment-service의 Shipment 상태 계약을 미러링한다(내부 API로 수신하는 값).

    진행 중(살아있는) 상태: HUB_WAITING/HUB_MOVING/HUB_ARRIVED/COMPANY_MOVING.
    종료 상태: DELIVERED/CANCELLED.
    """

    HUB_WAITING = "HUB_WAITING"
    HUB_MOVING = "HUB_MOVING"
    HUB_ARRIVED = "HUB_ARRIVED"
    COMPANY_MOVING = "COMPANY_MOVING"
    DELIVERED = "DELIVERED"
    CANCELLED = "CANCELLED"


class TriggerType(StrEnum):
    USER_QUERY = "USER_QUERY"
    SYSTEM_INCIDENT = "SYSTEM_INCIDENT"
    SCHEDULED_SCAN = "SCHEDULED_SCAN"


class DiagnosisStatus(StrEnum):
    NORMAL = "NORMAL"
    FAILED_COMPENSATED = "FAILED_COMPENSATED"
    FAILED_COMPENSATION_FAILED = "FAILED_COMPENSATION_FAILED"
    MANUAL_INTERVENTION_REQUIRED = "MANUAL_INTERVENTION_REQUIRED"
    RISK_DETECTED = "RISK_DETECTED"
    UNKNOWN = "UNKNOWN"


class Severity(StrEnum):
    """진단의 심각도/긴급도(design §13 dataset label). diagnosis_status에서 결정적으로
    매핑한다(domain.severity.severity_for). 복구 우선순위·정렬의 기준이 된다."""

    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"


class CompensationStatus(StrEnum):
    NOT_REQUIRED = "NOT_REQUIRED"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    PARTIAL = "PARTIAL"
    UNKNOWN = "UNKNOWN"


class FailureStep(StrEnum):
    ORDER_CREATION = "ORDER_CREATION"
    PRODUCT_VALIDATION = "PRODUCT_VALIDATION"
    INVENTORY_RESERVATION = "INVENTORY_RESERVATION"
    PRODUCT_HUB_LOOKUP = "PRODUCT_HUB_LOOKUP"
    RECIPIENT_HUB_LOOKUP = "RECIPIENT_HUB_LOOKUP"
    HUB_ROUTE_LOOKUP = "HUB_ROUTE_LOOKUP"
    SHIPMENT_CREATION = "SHIPMENT_CREATION"
    AI_ANALYSIS = "AI_ANALYSIS"
    SLACK_NOTIFICATION = "SLACK_NOTIFICATION"
    UNKNOWN = "UNKNOWN"


class ActionRiskLevel(StrEnum):
    READ_ONLY = "READ_ONLY"
    SAFE_WRITE = "SAFE_WRITE"
    RECOVERY_WRITE = "RECOVERY_WRITE"
    DANGEROUS_MANUAL = "DANGEROUS_MANUAL"


class ProposalStatus(StrEnum):
    """권장 조치 제안의 생애주기(design §12.4, §16.4).

    진단은 PROPOSED로 기록한다(§12.4). 승인 기반 recovery(T5b)가 실행 시:
    EXECUTED(실행 성공)·FAILED(실행 실패)·SUPERSEDED(실행 직전 재검증 결과 더 이상
    필요 없어 실행 안 함)로 전이한다. APPROVED/REJECTED는 승인/거부 상태값이다."""

    PROPOSED = "PROPOSED"
    APPROVED = "APPROVED"
    REJECTED = "REJECTED"
    EXECUTED = "EXECUTED"
    FAILED = "FAILED"
    SUPERSEDED = "SUPERSEDED"
