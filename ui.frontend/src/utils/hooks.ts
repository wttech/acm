import {useDateFormatter} from "@react-aria/i18n";
import {formatDistance, formatDuration, intervalToDuration} from "date-fns";

class Formatter {
    dateFormatter: Intl.DateTimeFormat;

    constructor() {
        this.dateFormatter = useDateFormatter({dateStyle: 'long', timeStyle: 'short'});
    }

    public date(value: string | Date) {
        if (value instanceof Date) {
            return this.dateFormatter.format(value);
        }
        return this.dateFormatter.format(new Date(value));
    }

    public duration(milliseconds: number): string {
        const duration = intervalToDuration({start: 0, end: milliseconds});
        let result = formatDuration(duration, {format: ['days', 'hours', 'minutes', 'seconds']});
        if (!result) {
            result = `${milliseconds} milliseconds`;
        }
        return result;
    }

    public durationExplained(milliseconds: number): string {
        const duration = intervalToDuration({start: 0, end: milliseconds});
        let result = `${milliseconds} ms`
        const formatted = formatDuration(duration, {format: ['days', 'hours', 'minutes', 'seconds']});
        if (formatted) {
            result += ` (${formatted})`;
        }
        return result
    }

    public dateRelative(value: string): string {
        return formatDistance(value, new Date(), {addSuffix: true, includeSeconds: true});
    }

    public dateExplained(value: string): string {
        return `${this.date(value)} (${this.dateRelative(value)})`
    }
}

export function useFormatter() {
    return new Formatter();
}