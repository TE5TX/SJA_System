package com.sja.system.service.storage;

import com.sja.system.application.model.CurrentActor;
import com.sja.system.application.model.UploadedFile;
import com.sja.system.dal.mysql.ImportArchiveRepository;
import com.sja.system.dal.mysql.ImportArchiveRepository.Archive;
import com.sja.system.dal.mysql.StorageLocationRepository;
import com.sja.system.dal.mysql.StorageLocationRepository.Location;
import com.sja.system.service.system.OperationLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 原件归档应以随机文件名保存，并经哈希校验后下载。 */
// 202609291708 - 覆盖了XLSX原件持久化及Windows/Linux通用的相对路径读取。
class ImportArchiveServiceTest {

    @TempDir Path root;

    @Test
    void savesAndDownloadsOriginalWorkbook() {
        StorageLocationRepository locationRepository = mock(StorageLocationRepository.class);
        ImportArchiveRepository archiveRepository = mock(ImportArchiveRepository.class);
        Location location = new Location(1, "IMPORT", "imports", true, 7L, LocalDateTime.now());
        when(locationRepository.findByCategory("IMPORT")).thenReturn(java.util.List.of(location));
        when(locationRepository.findById(1L)).thenReturn(location);
        StorageLocationService locations = new StorageLocationService(locationRepository,
                mock(OperationLogService.class), root.toString());
        ImportArchiveService service = new ImportArchiveService(locations, archiveRepository);
        byte[] contents = {80, 75, 3, 4, 1, 2, 3};
        UploadedFile file = new UploadedFile() {
            public String originalFilename() { return "名单.xlsx"; }
            public long size() { return contents.length; }
            public boolean empty() { return false; }
            public InputStream inputStream() { return new ByteArrayInputStream(contents); }
        };
        CurrentActor actor = new CurrentActor(7L, "管理员", 0);

        service.savePreview(file, "test-token", ImportArchiveService.ROSTER, actor, 2);
        ArgumentCaptor<Archive> saved = ArgumentCaptor.forClass(Archive.class);
        verify(archiveRepository).insert(saved.capture(), anyString());
        Archive archive = saved.getValue();
        assertEquals("名单.xlsx", archive.originalName());
        assertEquals("PREVIEWED", archive.status());
        when(archiveRepository.findById(archive.id())).thenReturn(archive);
        assertArrayEquals(contents, service.download(archive.id(), actor).content());
    }
}
