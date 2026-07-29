from sqlalchemy import Engine, create_engine, event
from sqlalchemy.orm import Session, sessionmaker

from logistics_agent_service.infrastructure.persistence.models import Base

SCHEMA = "agent_db"


def build_engine(database_url: str) -> Engine:
    engine = create_engine(database_url)

    # PostgreSQL은 스키마 격리를 search_path로 처리한다(모델에 스키마를 하드코딩하지
    # 않아 SQLite 테스트와도 호환). agent_db 스키마는 인프라에서 미리 생성한다.
    if engine.dialect.name == "postgresql":

        @event.listens_for(engine, "connect")
        def _set_search_path(dbapi_connection, _record):
            cursor = dbapi_connection.cursor()
            try:
                cursor.execute(f"SET search_path TO {SCHEMA}")
            finally:
                cursor.close()

    return engine


def build_session_factory(engine: Engine) -> sessionmaker[Session]:
    return sessionmaker(engine, expire_on_commit=False)


def create_all(engine: Engine) -> None:
    """개발 편의용 테이블 생성. 운영은 마이그레이션으로 대체한다."""
    Base.metadata.create_all(engine)
