import { Directive, ElementRef, Input, OnInit } from '@angular/core';

/**
 * Standalone directive to automatically focus an element when it's rendered
 * Usage: <input appAutofocus />
 * Or with delay: <input [appAutofocus]="500" />
 */
@Directive({
  selector: '[appAutofocus]',
  standalone: true
})
export class AutofocusDirective implements OnInit {
  /**
   * Optional delay in milliseconds before focusing
   */
  @Input() appAutofocus: number | '' = '';

  constructor(private elementRef: ElementRef<HTMLElement>) {}

  ngOnInit(): void {
    const delay = typeof this.appAutofocus === 'number' ? this.appAutofocus : 0;
    
    setTimeout(() => {
      this.elementRef.nativeElement.focus();
    }, delay);
  }
}