from typing import Protocol


class LogContextPort(Protocol):
    """주문 관련 최근 경고/오류 로그를 조회하는 포트(관측 evidence 보강).

    조회 불가(전송 오류·5xx·파싱 실패)면 None으로 강등하고, 결과가 없으면 빈 목록을
    반환한다. 로그는 분류 입력이 아니라 맥락 근거로만 쓰인다(design §5).
    """

    def search_order_logs(self, order_id: str) -> list[str] | None: ...
