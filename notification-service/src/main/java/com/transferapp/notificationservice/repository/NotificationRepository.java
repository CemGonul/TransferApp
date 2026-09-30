package com.transferapp.notificationservice.repository;

import com.transferapp.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {


}
