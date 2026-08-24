import pytest

from logistics_agent_service.domain.enums import (
    DiagnosisStatus,
    OrderStatus,
    Severity,
)
from logistics_agent_service.domain.rules import RuleBasedDiagnosisEngine
from logistics_agent_service.domain.severity import severity_for

_engine = RuleBasedDiagnosisEngine()


@pytest.mark.parametrize(
    ("status", "expected"),
    [
        (DiagnosisStatus.NORMAL, Severity.LOW),
        (DiagnosisStatus.FAILED_COMPENSATED, Severity.MEDIUM),
        (DiagnosisStatus.UNKNOWN, Severity.MEDIUM),
        (DiagnosisStatus.RISK_DETECTED, Severity.HIGH),
        (DiagnosisStatus.MANUAL_INTERVENTION_REQUIRED, Severity.HIGH),
        (DiagnosisStatus.FAILED_COMPENSATION_FAILED, Severity.CRITICAL),
    ],
)
def test_severity_for_mapping(status, expected) -> None:
    assert severity_for(status) is expected


def test_normal_order_is_low() -> None:
    assert _engine.diagnose(OrderStatus.PENDING, None).severity is Severity.LOW


def test_failed_compensated_is_medium() -> None:
    assert _engine.diagnose(OrderStatus.FAILED, None).severity is Severity.MEDIUM


def test_compensation_failed_is_critical() -> None:
    assert (
        _engine.diagnose(OrderStatus.COMPENSATION_FAILED, None).severity
        is Severity.CRITICAL
    )


def test_orphan_shipment_is_high() -> None:
    diagnosis = _engine.diagnose(OrderStatus.CANCELLED, ["HUB_MOVING"])
    assert diagnosis.diagnosis_status is DiagnosisStatus.RISK_DETECTED
    assert diagnosis.severity is Severity.HIGH


def test_missing_shipment_is_high() -> None:
    diagnosis = _engine.diagnose(OrderStatus.CONFIRMED, [])
    assert diagnosis.diagnosis_status is DiagnosisStatus.MANUAL_INTERVENTION_REQUIRED
    assert diagnosis.severity is Severity.HIGH


def test_unknown_order_is_medium() -> None:
    diagnosis = _engine.diagnose(None, None)
    assert diagnosis.diagnosis_status is DiagnosisStatus.UNKNOWN
    assert diagnosis.severity is Severity.MEDIUM
