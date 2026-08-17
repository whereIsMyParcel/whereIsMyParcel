"""진단 이력 → SFT/eval 데이터셋 export CLI(design §13).

사용법:
    python -m logistics_agent_service.core.dataset_export_cli [--sft] [출력경로]

출력경로 생략 시 stdout으로 출력한다. --sft면 chat 포맷으로 export한다.
DATABASE_URL 환경변수가 필요하다(agent_db 조회).
"""

import sys
from pathlib import Path

from logistics_agent_service.application.service.dataset_export_service import (
    DatasetExportService,
)
from logistics_agent_service.core.config import get_settings
from logistics_agent_service.infrastructure.persistence.engine import (
    build_engine,
    build_session_factory,
    create_all,
)
from logistics_agent_service.infrastructure.persistence.sqlalchemy_diagnosis_reader import (
    SqlAlchemyDiagnosisReader,
)


def main(argv: list[str] | None = None) -> int:
    args = argv if argv is not None else sys.argv[1:]
    sft = "--sft" in args
    positional = [arg for arg in args if not arg.startswith("--")]

    settings = get_settings()
    if not settings.database_url:
        print("DATABASE_URL이 필요합니다(agent_db 조회).", file=sys.stderr)
        return 2

    engine = build_engine(settings.database_url)
    create_all(engine)
    reader = SqlAlchemyDiagnosisReader(build_session_factory(engine))
    jsonl = DatasetExportService(reader).export_jsonl(sft=sft)

    count = jsonl.count("\n") + 1 if jsonl else 0
    if positional:
        Path(positional[0]).write_text(jsonl + "\n" if jsonl else "", encoding="utf-8")
        print(f"{count} samples -> {positional[0]}", file=sys.stderr)
    else:
        print(jsonl)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
