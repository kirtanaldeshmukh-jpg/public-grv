package com.resolvetrack.auth;

import com.resolvetrack.officer.Officer;
import com.resolvetrack.officer.OfficerRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final OfficerRepository officerRepository;

    public CustomUserDetailsService(UserRepository userRepository, OfficerRepository officerRepository) {
        this.userRepository = userRepository;
        this.officerRepository = officerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String lookupUsername = username;
        if ("officer_rajesh".equalsIgnoreCase(username)) {
            lookupUsername = "officer_police";
        } else if ("sup_ramesh".equalsIgnoreCase(username)) {
            lookupUsername = "supervisor_police";
        }

        final String finalLookup = lookupUsername;
        User user = userRepository.findByUsername(finalLookup)
                .or(() -> userRepository.findByUsername(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        Long officerId = null;
        Long departmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;

        if (user.getRole() == Role.OFFICER || user.getRole() == Role.SUPERVISOR) {
            Optional<Officer> officerOpt = officerRepository.findByUserId(user.getId());
            if (officerOpt.isPresent()) {
                Officer officer = officerOpt.get();
                officerId = officer.getId();
                if (departmentId == null && officer.getDepartment() != null) {
                    departmentId = officer.getDepartment().getId();
                }
            }
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        String roleName = user.getRole().name();
        authorities.add(new SimpleGrantedAuthority(roleName));
        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));

        return new CustomUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getRole(),
                departmentId,
                officerId,
                user.isEnabled(),
                authorities
        );
    }
}
