package com.deva.voice

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

/**
 * Unit tests for automated version management functionality.
 * 
 * Tests the version increment logic used in build.gradle.kts for release builds.
 */
class VersionManagementTest {
    
    private lateinit var testVersionFile: File
    private val testVersionFileName = "test_version.properties"
    
    @Before
    fun setUp() {
        testVersionFile = File(testVersionFileName)
        // Create a test version file
        createTestVersionFile(13, "1.0.13")
    }
    
    @After
    fun tearDown() {
        if (testVersionFile.exists()) {
            testVersionFile.delete()
        }
    }
    
    private fun createTestVersionFile(versionCode: Int, versionName: String) {
        val output = FileOutputStream(testVersionFile)
        output.use { fileOutput ->
            fileOutput.write("# Test version configuration\n".toByteArray())
            fileOutput.write("VERSION_CODE=$versionCode\n".toByteArray())
            fileOutput.write("VERSION_NAME=$versionName".toByteArray())
        }
    }
    
    private fun readVersionFromFile(): Pair<Int, String> {
        val props = Properties()
        props.load(FileInputStream(testVersionFile))
        return Pair(
            props.getProperty("VERSION_CODE").toInt(),
            props.getProperty("VERSION_NAME")
        )
    }
    
    @Test
    fun testVersionLoading() {
        val (versionCode, versionName) = readVersionFromFile()
        assertEquals("Version code should be loaded correctly", 13, versionCode)
        assertEquals("Version name should be loaded correctly", "1.0.13", versionName)
    }
    
    @Test
    fun testVersionCodeIncrement() {
        val props = Properties()
        props.load(FileInputStream(testVersionFile))
        
        val currentVersionCode = props.getProperty("VERSION_CODE").toInt()
        val newVersionCode = currentVersionCode + 1
        
        assertEquals("Version code should increment by 1", 14, newVersionCode)
    }
    
    @Test
    fun testVersionNameIncrement() {
        val props = Properties()
        props.load(FileInputStream(testVersionFile))
        
        val currentVersionName = props.getProperty("VERSION_NAME")
        val versionParts = currentVersionName.split(".")
        val newPatchVersion = if (versionParts.size >= 3) {
            versionParts[2].toInt() + 1
        } else {
            1
        }
        val newVersionName = if (versionParts.size >= 2) {
            "${versionParts[0]}.${versionParts[1]}.$newPatchVersion"
        } else {
            "1.0.$newPatchVersion"
        }
        
        assertEquals("Version name should increment patch version", "1.0.14", newVersionName)
    }
    
    @Test
    fun testCompleteVersionIncrement() {
        // Simulate the complete increment logic from build.gradle.kts
        val props = Properties()
        props.load(FileInputStream(testVersionFile))
        
        val currentVersionCode = props.getProperty("VERSION_CODE").toInt()
        val currentVersionName = props.getProperty("VERSION_NAME")
        
        // Increment version code
        val newVersionCode = currentVersionCode + 1
        
        // Increment patch version in semantic versioning (x.y.z -> x.y.z+1)
        val versionParts = currentVersionName.split(".")
        val newPatchVersion = if (versionParts.size >= 3) {
            versionParts[2].toInt() + 1
        } else {
            1
        }
        val newVersionName = if (versionParts.size >= 2) {
            "${versionParts[0]}.${versionParts[1]}.$newPatchVersion"
        } else {
            "1.0.$newPatchVersion"
        }
        
        // Update properties
        props.setProperty("VERSION_CODE", newVersionCode.toString())
        props.setProperty("VERSION_NAME", newVersionName)
        
        // Save back to file
        val output = FileOutputStream(testVersionFile)
        output.use { fileOutput ->
            fileOutput.write("# Test version configuration\n".toByteArray())
            fileOutput.write("VERSION_CODE=$newVersionCode\n".toByteArray())
            fileOutput.write("VERSION_NAME=$newVersionName".toByteArray())
        }
        
        // Verify the increment worked
        val (finalVersionCode, finalVersionName) = readVersionFromFile()
        assertEquals("Final version code should be incremented", 14, finalVersionCode)
        assertEquals("Final version name should be incremented", "1.0.14", finalVersionName)
    }
    
    @Test
    fun testMultipleIncrements() {
        var expectedCode = 13
        var expectedName = "1.0.13"
        
        // Test 3 sequential increments
        for (i in 1..3) {
            val props = Properties()
            props.load(FileInputStream(testVersionFile))
            
            val currentVersionCode = props.getProperty("VERSION_CODE").toInt()
            val currentVersionName = props.getProperty("VERSION_NAME")
            
            assertEquals("Current version code should match expected", expectedCode, currentVersionCode)
            assertEquals("Current version name should match expected", expectedName, currentVersionName)
            
            // Increment
            val newVersionCode = currentVersionCode + 1
            val versionParts = currentVersionName.split(".")
            val newPatchVersion = versionParts[2].toInt() + 1
            val newVersionName = "${versionParts[0]}.${versionParts[1]}.$newPatchVersion"
            
            // Update file
            val output = FileOutputStream(testVersionFile)
            output.use { fileOutput ->
                fileOutput.write("# Test version configuration\n".toByteArray())
                fileOutput.write("VERSION_CODE=$newVersionCode\n".toByteArray())
                fileOutput.write("VERSION_NAME=$newVersionName".toByteArray())
            }
            
            // Update expected values for next iteration
            expectedCode = newVersionCode
            expectedName = newVersionName
        }
        
        // Final verification
        val (finalVersionCode, finalVersionName) = readVersionFromFile()
        assertEquals("Final version code after 3 increments", 16, finalVersionCode)
        assertEquals("Final version name after 3 increments", "1.0.16", finalVersionName)
    }
    
    @Test
    fun testVersionParsingEdgeCases() {
        // Test with different version name formats
        createTestVersionFile(1, "2.5.0")
        val props = Properties()
        props.load(FileInputStream(testVersionFile))
        
        val versionName = props.getProperty("VERSION_NAME")
        val versionParts = versionName.split(".")
        val newPatchVersion = versionParts[2].toInt() + 1
        val newVersionName = "${versionParts[0]}.${versionParts[1]}.$newPatchVersion"
        
        assertEquals("Should handle different major.minor versions", "2.5.1", newVersionName)
    }
    
    @Test
    fun testVersionFileWithDefaults() {
        // Test fallback behavior when properties are missing
        val tempFile = File("temp_empty.properties")
        FileOutputStream(tempFile).use { it.write("".toByteArray()) }
        
        try {
            val props = Properties()
            props.load(FileInputStream(tempFile))
            
            val versionCode = props.getProperty("VERSION_CODE", "13").toInt()
            val versionName = props.getProperty("VERSION_NAME", "1.0.13")
            
            assertEquals("Should use default version code", 13, versionCode)
            assertEquals("Should use default version name", "1.0.13", versionName)
        } finally {
            tempFile.delete()
        }
    }
}
