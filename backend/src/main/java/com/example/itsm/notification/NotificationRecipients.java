package com.example.itsm.notification;

import com.example.itsm.user.User;
import com.example.itsm.user.UserRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationRecipients {

    private final UserRepository userRepository;

    public NotificationRecipients(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> admins() {
        return userRepository.findAllByOrderByNameAsc().stream()
                .filter(User::isActive)
                .filter(user -> user.getRole() == com.example.itsm.user.Role.ADMIN)
                .toList();
    }
}
