import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';

/**
 * Notification types for different severity levels
 */
export enum NotificationType {
  SUCCESS = 'success',
  ERROR = 'error',
  WARNING = 'warning',
  INFO = 'info'
}

/**
 * Notification model
 */
export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  message?: string;
  duration?: number;
  dismissible?: boolean;
  timestamp: Date;
}

/**
 * Service to manage user notifications across the application.
 * Provides methods to show success, error, warning, and info messages.
 */
@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notificationSubject = new Subject<Notification>();
  private removeNotificationSubject = new Subject<string>();
  
  /**
   * Observable for new notifications
   */
  public readonly notifications$: Observable<Notification> = this.notificationSubject.asObservable();
  
  /**
   * Observable for notification removal
   */
  public readonly removeNotification$: Observable<string> = this.removeNotificationSubject.asObservable();

  /**
   * Default notification duration in milliseconds
   */
  private readonly defaultDuration = 5000;

  /**
   * Shows a success notification
   * @param title The notification title
   * @param message Optional message details
   * @param duration Optional duration in milliseconds
   */
  public success(title: string, message?: string, duration?: number): void {
    this.show({
      type: NotificationType.SUCCESS,
      title,
      message,
      duration: duration || this.defaultDuration
    });
  }

  /**
   * Shows an error notification
   * @param title The notification title
   * @param message Optional message details
   * @param dismissible Whether the notification can be manually dismissed (default: true)
   */
  public error(title: string, message?: string, dismissible: boolean = true): void {
    this.show({
      type: NotificationType.ERROR,
      title,
      message,
      duration: 0, // Errors don't auto-dismiss by default
      dismissible
    });
  }

  /**
   * Shows a warning notification
   * @param title The notification title
   * @param message Optional message details
   * @param duration Optional duration in milliseconds
   */
  public warning(title: string, message?: string, duration?: number): void {
    this.show({
      type: NotificationType.WARNING,
      title,
      message,
      duration: duration || this.defaultDuration
    });
  }

  /**
   * Shows an info notification
   * @param title The notification title
   * @param message Optional message details
   * @param duration Optional duration in milliseconds
   */
  public info(title: string, message?: string, duration?: number): void {
    this.show({
      type: NotificationType.INFO,
      title,
      message,
      duration: duration || this.defaultDuration
    });
  }

  /**
   * Shows a custom notification
   * @param config Partial notification configuration
   */
  public show(config: Partial<Notification>): void {
    const notification: Notification = {
      id: this.generateId(),
      type: config.type || NotificationType.INFO,
      title: config.title || '',
      message: config.message,
      duration: config.duration !== undefined ? config.duration : this.defaultDuration,
      dismissible: config.dismissible !== undefined ? config.dismissible : true,
      timestamp: new Date()
    };

    this.notificationSubject.next(notification);

    // Auto-dismiss if duration is set
    if (notification.duration && notification.duration > 0) {
      setTimeout(() => {
        this.dismiss(notification.id);
      }, notification.duration);
    }
  }

  /**
   * Dismisses a notification
   * @param id The notification ID to dismiss
   */
  public dismiss(id: string): void {
    this.removeNotificationSubject.next(id);
  }

  /**
   * Dismisses all notifications
   */
  public dismissAll(): void {
    this.removeNotificationSubject.next('*');
  }

  /**
   * Generates a unique notification ID
   * @returns Unique ID string
   */
  private generateId(): string {
    return `notification-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }
}