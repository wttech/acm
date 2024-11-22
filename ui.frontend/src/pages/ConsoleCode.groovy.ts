export default `
/*
boolean shouldMigrate() {
    return contentor.instance.hasRunMode("author") && contentor.afterDate("2024-10-29 15:30:00") // <==> new Date().after(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2024-10-29 15:30:00"));
}
*/

//void doMigrate() {
    println "Migrating..."

    for (int i = 0; i < 20; i++) {
        println "Migrating (\${i + 1}/20)"
        Thread.sleep(500)
    }
    
    println "Migration done"
//}
`.trim()
