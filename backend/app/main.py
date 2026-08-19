from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.auth import router as auth_router
from app.api.telemetry import router as telemetry_router
from app.api.sync import router as sync_router
from app.api.admin import router as admin_router
from app.api.employee import router as employee_router
from app.database import engine
from app.cache import get_redis_client

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup tasks: Create database tables if SQLite is active
    from app.database import engine, db_url
    if "sqlite" in db_url:
        from app.models.base import Base
        async with engine.begin() as conn:
            await conn.run_sync(Base.metadata.create_all)
    yield
    # Shutdown tasks (close connection pools)
    await engine.dispose()
    redis_client = get_redis_client()
    await redis_client.aclose()

app = FastAPI(
    title="AdaptiveTrust Multi-Tenant Zero Trust API",
    description="Backend gateway services for company tenant registration and secure authentication.",
    version="1.0.0",
    lifespan=lifespan
)

# CORS configuration allowing cross-origin requests and custom ngrok tracking headers
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*", "ngrok-skip-browser-warning"],  # <-- Explicitly allows ngrok tunnel handshakes
)

# Register authentication, telemetry, sync, admin, and employee routes
app.include_router(auth_router, prefix="/api/v1")
app.include_router(telemetry_router, prefix="/api/v1")
app.include_router(sync_router, prefix="/api/v1")
app.include_router(admin_router, prefix="/api/v1")
app.include_router(employee_router, prefix="/api/v1")

@app.get("/health", tags=["Health"])
def health_check():
    """Simple API status checks."""
    return {"status": "healthy", "service": "AdaptiveTrust Mobile Gateway"}