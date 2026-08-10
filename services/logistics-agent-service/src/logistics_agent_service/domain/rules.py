from logistics_agent_service.domain.enums import (
    ActionRiskLevel,
    CompensationStatus,
    DiagnosisStatus,
    FailureStep,
    OrderStatus,
)
from logistics_agent_service.domain.models import (
    Diagnosis,
    Evidence,
    RecommendedAction,
)


class RuleBasedDiagnosisEngine:
    """보상 상태 판정의 1차 출처를 Order 상태 머신으로 둔다(design §7.1).

    company의 집계 재고(reservedQuantity)는 SKU 집계라 주문 단위 보상 상태를
    판정할 수 없으므로 사용하지 않는다. 배송 연계 이상(CONFIRMED인데 배송 없음)은
    shipment 상태 목록으로, 경로 정합성 이상은 hub 경로 검증 결과(route_ok)로 판정한다.
    조회가 불가하면(None) 해당 축의 판정을 강등하고 나머지로만 진단한다.
    """

    def diagnose(
        self,
        order_status: OrderStatus | None,
        shipment_statuses: list[str] | None,
        route_ok: bool | None = None,
    ) -> Diagnosis:
        if order_status is None:
            return self._unknown()

        evidence = [
            self._status_evidence(order_status),
            self._shipment_evidence(shipment_statuses),
        ]
        if route_ok is not None:
            evidence.append(self._route_evidence(route_ok))

        match order_status:
            case OrderStatus.FAILED:
                return Diagnosis(
                    diagnosis_status=DiagnosisStatus.FAILED_COMPENSATED,
                    compensation_status=CompensationStatus.COMPLETED,
                    failed_step=FailureStep.UNKNOWN,
                    confidence=0.9,
                    summary=(
                        "주문 생성이 실패했고 보상은 완료된 것으로 판단됩니다"
                        "(잔여 재고 예약 없음)."
                    ),
                    evidence=evidence,
                )
            case OrderStatus.COMPENSATION_FAILED:
                return Diagnosis(
                    diagnosis_status=DiagnosisStatus.FAILED_COMPENSATION_FAILED,
                    compensation_status=CompensationStatus.FAILED,
                    failed_step=FailureStep.UNKNOWN,
                    confidence=0.9,
                    summary=(
                        "주문 생성 보상까지 실패해 외부 서비스에 예약 또는 배송이 "
                        "남아 있을 수 있습니다. 운영자 확인이 필요합니다."
                    ),
                    evidence=evidence,
                    recommended_actions=[
                        RecommendedAction(
                            action_type="REVIEW_COMPENSATION_FAILURE",
                            risk_level=ActionRiskLevel.READ_ONLY,
                            description=(
                                "order-service 로그와 company/shipment 상태를 확인해 "
                                "잔여 예약 재고 또는 배송을 점검합니다."
                            ),
                            requires_approval=False,
                        )
                    ],
                )
            case OrderStatus.CONFIRMED:
                if shipment_statuses is not None and len(shipment_statuses) == 0:
                    return Diagnosis(
                        diagnosis_status=DiagnosisStatus.MANUAL_INTERVENTION_REQUIRED,
                        compensation_status=CompensationStatus.NOT_REQUIRED,
                        failed_step=FailureStep.SHIPMENT_CREATION,
                        confidence=0.8,
                        summary=(
                            "주문은 CONFIRMED인데 연결된 배송이 없습니다. "
                            "배송 누락 가능성이 있어 운영자 확인이 필요합니다."
                        ),
                        evidence=evidence,
                        recommended_actions=[
                            RecommendedAction(
                                action_type="CHECK_SHIPMENT_CREATION",
                                risk_level=ActionRiskLevel.READ_ONLY,
                                description=(
                                    "shipment-service에서 orderId 기준 배송 생성 여부와 "
                                    "로그를 확인합니다."
                                ),
                                requires_approval=False,
                            )
                        ],
                    )
                if route_ok is False:
                    return Diagnosis(
                        diagnosis_status=DiagnosisStatus.RISK_DETECTED,
                        compensation_status=CompensationStatus.NOT_REQUIRED,
                        failed_step=FailureStep.HUB_ROUTE_LOOKUP,
                        confidence=0.7,
                        summary=(
                            "주문은 CONFIRMED이고 배송도 있으나, 배송 경로가 "
                            "hub-service에서 확인되지 않습니다. 허브/경로 데이터 "
                            "정합성 확인이 필요합니다."
                        ),
                        evidence=evidence,
                        recommended_actions=[
                            RecommendedAction(
                                action_type="CHECK_HUB_ROUTE",
                                risk_level=ActionRiskLevel.READ_ONLY,
                                description=(
                                    "hub-service에서 배송 허브 간 경로와 허브 유효성을 "
                                    "확인합니다."
                                ),
                                requires_approval=False,
                            )
                        ],
                    )
                return Diagnosis(
                    diagnosis_status=DiagnosisStatus.NORMAL,
                    compensation_status=CompensationStatus.NOT_REQUIRED,
                    confidence=0.8,
                    summary="주문이 정상 상태(CONFIRMED)입니다.",
                    evidence=evidence,
                )
            case OrderStatus.COMPLETED | OrderStatus.CANCELLED:
                return Diagnosis(
                    diagnosis_status=DiagnosisStatus.NORMAL,
                    compensation_status=CompensationStatus.NOT_REQUIRED,
                    confidence=0.8,
                    summary=f"주문이 정상 상태({order_status.value})입니다.",
                    evidence=evidence,
                )
            case OrderStatus.PENDING | OrderStatus.STOCK_RESERVED:
                return Diagnosis(
                    diagnosis_status=DiagnosisStatus.NORMAL,
                    compensation_status=CompensationStatus.NOT_REQUIRED,
                    confidence=0.5,
                    summary=f"주문이 처리 진행 중({order_status.value})입니다.",
                    evidence=evidence,
                )

        return self._unknown()

    def _status_evidence(self, order_status: OrderStatus) -> Evidence:
        return Evidence(
            source_service="order-service",
            tool_name="get_order_context",
            result=f"Order status is {order_status.value}",
        )

    def _shipment_evidence(self, shipment_statuses: list[str] | None) -> Evidence:
        if shipment_statuses is None:
            result = "Shipment lookup unavailable"
        elif not shipment_statuses:
            result = "No shipments found"
        else:
            result = f"Shipments: {len(shipment_statuses)} ({', '.join(shipment_statuses)})"
        return Evidence(
            source_service="shipment-service",
            tool_name="get_shipments",
            result=result,
        )

    def _route_evidence(self, route_ok: bool) -> Evidence:
        result = (
            "Shipment routes valid"
            if route_ok
            else "Shipment route not found in hub-service"
        )
        return Evidence(
            source_service="hub-service",
            tool_name="route_exists",
            result=result,
        )

    def _unknown(self) -> Diagnosis:
        return Diagnosis(
            diagnosis_status=DiagnosisStatus.UNKNOWN,
            compensation_status=CompensationStatus.UNKNOWN,
            confidence=0.0,
            summary="주문 정보를 확인할 수 없어 진단이 불가합니다.",
        )
