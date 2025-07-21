/**
 * Prints output to both the console and the log files.
 * Useful for troubleshooting scripts on publish instances, where no GUI or repository browser is available.
 * In such cases, logs are the only way to view script results.
 *
 * @author Krystian Panek <krystian.panek@vml.com>
 */

boolean canRun() {
    return condition.always()
}

void doRun() {
    out.fromLogs()
    log.info "Hello Console and Logs!"
}