import {CodeInput, QueueCodeResponse, tester} from "./tester";

async function main(): Promise<void> {
    console.log('Queuing code to be executed');
    const codeInput: CodeInput = tester.readCodeInput('queue-code.json');

    const executionJob: QueueCodeResponse = await tester.queueCode(codeInput);
    if (!executionJob) {
        console.error('Queueing job failed.');
        return
    }

    const jobId: string = executionJob.data.id;
    if (!jobId) {
        console.error('Queueing job failed. Job ID not found.');
        return
    }

    console.log('Queued code successfully. Job ID:', jobId);
    const state = await tester.pollQueuedCodeJob(jobId);
    console.log('Queued code job state:', state);

    await tester.awaitQueuedCodeJob(jobId, 'ACTIVE');
    await tester.cancelQueuedCodeJob(jobId);
    await tester.awaitQueuedCodeJob(jobId, 'CANCELLED');
}

main();
