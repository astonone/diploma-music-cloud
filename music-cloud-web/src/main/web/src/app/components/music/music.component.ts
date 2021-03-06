import { Component, OnInit } from '@angular/core';
import { TrackService } from '../../services/track.service';
import { TrackList } from '../../dto/track-list';
import { Track } from '../../dto/track';
import { AddTrackToUserDialog } from './dialog/add-track-to-user-dialog';
import { MatDialog } from '@angular/material';
import { SharedService } from '../../services/shared.service';
import { User } from '../../dto/user';
import { DeleteTrackDialog } from './dialog/delete-track-dialog';
import { ChangeTrackDialog } from './dialog/change-track-dialog';
import { FileService } from '../../services/file.service';
import { Router } from '@angular/router';
import { Genre } from '../../dto/genre';
import { Mood } from '../../dto/mood';
import { GenreService } from '../../services/genre.service';
import { MoodService } from '../../services/mood.service';
import { GenreList } from '../../dto/genre-list';
import { MoodList } from '../../dto/mood-list';

@Component({
  selector: 'app-music',
  templateUrl: './music.component.html',
  styleUrls: ['./music.component.css']
})
export class MusicComponent implements OnInit {

  private user: User;
  public tracks: Track[] = [];
  private response: TrackList;
  public tracksLength = 10;
  public pageEvent: any;
  public page = 0;
  public pageSize = 10;
  public pageSizeOptions: any = [10, 25, 50];
  // filters
  public title = '';
  public artist = '';
  public genres: Genre[] = [];
  public moods: Mood[] = [];
  public selectedGenres:  Genre[] = [];
  public selectedMoods: Mood[] = [];
  private isFind = false;

  ngOnInit() {
    this.loadTracksList(null);
    this.loadGenres();
    this.loadMoods();
  }

  constructor(private trackService: TrackService,
              public dialog: MatDialog,
              public shared: SharedService,
              private fileService: FileService,
              private router: Router,
              private genreService: GenreService,
              private moodService: MoodService) {
    this.user = this.shared.getLoggedUser();
  }

  private loadTracksList(event) {
    if (event) {
      if (!this.isFind) {
        this.trackService.getAllTracks(event.pageIndex, event.pageSize)
            .subscribe(data => {
              this.response = new TrackList(data);
              this.tracksLength = this.response.allCount;
              this.tracks = this.response.tracks;
              this.loadAudioFiles();
            });
      } else {
        const genresObj = [];
        const moodsObj = [];
        this.selectedGenres.forEach(genre => genresObj.push(genre.toObject()));
        this.selectedMoods.forEach(mood => moodsObj.push(mood.toObject()));
        const request = {
          title: this.title,
          artist: this.artist,
          genres: genresObj,
          moods: moodsObj
        };
        this.trackService.findTracks(request, event.pageIndex, event.pageSize).subscribe(data => {
          this.response = new TrackList(data);
          this.tracksLength = this.response.allCount;
          this.tracks = this.response.tracks;
          this.loadAudioFiles();
        });
      }
    } else {
      if (!this.isFind) {
        this.trackService.getAllTracks(this.page, this.pageSize).subscribe(data => {
          this.response = new TrackList(data);
          this.tracks = this.response.tracks;
          this.tracksLength = this.response.allCount;
          this.loadAudioFiles();
        });
      } else {
        const genresObj = [];
        const moodsObj = [];
        this.selectedGenres.forEach(genre => genresObj.push(genre.toObject()));
        this.selectedMoods.forEach(mood => moodsObj.push(mood.toObject()));
        const request = {
          title: this.title,
          artist: this.artist,
          genres: genresObj,
          moods: moodsObj
        };
        this.trackService.findTracks(request, this.page, this.pageSize).subscribe(data => {
          this.response = new TrackList(data);
          this.tracksLength = this.response.allCount;
          this.tracks = this.response.tracks;
          this.loadAudioFiles();
        });
      }
    }
  }

  private loadAudioFiles() {
    for (let i = 0; i < this.tracks.length; i++) {
      this.loadFile(this.tracks[i]);
    }
  }

  public addTrackToUser(id: number) {
    this.trackService.addTrackToUser(this.user.id, id)
        .subscribe(data => {
          this.openTrackCreatedDialog(null);
        });
  }

  public deleteTrack(track: Track) {
    const dialogRef = this.dialog.open(DeleteTrackDialog, {
      data : null
    });
    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.trackService.deleteTrack(track.id).subscribe(() => {
          if (this.tracks.length === 1) {
            this.deleteTrackFromList(track.id, this.tracks);
          } else {
            this.loadTracksList(null);
          }
        });
      }
    });
  }

  private openTrackCreatedDialog(response: any): void {
    const dialogRef = this.dialog.open(AddTrackToUserDialog, {
      data : response
    });
    dialogRef.afterClosed().subscribe(result => {
    });
  }

  public changeTrack(track: Track) {
    const dialogRef = this.dialog.open(ChangeTrackDialog, {
      data : track
    });
    dialogRef.afterClosed().subscribe(result => {
    });
  }

  public rateTrack(track: Track) {
    this.trackService.rateTrack(track.id, this.user.id, track.tempRating)
        .subscribe(data => {
          const updatedTrack = new Track(data);
          track.rating = updatedTrack.rating;
          track.tempRating = null;
        });
  }

  private loadFile(track: Track) {
    track.files = this.fileService.getUploadedTrack(track.filename);
  }

  private deleteTrackFromList(trackId: number, tracks: Track[]) {
    const index = tracks.map(x => {
      return x.id;
    }).indexOf(trackId);

    tracks.splice(index, 1);
  }

  public findTracks() {
    this.isFind = true;
    this.tracks = [];
    this.tracksLength = 0;
    this.loadTracksList(null);
  }

  public clearFilters() {
    this.isFind = false;
    this.tracks = [];
    this.tracksLength = 0;
    this.title = '';
    this.artist = '';
    this.selectedGenres = [];
    this.selectedMoods = [];
    this.loadTracksList(null);
  }

  private loadGenres() {
    this.genreService.getAllGenres().subscribe(data => {
      const allGenres: GenreList = new GenreList(data);
      this.genres = allGenres.genres;
    });
  }

  private loadMoods() {
    this.moodService.getAllMoods().subscribe(data => {
      const allMoods: MoodList = new MoodList(data);
      this.moods = allMoods.moods;
    });
  }
}
