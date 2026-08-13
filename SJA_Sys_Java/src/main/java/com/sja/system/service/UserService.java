package com.sja.system.service;

import com.sja.system.common.PageResult;
import com.sja.system.dto.PasswordUpdateDTO;
import com.sja.system.dto.ProfileUpdateDTO;
import com.sja.system.dto.UserBatchUpdateDTO;
import com.sja.system.dto.UserCreateDTO;
import com.sja.system.dto.UserQueryDTO;
import com.sja.system.dto.UserUpdateDTO;
import com.sja.system.vo.UserVO;
import com.sja.system.vo.FeeContributorVO;

import java.util.List;

/**
 * User management service interface.
 */
public interface UserService {

    /**
     * Get a user's full profile including roles and departments.
     *
     * @param userId the user's primary key
     * @return populated UserVO
     */
    UserVO getUserProfile(Long userId);

    UserVO getManagedUserDetail(Long userId);

    /**
     * Update a user's self-service profile fields (phone, dormAddress, bankCard).
     *
     * @param userId the user performing the update
     * @param dto    the fields to update
     */
    void updateProfile(Long userId, ProfileUpdateDTO dto);

    void updateProfilePhoto(Long userId, String photo, Long operatorId, String operatorName, String operatorRole);

    /**
     * Change a user's password after verifying the old one.
     *
     * @param userId the user changing their password
     * @param dto    old and new password
     */
    void updatePassword(Long userId, PasswordUpdateDTO dto);

    /**
     * Re-authenticate the current user before a sensitive batch operation.
     */
    void verifyCurrentPassword(Long userId, String currentPassword);

    /**
     * Paginated user query with optional filters.
     *
     * @param query filter criteria
     * @return paginated user list
     */
    PageResult<UserVO> queryUsers(UserQueryDTO query);

    /**
     * Query the minimal identity fields required by the fee contributor selector.
     */
    List<FeeContributorVO> queryActiveFeeContributors();

    /**
     * Query all users matching the supplied filters for Excel export.
     */
    List<UserVO> queryUsersForExport(UserQueryDTO query);

    /**
     * Create a user account and its department and role associations.
     * The login username is kept in sync with the student number.
     */
    Long createUser(UserCreateDTO dto, Long operatorId, String operatorName, String operatorRole);

    /**
     * Admin update of a user's arbitrary fields.
     *
     * @param userId       target user
     * @param dto          validated complete user update
     * @param operatorId   who made the change
     * @param operatorName operator display name
     * @param operatorRole operator role name
     */
    void updateUser(Long userId, UserUpdateDTO dto, Long operatorId, String operatorName, String operatorRole);

    void resetUserPassword(Long userId, Long operatorId, String operatorName, String operatorRole);

    /**
     * Replace roles and/or update account status for multiple users.
     */
    void batchUpdateUsers(UserBatchUpdateDTO dto, Long operatorId, String operatorName, String operatorRole);

    /**
     * Enable or disable a user account.
     *
     * @param userId       target user
     * @param status       1=active, 0=disabled
     * @param operatorId   who made the change
     * @param operatorName operator display name
     * @param operatorRole operator role name
     */
    void updateUserStatus(Long userId, Integer status, Long operatorId, String operatorName, String operatorRole);
}
