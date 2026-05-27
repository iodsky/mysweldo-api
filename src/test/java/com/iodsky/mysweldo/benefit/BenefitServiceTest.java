package com.iodsky.mysweldo.benefit;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BenefitServiceTest {

    @Mock
    private BenefitRepository repository;

    @InjectMocks
    private BenefitService service;

    @Nested
    class CreateBenefitTests {

        @Test
        void shouldThrowConflict_whenBenefitExists() {
            BenefitRequest request = BenefitRequest.builder()
                    .code("TRANSPO")
                    .build();
            when(repository.existsById("TRANSPO")).thenReturn(true);

            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.createBenefit(request));

            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
            verify(repository, never()).save(any());
        }

        @Test
        void shouldThrowBadRequest_whenBenefit_IsTaxableWithNonTaxableLimit() {
            BenefitRequest request = BenefitRequest.builder()
                    .code("TRANSPO")
                    .taxable(true)
                    .nonTaxableLimit(new BigDecimal(1))
                    .build();
            when(repository.existsById("TRANSPO")).thenReturn(false);
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.createBenefit(request));

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            verify(repository, never()).save(any());
        }

        @Test
        void shouldCreateBenefit_whenBenefit_IsNonTaxableWithNonTaxableLimit() {
            BenefitRequest request = BenefitRequest.builder()
                    .code("TRANSPO")
                    .taxable(false)
                    .nonTaxableLimit(new BigDecimal(1500))
                    .build();
            Benefit benefit = Benefit.builder()
                    .code("TRANSPO")
                    .taxable(false)
                    .nonTaxableLimit(new BigDecimal(1500))
                    .build();
            when(repository.existsById("TRANSPO")).thenReturn(false);
            when(repository.save(any())).thenReturn(benefit);

            Benefit result = service.createBenefit(request);

            assertNotNull(result);
            assertEquals(benefit.getCode(), result.getCode());

            ArgumentCaptor<Benefit> captor = ArgumentCaptor.forClass(Benefit.class);
            verify(repository).save(captor.capture());
            Benefit saved = captor.getValue();
            assertEquals(request.getCode(), saved.getCode());
            assertFalse(saved.isTaxable());
            assertEquals(new BigDecimal(1500),saved.getNonTaxableLimit());
        }

        @Test
        void shouldCreateBenefit() {
            BenefitRequest request = BenefitRequest.builder()
                    .code("TRANSPO")
                    .taxable(true)
                    .build();
            Benefit benefit = Benefit.builder()
                    .code("TRANSPO")
                    .taxable(true)
                    .build();
            when(repository.existsById("TRANSPO")).thenReturn(false);
            when(repository.save(any())).thenReturn(benefit);

            Benefit result = service.createBenefit(request);

            assertNotNull(result);
            assertEquals(benefit.getCode(), result.getCode());

            ArgumentCaptor<Benefit> captor = ArgumentCaptor.forClass(Benefit.class);
            verify(repository).save(captor.capture());
            Benefit saved = captor.getValue();
            assertEquals(request.getCode(), saved.getCode());
            assertTrue(saved.isTaxable());
            assertNull(saved.getNonTaxableLimit());
        }
    }

}