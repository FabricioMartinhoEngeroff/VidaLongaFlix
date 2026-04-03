import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { VideoService } from './video.service';
import { LoggerService } from '../../../auth/services/logger.service';
import { environment } from '../../../../environments/environment';
import { Video, VideoRequest } from '../../types/videos';
import { FavoritesService } from '../favorites/favorites.service.';
import { ContentNotificationsService } from '../notifications/content-notifications.service';
import { NotificationService } from '../alert-message/alert-message.service';

const baseUrl = `${environment.apiUrl}/videos`;
const adminUrl = `${environment.apiUrl}/admin/videos`;

class FavoritesServiceMock {
  toggle = vi.fn();
  isFavorited = vi.fn().mockReturnValue(false);
}

class LoggerServiceMock {
  error = vi.fn();
}

class NotificationsMock {
  add = vi.fn();
}

class AlertMock {
  success = vi.fn();
  error = vi.fn();
}

const mockVideos: Video[] = [
  {
    id: '1',
    title: 'Bolo Fit',
    description: 'Bolo saudável',
    url: 'http://test.com/v1',
    cover: 'cover1.jpg',
    category: { id: '1', name: 'Bolos' },
    comments: [],
    commentCount: 0,
    views: 10,
    watchTime: 5.0,
    recipe: '',
    protein: 8,
    carbs: 20,
    fat: 3,
    fiber: 2,
    calories: 150,
    likesCount: 0,
    favorited: false,
  },
  {
    id: '2',
    title: 'Omelete Proteico',
    description: 'Omelete rápido',
    url: 'http://test.com/v2',
    cover: 'cover2.jpg',
    category: { id: '2', name: 'Salgados' },
    comments: [],
    commentCount: 0,
    views: 20,
    watchTime: 3.0,
    recipe: '',
    protein: 15,
    carbs: 2,
    fat: 5,
    fiber: 0,
    calories: 120,
    likesCount: 2,
    favorited: false,
  },
];

describe('VideoService', () => {
  let service: VideoService;
  let httpMock: HttpTestingController;
  let favoritesMock: FavoritesServiceMock;
  let notificationsMock: NotificationsMock;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        VideoService,
        { provide: FavoritesService, useClass: FavoritesServiceMock },
        { provide: LoggerService, useClass: LoggerServiceMock },
        { provide: ContentNotificationsService, useClass: NotificationsMock },
        { provide: NotificationService, useClass: AlertMock },
      ],
    });

    service = TestBed.inject(VideoService);
    httpMock = TestBed.inject(HttpTestingController);
    favoritesMock = TestBed.inject(FavoritesService) as any;
    notificationsMock = TestBed.inject(ContentNotificationsService) as any;

    httpMock.expectOne(baseUrl).flush(mockVideos);
  });

  afterEach(() => httpMock.verify());

  it('should create service', () => {
    expect(service).toBeTruthy();
  });

  it('should load videos from backend', () => {
    expect(service.videos().length).toBe(2);
    expect(service.totalVideos()).toBe(2);
  });

  it('should set empty array when backend fails', () => {
    service.loadVideos();
    httpMock.expectOne(baseUrl).error(new ProgressEvent('error'));
    expect(service.videos().length).toBe(0);
  });

  it('should create video with json payload and reload list', () => {
    const request: VideoRequest = {
      title: 'Novo Vídeo',
      description: 'Descrição',
      url: 'http://test.com/new',
      cover: 'cover.jpg',
      categoryId: '1',
      recipe: '',
      protein: 5,
      carbs: 10,
      fat: 2,
      fiber: 1,
      calories: 80,
      videoFile: null,
      coverFile: null,
    };

    service.addVideo(request).subscribe();

    const postReq = httpMock.expectOne(adminUrl);
    expect(postReq.request.method).toBe('POST');
    expect(postReq.request.body).toEqual({
      title: 'Novo Vídeo',
      description: 'Descrição',
      url: 'http://test.com/new',
      cover: 'cover.jpg',
      categoryId: '1',
      recipe: '',
      protein: 5,
      carbs: 10,
      fat: 2,
      fiber: 1,
      calories: 80,
    });
    postReq.flush(null);

    httpMock.expectOne(baseUrl).flush([
      ...mockVideos,
      { ...mockVideos[0], id: '3', title: 'Novo Vídeo' },
    ]);

    expect(notificationsMock.add).toHaveBeenCalledWith('VIDEO', 'Novo Vídeo');
    expect(service.totalVideos()).toBe(3);
  });

  it('should create video with multipart payload when files are provided', () => {
    const request: VideoRequest = {
      title: 'Novo Vídeo',
      description: 'Descrição',
      categoryId: '1',
      recipe: 'Passo 1',
      protein: 5,
      carbs: 10,
      fat: 2,
      fiber: 1,
      calories: 80,
      videoFile: new File(['video'], 'video.mp4', { type: 'video/mp4' }),
      coverFile: new File(['img'], 'cover.jpg', { type: 'image/jpeg' }),
    };

    service.addVideo(request).subscribe();

    const postReq = httpMock.expectOne(adminUrl);
    expect(postReq.request.method).toBe('POST');
    expect(postReq.request.body instanceof FormData).toBe(true);
    const body = postReq.request.body as FormData;
    expect(body.get('title')).toBe('Novo Vídeo');
    expect(body.get('categoryId')).toBe('1');
    expect(body.get('recipe')).toBe('Passo 1');
    expect(body.get('videoFile')).toBeTruthy();
    expect(body.get('coverFile')).toBeTruthy();
    postReq.flush(null);

    httpMock.expectOne(baseUrl).flush(mockVideos);
  });

  it('should update video and reload list', () => {
    service.updateVideo('1', { title: 'Bolo Fit Editado' });

    const putReq = httpMock.expectOne(`${adminUrl}/1`);
    expect(putReq.request.method).toBe('PUT');
    expect(putReq.request.body).toEqual({ title: 'Bolo Fit Editado' });
    putReq.flush(null);

    httpMock.expectOne(baseUrl).flush([
      { ...mockVideos[0], title: 'Bolo Fit Editado' },
      mockVideos[1],
    ]);

    expect(service.getVideoById('1')?.title).toBe('Bolo Fit Editado');
  });

  it('should optimistically remove video and keep removed on success', () => {
    service.removeVideo('1');
    expect(service.totalVideos()).toBe(1);

    const deleteReq = httpMock.expectOne(`${adminUrl}/1`);
    expect(deleteReq.request.method).toBe('DELETE');
    deleteReq.flush(null);

    expect(service.totalVideos()).toBe(1);
  });

  it('should rollback optimistic removal when backend delete fails', () => {
    service.removeVideo('1');
    expect(service.totalVideos()).toBe(1);

    const deleteReq = httpMock.expectOne(`${adminUrl}/1`);
    expect(deleteReq.request.method).toBe('DELETE');
    deleteReq.error(new ProgressEvent('error'), { status: 500, statusText: 'Server Error' });

    expect(service.totalVideos()).toBe(2);
    expect(service.getVideoById('1')).toBeTruthy();
  });

  it('should toggle favorite and update local state', () => {
    service.toggleFavorite('1');

    expect(favoritesMock.toggle).toHaveBeenCalledWith('1', 'VIDEO');

    const updated = service.videos().find((v) => v.id === '1')!;
    expect(updated.favorited).toBe(true);
    expect(updated.likesCount).toBe(1);
  });

  it('should decrement likesCount when unfavoriting', () => {
    service.toggleFavorite('1');
    service.toggleFavorite('1');

    const updated = service.videos().find((v) => v.id === '1')!;
    expect(updated.favorited).toBe(false);
    expect(updated.likesCount).toBe(0);
    expect(favoritesMock.toggle).toHaveBeenCalledTimes(2);
  });

  it('should get video by id', () => {
    expect(service.getVideoById('1')?.title).toBe('Bolo Fit');
  });

  it('should return undefined for unknown id', () => {
    expect(service.getVideoById('999')).toBeUndefined();
  });

  it('should filter videos by category', () => {
    const filtered = service.getVideosByCategory('1');
    expect(filtered.length).toBe(1);
    expect(filtered[0].category.name).toBe('Bolos');
  });

  it('should update totalLikes reactively', () => {
    const initial = service.totalLikes();
    service.toggleFavorite('1');
    expect(service.totalLikes()).toBe(initial + 1);
  });
});
