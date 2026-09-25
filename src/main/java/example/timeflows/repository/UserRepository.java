package example.timeflows.repository;

import example.timeflows.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(
            attributePaths = {
                "division",
                "division.department",
                "division.directorate",
                "division.tags",
                "subdivision",
                "roles",
                "tags"
            })
    Optional<User> findByUsername(String username);

    @EntityGraph(
            attributePaths = {
                "division",
                "division.department",
                "division.directorate",
                "division.tags",
                "subdivision",
                "roles",
                "tags"
            })
    Optional<User> findByEmail(String email);

    @EntityGraph(
            attributePaths = {
                "division",
                "division.department",
                "division.directorate",
                "division.tags",
                "subdivision",
                "roles",
                "tags"
            })
    Optional<User> findWithDivisionById(Long id);

    @EntityGraph(
            attributePaths = {
                "division",
                "division.department",
                "division.directorate",
                "division.tags",
                "subdivision",
                "roles",
                "tags"
            })
    List<User> findAllByOrderByUsernameAsc();

    @EntityGraph(
            attributePaths = {
                "division",
                "division.department",
                "division.directorate",
                "division.tags",
                "subdivision",
                "roles",
                "tags"
            })
    List<User> findByActiveTrueOrderByEmailAsc();

    @EntityGraph(
            attributePaths = {
                "division",
                "division.department",
                "division.directorate",
                "division.tags",
                "subdivision",
                "roles",
                "tags"
            })
    List<User> findByActiveFalseOrderByEmailAsc();

    @EntityGraph(
            attributePaths = {
                "division",
                "division.department",
                "division.directorate",
                "division.tags",
                "division.manager",
                "subdivision",
                "roles",
                "tags"
            })
    List<User> findByDivisionIdAndActiveTrueOrderByEmailAsc(Long divisionId);

    @EntityGraph(
            attributePaths = {
                "division",
                "division.department",
                "division.directorate",
                "division.tags",
                "division.manager",
                "subdivision",
                "roles",
                "tags"
            })
    List<User> findByDivisionDepartmentIdAndActiveTrueOrderByEmailAsc(Long departmentId);

    @EntityGraph(
            attributePaths = {
                "division",
                "division.department",
                "division.directorate",
                "division.tags",
                "subdivision",
                "roles",
                "tags"
            })
    List<User> findByDivisionDirectorateIdAndActiveTrueOrderByEmailAsc(Long directorateId);

    @EntityGraph(
            attributePaths = {
                "division",
                "division.department",
                "division.directorate",
                "division.tags",
                "subdivision",
                "roles",
                "tags"
            })
    List<User> findBySubdivisionIdAndActiveTrueOrderByEmailAsc(Long subdivisionId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Modifying
    @Query(
            value = "update divisions set manager_id = null where manager_id = :userId",
            nativeQuery = true)
    void clearDivisionManagerReferences(@Param("userId") Long userId);

    @Modifying
    @Query(
            value = "update directorates set manager_id = null where manager_id = :userId",
            nativeQuery = true)
    void clearDirectorateManagerReferences(@Param("userId") Long userId);

    @Modifying
    @Query(
            value = "delete from bonuses where user_id = :userId or created_by_id = :userId",
            nativeQuery = true)
    void deleteBonusData(@Param("userId") Long userId);

    @Modifying
    @Query(value = "delete from overtimes where user_id = :userId", nativeQuery = true)
    void deleteOvertimeData(@Param("userId") Long userId);

    @Modifying
    @Query(
            value = "delete from saved_overtime_filters where owner_id = :userId",
            nativeQuery = true)
    void deleteSavedOvertimeFilters(@Param("userId") Long userId);

    @Modifying
    @Query(value = "delete from mfa_recovery_codes where user_id = :userId", nativeQuery = true)
    void deleteMfaRecoveryCodes(@Param("userId") Long userId);
}
