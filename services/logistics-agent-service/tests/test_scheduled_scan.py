from uuid import UUID, uuid4

from logistics_agent_service.agent.graph.diagnosis_workflow import (
    LangGraphDiagnosisWorkflow,
)
from logistics_agent_service.application.dto import OrderContext
from logistics_agent_service.application.service.diagnosis_service import DiagnosisService
from logistics_agent_service.application.service.scheduled_scan_service import (
    ScheduledScanService,
)
from logistics_agent_service.domain.enums import OrderStatus, TriggerType
from logistics_agent_service.infrastructure.client.fake_hub_context_client import (
    FakeHubContextClient,
)
from logistics_agent_service.infrastructure.client.fake_log_context_client import (
    FakeLogContextClient,
)
from logistics_agent_service.infrastructure.client.fake_order_scan_client import (
    FakeOrderScanClient,
)
from logistics_agent_service.infrastructure.client.fake_shipment_context_client import (
    FakeShipmentContextClient,
)
from logistics_agent_service.infrastructure.llm.stub_report_generator import (
    StubReportGenerator,
)
from logistics_agent_service.infrastructure.persistence.in_memory_diagnosis_repository import (
    InMemoryDiagnosisRepository,
)


class _EchoOrderPort:
    """스캔한 identifier를 그대로 order_id로 돌려주는 fake.

    실제 order-service 계약처럼 열거된 orderId == 조회된 order_id여야 중복 방지가
    올바로 동작한다(진단 저장 order_id로 skip 판정하므로).
    """

    def __init__(self, status: OrderStatus = OrderStatus.COMPENSATION_FAILED) -> None:
        self._status = status
        self.seen: list[str] = []

    def get_order_context(self, identifier: str) -> OrderContext:
        self.seen.append(identifier)
        return OrderContext(
            order_id=UUID(identifier),
            order_number=f"ORD-{identifier[:8]}",
            order_status=self._status,
        )


def _scan_service(
    scan_client: FakeOrderScanClient,
    order_port: _EchoOrderPort,
    repository: InMemoryDiagnosisRepository,
    statuses: list[OrderStatus] | None = None,
) -> ScheduledScanService:
    workflow = LangGraphDiagnosisWorkflow(
        order_port=order_port,
        shipment_port=FakeShipmentContextClient(statuses=[]),
        hub_port=FakeHubContextClient(),
        report_port=StubReportGenerator(),
        repository=repository,
        log_port=FakeLogContextClient(),
    )
    return ScheduledScanService(
        scan_port=scan_client,
        diagnosed_port=repository,
        diagnosis_service=DiagnosisService(workflow),
        scan_statuses=statuses or [OrderStatus.COMPENSATION_FAILED],
    )


def test_diagnoses_all_candidates() -> None:
    ids = [uuid4(), uuid4()]
    repository = InMemoryDiagnosisRepository()
    order_port = _EchoOrderPort()
    scan = _scan_service(
        FakeOrderScanClient({OrderStatus.COMPENSATION_FAILED: ids}),
        order_port,
        repository,
    )

    summary = scan.run_once()

    assert summary.scanned == 2
    assert summary.diagnosed == 2
    assert summary.skipped == 0
    assert len(summary.diagnosis_ids) == 2
    assert order_port.seen == [str(ids[0]), str(ids[1])]
    assert len(repository.store) == 2


def test_persists_with_scheduled_scan_trigger() -> None:
    repository = InMemoryDiagnosisRepository()
    scan = _scan_service(
        FakeOrderScanClient({OrderStatus.COMPENSATION_FAILED: [uuid4()]}),
        _EchoOrderPort(),
        repository,
    )

    scan.run_once()

    saved = next(iter(repository.store.values()))
    assert saved.trigger_type == TriggerType.SCHEDULED_SCAN
    # scheduled scan은 사용자 질의가 아니므로 user_question은 비운다(§12.1).
    assert saved.user_question is None


def test_skips_already_diagnosed_on_second_run() -> None:
    ids = [uuid4(), uuid4()]
    repository = InMemoryDiagnosisRepository()
    scan = _scan_service(
        FakeOrderScanClient({OrderStatus.COMPENSATION_FAILED: ids}),
        _EchoOrderPort(),
        repository,
    )

    first = scan.run_once()
    second = scan.run_once()

    assert first.diagnosed == 2
    # 두 번째 실행은 이미 진단 이력이 있어 모두 skip한다.
    assert second.scanned == 2
    assert second.diagnosed == 0
    assert second.skipped == 2
    # 재진단하지 않으므로 저장소는 늘지 않는다.
    assert len(repository.store) == 2


def test_same_order_across_statuses_diagnosed_once() -> None:
    shared = uuid4()
    repository = InMemoryDiagnosisRepository()
    scan = _scan_service(
        FakeOrderScanClient(
            {
                OrderStatus.COMPENSATION_FAILED: [shared],
                OrderStatus.FAILED: [shared],
            }
        ),
        _EchoOrderPort(),
        repository,
        statuses=[OrderStatus.COMPENSATION_FAILED, OrderStatus.FAILED],
    )

    summary = scan.run_once()

    # 두 상태에 중복 등장해도 한 스캔 안에서 한 번만 진단한다.
    assert summary.scanned == 2
    assert summary.diagnosed == 1
    assert summary.skipped == 1
    assert len(repository.store) == 1


def test_no_candidates_is_noop() -> None:
    repository = InMemoryDiagnosisRepository()
    scan = _scan_service(FakeOrderScanClient(), _EchoOrderPort(), repository)

    summary = scan.run_once()

    assert summary.scanned == 0
    assert summary.diagnosed == 0
    assert summary.skipped == 0
    assert repository.store == {}
