import socket
from collections.abc import AsyncGenerator
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from app.config import settings

# Determine database URL: fall back to SQLite if PostgreSQL connection fails
db_url = settings.DATABASE_URL
is_sqlite = False

if "postgresql" in db_url:
    try:
        host = "localhost"
        port = 5432
        netloc = db_url.split("@")[-1].split("/")[0]
        if ":" in netloc:
            host, port = netloc.split(":")
            port = int(port)
        else:
            host = netloc
        
        with socket.create_connection((host, port), timeout=1.0):
            pass
    except Exception:
        db_url = "sqlite+aiosqlite:///./adaptivetrust.db"
        is_sqlite = True

engine_kwargs = {}
if is_sqlite:
    engine_kwargs = {}
else:
    engine_kwargs = {
        "pool_size": 20,
        "max_overflow": 10,
        "pool_recycle": 1800,
        "pool_pre_ping": True,
    }

# Create Async Engine
engine = create_async_engine(
    db_url,
    **engine_kwargs
)

# Create Session Factory
AsyncSessionLocal = async_sessionmaker(
    bind=engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autoflush=False,
)

# Session dependency generator for FastAPI endpoints
async def get_db() -> AsyncGenerator[AsyncSession, None]:
    async with AsyncSessionLocal() as session:
        try:
            yield session
        finally:
            await session.close()
