import {formatDate, formatDistance, formatDuration, intervalToDuration} from "date-fns";

export class Strings {
    static capitalize(text: string): string {
        return text.charAt(0).toUpperCase() + text.slice(1).toLowerCase();
    }

    static duration(milliseconds: number): string {
        const duration = intervalToDuration({start: 0, end: milliseconds});
        let result = formatDuration(duration, {format: ['days', 'hours', 'minutes', 'seconds']});
        if (!result) {
            result = `${milliseconds} milliseconds`;
        }
        return result;
    }

    static durationExplained(milliseconds: number): string {
        const duration = intervalToDuration({start: 0, end: milliseconds});
        let result = `${milliseconds} ms`
        const formatted = formatDuration(duration, {format: ['days', 'hours', 'minutes', 'seconds']});
        if (formatted) {
            result += ` (${formatted})`;
        }
        return result
    }

    static date(value: string): string {
        return formatDate(value, 'yyyy-MM-dd HH:mm:ss');
    }

    static dateRelative(value: string): string {
        return formatDistance(value, new Date(), {addSuffix: true, includeSeconds: true});
    }
}
