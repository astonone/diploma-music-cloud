package com.kulygin.musiccloud.web;

import com.kulygin.musiccloud.config.Constants;
import com.kulygin.musiccloud.domain.*;
import com.kulygin.musiccloud.dto.AllTracksDTO;
import com.kulygin.musiccloud.dto.ErrorResponseBody;
import com.kulygin.musiccloud.dto.TrackDTO;
import com.kulygin.musiccloud.dto.TrackFullInfoDTO;
import com.kulygin.musiccloud.enumeration.ApplicationErrorTypes;
import com.kulygin.musiccloud.exception.*;
import com.kulygin.musiccloud.service.GenreService;
import com.kulygin.musiccloud.service.MoodService;
import com.kulygin.musiccloud.service.TrackService;
import com.kulygin.musiccloud.service.UserService;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.annotation.MultipartConfig;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/track")
@MultipartConfig(fileSizeThreshold = 20971520) // Максимальный размер файла 20mb
public class TrackController {

    @Autowired
    private TrackService trackService;
    @Autowired
    private MoodService moodService;
    @Autowired
    private GenreService genreService;
    @Autowired
    private UserService userService;

    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public ResponseEntity<?> uploadFile(@RequestParam("uploadedFile") MultipartFile uploadedFileRef) throws PlaylistNotExistsException, TrackIsNotExistsException, FileIOException {
        // Получаем имя загруженного файла
        String fileName;
        // Генерируем уникальное имя файла
        UUID uuid = UUID.randomUUID();
        fileName = uuid.toString() + ".mp3";
        // Путь, где загруженный файл будет сохранен.
        String path = System.getProperty("user.home") + "/" + Constants.DOWNLOAD_PATH + fileName;
        // Буффер для хранения данных из uploadedFileRef
        byte[] buffer = new byte[1000];
        // Теперь создаем выходной файл outputFile на сервере
        File outputFile = new File(path);

        FileInputStream reader = null;
        FileOutputStream writer = null;
        int totalBytes = 0;
        try {
            outputFile.createNewFile();
            // Создаем входной поток для чтения данных из него
            reader = (FileInputStream) uploadedFileRef.getInputStream();
            // Создаем выходной поток для записи данных
            writer = new FileOutputStream(outputFile);
            // Считываем данные uploadedFileRef и пишем их в outputFile
            int bytesRead = 0;
            while ((bytesRead = reader.read(buffer)) != -1) {
                writer.write(buffer);
                totalBytes += bytesRead;
            }
        } catch (IOException iO) {
            return getErrorResponseBody(ApplicationErrorTypes.IO_ERROR);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException iO) {
                return getErrorResponseBody(ApplicationErrorTypes.IO_ERROR);
            }
        }
        try {
            return createTrack(fileName);
        } catch (InvalidDataException e) {
            return getErrorResponseBody(ApplicationErrorTypes.INVALID_DATA);
        }
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getTrack(@PathVariable("id") Long trackId) {
        Track track = trackService.getTrackById(trackId);
        if (track == null) {
            return getErrorResponseBody(ApplicationErrorTypes.TRACK_ID_NOT_FOUND);
        }
        return new ResponseEntity<>(convert(track), HttpStatus.OK);
    }

    @RequestMapping(value = "/findAllPagination", method = RequestMethod.GET)
    public ResponseEntity<?> getAllTracksPagination(@RequestParam("page") Long page, @RequestParam("pageSize") Long pageSize) {
        Page<Track> tracks = trackService.getAllTracksPagination(PageRequest.of(page.intValue(), pageSize.intValue(), new Sort(Sort.Direction.ASC, "id")));
        List<Track> resultListOfTrack = tracks.getContent();
        if (resultListOfTrack.size() == 0) {
            return getErrorResponseBody(ApplicationErrorTypes.DB_IS_EMPTY_OR_PAGE_IS_NOT_EXIST);
        }
        return new ResponseEntity<>(convert(resultListOfTrack), HttpStatus.OK);
    }

    @RequestMapping(value = "/getTracksByGenre/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getTracksByGenre(@PathVariable("id") Long genreId, @RequestParam("page") Long page, @RequestParam("pageSize") Long pageSize) {
        Genre genre = genreService.getGenreById(genreId);
        if (genre == null) {
            return getErrorResponseBody(ApplicationErrorTypes.GENRE_ID_NOT_FOUND);
        }
        Page<Track> tracks = trackService.getTracksByGenrePagination(PageRequest.of(page.intValue(), pageSize.intValue(), new Sort(Sort.Direction.ASC, "id")),
                genre);
        List<Track> resultListOfTrack = tracks.getContent();
        if (resultListOfTrack.size() == 0) {
            return getErrorResponseBody(ApplicationErrorTypes.DB_IS_EMPTY_OR_PAGE_IS_NOT_EXIST);
        }
        return new ResponseEntity<>(convert(resultListOfTrack), HttpStatus.OK);
    }

    @RequestMapping(value = "/getTracksByMood/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getTracksByMood(@PathVariable("id") Long moodId, @RequestParam("page") Long page, @RequestParam("pageSize") Long pageSize) {
        Mood mood = moodService.getMoodById(moodId);
        if (mood == null) {
            return getErrorResponseBody(ApplicationErrorTypes.MOOD_ID_NOT_FOUND);
        }
        Page<Track> tracks = trackService.getTracksByMoodPagination(PageRequest.of(page.intValue(), pageSize.intValue(), new Sort(Sort.Direction.ASC, "id")),
                mood);
        List<Track> resultListOfTrack = tracks.getContent();
        if (resultListOfTrack.size() == 0) {
            return getErrorResponseBody(ApplicationErrorTypes.DB_IS_EMPTY_OR_PAGE_IS_NOT_EXIST);
        }
        return new ResponseEntity<>(convert(resultListOfTrack), HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}/genre", method = RequestMethod.PUT)
    public ResponseEntity<?> addTrackGenre(@PathVariable("id") Long trackId, @RequestParam("genreId") Long genreId) {
        Track track = null;
        try {
            track = trackService.addTrackGenre(trackId, genreId);
        } catch (TrackIsNotExistsException trackIsNotExists) {
            return getErrorResponseBody(ApplicationErrorTypes.TRACK_ID_NOT_FOUND);
        } catch (GenreIsNotExistsException genreIsNotExists) {
            return getErrorResponseBody(ApplicationErrorTypes.GENRE_ID_NOT_FOUND);
        }
        return new ResponseEntity<>(convert(track), HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}/genre", method = RequestMethod.DELETE)
    public ResponseEntity<?> removeTrackGenre(@PathVariable("id") Long trackId, @RequestParam("genreId") Long genreId) {
        Track track = null;
        try {
            track = trackService.removeTrackGenre(trackId, genreId);
        } catch (TrackIsNotExistsException trackIsNotExists) {
            return getErrorResponseBody(ApplicationErrorTypes.TRACK_ID_NOT_FOUND);
        } catch (GenreIsNotExistsException genreIsNotExists) {
            return getErrorResponseBody(ApplicationErrorTypes.GENRE_ID_NOT_FOUND);
        } catch (TrackHasNotGenreException trackHasNotGenreException) {
            return getErrorResponseBody(ApplicationErrorTypes.TRACK_HAS_NOT_GENRE);
        }
        return new ResponseEntity<>(convert(track), HttpStatus.OK);
    }

    @RequestMapping(value = "{id}/mood", method = RequestMethod.PUT)
    public ResponseEntity<?> addTrackMood(@PathVariable("id") Long trackId, @RequestParam("moodId") Long moodId) {

        Track track = trackService.getTrackById(trackId);
        if (track == null) {
            return getErrorResponseBody(ApplicationErrorTypes.TRACK_ID_NOT_FOUND);
        }
        Mood mood = moodService.getMoodById(moodId);
        if (mood == null) {
            return getErrorResponseBody(ApplicationErrorTypes.MOOD_ID_NOT_FOUND);
        }
        track = trackService.addTrackMood(track, mood);
        return new ResponseEntity<>(convert(track), HttpStatus.OK);
    }

    @RequestMapping(value = "{id}/mood", method = RequestMethod.DELETE)
    public ResponseEntity<?> removeTrackMood(@PathVariable("id") Long trackId, @RequestParam("moodId") Long moodId) {
        Track track = trackService.getTrackById(trackId);
        if (track == null) {
            return getErrorResponseBody(ApplicationErrorTypes.TRACK_ID_NOT_FOUND);
        }
        Mood mood = moodService.getMoodById(moodId);
        if (mood == null) {
            return getErrorResponseBody(ApplicationErrorTypes.MOOD_ID_NOT_FOUND);
        }
        try {
            track = trackService.removeTrackMood(track, mood);
        } catch (MoodIsNotExistsException moodIsNotExists) {
            return getErrorResponseBody(ApplicationErrorTypes.MOOD_ID_NOT_FOUND);
        } catch (TrackHasNotThisMoodException trackHasNotThisMoodException) {
            return getErrorResponseBody(ApplicationErrorTypes.TRACK_HAS_NOT_THIS_MOOD);
        }
        return new ResponseEntity<Object>(convert(track), HttpStatus.OK);
    }

    @RequestMapping(value = "{id}/rating", method = RequestMethod.PUT)
    public ResponseEntity<?> addTrackRating(@PathVariable("id") Long trackId, @RequestParam("ratingValue") Integer ratingValue, @RequestParam("userId") Long userId) {
        Track track = trackService.getTrackById(trackId);
        if (track == null) {
            return getErrorResponseBody(ApplicationErrorTypes.TRACK_ID_NOT_FOUND);
        }
        User user = userService.getUserById(userId);
        if (user == null) {
            return getErrorResponseBody(ApplicationErrorTypes.USER_ID_NOT_FOUND);
        }
        track = trackService.addRating(track, ratingValue, user);
        return new ResponseEntity<>(convert(track), HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}/user", method = RequestMethod.PUT)
    public ResponseEntity<?> addTrackToUser(@PathVariable("id") Long trackId, @RequestParam("userId") Long userId) {
        try {
            trackService.addTrackToUser(userId, trackId);
        } catch (UserIsNotExistsException accountIsNotExists) {
            return getErrorResponseBody(ApplicationErrorTypes.USER_ID_NOT_FOUND);
        } catch (TrackIsNotExistsException e) {
            return getErrorResponseBody(ApplicationErrorTypes.TRACK_ID_NOT_FOUND);
        }
        return new ResponseEntity<>(convert(trackService.getTrackById(trackId)), HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}/user", method = RequestMethod.DELETE)
    public ResponseEntity<?> removeTrackFromUser(@PathVariable("id") Long trackId, @RequestParam("userId") Long userId) {
        try {
            trackService.removeTrackFromUser(userId, trackId);
        } catch (UserIsNotExistsException accountIsNotExists) {
            return getErrorResponseBody(ApplicationErrorTypes.USER_ID_NOT_FOUND);
        } catch (TrackIsNotExistsException e) {
            return getErrorResponseBody(ApplicationErrorTypes.TRACK_ID_NOT_FOUND);
        } catch (UserHasNotTrackException accountHasNotTrack) {
            return getErrorResponseBody(ApplicationErrorTypes.USER_HAS_NOT_TRACK);
        }
        return new ResponseEntity<>(convert(trackService.getTrackById(trackId)), HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}/fullInfo", method = RequestMethod.GET)
    public ResponseEntity<?> getTrackFullInfo(@PathVariable("id") Long trackId) {
        Track track = trackService.getTrackById(trackId);
        if (track == null) {
            return getErrorResponseBody(ApplicationErrorTypes.TRACK_ID_NOT_FOUND);
        }
        return new ResponseEntity<>(convertTrackFullInfo(track), HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteTrack(@PathVariable("id") Long trackId) throws TrackIsNotExistsException {
        try {
            trackService.deleteTrackById(trackId);
        } catch (TrackIsNotExistsException trackIsNotExists) {
            return getErrorResponseBody(ApplicationErrorTypes.TRACK_ID_NOT_FOUND);
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    private ResponseEntity<?> createTrack(String filename) throws PlaylistNotExistsException, TrackIsNotExistsException, InvalidDataException {
        Track track = null;
        try {
            track = trackService.createTrack(filename);
        } catch (TrackHasExistsException trackHasExists) {
            return getErrorResponseBody(ApplicationErrorTypes.TRACK_HAS_EXISTS);
        } catch (FileIsNotExistsException FileIsNotExists) {
            return getErrorResponseBody(ApplicationErrorTypes.FILE_NOT_FOUND);
        } catch (IOException iO) {
            getErrorResponseBody(ApplicationErrorTypes.IO_ERROR);
        } catch (UnsupportedTagException unsupportedTag) {
            getErrorResponseBody(ApplicationErrorTypes.UNSUPPORTED_TAG);
        }
        return new ResponseEntity<>(convert(track), HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}/update", method = RequestMethod.POST)
    public ResponseEntity<?> updateTrack(@PathVariable("id") Long trackId, @RequestBody TrackDTO trackInfo) {
        Track track = null;
        try {
            track = trackService.updateTrack(trackId, trackInfo.getTitle(), trackInfo.getArtist(), trackInfo.getAlbum(),
                    trackInfo.getYear(), trackInfo.getFilename(), trackInfo.getDuration());
        } catch (TrackIsNotExistsException trackIsNotExists) {
            return getErrorResponseBody(ApplicationErrorTypes.TRACK_ID_NOT_FOUND);
        }
        return new ResponseEntity<Object>(convert(track), HttpStatus.OK);
    }

    private TrackDTO convert(Track dbModel) {
        return (dbModel == null) ? null : new TrackDTO(dbModel);
    }

    private AllTracksDTO convert(List<Track> dbModel) {
        return (dbModel == null) ? null : new AllTracksDTO(dbModel);
    }

    private TrackFullInfoDTO convertTrackFullInfo(Track dbModel) {
        return (dbModel == null) ? null : new TrackFullInfoDTO(dbModel);
    }

    private ResponseEntity<ErrorResponseBody> getErrorResponseBody(ApplicationErrorTypes errorType) {
        return new ResponseEntity<>(new ErrorResponseBody(errorType), HttpStatus.NOT_FOUND);
    }
}