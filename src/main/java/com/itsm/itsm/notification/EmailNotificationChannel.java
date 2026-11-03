package com.itsm.itsm.notification;

import com.itsm.itsm.model.Incident;
import com.itsm.itsm.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

// Channel 2: real email through Gmail's SMTP server.
//
// Adapter pattern: the rest of our code only knows our NotificationChannel interface,
// send(recipient, incident, message). Spring's JavaMailSender works differently
// (it wants a SimpleMailMessage with to/subject/text). This class "adapts" one to the other,
// so NotificationListener never has to know that email is involved.
//
// Settings come from environment variables (see application.properties), never from GitHub.
// Without MAIL_PASSWORD, emails are only written to the console log.
@Component
public class EmailNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationChannel.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;
    private final String redirectTo;

    public EmailNotificationChannel(JavaMailSender mailSender,
                                    @Value("${spring.mail.username:}") String from,
                                    @Value("${spring.mail.password:}") String password,
                                    @Value("${itsm.mail.redirect-to:}") String redirectTo) {
        this.mailSender = mailSender;
        this.from = from;
        this.enabled = !password.isBlank();
        this.redirectTo = redirectTo;
    }

    @Override
    public void send(User recipient, Incident incident, String message) {
        // Demo accounts like asha@itsm.local don't exist, so all mail can be sent to one real inbox instead
        String to = redirectTo.isBlank() ? recipient.getEmail() : redirectTo;

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(to);
        mail.setSubject("[I-ITSM] " + incident.getNumber() + ": " + incident.getTitle());
        mail.setText("Hello " + recipient.getName() + ",\n\n" + message
                + "\n\n(For " + recipient.getEmail() + ")\n-- I-ITSM");

        if (!enabled) {
            log.info("[EMAIL off] To: {} | {}", recipient.getEmail(), message);
            return;
        }
        // Talking to Gmail takes a second or two, so send in the background
        // and let the page load straight away
        CompletableFuture.runAsync(() -> deliver(mail));
    }

    private void deliver(SimpleMailMessage mail) {
        try {
            mailSender.send(mail);
            log.info("[EMAIL sent] To: {} | {}", mail.getTo()[0], mail.getSubject());
        } catch (MailException e) {
            // No internet or a wrong password must not break the app; the in-app notification still arrives
            log.warn("[EMAIL failed] To: {} | {}", mail.getTo()[0], e.getMessage());
        }
    }
}
