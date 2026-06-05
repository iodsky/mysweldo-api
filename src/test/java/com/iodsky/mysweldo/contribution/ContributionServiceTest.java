package com.iodsky.mysweldo.contribution;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContributionServiceTest {

    @InjectMocks
    private ContributionService service;

    @Mock
    private ContributionRepository repository;

    @Nested
    class CreateContributionTests {

        @Test
        void shouldCreateAndReturnContributionWhenCodeDoesNotExist() {
            ContributionRequest request = ContributionRequest.builder()
                    .code("SSS")
                    .description("Social Security System")
                    .build();

            Contribution saved = Contribution.builder()
                    .code("SSS")
                    .description("Social Security System")
                    .build();

            when(repository.existsById("SSS")).thenReturn(false);
            when(repository.save(any(Contribution.class))).thenReturn(saved);

            Contribution result = service.createContribution(request);

            assertThat(result.getCode()).isEqualTo("SSS");
            assertThat(result.getDescription()).isEqualTo("Social Security System");
        }

        @Test
        void shouldThrowConflictWhenContributionWithSameCodeAlreadyExists() {
            ContributionRequest request = ContributionRequest.builder()
                    .code("SSS")
                    .description("Social Security System")
                    .build();

            when(repository.existsById("SSS")).thenReturn(true);

            assertThatThrownBy(() -> service.createContribution(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
        }
    }

    @Nested
    class GetContributionByCodeTests {

        @Test
        void shouldReturnContributionWhenItExistsAndIsNotDeleted() {
            Contribution contribution = Contribution.builder()
                    .code("SSS")
                    .description("Social Security System")
                    .build();

            when(repository.findById("SSS")).thenReturn(Optional.of(contribution));

            Contribution result = service.getContributionByCode("SSS");

            assertThat(result.getCode()).isEqualTo("SSS");
            assertThat(result.getDescription()).isEqualTo("Social Security System");
        }

        @Test
        void shouldThrowNotFoundWhenContributionDoesNotExist() {
            when(repository.findById("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getContributionByCode("UNKNOWN"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void shouldThrowNotFoundWhenContributionIsSoftDeleted() {
            Contribution deleted = Contribution.builder()
                    .code("SSS")
                    .description("Social Security System")
                    .build();
            deleted.setDeletedAt(Instant.now());

            when(repository.findById("SSS")).thenReturn(Optional.of(deleted));

            assertThatThrownBy(() -> service.getContributionByCode("SSS"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class UpdateContributionTests {

        @Test
        void shouldUpdateAndReturnContributionWithNewDescriptionWhenItExists() {
            Contribution existing = Contribution.builder()
                    .code("SSS")
                    .description("Old Description")
                    .build();

            ContributionRequest request = ContributionRequest.builder()
                    .code("SSS")
                    .description("Updated Description")
                    .build();

            when(repository.findById("SSS")).thenReturn(Optional.of(existing));
            when(repository.save(existing)).thenReturn(existing);

            Contribution result = service.updateContribution("SSS", request);

            assertThat(result.getDescription()).isEqualTo("Updated Description");
        }

        @Test
        void shouldThrowNotFoundWhenUpdatingNonExistentContribution() {
            ContributionRequest request = ContributionRequest.builder()
                    .code("UNKNOWN")
                    .description("Some Description")
                    .build();

            when(repository.findById("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateContribution("UNKNOWN", request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class DeleteContributionTests {

        @Test
        void shouldSoftDeleteContributionBySettingDeletedAtWhenItExists() {
            Contribution contribution = Contribution.builder()
                    .code("SSS")
                    .description("Social Security System")
                    .build();

            when(repository.findById("SSS")).thenReturn(Optional.of(contribution));

            service.deleteContribution("SSS");

            assertThat(contribution.getDeletedAt()).isNotNull();
            verify(repository).save(contribution);
        }

        @Test
        void shouldThrowNotFoundWhenDeletingNonExistentContribution() {
            when(repository.findById("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteContribution("UNKNOWN"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
