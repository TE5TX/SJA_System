package com.sja.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sja.system.common.ApiException;
import com.sja.system.entity.SysRefreshToken;
import com.sja.system.entity.SysUser;
import com.sja.system.mapper.SysRefreshTokenMapper;
import com.sja.system.mapper.SysUserMapper;
import com.sja.system.security.JwtUtils;
import com.sja.system.security.TokenPair;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final JwtUtils jwtUtils;
    private final SysUserMapper sysUserMapper;
    private final SysRefreshTokenMapper refreshTokenMapper;

    @Transactional(rollbackFor = Exception.class)
    public TokenPair issue(SysUser user) {
        int version = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), version);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername(), version);
        saveRefreshToken(user.getId(), refreshToken, UUID.randomUUID().toString());
        return new TokenPair(accessToken, refreshToken);
    }

    @Transactional(rollbackFor = Exception.class, noRollbackFor = ApiException.class)
    public TokenPair rotate(String refreshToken) {
        if (!jwtUtils.validateRefreshToken(refreshToken)) {
            throw ApiException.unauthorized("刷新令牌已过期或无效");
        }

        String tokenId = jwtUtils.getTokenId(refreshToken);
        SysRefreshToken stored = refreshTokenMapper.selectOne(new QueryWrapper<SysRefreshToken>()
                .eq("token_id", tokenId));
        if (stored == null || stored.getExpiresAt() == null
                || stored.getExpiresAt().isBefore(LocalDateTime.now())
                || !constantTimeEquals(stored.getTokenHash(), hash(refreshToken))) {
            throw ApiException.unauthorized("刷新令牌已被使用或撤销");
        }
        if (stored.getRevokedAt() != null) {
            if (stored.getReplacedBy() != null) {
                revokeFamilyForReuse(stored);
                throw ApiException.unauthorized("检测到刷新令牌重放，当前账户的会话已全部失效");
            }
            throw ApiException.unauthorized("刷新令牌已被撤销");
        }

        SysUser user = sysUserMapper.selectById(stored.getUserId());
        if (user == null || !Integer.valueOf(1).equals(user.getIsValid())
                || !user.getId().equals(jwtUtils.getUserId(refreshToken))
                || !user.getUsername().equals(jwtUtils.getUsername(refreshToken))
                || !currentVersion(user).equals(jwtUtils.getTokenVersion(refreshToken))) {
            revokeStored(stored, null);
            throw ApiException.unauthorized("用户状态或权限已变更，请重新登录");
        }

        String newRefreshToken = jwtUtils.generateRefreshToken(
                user.getId(), user.getUsername(), currentVersion(user));
        String newTokenId = jwtUtils.getTokenId(newRefreshToken);

        int updated = refreshTokenMapper.update(null, new UpdateWrapper<SysRefreshToken>()
                .eq("id", stored.getId())
                .isNull("revoked_at")
                .set("revoked_at", LocalDateTime.now())
                .set("replaced_by", newTokenId));
        if (updated != 1) {
            revokeFamilyForReuse(stored);
            throw ApiException.unauthorized("检测到刷新令牌重复使用，请重新登录");
        }

        saveRefreshToken(user.getId(), newRefreshToken, stored.getTokenFamily());
        String newAccessToken = jwtUtils.generateAccessToken(
                user.getId(), user.getUsername(), currentVersion(user));
        return new TokenPair(newAccessToken, newRefreshToken);
    }

    @Transactional(rollbackFor = Exception.class)
    public void revokeAll(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return;
        }
        sysUserMapper.update(null, new UpdateWrapper<SysUser>()
                .eq("id", userId)
                .setSql("token_version = token_version + 1"));
        refreshTokenMapper.update(null, new UpdateWrapper<SysRefreshToken>()
                .eq("user_id", userId)
                .isNull("revoked_at")
                .set("revoked_at", LocalDateTime.now()));
    }

    private void saveRefreshToken(Long userId, String token, String tokenFamily) {
        SysRefreshToken entity = new SysRefreshToken();
        entity.setTokenId(jwtUtils.getTokenId(token));
        entity.setTokenHash(hash(token));
        entity.setTokenFamily(tokenFamily);
        entity.setUserId(userId);
        entity.setExpiresAt(LocalDateTime.ofInstant(
                jwtUtils.getExpiration(token).toInstant(), ZoneId.systemDefault()));
        refreshTokenMapper.insert(entity);
    }

    private void revokeFamilyForReuse(SysRefreshToken reusedToken) {
        int revoked = refreshTokenMapper.update(null, new UpdateWrapper<SysRefreshToken>()
                .eq("user_id", reusedToken.getUserId())
                .eq("token_family", reusedToken.getTokenFamily())
                .isNull("revoked_at")
                .set("revoked_at", LocalDateTime.now()));
        if (revoked > 0) {
            sysUserMapper.update(null, new UpdateWrapper<SysUser>()
                    .eq("id", reusedToken.getUserId())
                    .setSql("token_version = token_version + 1"));
        }
    }

    private void revokeStored(SysRefreshToken stored, String replacement) {
        refreshTokenMapper.update(null, new UpdateWrapper<SysRefreshToken>()
                .eq("id", stored.getId())
                .isNull("revoked_at")
                .set("revoked_at", LocalDateTime.now())
                .set(replacement != null, "replaced_by", replacement));
    }

    private Integer currentVersion(SysUser user) {
        return user.getTokenVersion() == null ? 0 : user.getTokenVersion();
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        return left != null && right != null && MessageDigest.isEqual(
                left.getBytes(StandardCharsets.US_ASCII), right.getBytes(StandardCharsets.US_ASCII));
    }
}
