import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';

/**
 * Service to manage loading states across the application.
 * Tracks active HTTP requests and provides loading indicators.
 */
@Injectable({
  providedIn: 'root'
})
export class LoadingService {
  private loadingSubject = new BehaviorSubject<Set<string>>(new Set());
  private loadingMap = new Map<string, number>();

  /**
   * Observable that emits true when any loading operation is active
   */
  public readonly isLoading$: Observable<boolean> = this.loadingSubject.pipe(
    map(loadingSet => loadingSet.size > 0),
    distinctUntilChanged()
  );

  /**
   * Gets loading state for a specific key
   * @param key The loading key to check
   * @returns Observable that emits true when the specific operation is loading
   */
  public isLoadingKey$(key: string): Observable<boolean> {
    return this.loadingSubject.pipe(
      map(loadingSet => loadingSet.has(key)),
      distinctUntilChanged()
    );
  }

  /**
   * Starts a loading operation
   * @param key Unique key for the loading operation
   */
  public startLoading(key: string = 'global'): void {
    const count = this.loadingMap.get(key) || 0;
    this.loadingMap.set(key, count + 1);
    
    const currentSet = new Set(this.loadingSubject.value);
    currentSet.add(key);
    this.loadingSubject.next(currentSet);
  }

  /**
   * Stops a loading operation
   * @param key Unique key for the loading operation
   */
  public stopLoading(key: string = 'global'): void {
    const count = this.loadingMap.get(key) || 0;
    
    if (count > 1) {
      this.loadingMap.set(key, count - 1);
    } else {
      this.loadingMap.delete(key);
      const currentSet = new Set(this.loadingSubject.value);
      currentSet.delete(key);
      this.loadingSubject.next(currentSet);
    }
  }

  /**
   * Stops all loading operations
   */
  public stopAllLoading(): void {
    this.loadingMap.clear();
    this.loadingSubject.next(new Set());
  }

  /**
   * Gets the current loading state
   * @returns true if any loading operation is active
   */
  public get isLoading(): boolean {
    return this.loadingSubject.value.size > 0;
  }

  /**
   * Gets all active loading keys
   * @returns Array of active loading keys
   */
  public get activeLoadingKeys(): string[] {
    return Array.from(this.loadingSubject.value);
  }
}