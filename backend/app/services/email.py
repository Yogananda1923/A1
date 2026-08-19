import logging
import httpx
from app.config import settings

logger = logging.getLogger(__name__)

class EmailService:
    @staticmethod
    async def send_verification_email(recipient_email: str, recipient_name: str | None, otp_code: str) -> bool:
        """
        Dispatches a 6-digit verification code to the recipient.
        If BREVO_API_KEY is configured in settings, sends via Brevo Transactional REST API.
        Otherwise, operates in Dev/Test mode by logging the OTP code.
        """
        api_key = settings.BREVO_API_KEY
        sender_email = settings.BREVO_SENDER_EMAIL
        sender_name = settings.BREVO_SENDER_NAME

        if api_key and api_key.strip():
            # Send email via Brevo REST API v3
            url = "https://api.brevo.com/v3/smtp/email"
            headers = {
                "accept": "application/json",
                "api-key": api_key.strip(),
                "content-type": "application/json"
            }

            display_name = recipient_name or recipient_email
            payload = {
                "sender": {"name": sender_name, "email": sender_email},
                "to": [{"email": recipient_email, "name": display_name}],
                "subject": f"AdaptiveTrust - Your Verification Code is {otp_code}",
                "htmlContent": f"""
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; background-color: #0f172a; color: #f8fafc; padding: 32px; border-radius: 12px;">
                    <div style="text-align: center; margin-bottom: 24px;">
                        <h2 style="color: #6366f1; margin: 0; font-size: 24px;">AdaptiveTrust Security</h2>
                        <p style="color: #94a3b8; font-size: 14px;">Email Verification Request</p>
                    </div>
                    <div style="background: rgba(30, 41, 59, 0.8); padding: 24px; border-radius: 8px; border: 1px solid #334155; text-align: center;">
                        <p style="font-size: 16px; margin-bottom: 16px;">Hello {display_name},</p>
                        <p style="color: #94a3b8; font-size: 14px; margin-bottom: 24px;">Use the following 6-digit verification code to complete your registration:</p>
                        <div style="font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #38bdf8; background: #0284c71a; padding: 16px; border-radius: 8px; display: inline-block;">
                            {otp_code}
                        </div>
                        <p style="color: #64748b; font-size: 12px; margin-top: 24px;">This code will expire in 15 minutes. If you did not request this code, please ignore this email.</p>
                    </div>
                </div>
                """
            }

            try:
                async with httpx.AsyncClient(timeout=10.0) as client:
                    res = await client.post(url, json=payload, headers=headers)
                    if res.status_code in (200, 201, 202):
                        logger.info(f"Verification email successfully sent via Brevo to {recipient_email}")
                        return True
                    else:
                        logger.error(f"Brevo API error ({res.status_code}): {res.text}")
                        return False
            except Exception as e:
                logger.error(f"Failed to dispatch Brevo verification email to {recipient_email}: {e}")
                return False
        else:
            # Dev / Testing Fallback Mode
            logger.info(f"[DEV MODE EMAIL SERVICE] Verification OTP for {recipient_email}: {otp_code}")
            print(f"\n=======================================================")
            print(f" [DEV EMAIL SERVICE] VERIFICATION EMAIL TO: {recipient_email}")
            print(f" [DEV EMAIL SERVICE] OTP CODE: {otp_code}")
            print(f"=======================================================\n")
            return True
