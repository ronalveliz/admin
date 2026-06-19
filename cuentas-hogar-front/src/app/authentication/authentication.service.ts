import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { DecodedToken } from '../dto/decoded.token';

@Injectable({
  providedIn: 'root',
})
export class AuthenticationService {
  isLoggedin = new BehaviorSubject<boolean>(this.existsToken());
  userEmail = new BehaviorSubject<string>(this.getUserEmail());
  isAdmin = new BehaviorSubject<boolean>(this.getIsAdmin());
  userId = new BehaviorSubject<string | null>(this.getUserId());
  isPerfil = new BehaviorSubject<boolean>(this.getIsPerfil());
  avatarUrl = new BehaviorSubject<string>('');

  constructor() {}

  saveToken(token: string) {
    console.log('Saving token:', token);
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem('jwt_token', token);
      this.isLoggedin.next(true);
      this.userEmail.next(this.getUserEmail());
      this.isAdmin.next(this.getIsAdmin());
      this.userId.next(this.getUserId());
      this.isPerfil.next(this.getIsPerfil());
    }
  }

  getToken(): string {
    if (typeof localStorage === 'undefined') {
      return '';
    }
    return localStorage.getItem('jwt_token') || '';
  }


  existsToken(): boolean {
    if (typeof localStorage === 'undefined') {
      return false;
    }
    return !!localStorage.getItem('jwt_token');
  }

  removeToken() {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem('jwt_token');
      this.isLoggedin.next(false);
      this.userEmail.next('');
      this.isAdmin.next(false);
      this.userId.next(null);
      this.isPerfil.next(false);
    }
  }

  getUserEmail(): string {
    const decodedToken = this.decodeToken();
    return decodedToken?.email ?? '';
  }

  getIsAdmin(): boolean {
    const decodedToken = this.decodeToken();
    return decodedToken?.rolname === 'ADMIN';
  }

  getUserId(): string | null {
    const decodedToken = this.decodeToken();
    return decodedToken?.sub ?? null;
  }

  getIsPerfil(): boolean {
    const decodedToken = this.decodeToken();
    return decodedToken?.rolname === 'PERFIL';
  }

  setUserAvatar(avatar: string) {
    this.avatarUrl.next(avatar);
  }

  private decodeToken(): DecodedToken | null {
    if (typeof localStorage === 'undefined') {
      return null;
    }

    const token = localStorage.getItem('jwt_token');
    if (!token) {
      return null;
    }

    try {
      const payloadPart = token.split('.')[1];
      if (!payloadPart) {
        return null;
      }

      const normalizedBase64 = payloadPart
        .replace(/-/g, '+')
        .replace(/_/g, '/');
      const paddedBase64 = normalizedBase64.padEnd(
        normalizedBase64.length + ((4 - (normalizedBase64.length % 4)) % 4),
        '='
      );

      if (typeof atob !== 'function') {
        return null;
      }

      return JSON.parse(atob(paddedBase64)) as DecodedToken;
    } catch {
      return null;
    }
  }
}

