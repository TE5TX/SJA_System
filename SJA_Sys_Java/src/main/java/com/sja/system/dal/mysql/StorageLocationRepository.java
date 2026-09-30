package com.sja.system.dal.mysql;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/** 文件存储位置的持久化适配器；历史位置不删除，以保持旧文件可读。 */
// 202609291708 - 完成了跨平台存储目录的数据库持久化任务。
@Repository
public class StorageLocationRepository {

    private final JdbcTemplate jdbc;

    public StorageLocationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Location(long id, String category, String relativeDir, boolean active,
                           Long createdBy, LocalDateTime createdAt) { }

    public List<Location> findByCategory(String category) {
        return jdbc.query("SELECT id, category, relative_dir, active, created_by, create_time "
                        + "FROM sys_storage_location WHERE category = ? ORDER BY active DESC, id DESC",
                (rs, row) -> new Location(rs.getLong("id"), rs.getString("category"),
                        rs.getString("relative_dir"), rs.getBoolean("active"),
                        rs.getObject("created_by", Long.class),
                        rs.getTimestamp("create_time").toLocalDateTime()), category);
    }

    public Location findById(long id) {
        List<Location> locations = jdbc.query(
                "SELECT id, category, relative_dir, active, created_by, create_time "
                        + "FROM sys_storage_location WHERE id = ?",
                (rs, row) -> new Location(rs.getLong("id"), rs.getString("category"),
                        rs.getString("relative_dir"), rs.getBoolean("active"),
                        rs.getObject("created_by", Long.class),
                        rs.getTimestamp("create_time").toLocalDateTime()), id);
        return locations.isEmpty() ? null : locations.get(0);
    }

    public void activate(String category, String relativeDir, long actorId) {
        // 202609291708 - 串行化同类别目录切换，避免并发请求同时留下多个活动位置。
        jdbc.queryForList("SELECT id FROM sys_storage_location WHERE category = ? AND active = 1 FOR UPDATE",
                Long.class, category);
        jdbc.update("UPDATE sys_storage_location SET active = 0 WHERE category = ? AND active = 1", category);
        jdbc.update("INSERT INTO sys_storage_location(category, relative_dir, active, created_by, create_time) "
                        + "VALUES (?, ?, 1, ?, NOW())", category, relativeDir, actorId);
    }
}
