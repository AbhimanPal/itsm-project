package com.itsm.itsm.notification;

import com.itsm.itsm.exception.InvalidInputException;
import com.itsm.itsm.exception.NetworkFailureException;
import com.itsm.itsm.model.Admin;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.junit.jupiter.api.Assertions.*;

// Slice 7: the "Send test email" button, without talking to the real Gmail
class EmailNotificationChannelTest {

    Admin admin = new Admin("Admin", "admin@itsm.local", "hash");

    // A fake mail sender that always fails, as if there were no internet
    JavaMailSenderImpl brokenSender = new JavaMailSenderImpl() {
        @Override
        public void send(SimpleMailMessage... messages) {
            throw new MailSendException("Couldn't connect to host smtp.gmail.com");
        }
    };

    @Test
    void gmailUnreachableGivesNetworkFailure() {
        EmailNotificationChannel channel = new EmailNotificationChannel(brokenSender, "me@gmail.com", "app-password", "");
        assertThrows(NetworkFailureException.class, () -> channel.sendTest(admin));
    }

    @Test
    void withoutPasswordTheTestSaysEmailIsOff() {
        EmailNotificationChannel channel = new EmailNotificationChannel(brokenSender, "", "", "");
        assertThrows(InvalidInputException.class, () -> channel.sendTest(admin));
    }
}
