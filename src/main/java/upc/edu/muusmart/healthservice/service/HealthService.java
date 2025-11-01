package upc.edu.muusmart.healthservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import upc.edu.muusmart.healthservice.model.HealthRecord;
import upc.edu.muusmart.healthservice.repository.HealthRecordRepository;

import java.util.List;

/**
 * Business service that encapsulates operations on health records.
 *
 * <p>The service enforces basic access control by delegating ownership checks to
 * the caller, and provides methods for CRUD operations as well as calculating
 * cumulative health penalties used by the production service.</p>
 */
@Service
@RequiredArgsConstructor
public class HealthService {

    private final HealthRecordRepository repository;

    /**
     * Persists a new health record.
     *
     * @param record the record to save
     * @return the saved record
     */
    public HealthRecord createHealthRecord(HealthRecord record) {
        return repository.save(record);
    }

    /**
     * Retrieves all health records for a given animal. Ownership validation should
     * be performed by the caller if needed.
     *
     * @param animalId the animal identifier
     * @return list of health records
     */
    public List<HealthRecord> getHealthRecordsForAnimal(Long animalId) {
        return repository.findByAnimalId(animalId);
    }

    /**
     * Retrieves all records visible to a user. Administrators see all records,
     * whereas normal users see only those they created.
     *
     * @param username the authenticated username
     * @param isAdmin whether the caller has admin privileges
     * @return list of health records
     */
    public List<HealthRecord> getAllRecordsForUser(String username, boolean isAdmin) {
        if (isAdmin) {
            return repository.findAll();
        }
        return repository.findByOwnerUsername(username);
    }

    /**
     * Retrieves a single record and validates ownership. If the caller is not
     * the owner and not an admin, a {@link SecurityException} is thrown.
     *
     * @param id the record ID
     * @param username the caller's username
     * @param isAdmin whether the caller has admin privileges
     * @return the requested record
     */
    public HealthRecord getRecordByIdForUser(Long id, String username, boolean isAdmin) {
        HealthRecord record = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Health record not found"));
        if (isAdmin || record.getOwnerUsername().equals(username)) {
            return record;
        }
        throw new SecurityException("Access denied to health record " + id);
    }

    /**
     * Updates a record's fields after validating ownership. Only admins can
     * reassign the owner of a record.
     *
     * @param id the record ID
     * @param updated the updated record data
     * @param username the caller's username
     * @param isAdmin whether the caller has admin privileges
     * @return the updated record
     */
    @Transactional
    public HealthRecord updateRecordForUser(Long id, HealthRecord updated, String username, boolean isAdmin) {
        HealthRecord existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Health record not found"));
        if (!existing.getOwnerUsername().equals(username) && !isAdmin) {
            throw new SecurityException("Access denied to update health record " + id);
        }
        existing.setDiagnosis(updated.getDiagnosis());
        existing.setTreatment(updated.getTreatment());
        existing.setVaccine(updated.getVaccine());
        existing.setNotes(updated.getNotes());
        existing.setDate(updated.getDate());
        existing.setPenalty(updated.getPenalty());
        // Only admins can reassign ownership
        if (isAdmin && updated.getOwnerUsername() != null) {
            existing.setOwnerUsername(updated.getOwnerUsername());
        }
        return existing;
    }

    /**
     * Deletes a record after validating ownership.
     *
     * @param id the record ID
     * @param username the caller's username
     * @param isAdmin whether the caller has admin privileges
     */
    public void deleteRecordForUser(Long id, String username, boolean isAdmin) {
        HealthRecord record = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Health record not found"));
        if (record.getOwnerUsername().equals(username) || isAdmin) {
            repository.deleteById(id);
        } else {
            throw new SecurityException("Access denied to delete health record " + id);
        }
    }

    /**
     * Calculates a cumulative health penalty for a given animal by summing the penalty values of all its records.
     *
     * @param animalId the ID of the animal
     * @return the health penalty factor (0 if no records)
     */
    public double getHealthPenaltyForAnimal(Long animalId) {
        return repository.findByAnimalId(animalId)
                .stream()
                .map(record -> record.getPenalty() != null ? record.getPenalty() : 0.0)
                .reduce(0.0, Double::sum);
    }
}