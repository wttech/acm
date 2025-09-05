package dev.vml.es.acm.core.util;

/**
 * Simple validation class to test StringUtil.abbreviateStart method
 */
public class StringUtilValidation {
    
    public static void main(String[] args) {
        System.out.println("=== StringUtil.abbreviateStart Validation ===\n");
        
        // Test 1: Basic functionality with default prefix
        test("Basic with default prefix", 
             StringUtil.abbreviateStart("Hello World", 10), 
             "...o World");
        
        // Test 2: No truncation needed
        test("No truncation needed", 
             StringUtil.abbreviateStart("Hello", 10), 
             "Hello");
        
        // Test 3: Custom prefix
        test("Custom prefix", 
             StringUtil.abbreviateStart("Hello World", 10, "[...]"), 
             "[...]World");
        
        // Test 4: MaxLength smaller than prefix
        test("MaxLength smaller than prefix", 
             StringUtil.abbreviateStart("Hello World", 2, "..."), 
             "..");
        
        // Test 5: MaxLength equal to prefix
        test("MaxLength equal to prefix", 
             StringUtil.abbreviateStart("Hello World", 3, "..."), 
             "...");
        
        // Test 6: Real-world scenario - execution output
        String executionOutput = "Starting process...\nProcessing files...\nCompleted successfully with 150 files processed.";
        test("Real execution output", 
             StringUtil.abbreviateStart(executionOutput, 50), 
             "Expected to start with '...' and end with 'files processed.'");
        
        // Test 7: Null handling
        test("Null input", 
             StringUtil.abbreviateStart(null, 10), 
             null);
        
        // Test 8: Negative maxLength
        test("Negative maxLength", 
             StringUtil.abbreviateStart("Hello", -1), 
             "Hello");
        
        System.out.println("\n=== Validation Complete ===");
    }
    
    private static void test(String testName, String actual, String expected) {
        System.out.println("Test: " + testName);
        System.out.println("Expected: " + (expected == null ? "null" : "\"" + expected + "\""));
        System.out.println("Actual:   " + (actual == null ? "null" : "\"" + actual + "\""));
        
        boolean passed;
        if (testName.contains("Real execution output")) {
            // Special validation for real-world scenario
            passed = actual != null && actual.startsWith("...") && actual.contains("files processed.") && actual.length() == 50;
        } else {
            passed = (expected == null && actual == null) || (expected != null && expected.equals(actual));
        }
        
        System.out.println("Result:   " + (passed ? "✓ PASS" : "✗ FAIL"));
        System.out.println();
    }
}
