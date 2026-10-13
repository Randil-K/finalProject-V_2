package lk.tideline.cleanup.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lk.tideline.cleanup.config.TidelineProperties;
import lk.tideline.cleanup.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Sends the password reset link by email once a mail server is configured (SPRING_MAIL_HOST).
 * Without one — local development — the link is written to the server log instead, so the flow can
 * still be followed end to end.
 */
@Service
public class PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailer.class);

    /** The address the project ships with: a placeholder, not a mailbox anyone can send from. */
    private static final String PLACEHOLDER_FROM = "no-reply@tideline.lk";

    private final ObjectProvider<JavaMailSender> mailSender;
    private final TidelineProperties properties;
    private final String smtpUser;

    public PasswordResetMailer(ObjectProvider<JavaMailSender> mailSender, TidelineProperties properties,
                               @Value("${spring.mail.username:}") String smtpUser) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.smtpUser = smtpUser;
    }

    public void send(User user, String link, long validMinutes) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.info("No mail server configured. Password reset link for {}: {}", user.getEmail(), link);
            return;
        }

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            from(helper);
            helper.setTo(user.getEmail());
            helper.setSubject("Reset your Tideline password");
            // Both parts, so a mail reader that will not show HTML still gets the link.
            helper.setText(plainText(user, link, validMinutes), html(user, link, validMinutes));
            sender.send(message);
            log.info("Password reset email sent to {}", user.getEmail());
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            // The response never says whether the email exists, so a delivery failure is only logged.
            log.warn("Could not send the password reset email to {}: {}", user.getEmail(), e.toString());
        }
    }

    /**
     * Gmail and most other providers only let an account send as itself, so the signed-in SMTP user
     * wins unless a real from address has been set for a domain that allows it.
     */
    private void from(MimeMessageHelper helper) throws MessagingException, UnsupportedEncodingException {
        String configured = properties.getMail().getFrom();
        boolean placeholder = configured == null || configured.isBlank() || configured.contains(PLACEHOLDER_FROM);
        if (placeholder && !smtpUser.isBlank()) {
            helper.setFrom(smtpUser, "Tideline");
        } else {
            helper.setFrom(configured);
        }
    }

    private String plainText(User user, String link, long validMinutes) {
        return "Hi " + user.getFullName() + ",\n\n"
                + "Use this link to choose a new password. It works once and expires in "
                + validMinutes + " minutes.\n\n"
                + link + "\n\n"
                + "If you didn't ask to reset your password, you can ignore this email — nothing has changed.\n\n"
                + "Tideline · Community-Based Ocean & Coastal Cleanup";
    }

    private String html(User user, String link, long validMinutes) {
        String safeName = escape(user.getFullName());
        String safeLink = escape(link);
        return """
                <!doctype html>
                <html lang="en">
                  <body style="margin:0;padding:24px;background:#f4f1ea;font-family:Segoe UI,Helvetica,Arial,sans-serif;color:#1b2a33;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:520px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;">
                      <tr>
                        <td style="background:#0f3b45;padding:20px 28px;color:#ffffff;font-size:18px;font-weight:700;">Tideline</td>
                      </tr>
                      <tr>
                        <td style="padding:28px;">
                          <p style="margin:0 0 12px;font-size:20px;font-weight:700;">Reset your password</p>
                          <p style="margin:0 0 16px;font-size:15px;line-height:1.5;">Hi %s, use the button below to choose a new password. It works once and expires in %d minutes.</p>
                          <p style="margin:0 0 20px;">
                            <a href="%s" style="display:inline-block;background:#0e7c7b;color:#ffffff;text-decoration:none;padding:12px 22px;border-radius:8px;font-weight:600;font-size:15px;">Choose a new password</a>
                          </p>
                          <p style="margin:0 0 6px;font-size:13px;color:#5b6b75;">If the button does not work, paste this into your browser:</p>
                          <p style="margin:0 0 20px;font-size:13px;word-break:break-all;"><a href="%s" style="color:#0e7c7b;">%s</a></p>
                          <p style="margin:0;font-size:13px;color:#5b6b75;">If you didn't ask to reset your password, you can ignore this email — nothing has changed.</p>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding:16px 28px;background:#f4f1ea;font-size:12px;color:#5b6b75;">Tideline · Community-Based Ocean &amp; Coastal Cleanup</td>
                      </tr>
                    </table>
                  </body>
                </html>
                """.formatted(safeName, validMinutes, safeLink, safeLink, safeLink);
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
