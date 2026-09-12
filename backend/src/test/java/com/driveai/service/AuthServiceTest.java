package com.driveai.service;

import com.driveai.config.AppProperties;
import com.driveai.domain.enums.UserRole;
import com.driveai.domain.model.AppUser;
import com.driveai.dto.auth.AuthResponse;
import com.driveai.dto.auth.LoginRequest;
import com.driveai.dto.auth.RegisterRequest;
import com.driveai.exception.ConflictException;
import com.driveai.repository.AppUserRepository;
import com.driveai.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesCustomerWithEncodedPasswordAndReturnsToken() {
        when(userRepository.existsByEmailIgnoreCase("cliente@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("cliente123")).thenReturn("$2a$hashed");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtService.generateToken(1L, "cliente@mail.com", "CUSTOMER")).thenReturn("jwt-demo");

        AuthResponse response = authService.register(
                new RegisterRequest("Cliente@Mail.com", "cliente123", "Ana Cliente"));

        assertThat(response.token()).isEqualTo("jwt-demo");
        assertThat(response.user().email()).isEqualTo("cliente@mail.com");
        assertThat(response.user().role()).isEqualTo(UserRole.CUSTOMER);
    }

    @Test
    void registerWithDuplicateEmailThrowsConflict() {
        when(userRepository.existsByEmailIgnoreCase("cliente@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("cliente@mail.com", "cliente123", "Ana")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Ya existe una cuenta");
    }

    @Test
    void loginWithValidCredentialsReturnsToken() {
        AppUser user = AppUser.builder()
                .id(3L)
                .email("cliente@mail.com")
                .passwordHash("$2a$hashed")
                .fullName("Ana")
                .role(UserRole.CUSTOMER)
                .build();
        when(userRepository.findByEmailIgnoreCase("cliente@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("cliente123", "$2a$hashed")).thenReturn(true);
        when(jwtService.generateToken(3L, "cliente@mail.com", "CUSTOMER")).thenReturn("jwt-demo");

        AuthResponse response = authService.login(new LoginRequest("cliente@mail.com", "cliente123"));

        assertThat(response.token()).isEqualTo("jwt-demo");
    }

    @Test
    void loginWithWrongPasswordThrowsBadCredentials() {
        AppUser user = AppUser.builder()
                .id(3L)
                .email("cliente@mail.com")
                .passwordHash("$2a$hashed")
                .fullName("Ana")
                .role(UserRole.CUSTOMER)
                .build();
        when(userRepository.findByEmailIgnoreCase("cliente@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("incorrecta", "$2a$hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("cliente@mail.com", "incorrecta")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void ensureAdminAccountSkipsWhenCredentialsMissing() {
        AppProperties.Admin blank = new AppProperties.Admin("", "");
        when(appProperties.admin()).thenReturn(blank);

        authService.ensureAdminAccount();

        verifyNoInteractionWithRepository();
    }

    @Test
    void ensureAdminAccountCreatesAdminOnceWhenNotExists() {
        when(appProperties.admin()).thenReturn(new AppProperties.Admin("Admin@Driveai.com", "AdminPass123!"));
        when(userRepository.existsByEmailIgnoreCase("admin@driveai.com")).thenReturn(false);
        when(passwordEncoder.encode("AdminPass123!")).thenReturn("$2a$admin");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.ensureAdminAccount();

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        org.mockito.Mockito.verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("admin@driveai.com");
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.ADMIN);
    }

    private void verifyNoInteractionWithRepository() {
        org.mockito.Mockito.verifyNoInteractions(userRepository);
    }
}