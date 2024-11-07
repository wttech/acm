export default `
/*
boolean shouldMigrate() {
    return migrator.instance.hasRunMode("author") && migrator.afterDate("2024-10-29 15:30:00") // <==> new Date().after(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2024-10-29 15:30:00"));
}
*/

//void doMigrate() {
    println "Migrating..."

    def foo = resourceResolver.getResource("/content/foo")
    // migrate foo
    
    println "Migration done"
//}
`.trim()
