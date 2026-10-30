package com.itsm.itsm.web;

import com.itsm.itsm.notification.NotificationRepository;
import com.itsm.itsm.repository.UserRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

// Adds the unread-notification count to every page, for the badge in the top bar
@ControllerAdvice
public class GlobalModelAttributes {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public GlobalModelAttributes(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @ModelAttribute("unreadCount")
    public long unreadCount(Principal principal) {
        if (principal == null) {
            return 0;
        }
        return userRepository.findByEmail(principal.getName())
                .map(notificationRepository::countByRecipientAndSeenFalse)
                .orElse(0L);
    }
}
