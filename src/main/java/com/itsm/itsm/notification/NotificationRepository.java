package com.itsm.itsm.notification;

import com.itsm.itsm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientOrderByCreatedAtDescIdDesc(User recipient);

    long countByRecipientAndSeenFalse(User recipient);
}
