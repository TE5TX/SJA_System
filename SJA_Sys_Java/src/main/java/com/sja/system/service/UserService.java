package com.sja.system.service;

import com.sja.system.common.PageResult;
import com.sja.system.dto.PasswordUpdateDTO;
import com.sja.system.dto.ProfileUpdateDTO;
import com.sja.system.dto.UserQueryDTO;
import com.sja.system.vo.UserVO;

import java.util.Map;

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

    /**
     * Update a user's self-service profile fields (phone, dormAddress, bankCard).
     *
     * @param userId the user performing the update
     * @param dto    the fields to update
     */
    void updateProfile(Long userId, ProfileUpdateDTO dto);

    /**
     * Change a user's password after verifying the old one.
     *
     * @param userId the user changing their password
     * @param dto    old and new password
     */
    void updatePassword(Long userId, PasswordUpdateDTO dto);

    /**
     * Paginated user query with optional filters.
     *
     * @param query filter criteria
     * @return paginated user list
     */
    PageResult<UserVO> queryUsers(UserQueryDTO query);

    /**
     * Admin update of a user's arbitrary fields.
     *
     * @param userId       target user
     * @param updates      field name -> value map
     * @param operatorId   who made the change
     * @param operatorName operator display name
     * @param operatorRole operator role name
     */
    void updateUser(Long userId, Map<String, Object> updates, Long operatorId, String operatorName, String operatorRole);

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
