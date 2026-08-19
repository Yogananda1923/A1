import logging
import random
import uuid
from datetime import datetime, timezone, timedelta
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.database import get_db
from app.models import Company, User, UserRole, TrustLog, generate_company_code
from app.schemas.auth import (
    AdminRegisterRequest,
    EmployeeRegisterRequest,
    LoginRequest,
    TokenResponse,
    AdminRegisterResponse,
    UserResponse,
    CompanyResponse,
    VerifyEmailRequest,
    ResendCodeRequest,
    VerifyEmailResponse,
)
from app.utils.security import hash_password, verify_password, create_access_token
from app.cache import cache_trust_score
from app.services.email import EmailService

router = APIRouter(prefix="/auth", tags=["Authentication"])
logger = logging.getLogger(__name__)

def generate_otp() -> str:
    """Generates a secure 6-digit OTP string."""
    return f"{random.randint(100000, 999999)}"

@router.post("/register/admin", response_model=AdminRegisterResponse, status_code=status.HTTP_201_CREATED)
async def register_admin(
    payload: AdminRegisterRequest,
    db: AsyncSession = Depends(get_db)
):
    """
    Admin Workspace Provisioning:
    Creates a new Company tenant row, generates a unique 8-character code,
    registers the first ADMIN user, generates a 6-digit OTP, and sends a verification email.
    """
    # 1. Uniqueness check for email
    existing_user = await db.execute(select(User).where(User.email == payload.email))
    if existing_user.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email is already registered."
        )

    # 2. Uniqueness check for company name
    existing_company = await db.execute(select(Company).where(Company.name == payload.company_name))
    if existing_company.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Company name is already registered."
        )

    # 3. Create the Company tenant row
    new_company = Company(
        id=uuid.uuid4(),
        name=payload.company_name,
        company_code=generate_company_code(),
        is_active=True
    )
    db.add(new_company)
    await db.flush()

    # 4. Create the Admin User with email verification pending
    hashed_pwd = hash_password(payload.password)
    otp_code = generate_otp()
    expires_at = datetime.now(timezone.utc) + timedelta(minutes=15)

    new_admin = User(
        id=uuid.uuid4(),
        company_id=new_company.id,
        email=payload.email,
        hashed_password=hashed_pwd,
        role=UserRole.ADMIN,
        full_name=payload.full_name,
        is_active=True,
        is_email_verified=False,
        verification_code=otp_code,
        verification_expires_at=expires_at,
    )
    db.add(new_admin)
    await db.commit()
    await db.refresh(new_admin)
    await db.refresh(new_company)

    # 5. Dispatch Verification Email
    await EmailService.send_verification_email(new_admin.email, new_admin.full_name, otp_code)

    return AdminRegisterResponse(
        admin=UserResponse.model_validate(new_admin),
        company=CompanyResponse.model_validate(new_company)
    )

@router.post("/register/employee", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
async def register_employee(
    payload: EmployeeRegisterRequest,
    db: AsyncSession = Depends(get_db)
):
    """
    Employee Verification & Registration:
    Validates company code, registers user with baseline trust score = 100,
    generates 6-digit verification code, and dispatches verification email.
    """
    # 1. Check if email is already registered
    existing_user = await db.execute(select(User).where(User.email == payload.email))
    if existing_user.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email is already registered."
        )

    # 2. Validate company code
    company_stmt = select(Company).where(Company.company_code == payload.company_code.upper().strip())
    company_res = await db.execute(company_stmt)
    company = company_res.scalar_one_or_none()
    if not company:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid company registration code."
        )
    if not company.is_active:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="The company tenant is currently inactive."
        )

    # 3. Create the Employee User with email verification pending
    hashed_pwd = hash_password(payload.password)
    otp_code = generate_otp()
    expires_at = datetime.now(timezone.utc) + timedelta(minutes=15)

    new_employee = User(
        id=uuid.uuid4(),
        company_id=company.id,
        email=payload.email,
        hashed_password=hashed_pwd,
        role=UserRole.EMPLOYEE,
        full_name=payload.full_name,
        is_active=True,
        is_email_verified=False,
        verification_code=otp_code,
        verification_expires_at=expires_at,
    )
    db.add(new_employee)
    await db.flush()

    # 4. Write initial baseline entry into trust_logs
    initial_log = TrustLog(
        id=uuid.uuid4(),
        company_id=company.id,
        user_id=new_employee.id,
        score_before=100,
        score_after=100,
        cause_of_change="Baseline trust initialization"
    )
    db.add(initial_log)
    await db.commit()
    await db.refresh(new_employee)

    # 5. Populate Redis Active Cache Layer
    try:
        await cache_trust_score(user_id=new_employee.id, current_score=100, status="ACTIVE")
    except Exception as e:
        logger.error(f"Failed to cache employee trust score: {e}")

    # 6. Dispatch Verification Email
    await EmailService.send_verification_email(new_employee.email, new_employee.full_name, otp_code)

    return UserResponse.model_validate(new_employee)

@router.post("/verify-email", response_model=VerifyEmailResponse)
async def verify_email(
    payload: VerifyEmailRequest,
    db: AsyncSession = Depends(get_db)
):
    """
    Validates user 6-digit OTP verification code and marks email as verified.
    """
    stmt = select(User).where(User.email == payload.email)
    res = await db.execute(stmt)
    user = res.scalar_one_or_none()

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User with this email address was not found."
        )

    if user.is_email_verified:
        return VerifyEmailResponse(
            message="Email address is already verified.",
            email=user.email,
            is_email_verified=True
        )

    if not user.verification_code or user.verification_code != payload.code.strip():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid 6-digit verification code."
        )

    now = datetime.now(timezone.utc)
    if user.verification_expires_at and user.verification_expires_at.tzinfo is None:
        expires_at = user.verification_expires_at.replace(tzinfo=timezone.utc)
    else:
        expires_at = user.verification_expires_at

    if expires_at and now > expires_at:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Verification code has expired. Please request a new code."
        )

    # Mark user email as verified
    user.is_email_verified = True
    user.verification_code = None
    user.verification_expires_at = None
    await db.commit()
    await db.refresh(user)

    return VerifyEmailResponse(
        message="Email verified successfully. You may now log in.",
        email=user.email,
        is_email_verified=True
    )

@router.post("/resend-code")
async def resend_verification_code(
    payload: ResendCodeRequest,
    db: AsyncSession = Depends(get_db)
):
    """
    Generates a new 6-digit OTP code and dispatches a fresh verification email.
    """
    stmt = select(User).where(User.email == payload.email)
    res = await db.execute(stmt)
    user = res.scalar_one_or_none()

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User with this email address was not found."
        )

    if user.is_email_verified:
        return {"message": "Email address is already verified.", "is_email_verified": True}

    otp_code = generate_otp()
    expires_at = datetime.now(timezone.utc) + timedelta(minutes=15)

    user.verification_code = otp_code
    user.verification_expires_at = expires_at
    await db.commit()

    await EmailService.send_verification_email(user.email, user.full_name, otp_code)

    return {"message": "Verification code resent successfully.", "email": user.email}

@router.post("/login", response_model=TokenResponse)
async def login(
    payload: LoginRequest,
    db: AsyncSession = Depends(get_db)
):
    """
    Tokenization & Authentication:
    Verifies credentials and checks email verification status.
    """
    stmt = select(User).where(User.email == payload.email)
    res = await db.execute(stmt)
    user = res.scalar_one_or_none()

    if not user or not verify_password(payload.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password."
        )

    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Your user account is inactive."
        )

    if not user.is_email_verified:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="email_not_verified"
        )

    # Token payload definition
    token_data = {
        "sub": str(user.id),
        "company_id": str(user.company_id),
        "role": user.role.value
    }
    access_token = create_access_token(data=token_data)
    
    return TokenResponse(access_token=access_token)
