import { Injectable, PLATFORM_ID, Inject } from '@angular/core'; // Import PLATFORM_ID and Inject
import { isPlatformBrowser } from '@angular/common'; // Import isPlatformBrowser
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';

// Define interfaces for request and response types for better type safety
interface LoginRequest {
  username?: string; // Made optional for form binding
  password?: string; // Made optional for form binding
}

interface SignupRequest {
  username?: string;
  email?: string;
  password?: string;
  firstName?: string;
  lastName?: string;
}

interface JwtResponse {
  token: string;
  type: string;
  id: number;
  username: string;
  email: string;
  roles: string[];
}

interface MessageResponse {
  message: string;
}

const AUTH_API = 'http://localhost:8080/api/auth/'; // Your backend API URL

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable({
  providedIn: 'root' // Provided in root, available application-wide
})
export class AuthService {
  private currentUserSubject: BehaviorSubject<JwtResponse | null>;
  public currentUser: Observable<JwtResponse | null>;
  private readonly TOKEN_KEY = 'auth-token';
  private readonly USER_KEY = 'auth-user';
  private isBrowser: boolean;

  constructor(
    private http: HttpClient,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object // Inject PLATFORM_ID
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId); // Check if running in browser
    const user = this.getUser();
    this.currentUserSubject = new BehaviorSubject<JwtResponse | null>(user);
    this.currentUser = this.currentUserSubject.asObservable();
  }

  public get currentUserValue(): JwtResponse | null {
    return this.currentUserSubject.value;
  }

  login(credentials: LoginRequest): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(AUTH_API + 'login', credentials, httpOptions).pipe(
      tap(response => {
        this.saveToken(response.token);
        this.saveUser(response); // Save the whole JwtResponse object
        this.currentUserSubject.next(response);
      })
    );
  }

  register(user: SignupRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(AUTH_API + 'register', user, httpOptions);
  }

  logout(): void {
    if (this.isBrowser) { // Check before accessing localStorage
      window.localStorage.removeItem(this.TOKEN_KEY);
      window.localStorage.removeItem(this.USER_KEY);
    }
    this.currentUserSubject.next(null);
    this.router.navigate(['/auth/login']);
  }

  public saveToken(token: string): void {
    if (this.isBrowser) { // Check before accessing localStorage
      window.localStorage.removeItem(this.TOKEN_KEY);
      window.localStorage.setItem(this.TOKEN_KEY, token);
    }
  }

  public getToken(): string | null {
    if (this.isBrowser) { // Check before accessing localStorage
      return window.localStorage.getItem(this.TOKEN_KEY);
    }
    return null;
  }

  public saveUser(user: JwtResponse): void {
    if (this.isBrowser) { // Check before accessing localStorage
      window.localStorage.removeItem(this.USER_KEY);
      window.localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    }
  }

  public getUser(): JwtResponse | null {
    if (this.isBrowser) { // Check before accessing localStorage
      const user = window.localStorage.getItem(this.USER_KEY);
      if (user) {
        return JSON.parse(user);
      }
    }
    return null;
  }

  public isLoggedIn(): boolean {
    return !!this.getToken();
  }
}
