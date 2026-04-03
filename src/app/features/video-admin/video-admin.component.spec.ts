import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { signal, WritableSignal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { VideoAdminComponent } from './video-admin.component';
import { VideoService } from '../../shared/services/video/video.service';
import { NotificationService } from '../../shared/services/alert-message/alert-message.service';
import { environment } from '../../../environments/environment';

describe('VideoAdminComponent', () => {
  let component: VideoAdminComponent;
  let fixture: ComponentFixture<VideoAdminComponent>;
  let httpMock: HttpTestingController;
  let addVideoSpy: ReturnType<typeof vi.fn>;
  let removeVideoSpy: ReturnType<typeof vi.fn>;
  let alertErrorSpy: ReturnType<typeof vi.fn>;
  let videosSignal: WritableSignal<any[]>;

  const mockCategories = [
    { id: 'cat-uuid-1', name: 'Bolos', type: 'VIDEO' },
    { id: 'cat-uuid-2', name: 'Salgados', type: 'VIDEO' },
  ];

  const baseVideo = {
    id: 'v1',
    title: 'Video 1',
    category: { id: 'cat-uuid-1', name: 'Bolos', type: 'VIDEO' },
  };

  async function createComponent(initialCategories = mockCategories) {
    await TestBed.configureTestingModule({
      imports: [VideoAdminComponent, HttpClientTestingModule],
      providers: [
        {
          provide: VideoService,
          useValue: {
            addVideo: addVideoSpy,
            removeVideo: removeVideoSpy,
            videos: videosSignal.asReadonly(),
          },
        },
        {
          provide: NotificationService,
          useValue: {
            error: alertErrorSpy,
            success: vi.fn(),
            warning: vi.fn(),
            info: vi.fn(),
            showDefault: vi.fn(),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VideoAdminComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);

    const req = httpMock.expectOne((r) =>
      r.method === 'GET' &&
      r.url === `${environment.apiUrl}/categories` &&
      r.params.get('type') === 'VIDEO'
    );
    req.flush(initialCategories);

    fixture.detectChanges();
  }

  function patchValidForm(overrides: Record<string, any> = {}) {
    component.form.patchValue({
      title: 'Bolo de Cenoura',
      description: 'Receita simples e deliciosa',
      url: 'https://cdn.example.com/video.mp4',
      cover: 'https://cdn.example.com/cover.jpg',
      categoryName: 'Bolos',
      recipe: 'Misture tudo e asse.',
      protein: 3,
      carbs: 20,
      fat: 5,
      fiber: 2,
      calories: 120,
      ...overrides,
    });
  }

  beforeEach(async () => {
    addVideoSpy = vi.fn(() => of(void 0));
    removeVideoSpy = vi.fn();
    alertErrorSpy = vi.fn();
    videosSignal = signal<any[]>([baseVideo]);

    await createComponent();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should load categories from backend', () => {
    expect(component.categories.length).toBe(2);
    expect(component.categories[0].name).toBe('Bolos');
  });

  it('should render csv upload above the form heading', () => {
    const csvSection = fixture.nativeElement.querySelector('app-csv-upload');
    const h2 = fixture.nativeElement.querySelector('h2');

    expect(csvSection).toBeTruthy();
    expect(h2.textContent).toContain('Adicionar Vídeo');

    const children = Array.from(csvSection.parentElement!.children);
    expect(children.indexOf(csvSection)).toBeLessThan(children.indexOf(h2));
  });

  it('should render public url fields and upload areas', () => {
    const urlInput = fixture.nativeElement.querySelector('input[formControlName="url"]');
    const coverInput = fixture.nativeElement.querySelector('input[formControlName="cover"]');
    const fileInputs = fixture.nativeElement.querySelectorAll('input[type="file"]');

    expect(urlInput).toBeTruthy();
    expect(coverInput).toBeTruthy();
    expect(fileInputs).toHaveLength(2);
    expect((fileInputs[0] as HTMLInputElement).accept).toBe('video/*');
    expect((fileInputs[1] as HTMLInputElement).accept).toBe('image/*');
  });

  it('should render description and recipe as textareas', () => {
    expect(fixture.nativeElement.querySelector('textarea[formControlName="description"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('textarea[formControlName="recipe"]')).toBeTruthy();
  });

  it('should render populated category datalist', () => {
    const options = fixture.nativeElement.querySelectorAll('#video-category-list option');
    expect(options).toHaveLength(2);
    expect((options[0] as HTMLOptionElement).value).toBe('Bolos');
    expect((options[1] as HTMLOptionElement).value).toBe('Salgados');
  });

  it('should render delete buttons with aria labels', () => {
    expect(fixture.nativeElement.querySelector('[aria-label="Deletar vídeo"]')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('[aria-label="Deletar categoria"]')).toBeTruthy();
  });

  it('should show empty video state when list is empty', () => {
    videosSignal.set([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.resource-empty')?.textContent).toContain(
      'Nenhum vídeo cadastrado.'
    );
  });

  it('should show empty categories state when categories list is empty', () => {
    component.categories = [];
    fixture.detectChanges();

    const emptyStates = Array.from(fixture.nativeElement.querySelectorAll('.resource-empty')).map((el: any) =>
      el.textContent.trim()
    );

    expect(emptyStates).toContain('Nenhuma categoria cadastrada.');
  });

  it('should keep submit disabled while form is invalid', () => {
    const saveButton = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(saveButton.disabled).toBe(true);
  });

  it('should validate required and min length fields', () => {
    const title = component.form.get('title')!;
    const description = component.form.get('description')!;
    const categoryName = component.form.get('categoryName')!;

    title.setValue('aa');
    description.setValue('1234');
    categoryName.setValue('');

    expect(title.hasError('minlength')).toBe(true);
    expect(description.hasError('minlength')).toBe(true);
    expect(categoryName.hasError('required')).toBe(true);
  });

  it('should enable submit when public url flow is valid', () => {
    patchValidForm();
    fixture.detectChanges();

    const saveButton = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(component.isSaveDisabled).toBe(false);
    expect(saveButton.disabled).toBe(false);
  });

  it('should preserve multiline description and recipe values in save payload', async () => {
    patchValidForm({
      description: 'Linha 1\nLinha 2',
      recipe: 'Passo 1\nPasso 2',
    });

    await component.save();

    expect(addVideoSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        description: 'Linha 1\nLinha 2',
        recipe: 'Passo 1\nPasso 2',
      })
    );
  });

  it('should not save if required fields are invalid', async () => {
    component.form.patchValue({
      title: '',
      description: '',
      categoryName: '',
    });

    await component.save();
    expect(addVideoSpy).not.toHaveBeenCalled();
  });

  it('should not save and should alert when there is no video source', async () => {
    patchValidForm({ url: '', cover: '' });

    await component.save();

    expect(addVideoSpy).not.toHaveBeenCalled();
    expect(alertErrorSpy).toHaveBeenCalledWith(
      'Informe uma URL pública do vídeo ou selecione um arquivo.'
    );
  });

  it('should call addVideo with json request when public url flow is used', async () => {
    patchValidForm();

    await component.save();

    expect(addVideoSpy).toHaveBeenCalledWith({
      title: 'Bolo de Cenoura',
      description: 'Receita simples e deliciosa',
      url: 'https://cdn.example.com/video.mp4',
      cover: 'https://cdn.example.com/cover.jpg',
      categoryId: 'cat-uuid-1',
      recipe: 'Misture tudo e asse.',
      protein: 3,
      carbs: 20,
      fat: 5,
      fiber: 2,
      calories: 120,
      videoFile: null,
      coverFile: null,
    });
  });

  it('should coerce numeric fields before saving', async () => {
    patchValidForm({
      protein: '8',
      carbs: '15',
      fat: '4',
      fiber: '3',
      calories: '99',
    });

    await component.save();

    expect(addVideoSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        protein: 8,
        carbs: 15,
        fat: 4,
        fiber: 3,
        calories: 99,
      })
    );
  });

  it('should use video url as cover if cover is empty in json flow', async () => {
    patchValidForm({ cover: '' });

    await component.save();

    const callArg = addVideoSpy.mock.calls[0][0];
    expect(callArg.cover).toBe('https://cdn.example.com/video.mp4');
  });

  it('should find category from fresh api list when not in local list', async () => {
    patchValidForm({
      title: 'Novo Video',
      description: 'Descricao longa',
      url: 'https://cdn.example.com/novo.mp4',
      cover: '',
      categoryName: 'Doces',
      recipe: '',
      protein: 0,
      carbs: 0,
      fat: 0,
      fiber: 0,
      calories: 0,
    });

    const p = component.save();

    const listReq = httpMock.expectOne((r) =>
      r.method === 'GET' &&
      r.url === `${environment.apiUrl}/categories` &&
      r.params.get('type') === 'VIDEO'
    );
    listReq.flush([...mockCategories, { id: 'cat-new', name: 'Doces', type: 'VIDEO' }]);

    await p;

    expect(addVideoSpy).toHaveBeenCalledWith({
      title: 'Novo Video',
      description: 'Descricao longa',
      url: 'https://cdn.example.com/novo.mp4',
      cover: 'https://cdn.example.com/novo.mp4',
      categoryId: 'cat-new',
      recipe: '',
      protein: 0,
      carbs: 0,
      fat: 0,
      fiber: 0,
      calories: 0,
      videoFile: null,
      coverFile: null,
    });
  });

  it('should append freshly resolved category to local list', async () => {
    patchValidForm({ categoryName: 'Doces' });

    const p = component.save();

    const listReq = httpMock.expectOne((r) =>
      r.method === 'GET' &&
      r.url === `${environment.apiUrl}/categories` &&
      r.params.get('type') === 'VIDEO'
    );
    listReq.flush([...mockCategories, { id: 'cat-new', name: 'Doces', type: 'VIDEO' }]);

    await p;

    expect(component.categories.some((c) => c.id === 'cat-new' && c.name === 'Doces')).toBe(true);
  });

  it('should show alert and stop save when ensureCategoryId fails', async () => {
    patchValidForm({ categoryName: 'Doces' });

    const p = component.save();

    const listReq = httpMock.expectOne((r) =>
      r.method === 'GET' &&
      r.url === `${environment.apiUrl}/categories` &&
      r.params.get('type') === 'VIDEO'
    );
    listReq.flush([]);

    const createReq = httpMock.expectOne(`${environment.apiUrl}/categories`);
    expect(createReq.request.method).toBe('POST');
    createReq.error(new ProgressEvent('error'));

    await p;

    expect(addVideoSpy).not.toHaveBeenCalled();
    expect(alertErrorSpy).toHaveBeenCalled();
  });

  it('should reset form and selected file names after successful save', async () => {
    patchValidForm();
    component.videoFile = new File(['video'], 'video.mp4', { type: 'video/mp4' });
    component.coverFile = new File(['img'], 'cover.jpg', { type: 'image/jpeg' });
    component.videoFileName = 'video.mp4';
    component.coverFileName = 'cover.jpg';

    await component.save();

    expect(component.form.get('title')?.value).toBe('');
    expect(component.form.get('categoryName')?.value).toBe('');
    expect(component.form.get('protein')?.value).toBe(0);
    expect(component.videoFileName).toBe('');
    expect(component.coverFileName).toBe('');
    expect(component.videoFile).toBeNull();
    expect(component.coverFile).toBeNull();
  });

  it('should store selected video file and clear public url', () => {
    component.form.patchValue({ url: 'https://cdn.example.com/video.mp4' });
    const file = new File(['video'], 'video.mp4', { type: 'video/mp4' });

    component.onVideoFile({ target: { files: [file] } } as any);

    expect(component.videoFileName).toBe('video.mp4');
    expect(component.videoFile).toBe(file);
    expect(component.form.get('url')?.value).toBe('');
  });

  it('should ignore empty video file selection', () => {
    component.form.patchValue({ url: '' });

    component.onVideoFile({ target: { files: [] } } as any);

    expect(component.videoFileName).toBe('');
    expect(component.videoFile).toBeNull();
    expect(component.form.get('url')?.value).toBe('');
  });

  it('should store selected cover file and clear public cover', () => {
    component.form.patchValue({ cover: 'https://cdn.example.com/cover.jpg' });
    const file = new File(['img'], 'cover.jpg', { type: 'image/jpeg' });

    component.onCoverFile({ target: { files: [file] } } as any);

    expect(component.coverFileName).toBe('cover.jpg');
    expect(component.coverFile).toBe(file);
    expect(component.form.get('cover')?.value).toBe('');
  });

  it('should ignore empty cover file selection', () => {
    component.form.patchValue({ cover: '' });

    component.onCoverFile({ target: { files: [] } } as any);

    expect(component.coverFileName).toBe('');
    expect(component.coverFile).toBeNull();
    expect(component.form.get('cover')?.value).toBe('');
  });

  it('should clear selected video file when public video url is typed', () => {
    component.videoFile = new File(['video'], 'video.mp4', { type: 'video/mp4' });
    component.videoFileName = 'video.mp4';
    component.form.patchValue({ url: 'https://cdn.example.com/public.mp4' });

    component.onUrlInput();

    expect(component.videoFile).toBeNull();
    expect(component.videoFileName).toBe('');
  });

  it('should clear selected cover file when public cover url is typed', () => {
    component.coverFile = new File(['img'], 'cover.jpg', { type: 'image/jpeg' });
    component.coverFileName = 'cover.jpg';
    component.form.patchValue({ cover: 'https://cdn.example.com/public-cover.jpg' });

    component.onCoverInput();

    expect(component.coverFile).toBeNull();
    expect(component.coverFileName).toBe('');
  });

  it('should allow local file flow and call addVideo with multipart-ready payload', async () => {
    const videoFile = new File(['video'], 'video.mp4', { type: 'video/mp4' });
    const coverFile = new File(['img'], 'cover.jpg', { type: 'image/jpeg' });
    patchValidForm({ url: '', cover: '' });
    component.videoFile = videoFile;
    component.coverFile = coverFile;
    component.videoFileName = 'video.mp4';
    component.coverFileName = 'cover.jpg';
    fixture.detectChanges();

    await component.save();

    expect(addVideoSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        url: undefined,
        cover: undefined,
        videoFile,
        coverFile,
      })
    );
  });

  it('should keep form state when addVideo fails', async () => {
    addVideoSpy.mockReturnValueOnce(throwError(() => new Error('upload failed')));
    patchValidForm();

    await component.save();

    expect(component.form.get('title')?.value).toBe('Bolo de Cenoura');
    expect(component.form.get('url')?.value).toBe('https://cdn.example.com/video.mp4');
  });

  it('should prevent default on drag over', () => {
    const preventDefault = vi.fn();

    component.onDragOver({ preventDefault } as any);

    expect(preventDefault).toHaveBeenCalled();
  });

  it('should store video file and reset dragging on video drop', () => {
    const file = new File(['video'], 'drop-video.mp4', { type: 'video/mp4' });
    const preventDefault = vi.fn();
    component.isDraggingVideo = true;
    component.form.patchValue({ url: 'https://cdn.example.com/video.mp4' });

    component.onDropVideo({ preventDefault, dataTransfer: { files: [file] } } as any);

    expect(preventDefault).toHaveBeenCalled();
    expect(component.isDraggingVideo).toBe(false);
    expect(component.videoFileName).toBe('drop-video.mp4');
    expect(component.videoFile).toBe(file);
    expect(component.form.get('url')?.value).toBe('');
  });

  it('should ignore empty video drop', () => {
    const preventDefault = vi.fn();
    component.isDraggingVideo = true;

    component.onDropVideo({ preventDefault, dataTransfer: { files: [] } } as any);

    expect(preventDefault).toHaveBeenCalled();
    expect(component.isDraggingVideo).toBe(false);
    expect(component.videoFileName).toBe('');
    expect(component.videoFile).toBeNull();
  });

  it('should store cover file and reset dragging on cover drop', () => {
    const file = new File(['img'], 'drop-cover.jpg', { type: 'image/jpeg' });
    const preventDefault = vi.fn();
    component.isDraggingCover = true;
    component.form.patchValue({ cover: 'https://cdn.example.com/cover.jpg' });

    component.onDropCover({ preventDefault, dataTransfer: { files: [file] } } as any);

    expect(preventDefault).toHaveBeenCalled();
    expect(component.isDraggingCover).toBe(false);
    expect(component.coverFileName).toBe('drop-cover.jpg');
    expect(component.coverFile).toBe(file);
    expect(component.form.get('cover')?.value).toBe('');
  });

  it('should ignore empty cover drop', () => {
    const preventDefault = vi.fn();
    component.isDraggingCover = true;

    component.onDropCover({ preventDefault, dataTransfer: { files: [] } } as any);

    expect(preventDefault).toHaveBeenCalled();
    expect(component.isDraggingCover).toBe(false);
    expect(component.coverFileName).toBe('');
    expect(component.coverFile).toBeNull();
  });

  it('should open confirmation modal with video context', () => {
    component.askDeleteVideo('v1', 'Video 1');
    fixture.detectChanges();

    expect(component.isDeleteModalOpen).toBe(true);
    expect(component.deleteTitle).toBe('Deletar vídeo');
    expect(component.deleteMessage).toContain('Video 1');
  });

  it('should cancel delete and clear pending state', () => {
    component.askDeleteVideo('v1', 'Video 1');

    component.cancelDelete();

    expect(component.isDeleteModalOpen).toBe(false);
    expect(component.deleteMessage).toContain('');
  });

  it('should ask confirmation and call removeVideo when confirmed', () => {
    const btn = fixture.nativeElement.querySelector('[aria-label="Deletar vídeo"]') as HTMLButtonElement;
    btn.click();
    fixture.detectChanges();

    const confirm = fixture.nativeElement.querySelector('.confirm-btn') as HTMLButtonElement;
    confirm.click();

    expect(removeVideoSpy).toHaveBeenCalledWith('v1');
    expect(component.isDeleteModalOpen).toBe(false);
  });

  it('should open confirmation modal with category context', () => {
    component.askDeleteCategory('cat-uuid-1', 'Bolos');

    expect(component.deleteTitle).toBe('Deletar categoria');
    expect(component.deleteMessage).toContain('Bolos');
  });

  it('should delete category and remove it from local list when confirmed', () => {
    component.askDeleteCategory('cat-uuid-1', 'Bolos');

    component.confirmDelete();

    const deleteReq = httpMock.expectOne(`${environment.apiUrl}/categories/cat-uuid-1`);
    expect(deleteReq.request.method).toBe('DELETE');
    deleteReq.flush(null);
    fixture.detectChanges();

    expect(component.categories.map((c) => c.id)).not.toContain('cat-uuid-1');
    expect(component.isDeleteModalOpen).toBe(false);
  });

  it('should close modal and keep categories when category delete fails', () => {
    component.askDeleteCategory('cat-uuid-1', 'Bolos');

    component.confirmDelete();

    const deleteReq = httpMock.expectOne(`${environment.apiUrl}/categories/cat-uuid-1`);
    deleteReq.error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect(component.categories.map((c) => c.id)).toContain('cat-uuid-1');
    expect(component.isDeleteModalOpen).toBe(false);
  });
});
