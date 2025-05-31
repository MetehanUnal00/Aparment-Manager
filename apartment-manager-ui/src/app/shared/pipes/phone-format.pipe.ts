import { Pipe, PipeTransform } from '@angular/core';

/**
 * Standalone pipe for formatting phone numbers
 * Usage: {{ phoneNumber | phoneFormat:'US' }}
 */
@Pipe({
  name: 'phoneFormat',
  standalone: true
})
export class PhoneFormatPipe implements PipeTransform {
  /**
   * Transform phone number to formatted string
   * @param value The phone number to format
   * @param country Country code for formatting (default: 'US')
   * @returns Formatted phone number string
   */
  transform(value: string | null | undefined, country: string = 'US'): string {
    if (!value) {
      return '';
    }

    // Remove all non-numeric characters
    const cleaned = value.replace(/\D/g, '');

    // Format based on country
    switch (country) {
      case 'US':
        return this.formatUS(cleaned);
      case 'UK':
        return this.formatUK(cleaned);
      case 'TR':
        return this.formatTR(cleaned);
      default:
        return this.formatInternational(cleaned);
    }
  }

  /**
   * Format US phone number (XXX) XXX-XXXX
   */
  private formatUS(phone: string): string {
    if (phone.length !== 10) {
      return phone;
    }
    return `(${phone.slice(0, 3)}) ${phone.slice(3, 6)}-${phone.slice(6)}`;
  }

  /**
   * Format UK phone number XXXXX XXXXXX
   */
  private formatUK(phone: string): string {
    if (phone.length !== 11) {
      return phone;
    }
    return `${phone.slice(0, 5)} ${phone.slice(5)}`;
  }

  /**
   * Format Turkish phone number 0XXX XXX XX XX
   */
  private formatTR(phone: string): string {
    if (phone.length !== 11 && phone.length !== 10) {
      return phone;
    }
    
    // Add leading 0 if not present
    const normalized = phone.length === 10 ? '0' + phone : phone;
    return `${normalized.slice(0, 4)} ${normalized.slice(4, 7)} ${normalized.slice(7, 9)} ${normalized.slice(9)}`;
  }

  /**
   * Format international phone number with spaces every 3-4 digits
   */
  private formatInternational(phone: string): string {
    // Format as groups of 3-4 digits
    const groups = [];
    let remaining = phone;
    
    while (remaining.length > 0) {
      if (remaining.length <= 4) {
        groups.push(remaining);
        break;
      }
      groups.push(remaining.slice(0, 3));
      remaining = remaining.slice(3);
    }
    
    return groups.join(' ');
  }
}