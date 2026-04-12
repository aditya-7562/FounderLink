package com.founderlink.User_Service.repository;

import com.founderlink.User_Service.entity.Role;
import com.founderlink.User_Service.entity.UserStatus;
import com.founderlink.User_Service.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findByRole(@Param("role") Role role);

    @Query("SELECT u FROM User u WHERE u.role = :role")
    Page<User> findByRole(@Param("role") Role role, Pageable pageable);

    long countByRole(Role role);

    @Query("SELECT u FROM User u WHERE u.role = :role AND " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> findByRoleAndKeyword(@Param("role") Role role,
                                    @Param("keyword") String keyword,
                                    Pageable pageable);

    @Query("SELECT u FROM User u WHERE " +
           "(:email IS NULL OR u.email LIKE %:email%) AND " +
           "(:name IS NULL OR u.name LIKE %:name%) AND " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:status IS NULL OR u.status = :status)")
    Page<User> searchUsers(@Param("email") String email,
                           @Param("name") String name,
                           @Param("role") Role role,
                           @Param("status") UserStatus status,
                           Pageable pageable);
}
