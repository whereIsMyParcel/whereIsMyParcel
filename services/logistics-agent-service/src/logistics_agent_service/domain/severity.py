"""진단 심각도(Severity) 매핑(design §13).

diagnosis_status에서 severity를 결정적으로 유도한다. severity는 diagnosis_status의
함수이므로 별도 eval 축으로 두지 않는다(diagnosis 정확도가 이미 함의, §14).
"""

from logistics_agent_service.domain.enums import DiagnosisStatus, Severity

_SEVERITY_BY_STATUS = {
    DiagnosisStatus.NORMAL: Severity.LOW,
    # 실패했으나 보상 완료(dangling 없음) → 안전하지만 실패 사실은 남음.
    DiagnosisStatus.FAILED_COMPENSATED: Severity.MEDIUM,
    # 평가 불가 → 사람 확인 필요.
    DiagnosisStatus.UNKNOWN: Severity.MEDIUM,
    # 정합성 이상/수동 개입 → 조치 필요.
    DiagnosisStatus.RISK_DETECTED: Severity.HIGH,
    DiagnosisStatus.MANUAL_INTERVENTION_REQUIRED: Severity.HIGH,
    # 보상 실패(dangling 재고/배송, 경계 깨짐) → 최우선.
    DiagnosisStatus.FAILED_COMPENSATION_FAILED: Severity.CRITICAL,
}


def severity_for(diagnosis_status: DiagnosisStatus) -> Severity:
    """diagnosis_status에 대응하는 severity. 미정의 상태는 보수적으로 MEDIUM."""
    return _SEVERITY_BY_STATUS.get(diagnosis_status, Severity.MEDIUM)
