import {CodeInput, QueueCodeResponse, tester} from "./tester";

async function main(): Promise<void> {
    console.log('Queuing code to be executed');
    const codeInput: CodeInput = tester.readCodeInput('queue-code.json');
    const executionJob: QueueCodeResponse = await tester.queueCode(codeInput);
    if (executionJob) {
        const jobId: string = executionJob.data.id;
        if (jobId) {
            console.log('Queued code successfully. Job ID:', jobId);
            await tester.awaitQueuedCodeJobSucceeded(jobId);
            console.log('Queued code executed successfully');
        } else {
            console.error('Queueing job failed. Job ID not found.');
        }
    } else {
        console.error('Queueing job failed.');
    }
}

main();
