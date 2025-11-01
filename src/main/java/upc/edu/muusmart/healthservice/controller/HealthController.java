package upc.edu.muusmart.healthservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import upc.edu.muusmart.healthservice.model.HealthRecord;
import upc.edu.muusmart.healthservice.payload.HealthRequest;
import upc.edu.muusmart.healthservice.service.HealthService;

import java.util.List;

/**
 * REST controller exposing endpoints for managing health records.
 *
 * <p>Normal users can manage health records associated with their own animals. Administrators can manage
 * all records. JWT authentication is enforced globally, and access decisions are made per endpoint.</p>
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {
    private final HealthService healthService;

    /**
     * Creates a new health record for an animal. The record is automatically associated with the
     * authenticated user via the ownerUsername field.
     *
     * @param request  the request body containing health data
     * @param principal the authenticated user (ignored; username is obtained from the security context)
     * @return the created health record
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PostMapping
    public ResponseEntity<HealthRecord> createRecord(@Valid @RequestBody HealthRequest request,
                                                     @AuthenticationPrincipal Object principal) {
        // Extract the username from the security context. The principal may be a String (subject).
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        HealthRecord record = new HealthRecord();
        record.setAnimalId(request.getAnimalId());
        record.setDiagnosis(request.getDiagnosis());
        record.setTreatment(request.getTreatment());
        record.setVaccine(request.getVaccine());
        record.setNotes(request.getNotes());
        record.setDate(request.getDate());
        record.setPenalty(request.getPenalty());
        record.setOwnerUsername(username);
        return ResponseEntity.ok(healthService.createHealthRecord(record));
    }

    /**
     * Retrieves a specific health record by ID, validating that the caller is either the owner
     * or an administrator.
     *
     * @param id the record ID
     * @param principal the authenticated user
     * @return the health record
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<HealthRecord> getRecord(@PathVariable Long id,
                                                  @AuthenticationPrincipal Object principal) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(healthService.getRecordByIdForUser(id, username, isAdmin));
    }

    /**
     * Retrieves all health records for a specific animal. If the caller is an administrator, all
     * records are returned. Otherwise, the caller must own all returned records.
     *
     * @param animalId the animal identifier
     * @param principal the authenticated user
     * @return list of health records for the animal
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/animal/{animalId}")
    public ResponseEntity<List<HealthRecord>> getRecordsForAnimal(@PathVariable Long animalId,
                                                                  @AuthenticationPrincipal Object principal) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        List<HealthRecord> records = healthService.getHealthRecordsForAnimal(animalId);
        if (isAdmin || records.stream().allMatch(r -> r.getOwnerUsername().equals(username))) {
            return ResponseEntity.ok(records);
        }
        throw new SecurityException("Access denied to records for animal " + animalId);
    }

    /**
     * Retrieves all health records visible to the authenticated user. Administrators see all records.
     *
     * @param principal the authenticated user
     * @return list of health records
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping
    public ResponseEntity<List<HealthRecord>> getAllRecords(@AuthenticationPrincipal Object principal) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(healthService.getAllRecordsForUser(username, isAdmin));
    }

    /**
     * Updates a health record. Only the owner or an administrator may perform this operation.
     *
     * @param id the record ID
     * @param request the new data for the record
     * @param principal the authenticated user
     * @return the updated health record
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<HealthRecord> updateRecord(@PathVariable Long id,
                                                     @Valid @RequestBody HealthRequest request,
                                                     @AuthenticationPrincipal Object principal) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        HealthRecord updated = new HealthRecord();
        updated.setDiagnosis(request.getDiagnosis());
        updated.setTreatment(request.getTreatment());
        updated.setVaccine(request.getVaccine());
        updated.setNotes(request.getNotes());
        updated.setDate(request.getDate());
        updated.setPenalty(request.getPenalty());
        return ResponseEntity.ok(healthService.updateRecordForUser(id, updated, username, isAdmin));
    }

    /**
     * Deletes a health record. Only the owner or an administrator may perform this operation.
     *
     * @param id the record ID
     * @param principal the authenticated user
     * @return HTTP 204 on success
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id,
                                             @AuthenticationPrincipal Object principal) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        healthService.deleteRecordForUser(id, username, isAdmin);
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns the cumulative health penalty for an animal. This endpoint can be used by other services
     * such as the Production Service to adjust their calculations.
     *
     * @param animalId the animal identifier
     * @param principal the authenticated user
     * @return the health penalty as a decimal value
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/condition/{animalId}")
    public ResponseEntity<Double> getHealthPenalty(@PathVariable Long animalId,
                                                   @AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok(healthService.getHealthPenaltyForAnimal(animalId));
    }
}