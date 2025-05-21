import { CalendarDate, CalendarDateTime, Time, parseDate, parseDateTime, parseTime } from '@internationalized/date';

export class Dates {
  static toCalendarDateTime(value: string | Date): CalendarDateTime {
    if (typeof value === 'string') {
      return parseDateTime(value);
    }
    return new CalendarDateTime(value.getFullYear(), value.getMonth() + 1, value.getDate(), value.getHours(), value.getMinutes(), value.getSeconds());
  }

  static toCalendarDateTimeOrNull(value: string | Date | null): CalendarDateTime | null {
    if (!value) {
      return null;
    }
    return Dates.toCalendarDateTime(value);
  }

  static toCalendarDate(value: string | Date): CalendarDate {
    if (typeof value === 'string') {
      return parseDate(value);
    }
    return new CalendarDate(value.getFullYear(), value.getMonth() + 1, value.getDate());
  }

  static toCalendarDateOrNull(value: string | Date | null): CalendarDate | null {
    if (!value) {
      return null;
    }
    return Dates.toCalendarDate(value);
  }

  static toTime(value: string | Date): Time | null {
    if (typeof value === 'string') {
      return parseTime(value);
    }
    return new Time(value.getHours(), value.getMinutes(), value.getSeconds());
  }

  static toTimeOrNull(value: string | null): Time | null {
    if (!value) {
      return null;
    }
    return Dates.toTime(value);
  }

  static daysAgoAtMidnight(days: number): Date {
    const result = new Date();
    result.setDate(result.getDate() - days);
    result.setHours(0, 0, 0, 0);
    return result;
  }
}
