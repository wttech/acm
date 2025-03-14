import { useDateFormatter } from '@react-aria/i18n';
import { formatDistance, formatDuration, intervalToDuration } from 'date-fns';
import { toZonedTime } from 'date-fns-tz';
import {useContext} from "react";
import {AppContext} from "../../AppContext.tsx";
import {DateFormatter} from "@adobe/react-spectrum";

class Formatter {
  instanceTimezoneId: string;
  instanceDateFormatter: DateFormatter;
  userTimeZoneId: string;
  userDateFormatter: DateFormatter;

  constructor(timezoneId: string) {
    this.instanceTimezoneId = timezoneId;
    this.instanceDateFormatter = useDateFormatter({
      dateStyle: 'long',
      timeStyle: 'short',
      timeZone: this.instanceTimezoneId,
    });
    this.userTimeZoneId = Intl.DateTimeFormat().resolvedOptions().timeZone;
    this.userDateFormatter = useDateFormatter({
      dateStyle: 'long',
      timeStyle: 'short',
      timeZone: this.userTimeZoneId,
    });
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

  public dateExplained(value: string): string {
    return `${this.dateAtInstance(value)} (${this.dateRelative(value)})`;
  }

  public instanceTimezone(): string {
    return this.instanceTimezoneId;
  }

  public userTimezone(): string {
      return this.userTimeZoneId;
  }
}

export function useFormatter() {
  const context = useContext(AppContext)!;
  const instanceTimezoneId =  context.instanceSettings.timezoneId;

  return new Formatter(instanceTimezoneId);
}