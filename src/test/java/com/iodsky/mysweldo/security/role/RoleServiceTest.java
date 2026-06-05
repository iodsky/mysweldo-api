package com.iodsky.mysweldo.security.role;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @InjectMocks
    private RoleService service;

    @Mock
    private RoleRepository repository;

    @Nested
    class CreateRoleTests {

        @Test
        void shouldCreateAndReturnRoleWhenNameDoesNotExist() {
            RoleRequest request = new RoleRequest();
            request.setName("ADMIN");
            request.setDescription("Administrator role");

            Role savedRole = Role.builder().name("ADMIN").description("Administrator role").build();

            when(repository.existsByName("ADMIN")).thenReturn(false);
            when(repository.save(any(Role.class))).thenReturn(savedRole);

            Role result = service.createRole(request);

            assertThat(result.getName()).isEqualTo("ADMIN");
            assertThat(result.getDescription()).isEqualTo("Administrator role");
        }

        @Test
        void shouldThrowConflictWhenRoleNameAlreadyExists() {
            RoleRequest request = new RoleRequest();
            request.setName("ADMIN");

            when(repository.existsByName("ADMIN")).thenReturn(true);

            assertThatThrownBy(() -> service.createRole(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
        }
    }

    @Nested
    class GetRoleByIdTests {

        @Test
        void shouldReturnRoleWhenIdExists() {
            Role role = Role.builder().name("ADMIN").build();
            role.setId(1L);

            when(repository.findById(1L)).thenReturn(Optional.of(role));

            Role result = service.getRoleById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("ADMIN");
        }

        @Test
        void shouldThrowNotFoundWhenIdDoesNotExist() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getRoleById(99L))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class GetRoleByNameTests {

        @Test
        void shouldReturnRoleWhenNameExists() {
            Role role = Role.builder().name("USER").build();

            when(repository.findByName("USER")).thenReturn(Optional.of(role));

            Role result = service.getRoleByName("USER");

            assertThat(result.getName()).isEqualTo("USER");
        }

        @Test
        void shouldThrowNotFoundWhenNameDoesNotExist() {
            when(repository.findByName("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getRoleByName("UNKNOWN"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class UpdateRoleTests {

        @Test
        void shouldUpdateBothNameAndDescriptionWhenBothProvided() {
            Role existingRole = Role.builder().name("OLD_NAME").description("Old desc").build();
            existingRole.setId(1L);

            RoleRequest request = new RoleRequest();
            request.setName("NEW_NAME");
            request.setDescription("New desc");

            when(repository.findById(1L)).thenReturn(Optional.of(existingRole));
            when(repository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            Role result = service.updateRole(1L, request);

            assertThat(result.getName()).isEqualTo("NEW_NAME");
            assertThat(result.getDescription()).isEqualTo("New desc");
        }

        @Test
        void shouldUpdateOnlyNameWhenDescriptionIsNull() {
            Role existingRole = Role.builder().name("OLD_NAME").description("Existing desc").build();
            existingRole.setId(1L);

            RoleRequest request = new RoleRequest();
            request.setName("NEW_NAME");

            when(repository.findById(1L)).thenReturn(Optional.of(existingRole));
            when(repository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            Role result = service.updateRole(1L, request);

            assertThat(result.getName()).isEqualTo("NEW_NAME");
            assertThat(result.getDescription()).isEqualTo("Existing desc");
        }

        @Test
        void shouldUpdateOnlyDescriptionWhenNameIsNull() {
            Role existingRole = Role.builder().name("ADMIN").description("Old desc").build();
            existingRole.setId(1L);

            RoleRequest request = new RoleRequest();
            request.setDescription("New desc");

            when(repository.findById(1L)).thenReturn(Optional.of(existingRole));
            when(repository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            Role result = service.updateRole(1L, request);

            assertThat(result.getName()).isEqualTo("ADMIN");
            assertThat(result.getDescription()).isEqualTo("New desc");
        }

        @Test
        void shouldThrowNotFoundWhenRoleToUpdateDoesNotExist() {
            RoleRequest request = new RoleRequest();
            request.setName("NEW_NAME");

            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateRole(99L, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class DeleteRoleTests {

        @Test
        void shouldSoftDeleteRoleBySettingDeletedAtWhenRoleExistsAndIsNotInUse() {
            Role role = Role.builder().name("ADMIN").build();
            role.setId(1L);

            when(repository.findById(1L)).thenReturn(Optional.of(role));
            when(repository.isRoleUsedById(1L)).thenReturn(false);
            when(repository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

            service.deleteRole(1L);

            assertThat(role.getDeletedAt()).isNotNull();
        }

        @Test
        void shouldThrowConflictWhenRoleIsStillAssignedToUsers() {
            Role role = Role.builder().name("ADMIN").build();
            role.setId(1L);

            when(repository.findById(1L)).thenReturn(Optional.of(role));
            when(repository.isRoleUsedById(1L)).thenReturn(true);

            assertThatThrownBy(() -> service.deleteRole(1L))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        void shouldThrowNotFoundWhenRoleToDeleteDoesNotExist() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteRole(99L))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
