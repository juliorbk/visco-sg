package com.visco.backend.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "senderEmail", "admin@visco.com");
    }

    @Test
    void sendWelcomeEmail_Success() {
        when(mailSender.createMimeMessage()).thenReturn(mock(jakarta.mail.internet.MimeMessage.class));

        emailService.sendWelcomeEmail("test@example.com", "Test User");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    @Test
    void sendWelcomeEmail_HandlesMessagingException() throws Exception {
        jakarta.mail.internet.MimeMessage mimeMessage = mock(jakarta.mail.internet.MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new jakarta.mail.MessagingException("Mail error")).when(mailSender).send(any(jakarta.mail.internet.MimeMessage.class));

        emailService.sendWelcomeEmail("test@example.com", "Test User");

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(jakarta.mail.internet.MimeMessage.class));
    }
}
