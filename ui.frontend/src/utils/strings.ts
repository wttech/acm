import {formatDate, formatDistance, formatDuration, intervalToDuration} from "date-fns";

export class Strings {
    static capitalize(text: string): string {
        return text.charAt(0).toUpperCase() + text.slice(1).toLowerCase();
    }

    static duration(milliseconds: number): string {
        const duration = intervalToDuration({start: 0, end: milliseconds});
        return formatDuration(duration);
    }

    static date(value: string): string {
        return formatDate(value, 'yyyy-MM-dd HH:mm:ss');
    }

    static dateRelative(value: string): string {
        return formatDistance(value, new Date(), {addSuffix: true, includeSeconds: true});
    }
}
