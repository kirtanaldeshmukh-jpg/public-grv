package com.resolvetrack.auth;

import com.resolvetrack.common.BusinessRuleException;
import com.resolvetrack.department.Department;
import com.resolvetrack.department.DepartmentRepository;
import com.resolvetrack.officer.Officer;
import com.resolvetrack.officer.OfficerRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OfficerRepository officerRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       OfficerRepository officerRepository,
                       DepartmentRepository departmentRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.officerRepository = officerRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        String lookupUsername = authentication.getName();
        User user = userRepository.findByUsername(lookupUsername)
                .or(() -> userRepository.findByUsername(request.getUsername()))
                .orElseThrow(() -> new BusinessRuleException("User not found"));

        Long officerId = null;
        Long departmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;

        Optional<Officer> officerOpt = officerRepository.findByUserId(user.getId());
        if (officerOpt.isPresent()) {
            Officer officer = officerOpt.get();
            officerId = officer.getId();
            if (departmentId == null && officer.getDepartment() != null) {
                departmentId = officer.getDepartment().getId();
            }
        }

        return AuthDto.AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .departmentId(departmentId)
                .officerId(officerId)
                .build();
    }

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessRuleException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("Email is already registered");
        }

        Role assignedRole = request.getRole() != null ? request.getRole() : Role.CITIZEN;
        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId()).orElse(null);
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .department(department)
                .enabled(true)
                .build();

        userRepository.save(user);

        Long officerId = null;
        Long deptId = department != null ? department.getId() : null;

        String token = tokenProvider.generateTokenForUser(user, officerId);

        return AuthDto.AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .departmentId(deptId)
                .officerId(officerId)
                .build();
    }
}
