package com.example.itsm.user;

import com.example.itsm.catalog.Category;
import com.example.itsm.catalog.CategoryRepository;
import com.example.itsm.common.BusinessRuleException;
import com.example.itsm.common.ResourceNotFoundException;
import com.example.itsm.incident.IncidentRepository;
import com.example.itsm.incident.IncidentStatusGroups;
import com.example.itsm.security.CurrentUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final IncidentRepository incidentRepository;
    private final UserFactory userFactory;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    public AdminUserService(
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            IncidentRepository incidentRepository,
            UserFactory userFactory,
            PasswordEncoder passwordEncoder,
            CurrentUserService currentUserService
    ) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.incidentRepository = incidentRepository;
        this.userFactory = userFactory;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return userRepository.findAllByOrderByNameAsc().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listTechnicians() {
        return userRepository.findAllByOrderByNameAsc().stream()
                .filter(Technician.class::isInstance)
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String email = User.normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("EMAIL_IN_USE", "An account already exists for this email.");
        }

        Set<Category> skills = loadSkills(request.skillCategoryIds());
        User user = userFactory.create(
                request.role(),
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                request.phone(),
                request.department(),
                request.jobTitle(),
                request.experienceYears(),
                request.maxCapacity(),
                skills
        );
        userRepository.save(user);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));

        if (request.active() != null && !request.active()) {
            if (currentUserService.id().equals(id)) {
                throw new BusinessRuleException("CANNOT_DEACTIVATE_SELF", "You cannot deactivate your own account.");
            }
            if (user instanceof Technician technician) {
                long activeIncidents = incidentRepository.countByAssignedTechnicianIdAndStatusIn(
                        technician.getId(),
                        IncidentStatusGroups.ACTIVE_WORK
                );
                if (activeIncidents > 0) {
                    throw new BusinessRuleException(
                            "TECHNICIAN_HAS_ACTIVE_INCIDENTS",
                            "Reassign this technician's active incidents before deactivating the account."
                    );
                }
            }
            if (user.getRole() == Role.ADMIN) {
                long activeAdmins = userRepository.findAllByOrderByNameAsc().stream()
                        .filter(candidate -> candidate.getRole() == Role.ADMIN && candidate.isActive())
                        .count();
                if (activeAdmins <= 1) {
                    throw new BusinessRuleException(
                            "LAST_ADMIN",
                            "At least one active administrator account must remain."
                    );
                }
            }
        }

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BusinessRuleException("INVALID_NAME", "Name must not be blank.");
            }
            user.updateProfile(request.name().trim(), request.phone());
        } else if (request.phone() != null) {
            user.updateProfile(user.getName(), request.phone());
        }

        if (request.active() != null) {
            user.setActive(request.active());
        }

        if (request.password() != null) {
            user.changePasswordHash(passwordEncoder.encode(request.password()));
        }

        if (user instanceof Employee employee && request.department() != null) {
            employee.setDepartment(request.department());
        }

        if (user instanceof Administrator administrator && request.jobTitle() != null) {
            administrator.setJobTitle(request.jobTitle());
        }

        if (user instanceof Technician technician) {
            technician.updateProfessionalProfile(
                    request.experienceYears(),
                    request.maxCapacity(),
                    request.available()
            );
            if (request.skillCategoryIds() != null) {
                technician.replaceSkills(loadSkills(request.skillCategoryIds()));
            }
        }

        return UserResponse.from(user);
    }

    private Set<Category> loadSkills(Set<Long> skillCategoryIds) {
        if (skillCategoryIds == null || skillCategoryIds.isEmpty()) {
            return Set.of();
        }
        List<Category> categories = categoryRepository.findAllById(skillCategoryIds);
        if (categories.size() != skillCategoryIds.size()) {
            throw new BusinessRuleException("UNKNOWN_SKILL", "One or more selected skill categories do not exist.");
        }
        return new LinkedHashSet<>(categories);
    }
}
