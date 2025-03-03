import { useDateFormatter } from '@react-aria/i18n';
import { formatDistance, formatDuration, intervalToDuration } from 'date-fns';
import { toZonedTime } from 'date-fns-tz';
import {useContext} from "react";
import {AppContext} from "../AppContext.tsx";

class Formatter {
  dateFormatter: Intl.DateTimeFormat;
  instanceTimezoneId: string;
  userTimeZoneId: string;

  constructor(timezoneId: string) {
    this.instanceTimezoneId = timezoneId;
    this.userTimeZoneId = Intl.DateTimeFormat().resolvedOptions().timeZone;
    this.dateFormatter = useDateFormatter({
      dateStyle: 'long',
      timeStyle: 'short',
      timeZone: timezoneId,
    });
  }

  public isTimezoneDifference(): boolean {
    return this.instanceTimezoneId !== this.userTimeZoneId;
  }

  public dateAtInstance(value: string | Date) {
    const date = value instanceof Date ? value : new Date(value);
    const zonedDate = toZonedTime(date, this.instanceTimezoneId);
    return this.dateFormatter.format(zonedDate);
  }

  public dateAtUser(value: string | Date) {
    const date = value instanceof Date ? value : new Date(value);
    const zonedDate = toZonedTime(date, this.userTimeZoneId);
    return this.dateFormatter.format(zonedDate);
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

  public dateRelative(value: string): string {
    return formatDistance(value, new Date(), {
      addSuffix: true,
      includeSeconds: true,
    });
  }

  public dateExplained(value: string): string {
    return `${this.dateAtInstance(value)} (${this.dateRelative(value)})`;
  }
}

export function useFormatter() {
  const context = useContext(AppContext)!;
  const instanceTimezoneId =  context.instanceSettings.timezoneId;

  return new Formatter(instanceTimezoneId);
}