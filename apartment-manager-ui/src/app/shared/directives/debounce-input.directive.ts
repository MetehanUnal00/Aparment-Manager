import { Directive, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { fromEvent, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';

/**
 * Standalone directive to debounce input events
 * Usage: <input appDebounceInput (debounceInput)="onSearch($event)" [debounceTime]="300" />
 */
@Directive({
  selector: '[appDebounceInput]',
  standalone: true
})
export class DebounceInputDirective implements OnInit, OnDestroy {
  /**
   * Debounce time in milliseconds
   */
  @Input() debounceTime = 300;
  
  /**
   * Event emitted after debounce
   */
  @Output() debounceInput = new EventEmitter<string>();
  
  private destroy$ = new Subject<void>();

  constructor(private elementRef: ElementRef<HTMLInputElement>) {}

  ngOnInit(): void {
    fromEvent(this.elementRef.nativeElement, 'input')
      .pipe(
        debounceTime(this.debounceTime),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe((event: Event) => {
        const value = (event.target as HTMLInputElement).value;
        this.debounceInput.emit(value);
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}