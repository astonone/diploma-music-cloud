package com.kulygin.musiccloud.service.impl.yandex;

import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.ResourcesArgs;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.ServerException;
import com.yandex.disk.rest.exceptions.http.HttpCodeException;
import com.yandex.disk.rest.json.Link;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Log4j
public class YandexAPI {
    private RestClient restClient;
    private Credentials credentials;

    @Value("${storage.user}")
    private String user;

    @Value("${storage.token}")
    private String token;

    @PostConstruct
    public void init() {
        credentials = new Credentials(user, token);
        restClient = new RestClient(credentials);
    }

    public File uploadFileToYandexDisk(MultipartFile uploadedFileRef, boolean isPicture) throws IOException, ServerException {
        String serverPathAudio = "audio-storage/";
        String serverPathPhoto = "photo-storage/";
        String localPathPhoto = "storage-photo/";
        String localPathAudio = "storage-audio/";
        String serverPath = isPicture ? serverPathPhoto : serverPathAudio;
        String localPath = isPicture ? localPathPhoto : localPathAudio;

        String filename = UUID.randomUUID().toString() + uploadedFileRef.getOriginalFilename().substring(uploadedFileRef.getOriginalFilename().lastIndexOf("."));

        Link uploadLink = restClient.getUploadLink(serverPath + filename, true);

        File file =  multipartToFile(uploadedFileRef, localPath, filename);

        restClient.uploadFile(uploadLink, true, file, null);

        return file;
    }

    private File multipartToFile(MultipartFile multipart, String serverPath, String filename) throws IllegalStateException, IOException {
        File convFile = new File(serverPath + filename);
        if (!Files.exists(Paths.get(serverPath))) {
            Files.createDirectory(Paths.get(serverPath));
        }
        convFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(multipart.getBytes());
        fos.close();
        return convFile;
    }

    public Resource loadAudioFileFromYandexDisk(String filename) {
        File file = null;
        try {
            if (!Files.exists(Paths.get("storage-audio"))) {
                Files.createDirectory(Paths.get("storage-audio"));
            }
            file = new File("storage-audio/" + filename);
            file.createNewFile();
            com.yandex.disk.rest.json.Resource resource = restClient.getResources(new ResourcesArgs.Builder()
                    .setPath("/audio-storage/" + filename)
                    .setLimit(1)
                    .setOffset(2)
                    .build());

            restClient.downloadFile(resource.getPath().getPath(), file, null);
        } catch (HttpCodeException e) {
            // ignore it
        } catch (Exception e) {
            log.error("Problems with file downloading: ", e);
        }
        try {
            UrlResource urlResource = new UrlResource(file.toPath().toUri());
            return urlResource;
        } catch (MalformedURLException e) {
            log.error("Convertation file to resource error: ", e);
            return null;
        }
    }

    public Resource loadPhotoFileFromYandexDisk(String filename) {
        File file = null;
        try {
            if (!Files.exists(Paths.get("storage-photo"))) {
                Files.createDirectory(Paths.get("storage-photo"));
            }
            file = new File("storage-photo/" + filename);
            file.createNewFile();
            com.yandex.disk.rest.json.Resource resource = restClient.getResources(new ResourcesArgs.Builder()
                    .setPath("/photo-storage/" + filename)
                    .setLimit(1)
                    .setOffset(2)
                    .build());

            restClient.downloadFile(resource.getPath().getPath(), file, null);
        } catch (HttpCodeException e) {
            // ignore it
        } catch (Exception e) {
            log.error("Problems with file downloading: ", e);
        }
        try {
            UrlResource urlResource = new UrlResource(file.toPath().toUri());
            return urlResource;
        } catch (MalformedURLException e) {
            log.error("Convertation file to resource error: ", e);
            return null;
        }
    }
}
