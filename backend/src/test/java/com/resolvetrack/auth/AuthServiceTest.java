package com.resolvetrack.auth;

import com.resolvetrack.common.BusinessRuleException;
import com.resolvetrack.department.Department;
import com.resolvetrack.department.DepartmentRepository;
import com.resolvetrack.officer.Officer;
import com.resolvetrack.officer.OfficerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private AuthService authService;
    private InMemoryUserRepository userRepository;
    private InMemoryOfficerRepository officerRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        officerRepository = new InMemoryOfficerRepository();
        passwordEncoder = new BCryptPasswordEncoder();
        tokenProvider = new JwtTokenProvider("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", 3600000);

        AuthenticationManager authenticationManager = authentication -> {
            String username = (String) authentication.getPrincipal();
            String password = (String) authentication.getCredentials();

            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPasswordHash())) {
                throw new BadCredentialsException("Invalid username or password");
            }

            User user = userOpt.get();
            CustomUserDetails details = new CustomUserDetails(
                    user.getId(),
                    user.getUsername(),
                    user.getPasswordHash(),
                    user.getRole(),
                    user.getDepartment() != null ? user.getDepartment().getId() : null,
                    null,
                    true,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );

            return new UsernamePasswordAuthenticationToken(details, password, details.getAuthorities());
        };

        authService = new AuthService(
                userRepository,
                officerRepository,
                null,
                passwordEncoder,
                authenticationManager,
                tokenProvider
        );

        // Seed an admin user
        User admin = User.builder()
                .id(1L)
                .username("admin")
                .email("admin@resolvetrack.gov.in")
                .passwordHash(passwordEncoder.encode("Admin@123"))
                .fullName("System Administrator")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.save(admin);

        // Seed an officer user
        User officerUser = User.builder()
                .id(4L)
                .username("officer_police")
                .email("officer@resolvetrack.gov.in")
                .passwordHash(passwordEncoder.encode("Officer@123"))
                .fullName("Inspector Rajesh Kumar")
                .role(Role.OFFICER)
                .enabled(true)
                .build();
        userRepository.save(officerUser);

        Officer officer = Officer.builder()
                .id(101L)
                .user(officerUser)
                .employeeCode("EMP-POL-001")
                .designation("Inspector")
                .build();
        officerRepository.save(officer);
    }

    @Test
    @DisplayName("Login with valid credentials returns JWT token and user details")
    void testLogin_Success() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("admin", "Admin@123");
        AuthDto.AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertTrue(tokenProvider.validateToken(response.getToken()));
        assertEquals("admin", response.getUsername());
        assertEquals(Role.ADMIN, response.getRole());
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    @DisplayName("Login with invalid password throws BadCredentialsException")
    void testLogin_InvalidPassword_ThrowsBadCredentials() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("admin", "WrongPassword");
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Officer login resolves officer ID and role")
    void testOfficerLogin_ResolvesOfficerId() {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("officer_police", "Officer@123");
        AuthDto.AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals(Role.OFFICER, response.getRole());
        assertEquals(101L, response.getOfficerId());
    }

    @Test
    @DisplayName("Registering new citizen creates user and returns token")
    void testRegister_Success() {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest();
        request.setUsername("citizen_new");
        request.setEmail("citizen.new@example.com");
        request.setFullName("Deepa Rao");
        request.setPassword("Secret@123");
        request.setRole(Role.CITIZEN);

        AuthDto.AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("citizen_new", response.getUsername());
        assertEquals(Role.CITIZEN, response.getRole());
        assertTrue(userRepository.existsByUsername("citizen_new"));
    }

    // =========================================================================
    // In-memory repositories for AuthService test
    // =========================================================================

    private static class InMemoryUserRepository implements UserRepository {
        private final Map<String, User> byUsername = new HashMap<>();
        private final Map<String, User> byEmail = new HashMap<>();

        @Override public Optional<User> findByUsername(String username) { return Optional.ofNullable(byUsername.get(username)); }
        @Override public Optional<User> findByEmail(String email) { return Optional.ofNullable(byEmail.get(email)); }
        @Override public boolean existsByUsername(String username) { return byUsername.containsKey(username); }
        @Override public boolean existsByEmail(String email) { return byEmail.containsKey(email); }
        @Override public <S extends User> S save(S entity) {
            byUsername.put(entity.getUsername(), entity);
            byEmail.put(entity.getEmail(), entity);
            return entity;
        }
        @Override public void flush() {}
        @Override public <S extends User> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends User> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<User> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public User getOne(Long aLong) { return null; }
        @Override public User getById(Long aLong) { return null; }
        @Override public User getReferenceById(Long aLong) { return null; }
        @Override public <S extends User> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends User> List<S> findAll(org.springframework.data.domain.Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends User> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return Collections.emptyList(); }
        @Override public <S extends User> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public <S extends User> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends User> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends User, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends User> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public Optional<User> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<User> findAll() { return new ArrayList<>(byUsername.values()); }
        @Override public List<User> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return byUsername.size(); }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(User entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends User> entities) {}
        @Override public void deleteAll() {}
        @Override public List<User> findAll(org.springframework.data.domain.Sort sort) { return Collections.emptyList(); }
        @Override public org.springframework.data.domain.Page<User> findAll(org.springframework.data.domain.Pageable pageable) { return null; }
    }

    private static class InMemoryOfficerRepository implements OfficerRepository {
        private final Map<Long, Officer> byUserId = new HashMap<>();

        @Override public Optional<Officer> findByUserId(Long userId) { return Optional.ofNullable(byUserId.get(userId)); }
        @Override public Optional<Officer> findByEmployeeCode(String employeeCode) { return Optional.empty(); }
        @Override public List<Officer> findAllByDepartmentId(Long departmentId) { return Collections.emptyList(); }
        @Override public <S extends Officer> S save(S entity) {
            if (entity.getUser() != null) byUserId.put(entity.getUser().getId(), entity);
            return entity;
        }
        @Override public void flush() {}
        @Override public <S extends Officer> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Officer> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<Officer> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> longs) {}
        @Override public void deleteAllInBatch() {}
        @Override public Officer getOne(Long aLong) { return null; }
        @Override public Officer getById(Long aLong) { return null; }
        @Override public Officer getReferenceById(Long aLong) { return null; }
        @Override public <S extends Officer> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends Officer> List<S> findAll(org.springframework.data.domain.Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends Officer> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return Collections.emptyList(); }
        @Override public <S extends Officer> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return null; }
        @Override public <S extends Officer> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends Officer> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends Officer, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public <S extends Officer> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public Optional<Officer> findById(Long aLong) { return Optional.empty(); }
        @Override public boolean existsById(Long aLong) { return false; }
        @Override public List<Officer> findAll() { return Collections.emptyList(); }
        @Override public List<Officer> findAllById(Iterable<Long> longs) { return Collections.emptyList(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long aLong) {}
        @Override public void delete(Officer entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> longs) {}
        @Override public void deleteAll(Iterable<? extends Officer> entities) {}
        @Override public void deleteAll() {}
        @Override public List<Officer> findAll(org.springframework.data.domain.Sort sort) { return Collections.emptyList(); }
        @Override public org.springframework.data.domain.Page<Officer> findAll(org.springframework.data.domain.Pageable pageable) { return null; }
    }
}

