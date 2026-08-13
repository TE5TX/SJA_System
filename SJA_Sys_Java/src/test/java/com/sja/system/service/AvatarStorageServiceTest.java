package com.sja.system.service;

import com.sja.system.common.ApiException;
import com.sja.system.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvatarStorageServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void decodesResizesAndReencodesRealImage() throws Exception {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        AvatarStorageService service = new AvatarStorageService(
                userMapper, tempDirectory.toString(), 1_000_000L, 1_000, 64);

        BufferedImage source = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = source.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, 200, 100);
        graphics.dispose();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(source, "PNG", bytes);

        String url = service.store(new MockMultipartFile(
                "file", "avatar.png", "image/png", bytes.toByteArray()));
        BufferedImage stored = ImageIO.read(service.resolveUrl(url).toFile());

        assertTrue(url.matches("^/api/v1/upload/avatar/[0-9a-f-]{36}\\.(jpg|png)$"));
        assertTrue(Files.isRegularFile(service.resolveUrl(url)));
        assertEquals(64, stored.getWidth());
        assertEquals(32, stored.getHeight());
    }

    @Test
    void rejectsFileThatOnlyPretendsToBePng() {
        AvatarStorageService service = new AvatarStorageService(
                mock(SysUserMapper.class), tempDirectory.toString(), 1_000_000L, 1_000, 64);
        MockMultipartFile fake = new MockMultipartFile(
                "file", "avatar.png", "image/png", "not an image".getBytes(StandardCharsets.UTF_8));

        ApiException error = assertThrows(ApiException.class, () -> service.store(fake));

        assertEquals(400, error.getStatus());
    }

    @Test
    void refusesToAssignAnAvatarAlreadyReferencedByAnotherUser() throws Exception {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        AvatarStorageService service = new AvatarStorageService(
                userMapper, tempDirectory.toString(), 1_000_000L, 1_000, 64);
        String url = "/api/v1/upload/avatar/11111111-1111-1111-1111-111111111111.png";
        Files.write(service.resolveUrl(url), new byte[]{1});
        when(userMapper.selectCount(any())).thenReturn(1L);

        ApiException error = assertThrows(ApiException.class,
                () -> service.validateAssignableAvatar(url, 7L));

        assertEquals(409, error.getStatus());
    }
}
