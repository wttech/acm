export default `
/*
boolean canRun() {
    return contentor.instance.hasRunMode("author") && contentor.afterDate("2024-10-29 15:30:00") // <==> new Date().after(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2024-10-29 15:30:00"));
}
*/

//void doRun() {
    println "Migrating..."
    
    println "Updating ACLs..."
    acl.createUser { id = "john.doe" ; fullName = "John Doe"; password = "ilovekittens"; skipIfExists() }
    acl.save()

    println "Processing resources..."
    for (int i = 0; i < 10; i++) {
        println "Migrating (\${i + 1}/20)"
        Thread.sleep(500)
    }
    
    println "Migration done"
//}
`.trim()
