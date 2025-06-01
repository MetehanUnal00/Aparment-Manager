import { Directive, ElementRef, HostListener, Input, OnInit, Renderer2 } from '@angular/core';

/**
 * Directive to make tables responsive by converting them to card layouts on mobile devices
 * Usage: <table appResponsiveTable [breakpoint]="768">
 */
@Directive({
  selector: '[appResponsiveTable]',
  standalone: true
})
export class ResponsiveTableDirective implements OnInit {
  // Breakpoint in pixels below which the table becomes cards
  @Input() breakpoint: number = 768;
  
  // Priority columns that should always be visible (comma-separated indices)
  @Input() priorityColumns: string = '';
  
  private isCardView = false;
  private priorityIndices: number[] = [];

  constructor(
    private el: ElementRef,
    private renderer: Renderer2
  ) {}

  ngOnInit() {
    // Parse priority columns
    if (this.priorityColumns) {
      this.priorityIndices = this.priorityColumns
        .split(',')
        .map(col => parseInt(col.trim(), 10))
        .filter(col => !isNaN(col));
    }

    // Add responsive wrapper if not already present
    const table = this.el.nativeElement as HTMLTableElement;
    if (!table.parentElement?.classList.contains('table-responsive')) {
      const wrapper = this.renderer.createElement('div');
      this.renderer.addClass(wrapper, 'table-responsive');
      this.renderer.insertBefore(table.parentElement, wrapper, table);
      this.renderer.appendChild(wrapper, table);
    }

    // Initial check
    this.checkViewport();
  }

  @HostListener('window:resize')
  onResize() {
    this.checkViewport();
  }

  private checkViewport() {
    const width = window.innerWidth;
    const shouldBeCardView = width < this.breakpoint;

    if (shouldBeCardView !== this.isCardView) {
      this.isCardView = shouldBeCardView;
      if (this.isCardView) {
        this.convertToCards();
      } else {
        this.convertToTable();
      }
    }
  }

  private convertToCards() {
    const table = this.el.nativeElement as HTMLTableElement;
    const thead = table.querySelector('thead');
    const tbody = table.querySelector('tbody');
    
    if (!thead || !tbody) return;

    // Get header labels
    const headers = Array.from(thead.querySelectorAll('th')).map(th => th.textContent?.trim() || '');
    
    // Add mobile card class to table
    this.renderer.addClass(table, 'mobile-cards');
    
    // Hide thead on mobile
    this.renderer.setStyle(thead, 'display', 'none');
    
    // Convert each row to a card
    const rows = tbody.querySelectorAll('tr');
    rows.forEach(row => {
      this.renderer.addClass(row, 'mobile-card');
      
      const cells = row.querySelectorAll('td');
      cells.forEach((cell, index) => {
        // Skip if not a priority column and we have priority columns defined
        if (this.priorityIndices.length > 0 && !this.priorityIndices.includes(index)) {
          this.renderer.setStyle(cell, 'display', 'none');
          return;
        }
        
        // Add data label for mobile
        const label = headers[index];
        if (label && !cell.querySelector('.mobile-label')) {
          const labelSpan = this.renderer.createElement('span');
          this.renderer.addClass(labelSpan, 'mobile-label');
          this.renderer.appendChild(labelSpan, this.renderer.createText(label + ': '));
          this.renderer.insertBefore(cell, labelSpan, cell.firstChild);
        }
        
        this.renderer.addClass(cell, 'mobile-cell');
      });
    });
  }

  private convertToTable() {
    const table = this.el.nativeElement as HTMLTableElement;
    const thead = table.querySelector('thead');
    const tbody = table.querySelector('tbody');
    
    if (!thead || !tbody) return;

    // Remove mobile classes
    this.renderer.removeClass(table, 'mobile-cards');
    
    // Show thead
    this.renderer.removeStyle(thead, 'display');
    
    // Convert cards back to table rows
    const rows = tbody.querySelectorAll('tr');
    rows.forEach(row => {
      this.renderer.removeClass(row, 'mobile-card');
      
      const cells = row.querySelectorAll('td');
      cells.forEach(cell => {
        // Show all cells
        this.renderer.removeStyle(cell, 'display');
        
        // Remove mobile label
        const label = cell.querySelector('.mobile-label');
        if (label) {
          this.renderer.removeChild(cell, label);
        }
        
        this.renderer.removeClass(cell, 'mobile-cell');
      });
    });
  }
}