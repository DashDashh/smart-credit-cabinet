import logging
import time
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from .rules import decide
from .schemas import DecisionRequest, DecisionResponse
from .scoring import (
    get_load_error,
    get_metrics,
    get_model_version,
    is_ml_available,
    load_model,
)


# ЛОГИРОВАНИЕ
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)s | %(name)s | %(message)s",
)
logger = logging.getLogger("decision-engine")


# LIFESPAN (загрузка модели при старте)
@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Запуск decision engine...")
    ok = load_model()
    if ok:
        logger.info(f"ML модель загружена. Версия: {get_model_version()}")
    else:
        logger.warning(
            f"ML модель НЕ загружена: {get_load_error()}. "
            f"Будет использован fallback-scorecard."
        )
    yield
    logger.info("Остановка decision engine.")


app = FastAPI(
    title="Smart Credit Cabinet - Decision Engine",
    description=(
        "Сервис скоринга и принятия решения по кредитным заявкам МСП. "
        "Принимает DecisionRequest, возвращает DecisionResponse. "
        "Если ML недоступна - использует детерминированный fallback."
    ),
    version="1.0.0",
    lifespan=lifespan,
)


# CORS (если фронт на другом порту)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],       # в проде будут конкретные домены
    allow_methods=["*"],
    allow_headers=["*"],
)


# MIDDLEWARE: логирование времени
@app.middleware("http")
async def log_requests(request: Request, call_next):
    start = time.time()
    response = await call_next(request)
    duration_ms = (time.time() - start) * 1000
    logger.info(
        f"{request.method} {request.url.path} -> {response.status_code} "
        f"({duration_ms:.1f} ms)"
    )
    return response


# КОРЕНЬ
@app.get("/", tags=["service"])
def root():
    return {
        "service": "decision-engine",
        "version": "1.0.0",
        "docs": "/docs",
        "health": "/health",
        "decision": "POST /decision",
    }


# HEALTH
@app.get("/health", tags=["service"])
def health():
    """
    Статус сервиса. Всегда 200, если процесс жив.
    Содержит, доступна ли ML и какая версия.
    """
    return {
        "status": "ok",
        "ml_available": is_ml_available(),
        "model_version": get_model_version(),
        "load_error": get_load_error(),
    }


# DECISION
@app.post(
    "/decision",
    response_model=DecisionResponse,
    tags=["decision"],
    summary="Принять решение по заявке",
    responses={
        200: {"description": "Решение принято"},
        422: {"description": "Невалидный запрос (Pydantic)"},
        500: {"description": "Внутренняя ошибка"},
    },
)
def decision(req: DecisionRequest) -> DecisionResponse:
    """
    Принимает DecisionRequest (тело запроса — JSON), возвращает DecisionResponse.
    """
    try:
        return decide(req)
    except Exception as e:
        logger.exception(f"Ошибка в decide для application_id={req.application_id}")
        raise HTTPException(
            status_code=500,
            detail=f"decision engine error: {type(e).__name__}: {e}",
        )


# ГЛОБАЛЬНЫЙ ОБРАБОТЧИК ОШИБОК
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.exception(f"Unhandled error on {request.url.path}")
    return JSONResponse(
        status_code=500,
        content={"detail": f"internal error: {type(exc).__name__}: {exc}"},
    )