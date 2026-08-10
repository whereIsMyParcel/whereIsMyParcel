import json
from pathlib import Path

from logistics_agent_service.application.eval import EvalSample


def load_samples(path: str | Path) -> list[EvalSample]:
    """JSONL(한 줄에 한 EvalSample) 파일을 읽어 EvalSample 목록으로 로드한다.

    빈 줄은 건너뛴다.
    """
    samples: list[EvalSample] = []
    with Path(path).open(encoding="utf-8") as file:
        for line in file:
            line = line.strip()
            if not line:
                continue
            samples.append(EvalSample.model_validate(json.loads(line)))
    return samples
