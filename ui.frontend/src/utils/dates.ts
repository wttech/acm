import { CalendarDateTime } from '@internationalized/date';

export class Dates {
  static toCalendar(value: string | Date): CalendarDateTime {
    const date = value instanceof Date ? value : new Date(value);
    return new CalendarDateTime(date.getFullYear(), date.getMonth() + 1, date.getDate(), date.getHours(), date.getMinutes(), date.getSeconds());
  }

  static toCalendarOrNull(value: string | Date | null): CalendarDateTime | null {
    if (!value) {
      return null;
    }
    return Dates.toCalendar(value);
  }

  static daysAgo(days: number): Date {
    const result = new Date();
    result.setDate(result.getDate() - days);
    return result;
  }
}
