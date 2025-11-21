/**
  * This script creates content author groups for each tenant-country-language combination.
  * 
  * The groups are named in the format: `{tenant}-{country}-{language}-content-authors`.
  * Each group is granted read, write, and replicate permissions on the corresponding content and DAM paths.
  */

def scheduleRun() {
  return schedules.cron("0 10 * ? * * *") // every hour at minute 10
}

boolean canRun() {
  return conditions.always()
} 

void doRun() {
  out.info "ACL setup started"
  
  def tenantPaths = ["/content/acme", "/content/wknd", "/content/we-retail"]
  def groupsCreated = 0
  
  for (def tenantRoot : tenantPaths.collect { repo.get(it) }.findAll { it.exists() }) {
    def tenant = tenantRoot.name
    for (def countryRoot : tenantRoot.children().findAll { isRoot(it) }) {
      def country = countryRoot.name
      for (def languageRoot : countryRoot.children().findAll { isRoot(it) }) {
        def language = languageRoot.name
        def prefix = "${tenant}-${country}-${language}"
        
        acl.createGroup { id = "${prefix}-content-authors" }.tap {
          allow { path = "/content/${tenant}/${country}/${language}"; permissions = ["jcr:read", "rep:write", "crx:replicate"] }
          allow { path = "/content/dam/${tenant}/${country}/${language}"; permissions = ["jcr:read", "rep:write", "crx:replicate"] }
        }
        groupsCreated++
      }
    }
  }
  
  out.success "ACL setup completed. Processed ${groupsCreated} content author group(s)."
}

def isRoot(root) {
  return root.type() == "cq:Page" && root.name != "language-masters"
}
