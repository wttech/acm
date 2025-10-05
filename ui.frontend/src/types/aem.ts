import { isProduction } from "../utils/node";

export enum NodeType {
  FOLDER = 'nt:folder',
  ORDERED_FOLDER = 'sling:OrderedFolder',
  SLING_FOLDER = 'sling:Folder',
  CQ_PROJECTS = 'cq/projects',
  REDIRECT = 'sling:redirect',
  ACL = 'rep:ACL',
  PAGE = 'cq:Page',
  FILE = 'nt:file'
}

export enum JcrConstants {
  JCR_CONTENT = 'jcr:content'
}
export enum InstanceRole {
  AUTHOR = 'AUTHOR',
  PUBLISH = 'PUBLISH'
}

export enum InstanceType {
  ON_PREM = 'ON_PREM',
  CLOUD_SDK = 'CLOUD_SDK',
  CLOUD_CONTAINER = 'CLOUD_CONTAINER'
}

export const instancePrefix = isProduction() ? '' : 'http://localhost:5502';

export enum InstanceOsgiServicePid {
  SCRIPT_SCHEDULER = 'dev.vml.es.acm.core.script.ScriptScheduler',

  CODE_EXECUTOR = 'dev.vml.es.acm.core.code.Executor',
  CODE_EXECUTION_QUEUE = 'dev.vml.es.acm.core.code.ExecutionQueue',
  SLING_QUEUE = 'org.apache.sling.event.jobs.QueueConfiguration~acmexecutionqueue',

  SPA_SETTINGS = 'dev.vml.es.acm.core.gui.SpaSettings',
  CODE_REPOSITORY = 'dev.vml.es.acm.core.code.CodeRepository',
  CODE_ASSISTANCER = 'dev.vml.es.acm.core.assist.Assistancer',
  MOCK_HTTP_FILTER = 'dev.vml.es.acm.core.mock.MockHttpFilter',
  INSTANCE_INFO = 'dev.vml.es.acm.core.osgi.InstanceInfo',

  NOTIFICATION_SLACK_FACTORY = 'dev.vml.es.acm.core.notification.slack.SlackFactory',
  NOTIFICATION_TEAMS_FACTORY = 'dev.vml.es.acm.core.notification.teams.TeamsFactory'
}

export function instanceOsgiServiceConfigUrl(pid: InstanceOsgiServicePid): string {
  return `${instancePrefix}/system/console/configMgr/${pid}`;
}
export const UserIdServicePrefix = 'acm-';

