const fs = require('fs');
const axios = require('axios');

const host = 'http://localhost:4502';
const codeInputPath = 'queue-code.json';
const queueApiUrl = `${host}/apps/migrator/api/queue-code.json`;
const pollInterval = 1000;

function readCodeInput(filePath) {
    console.log(`Reading job details from ${filePath}`);
    const data = fs.readFileSync(filePath, 'utf8');
    return JSON.parse(data);
}

async function queueCode(jobDetails) {
    console.log('Submitting job via POST request');
    try {
        const response = await axios.post(queueApiUrl, jobDetails, {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'basic YWRtaW46YWRtaW4='
            }
        });
        return response.data;
    } catch (error) {
        console.error('Error submitting job:', error);
        return null;
    }
}

async function pollExecutionJobState(jobId) {
    console.log(`Polling queued code execution by job ID: ${jobId}`);
    try {
        const response = await axios.get(`${queueApiUrl}?jobId=${jobId}`, {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'basic YWRtaW46YWRtaW4='
            }
        });
        console.log(`Polled queued code execution response by job ID '${jobId}`, response.data);
        return response.data.data.state;
    } catch (error) {
        console.error(`Polling queued code execution failed by job ID '${jobId}'`, error);
        return null;
    }
}

async function waitQueuedCodeToBeExecuted(jobId) {
    console.log(`Waiting for job ${jobId} to succeed`);
    let state = null;
    while (state !== 'SUCCEEDED') {
        state = await pollExecutionJobState(jobId);
        if (state === 'SUCCEEDED') {
            console.log(`Job ${jobId} succeeded.`);
            break;
        } else {
            console.log(`Job ${jobId} is in state ${state}. Retrying in ${pollInterval} ms...`);
            await new Promise(resolve => setTimeout(resolve, pollInterval));
        }
    }
}

async function main() {
    console.log('Queuing code to be executed');
    const codeInput = readCodeInput(codeInputPath);
    const executionJob = await queueCode(codeInput);
    if (executionJob) {
        const jobId = executionJob.data.id;
        if (jobId) {
            console.log(`Job ID found: ${jobId}`);
            await waitQueuedCodeToBeExecuted(jobId);
            console.log('Queued code executed successfully');
        } else {
            console.error('Job ID not found!');
        }
    } else {
        console.error('Queueing job failed.');
    }
}

main();
