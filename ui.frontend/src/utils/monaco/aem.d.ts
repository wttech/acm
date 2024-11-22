declare namespace org.slf4j {
  interface Logger {
    info(message: string): void;
    error(message: string): void;
  }

  interface LoggerFactory {
    getLogger(name: string): Logger;
  }
}

declare namespace com.day.cq.wcm.api {
  interface PageManager {
    getPage(path: string): Page;
  }

  interface Page {
    getTitle(): string;
  }
}

declare namespace org.apache.sling.api.resource {
  interface ResourceResolver {
    resolve(path: string): Resource;
  }

  interface Resource {
    getPath(): string;
  }
}

declare namespace com.day.cq.tagging {
  interface Tag {
    getTitle(): string;
  }
}

declare namespace com.day.cq.dam.api {
  interface Asset {
    getOriginal(): org.apache.sling.api.resource.Resource;
  }
}
