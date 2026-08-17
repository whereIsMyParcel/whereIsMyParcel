import json

from logistics_agent_service.application.dataset import DatasetSample
from logistics_agent_service.application.port.diagnosis_reader_port import (
    DiagnosisReaderPort,
)


class DatasetExportService:
    """진단 이력을 SFT/eval 데이터셋(JSONL)으로 export한다(design §13)."""

    def __init__(self, reader: DiagnosisReaderPort) -> None:
        self._reader = reader

    def build_samples(self) -> list[DatasetSample]:
        return [
            DatasetSample.from_persisted(index, diagnosis)
            for index, diagnosis in enumerate(self._reader.list_all(), start=1)
        ]

    def export_jsonl(self, sft: bool = False) -> str:
        """샘플을 JSONL 문자열로 직렬화한다. sft=True면 chat 포맷으로 변환한다."""
        samples = self.build_samples()
        rows = [
            sample.to_sft_messages() if sft else sample.model_dump(mode="json")
            for sample in samples
        ]
        return "\n".join(json.dumps(row, ensure_ascii=False) for row in rows)
