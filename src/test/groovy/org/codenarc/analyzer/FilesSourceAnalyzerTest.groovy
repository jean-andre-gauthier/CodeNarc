/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codenarc.analyzer

import static org.codenarc.test.TestUtil.assertEqualSets
import static org.codenarc.test.TestUtil.shouldFailWithMessageContaining

import org.codenarc.results.DirectoryResults
import org.codenarc.results.FileResults
import org.codenarc.results.Results
import org.codenarc.rule.FakeCountRule
import org.codenarc.rule.FakePathRule
import org.codenarc.ruleset.ListRuleSet
import org.codenarc.test.AbstractTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for FilesSourceAnalyzer.
 *
 * @author Nicolas Vuillamy
 */
class FilesSourceAnalyzerTest extends AbstractTestCase {

    private static final BASE_DIR = 'src/test/resources/sourcewithdirs'

    private analyzer = new FilesSourceAnalyzer()
    private testCountRule = new FakeCountRule()
    private ruleSet = new ListRuleSet([new FakePathRule(), testCountRule])

    @Test
    void test_analyze_NullRuleSet() {
        analyzer.baseDirectory = BASE_DIR
        analyzer.sourceFiles = 'SourceFile1.groovy'
        shouldFailWithMessageContaining('ruleSet') { analyzer.analyze(null) }
    }

    @Test
    void test_analyze_OneFile() {
        analyzer.baseDirectory = BASE_DIR
        analyzer.sourceFiles = ['SourceFile1.groovy']
        ruleSet = new ListRuleSet([new FakePathRule()])     // override
        def results = analyzer.analyze(ruleSet)
        log("results=$results")

        def paths = resultsPaths(results)
        log("paths=$paths")
        assertEqualSets(paths, ['SourceFile1.groovy'])

        def fullPaths = results.violations*.message
        assertEqualSets(fullPaths, [
                'src/test/resources/sourcewithdirs/SourceFile1.groovy'
        ])
        assert results.getNumberOfFilesWithViolations(3) == 1
        assert results.totalNumberOfFiles == 1
    }

    @Test
    void test_analyze_MultipleFiles() {
        analyzer.baseDirectory = BASE_DIR
        analyzer.sourceFiles = [
                'SourceFile1.groovy',
                'subdir1/Subdir1File1.groovy',
                'subdir1/Subdir1File2.groovy'
                ]
        def results = analyzer.analyze(ruleSet)
        log("results=$results")

        def fullPaths = results.violations*.message
        assertEqualSets(fullPaths, [
                'src/test/resources/sourcewithdirs/SourceFile1.groovy',
                'src/test/resources/sourcewithdirs/subdir1/Subdir1File1.groovy',
                'src/test/resources/sourcewithdirs/subdir1/Subdir1File2.groovy'
        ])
        assert testCountRule.count == 3
        assert results.getNumberOfFilesWithViolations(3) == 3
        assert results.totalNumberOfFiles == 3

        // Verify that the directory structure is properly reflected within the results
        assert childResultsClasses(results) == [DirectoryResults]
        def top = results.children[0]
        assertEqualSets(childResultsClasses(top), [FileResults, DirectoryResults, DirectoryResults])
    }

    @Test
    void test_analyze_MultipleFilesNestedDirs() {
        analyzer.baseDirectory = BASE_DIR
        analyzer.sourceFiles = [
                'SourceFile1.groovy',
                'subdir1/Subdir1File1.groovy',
                'subdir1/Subdir1File2.groovy',
                'subdir2/subdir2a/Subdir2aFile1.groovy',
                'subdir2/Subdir2File1.groovy'
        ]
        def results = analyzer.analyze(ruleSet)
        log("results=$results")

        def fullPaths = results.violations*.message
        assertEqualSets(fullPaths, [
                'src/test/resources/sourcewithdirs/SourceFile1.groovy',
                'src/test/resources/sourcewithdirs/subdir1/Subdir1File1.groovy',
                'src/test/resources/sourcewithdirs/subdir1/Subdir1File2.groovy',
                'src/test/resources/sourcewithdirs/subdir2/subdir2a/Subdir2aFile1.groovy',
                'src/test/resources/sourcewithdirs/subdir2/Subdir2File1.groovy'
        ])
        assert testCountRule.count == 5
        assert results.getNumberOfFilesWithViolations(3) == 5
        assert results.totalNumberOfFiles == 5

        // Verify that the directory structure is properly reflected within the results
        assert childResultsClasses(results) == [DirectoryResults]
        def top = results.children[0]
        assertEqualSets(childResultsClasses(top), [FileResults, DirectoryResults, DirectoryResults])
    }

    @Test
    void test_analyze_NoViolations() {
        analyzer.baseDirectory = BASE_DIR
        analyzer.sourceFiles = [
                'SourceFile1.groovy',
                'subdir1/Subdir1File1.groovy',
                'subdir1/Subdir1File2.groovy',
                'subdir2/subdir2a/Subdir2aFile1.groovy',
                'subdir2/Subdir2File1.groovy'
        ]
        ruleSet = new ListRuleSet([testCountRule])
        def results = analyzer.analyze(ruleSet)
        log("results=$results")

        def paths = resultsPaths(results)
        assertEqualSets(paths, ['SourceFile1.groovy', 'subdir1', 'subdir1/Subdir1File2.groovy', 'subdir1/Subdir1File1.groovy', 'subdir2', 'subdir2/subdir2a', 'subdir2/subdir2a/Subdir2aFile1.groovy', 'subdir2/Subdir2File1.groovy'])

        assert testCountRule.count == 5
        assert results.getNumberOfFilesWithViolations(3) == 0
        assert results.totalNumberOfFiles == 5
    }

    private List resultsPaths(Results results, List paths=[]) {
        if (results.path) {
            paths << results.path
        }
        if (results instanceof FileResults) {
            assert results.sourceCode
        }
        results.children.each { child -> resultsPaths(child, paths) }
        log("resultsPaths=$paths")
        paths
    }

    private List childResultsClasses(Results results) {
        results.children*.getClass()
    }
}
