package com.founderlink.startup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.founderlink.startup.entity.Startup;
import com.founderlink.startup.entity.StartupStage;
import com.founderlink.startup.events.StartupDeletedEvent;
import com.founderlink.startup.events.StartupEventPublisher;
import com.founderlink.startup.exception.ForbiddenAccessException;
import com.founderlink.startup.exception.StartupNotFoundException;
import com.founderlink.startup.mapper.StartupMapper;
import com.founderlink.startup.repository.StartupRepository;
import com.founderlink.startup.serviceImpl.StartupServiceImpl;

@ExtendWith(MockitoExtension.class)
class DeleteStartupTest {

    @Mock
    private StartupRepository startupRepository;

    @Mock
    private StartupMapper startupMapper;

    @Mock
    private StartupEventPublisher eventPublisher;

    @InjectMocks
    private StartupServiceImpl startupService;

    private Startup startup;

    @BeforeEach
    void setUp() {
        startup = new Startup();
        startup.setId(1L);
        startup.setName("EduReach");
        startup.setFounderId(5L);
        startup.setIsDeleted(false);
        startup.setStage(StartupStage.MVP);
        startup.setFundingGoal(
                new BigDecimal("5000000.00"));
        startup.setCreatedAt(LocalDateTime.now());
    }


    // SUCCESS — SOFT DELETE

    @Test
    void deleteStartup_Success() {

        // Arrange
        when(startupRepository
                .findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(startup));
        when(startupRepository.save(
                any(Startup.class)))
                .thenReturn(startup);

        // Act
        startupService.deleteStartup(1L, 5L);

        // Assert soft delete
        assertThat(startup.getIsDeleted())
                .isTrue();

        // Verify save called not delete
        verify(startupRepository, times(1))
                .save(startup);
        verify(startupRepository, never())
                .delete(any(Startup.class));

        // Verify event published
        verify(eventPublisher, times(1))
                .publishStartupDeletedEvent(
                        any(StartupDeletedEvent.class));
    }


    // NOT FOUND
    
    @Test
    void deleteStartup_NotFound_ThrowsException() {

        // Arrange
        when(startupRepository
                .findByIdAndIsDeletedFalse(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                startupService
                        .deleteStartup(999L, 5L))
                .isInstanceOf(
                        StartupNotFoundException.class)
                .hasMessage(
                        "Startup not found with id: 999");

        verify(startupRepository, never())
                .save(any(Startup.class));
        verify(eventPublisher, never())
                .publishStartupDeletedEvent(any());
    }

    // NOT OWNER

    @Test
    void deleteStartup_NotOwner_ThrowsException() {

        // Arrange
        when(startupRepository
                .findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.of(startup));

        // Act & Assert
        assertThatThrownBy(() ->
                startupService
                        .deleteStartup(1L, 99L))
                .isInstanceOf(
                        ForbiddenAccessException.class)
                .hasMessage(
                        "You are not authorized " +
                        "to delete this startup");

        verify(startupRepository, never())
                .save(any(Startup.class));
        verify(eventPublisher, never())
                .publishStartupDeletedEvent(any());
    }

    // ALREADY DELETED
    
    @Test
    void deleteStartup_AlreadyDeleted_ThrowsException() {

        // Arrange
        // findByIdAndIsDeletedFalse returns empty
        // because already deleted
        when(startupRepository
                .findByIdAndIsDeletedFalse(1L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() ->
                startupService
                        .deleteStartup(1L, 5L))
                .isInstanceOf(
                        StartupNotFoundException.class)
                .hasMessage(
                        "Startup not found with id: 1");

        verify(startupRepository, never())
                .save(any(Startup.class));
    }
}