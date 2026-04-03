export interface UserComment {
  id: string;
  name: string;
}

export interface Comment {
  id: string;
  text: string;
  date: string;
  user: UserComment;
}

export type CategoryType = 'VIDEO' | 'MENU';

export interface Category {
  id: string;
  name: string;
  type: CategoryType;
}

export interface Video {
  id: string;
  title: string;
  description: string;
  url: string;
  cover: string;
  category: { id: string; name: string };
  comments: Comment[];
  commentCount: number;
  views: number;
  watchTime: number;
  recipe: string;
  protein: number;
  carbs: number;
  fat: number;
  fiber: number;
  calories: number;
  favorited: boolean;
  likesCount: number;
}

export interface VideoRequest {
  title: string;
  description: string;
  categoryId: string;
  url?: string;
  cover?: string;
  recipe?: string;
  protein?: number;
  carbs?: number;
  fat?: number;
  fiber?: number;
  calories?: number;
  videoFile?: File | null;
  coverFile?: File | null;
}
