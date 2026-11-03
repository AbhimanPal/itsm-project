package com.itsm.itsm.web;

import com.itsm.itsm.exception.InvalidLoginException;
import com.itsm.itsm.model.User;
import com.itsm.itsm.notification.Notification;
import com.itsm.itsm.notification.NotificationRepository;
import com.itsm.itsm.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

// Every role has a Notifications page
@Controller
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/notifications")
    @Transactional
    public String notifications(Principal principal, Model model) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new InvalidLoginException(principal.getName()));
        List<Notification> notifications = notificationRepository.findByRecipientOrderByCreatedAtDescIdDesc(user);

        // Show which ones are new on this visit, then mark them all as seen
        List<Long> newIds = notifications.stream().filter(n -> !n.isSeen()).map(Notification::getId).toList();
        notifications.forEach(Notification::markSeen);

        model.addAttribute("notifications", notifications);
        model.addAttribute("newIds", newIds);
        model.addAttribute("role", user.getRole().name().toLowerCase());
        model.addAttribute("unreadCount", 0L);
        return "notifications";
    }
}
