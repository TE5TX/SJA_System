package com.sja.system.dal.mysql;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** 人员导入原件及执行结果的数据库适配器。 */
// 202609291708 - 完成了原始XLSX元数据和导入状态的持久化任务。
@Repository
public class ImportArchiveRepository {

    private final JdbcTemplate jdbc;

    public ImportArchiveRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Archive(String id, long locationId, String storedName, String originalName,
                          String sha256, long fileSize, String importType, long operatorId,
                          String operatorName, String status, int totalCount, int importedCount,
                          int skippedCount, int unprocessedCount, String message,
                          LocalDateTime createdAt, LocalDateTime confirmedAt) { }

    private static Archive map(ResultSet rs, int row) throws SQLException {
        var confirmed = rs.getTimestamp("confirmed_at");
        return new Archive(rs.getString("id"), rs.getLong("storage_location_id"),
                rs.getString("stored_name"), rs.getString("original_name"), rs.getString("sha256"),
                rs.getLong("file_size"), rs.getString("import_type"), rs.getLong("operator_id"),
                rs.getString("operator_name"), rs.getString("status"), rs.getInt("total_count"),
                rs.getInt("imported_count"), rs.getInt("skipped_count"), rs.getInt("unprocessed_count"),
                rs.getString("result_message"), rs.getTimestamp("create_time").toLocalDateTime(),
                confirmed == null ? null : confirmed.toLocalDateTime());
    }

    private static final String COLUMNS = "id, storage_location_id, stored_name, original_name, sha256, "
            + "file_size, import_type, operator_id, operator_name, status, total_count, "
            + "imported_count, skipped_count, unprocessed_count, result_message, create_time, confirmed_at";

    public void insert(Archive archive, String tokenHash) {
        jdbc.update("INSERT INTO sys_import_archive(" + COLUMNS + ", token_hash) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NULL, ?)",
                archive.id(), archive.locationId(), archive.storedName(), archive.originalName(),
                archive.sha256(), archive.fileSize(), archive.importType(), archive.operatorId(),
                archive.operatorName(), archive.status(), archive.totalCount(), archive.importedCount(),
                archive.skippedCount(), archive.unprocessedCount(), archive.message(), tokenHash);
    }

    public Archive findByToken(String tokenHash, long operatorId) {
        List<Archive> rows = jdbc.query("SELECT " + COLUMNS + " FROM sys_import_archive "
                        + "WHERE token_hash = ? AND operator_id = ?", ImportArchiveRepository::map,
                tokenHash, operatorId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public Archive findById(String id) {
        List<Archive> rows = jdbc.query("SELECT " + COLUMNS + " FROM sys_import_archive WHERE id = ?",
                ImportArchiveRepository::map, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Archive> page(int limit, int offset) {
        return jdbc.query("SELECT " + COLUMNS + " FROM sys_import_archive "
                + "ORDER BY create_time DESC, id DESC LIMIT ? OFFSET ?",
                ImportArchiveRepository::map, limit, offset);
    }

    public long count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sys_import_archive", Long.class);
    }

    public int finish(String id, String status, int imported, int skipped, int unprocessed, String message) {
        return jdbc.update("UPDATE sys_import_archive SET status = ?, imported_count = ?, skipped_count = ?, "
                        + "unprocessed_count = ?, result_message = ?, confirmed_at = NOW() "
                        + "WHERE id = ? AND status = 'PREVIEWED'",
                status, imported, skipped, unprocessed, message, id);
    }

    public void noteFailedAttempt(String id, String message) {
        jdbc.update("UPDATE sys_import_archive SET result_message = ? WHERE id = ? AND status = 'PREVIEWED'",
                message, id);
    }
}
