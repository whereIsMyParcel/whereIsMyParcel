class FakeLogContextClient:
    """서비스 없이 실행/테스트할 때 쓰는 LogContextPort 구현.

    기본값은 빈 목록(로그 없음)이라, Loki 미배선 환경에서 진단이 로그 evidence 없이
    정상 동작한다. 특정 시나리오는 lines로 주입한다.
    """

    def __init__(self, lines: list[str] | None = None) -> None:
        self._lines = [] if lines is None else lines

    def search_order_logs(self, order_id: str) -> list[str] | None:
        return list(self._lines)
