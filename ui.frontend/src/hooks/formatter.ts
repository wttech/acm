import { DateFormatter } from '@adobe/react-spectrum';
import { useDateFormatter } from '@react-aria/i18n';
import { formatDistance, formatDuration, intervalToDuration } from 'date-fns';
import { toZonedTime } from 'date-fns-tz';
import { useContext } from 'react';
import { AppContext } from '../AppContext';

export function useFormatter() {
  const context = useContext(AppContext)!;

  const instanceTimezoneId = context.instanceSettings.timezoneId;
  const instanceDateFormatter = useDateFormatter({
    dateStyle: 'long',
    timeStyle: 'short',
    timeZone: instanceTimezoneId,
  });

  const userTimezoneId = Intl.DateTimeFormat().resolvedOptions().timeZone;
  const userDateFormatter = useDateFormatter({
    dateStyle: 'long',
    timeStyle: 'short',
    timeZone: userTimezoneId,
  });

  return new Formatter(instanceTimezoneId, instanceDateFormatter, userTimezoneId, userDateFormatter);
}

class Formatter {
  instanceTimezoneId: string;
  instanceDateFormatter: DateFormatter;
  userTimeZoneId: string;
  userDateFormatter: DateFormatter;

  constructor(timezoneId: string, instanceDateFormatter: DateFormatter, userTimeZoneId: string, userDateFormatter: DateFormatter) {
    this.instanceTimezoneId = timezoneId;
    this.instanceDateFormatter = instanceDateFormatter;
    this.userTimeZoneId = userTimeZoneId;
    this.userDateFormatter = userDateFormatter;
  }

  public isTimezoneDifference(): boolean {
    return this.instanceTimezoneId !== this.userTimeZoneId;
  }

  public dateAtInstance(value: string | Date) {
    const date = value instanceof Date ? value : new Date(value);
    return this.instanceDateFormatter.format(date);
  }

  public dateAtUser(value: string | Date) {
    const date = value instanceof Date ? value : new Date(value);
    return this.userDateFormatter.format(date);
  }

  public duration(milliseconds: number): string {
    const duration = intervalToDuration({ start: 0, end: milliseconds });
    let result = formatDuration(duration, {
      format: ['days', 'hours', 'minutes', 'seconds'],
    });
    if (!result) {
      result = `${milliseconds} milliseconds`;
    }
    return result;
  }

  public durationShort(milliseconds: number): string {
    if (milliseconds < 1000) {
      return `${milliseconds} ms`;
    }
    const duration = intervalToDuration({ start: 0, end: milliseconds });
    const parts = [];
    if (duration.days) parts.push(`${duration.days}d`);
    if (duration.hours) parts.push(`${duration.hours}h`);
    if (duration.minutes) parts.push(`${duration.minutes}m`);
    if (duration.seconds) parts.push(`${duration.seconds}s`);
    return parts.join(' ');
  }

  public durationExplained(milliseconds: number): string {
    const duration = intervalToDuration({ start: 0, end: milliseconds });
    let result = `${milliseconds} ms`;
    const formatted = formatDuration(duration, {
      format: ['days', 'hours', 'minutes', 'seconds'],
    });
    if (formatted) {
      result += ` (${formatted})`;
    }
    return result;
  }

  public dateRelative(value: string | Date): string {
    const date = value instanceof Date ? value : new Date(value);
    const zonedDate = toZonedTime(date, this.userTimeZoneId);
    const now = toZonedTime(new Date(), this.userTimeZoneId);

    return formatDistance(zonedDate, now, {
      addSuffix: true,
      includeSeconds: true,
    });
  }

  public dateExplained(value: string | Date): string {
    if (!value) {
      return '';
    }

    return `${this.dateAtInstance(value)} (${this.dateRelative(value)})`;
  }

  public isRecent(value: string | Date, timeFrameMs: number): boolean {
    if (!value) {
      return false;
    }

    const now = new Date();
    const targetDate = toZonedTime(new Date(value), this.userTimeZoneId);
    const timeDifference = now.getTime() - targetDate.getTime();
    return timeDifference <= timeFrameMs;
  }

  public instanceTimezone(): string {
    return this.instanceTimezoneId;
  }

  public userTimezone(): string {
    return this.userTimeZoneId;
  }
}
