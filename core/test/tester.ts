import fs from 'fs';
import axios from 'axios';

export interface CodeInput {
    id: string;
    code: string;
}

export interface QueueCodeResponse {
    data: {
        id: string;
        state: string;
    }
}

const instanceBaseUrl: string = 'http://localhost:4502';
const queueApiUrl: string = `${instanceBaseUrl}/apps/migrator/api/queue-code.json`;
const queuePollInterval: number = 1000;

export class Tester {

    readCodeInput(filePath: string): CodeInput {
        console.log(`Reading code input from '${filePath}'`);
        const data: string = fs.readFileSync(filePath, 'utf8');
        console.log('Read code input:', data);
        return JSON.parse(data);
    }

    async queueCode(codeInput: CodeInput): Promise<QueueCodeResponse> {
        console.log('Queueing code execution', codeInput);
        try {
            const response = await axios.post(queueApiUrl, codeInput, {
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'basic YWRtaW46YWRtaW4='
                }
            });
            console.log('Queued code execution response:', response.data);
            return response.data;
        } catch (error) {
            console.error('Queueing code execution failed!', error);
            throw new Error(`Queueing code execution failed!`);
        }
    }

    async pollQueuedCodeJob(jobId: string): Promise<string> {
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
            throw new Error(`Polling queued code execution failed by job ID '${jobId}'`);
        }
    }

    async awaitQueuedCodeJobSucceeded(jobId: string): Promise<void> {
        console.log(`Waiting for job ${jobId} to succeed`);
        let state: string | null = null;
        while (state !== 'SUCCEEDED') {
            state = await this.pollQueuedCodeJob(jobId);
            if (state === 'SUCCEEDED') {
                console.log(`Job ${jobId} succeeded.`);
                break;
            } else {
                console.log(`Job ${jobId} is in state ${state}. Retrying in ${queuePollInterval} ms...`);
                await new Promise(resolve => setTimeout(resolve, queuePollInterval));
            }
        }
    }
}

export const tester = new Tester();
