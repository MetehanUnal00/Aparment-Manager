import { Pipe, PipeTransform } from '@angular/core';

/**
 * Standalone pipe for formatting currency values
 * Usage: {{ amount | currencyFormat:'USD':'symbol':'1.2-2' }}
 */
@Pipe({
  name: 'currencyFormat',
  standalone: true
})
export class CurrencyFormatPipe implements PipeTransform {
  /**
   * Transform number to formatted currency string
   * @param value The numeric value to format
   * @param currencyCode Currency code (default: 'USD')
   * @param display How to display the currency (default: 'symbol')
   * @param digitsInfo Decimal places format (default: '1.2-2')
   * @returns Formatted currency string
   */
  transform(
    value: number | string | null | undefined,
    currencyCode: string = 'USD',
    display: 'code' | 'symbol' | 'narrowSymbol' = 'symbol',
    digitsInfo: string = '1.2-2'
  ): string {
    if (value === null || value === undefined || value === '') {
      return '';
    }

    const numValue = typeof value === 'string' ? parseFloat(value) : value;
    
    if (isNaN(numValue)) {
      return '';
    }

    // Parse digitsInfo format (minIntegerDigits.minFractionDigits-maxFractionDigits)
    const parts = digitsInfo.split('.');
    const fractionParts = parts[1]?.split('-') || ['2', '2'];
    const minFractionDigits = parseInt(fractionParts[0], 10) || 2;
    const maxFractionDigits = parseInt(fractionParts[1], 10) || 2;

    try {
      // Map our display values to valid Intl.NumberFormat options
      const currencyDisplayMap: { [key: string]: 'code' | 'symbol' | 'narrowSymbol' | 'name' } = {
        'code': 'code',
        'symbol': 'symbol',
        'narrowSymbol': 'narrowSymbol'
      };

      const formatter = new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: currencyCode,
        currencyDisplay: currencyDisplayMap[display] || 'symbol',
        minimumFractionDigits: minFractionDigits,
        maximumFractionDigits: maxFractionDigits
      });

      return formatter.format(numValue);
    } catch (error) {
      console.error('Currency formatting error:', error);
      return numValue.toString();
    }
  }
}